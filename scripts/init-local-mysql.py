"""One-time local MySQL bootstrap. Python 3.10+ and mysql client required."""
import argparse
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from urllib.parse import urlsplit, parse_qs

ROOT = Path(__file__).resolve().parents[1]
VERSION = '2026-11-02-v1'


def parse_target(url):
    if not url.startswith('jdbc:mysql://'):
        raise ValueError('Only jdbc:mysql:// URLs are allowed.')
    parsed = urlsplit(url[5:])
    if parsed.hostname not in ('localhost', '127.0.0.1', '::1') or parsed.username or parsed.password:
        raise ValueError('Only loopback MySQL hosts without URL credentials are allowed.')
    database = parsed.path.removeprefix('/')
    if not re.fullmatch(r'[A-Za-z0-9_]+', database) or database.lower() not in ('event_platform', 'event_platform_dev', 'event_platform_test'):
        raise ValueError('Database must be event_platform, event_platform_dev, or event_platform_test.')
    allowed = {'connectionTimeZone', 'characterEncoding', 'serverTimezone', 'useSSL', 'allowPublicKeyRetrieval', 'forceConnectionTimeZoneToSession'}
    if parsed.fragment or set(parse_qs(parsed.query, keep_blank_values=True)) - allowed:
        raise ValueError('Unsupported JDBC URL options. Refusing ambiguous connection settings.')
    return parsed.hostname, parsed.port or 3306, database


def read_env(path):
    values = {}
    for line in path.read_text(encoding='utf-8-sig').splitlines():
        match = re.match(r'^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$', line)
        if not match:
            continue
        key, value = match.groups()
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in ('"', "'"):
            value = value[1:-1]
        values[key] = value
    return values


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--verify-only', action='store_true', help='Read-only seed and schema checks')
    args = parser.parse_args()
    # Deliberately never read SUPABASE_* or SPRING_PROFILES_ACTIVE.
    config = read_env(ROOT / '.env')
    host, port, database = parse_target(config.get('MYSQL_DB_URL', 'jdbc:mysql://localhost:3306/event_platform'))
    username = config.get('MYSQL_DB_USERNAME', 'root')
    password = config.get('MYSQL_DB_PASSWORD')
    if password is None or password in ('YOUR-PASSWORD', 'replace-me'):
        raise ValueError('Set MYSQL_DB_PASSWORD in .env first.')
    client = shutil.which('mysql')
    if not client:
        candidates = sorted(Path('C:/Program Files/MySQL').glob('MySQL Server */bin/mysql.exe'))
        client = str(candidates[-1]) if candidates else None
    if not client:
        raise ValueError('mysql client not found. Install MySQL 8.0+ and add mysql to PATH.')
    environment = os.environ.copy()
    environment['MYSQL_PWD'] = password
    disabled_login_file = ROOT / '.gradle' / 'local-bootstrap-no-login.cnf'
    if disabled_login_file.exists():
        raise ValueError('Unexpected bootstrap login file exists. Refusing saved credentials.')
    environment['MYSQL_TEST_LOGIN_FILE'] = str(disabled_login_file)
    # Do not inherit a client's remote defaults, login paths, or a parent MYSQL_HOST.
    command = [client, '--no-defaults', '--protocol=TCP',
               f'--host={host}', f'--port={port}', f'--user={username}',
               '--default-character-set=utf8mb4', '--connect-timeout=10',
               '--batch', '--raw', '--skip-column-names']

    def execute(sql, select_database=True):
        process = subprocess.run(command + ([database] if select_database else []),
                                 input='SET SESSION time_zone = \'+00:00\';\n' + sql,
                                 text=True, encoding='utf-8', capture_output=True, env=environment)
        if process.returncode:
            raise RuntimeError(process.stderr.replace(password, '[REDACTED]') if password else process.stderr)
        return process.stdout.strip()

    print(f'Target: local MySQL {host}:{port}/{database}')
    server = execute('SELECT VERSION();', False)
    if 'MariaDB' in server or int(server.split('.')[0]) < 8:
        raise ValueError('MySQL 8.0+ is required.')
    sql_root = ROOT / 'src/main/resources/db/local-mysql'
    schema = (sql_root / '01-schema.sql').read_text(encoding='utf-8')
    seed = (sql_root / '02-seed.sql').read_text(encoding='utf-8')
    fingerprint = hashlib.sha256((schema + '\n' + seed).encode()).hexdigest()
    exists = execute(f"SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='{database}';", False) == '1'
    if not exists:
        if args.verify_only:
            raise ValueError('Local database does not exist.')
        execute(f'CREATE DATABASE `{database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;', False)
    tables = execute('SHOW TABLES;').splitlines()
    marker = '_local_dev_bootstrap'
    if tables and marker not in tables:
        raise ValueError('Database already contains tables without a bootstrap marker. Refusing to overwrite. Use a new event_platform_dev database.')
    state = ''
    if marker in tables:
        state = execute(f"SELECT CONCAT(version, '|', fingerprint, '|', status) FROM {marker} WHERE id=1;")
        if not state:
            raise ValueError('Bootstrap metadata is incomplete. Inspect the local database before retrying.')
        if not state.startswith(f'{VERSION}|{fingerprint}|'):
            raise ValueError('Bootstrap version or SQL fingerprint changed. Use a new local database or a reviewed migration.')
    if state.endswith('|COMPLETE'):
        print('Already initialized. Schema and seed skipped.')
    elif args.verify_only:
        raise ValueError('Bootstrap has not completed.')
    else:
        # MySQL DDL auto-commits. A durable marker allows a failed schema import to
        # resume; all seed rows and the COMPLETE marker commit in one transaction.
        # A server lock and guarded status recheck prevent simultaneous seeding.
        bootstrap = f"""
SET @bootstrap_lock = GET_LOCK(CONCAT(DATABASE(), ':local-bootstrap'), 30);
CREATE TEMPORARY TABLE bootstrap_guard (ok INT NOT NULL CHECK (ok = 1));
INSERT INTO bootstrap_guard VALUES (@bootstrap_lock);
CREATE TABLE IF NOT EXISTS {marker} (
 id INT NOT NULL PRIMARY KEY, version VARCHAR(40) NOT NULL,
 fingerprint CHAR(64) NOT NULL, status VARCHAR(20) NOT NULL,
 completed_at DATETIME(6) NULL
) ENGINE=InnoDB;
INSERT INTO {marker} (id, version, fingerprint, status)
SELECT 1, '{VERSION}', '{fingerprint}', 'PENDING'
WHERE NOT EXISTS (SELECT 1 FROM {marker} WHERE id=1);
INSERT INTO bootstrap_guard
SELECT IF(version='{VERSION}' AND fingerprint='{fingerprint}' AND status='PENDING', 1, 0)
FROM {marker} WHERE id=1;
{schema}
START TRANSACTION;
{seed}
UPDATE {marker} SET status='COMPLETE', completed_at=UTC_TIMESTAMP(6) WHERE id=1;
COMMIT;
DO RELEASE_LOCK(CONCAT(DATABASE(), ':local-bootstrap'));
"""
        execute(bootstrap)
        print('Schema and seed committed successfully.')
    verification = execute((sql_root / '03-verify.sql').read_text(encoding='utf-8'))
    print(verification)
    failed = [line for line in verification.splitlines() if line.startswith('FAIL\t')]
    if failed:
        raise RuntimeError('Verification failed. See FAIL rows above.')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, RuntimeError, OSError) as error:
        print(f'Initialization stopped: {error}', file=sys.stderr)
        sys.exit(1)
