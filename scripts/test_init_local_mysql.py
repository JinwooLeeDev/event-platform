import importlib.util
from pathlib import Path
import unittest
import tempfile

SPEC = importlib.util.spec_from_file_location('initializer', Path(__file__).with_name('init-local-mysql.py'))
initializer = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(initializer)


class TargetSafetyTest(unittest.TestCase):
    def test_accepts_only_loopback_mysql(self):
        for host in ('localhost', '127.0.0.1', '[::1]'):
            self.assertEqual(initializer.parse_target(f'jdbc:mysql://{host}:3306/event_platform')[2], 'event_platform')

    def test_rejects_remote_and_non_mysql_targets(self):
        for url in ('jdbc:postgresql://localhost/postgres',
                    'jdbc:mysql://aws-0-eu-west-1.pooler.supabase.com/postgres',
                    'jdbc:mysql://example.com/dev', 'jdbc:mysql://localhost/mysql',
                    'jdbc:mysql://localhost/a-b', 'jdbc:mysql://localhost:3306/production',
                    'jdbc:mysql://localhost:3306/event_platform?socketFactory=example.Factory'):
            with self.subTest(url=url), self.assertRaises(ValueError):
                initializer.parse_target(url)

    def test_reads_passwords_as_literals_without_expanding_them(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / '.env'
            path.write_text('# comment\nMYSQL_DB_PASSWORD="a#b$c!="\nMYSQL_DB_USERNAME=local\n', encoding='utf-8')
            values = initializer.read_env(path)
            self.assertEqual(values['MYSQL_DB_PASSWORD'], 'a#b$c!=')
            self.assertEqual(values['MYSQL_DB_USERNAME'], 'local')


if __name__ == '__main__':
    unittest.main()
