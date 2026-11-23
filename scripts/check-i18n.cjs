// Static UI translation gate. Uses Node built-ins; optional Babel improves literal classification.
// Run from the repository root: node scripts/check-i18n.cjs
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '..');
const resources = path.join(root, 'src/main/resources');
const failures = [];
const remaining = [];
const counts = {catalogs: 4, keys: 0, mustacheReferences: 0, javascriptReferences: 0, javascriptSyntaxChecks: 0};
const hangul = /[\uac00-\ud7a3]/;
let babel;
try { babel = require(path.join(root, 'build/i18n1b/tools/node_modules/@babel/parser')); } catch (_) { /* Optional tooling only. */ }

function files(directory, suffix) {
    return fs.readdirSync(directory, {withFileTypes: true}).flatMap(entry => {
        const file = path.join(directory, entry.name);
        return entry.isDirectory() ? files(file, suffix) : file.endsWith(suffix) ? [file] : [];
    });
}
function where(file, source, offset) {
    return path.relative(root, file).replaceAll('\\', '/') + ':' + (source.slice(0, offset).match(/\n/g) || []).length.toString().replace(/^/, '');
}
function location(file, source, offset) {
    return path.relative(root, file).replaceAll('\\', '/') + ':' + (1 + (source.slice(0, offset).match(/\n/g) || []).length);
}
function decode(value) {
    return value.replace(/\\u([\da-fA-F]{4})|\\(.)/g, (_, unicode, escaped) => unicode ? String.fromCharCode(parseInt(unicode, 16)) : ({n:'\n', r:'\r', t:'\t', f:'\f'}[escaped] ?? escaped));
}
function properties(file) {
    const result = new Map();
    const physical = fs.readFileSync(file, 'utf8').replace(/^\ufeff/, '').split(/\r?\n/);
    for (let i = 0; i < physical.length; i++) {
        const lineNumber = i + 1;
        let line = physical[i];
        while ((line.match(/\\+$/)?.[0].length || 0) % 2 === 1 && i + 1 < physical.length) line = line.slice(0, -1) + physical[++i].trimStart();
        line = line.trimStart();
        if (!line || /^[#!]/.test(line)) continue;
        const match = /^((?:\\.|[^\s=:])+)(?:\s*[:=]\s*|\s+)?(.*)$/.exec(line);
        if (!match) { failures.push(`${path.basename(file)}:${lineNumber}: invalid property`); continue; }
        const key = decode(match[1]), value = decode(match[2]);
        if (result.has(key)) failures.push(`${path.basename(file)}:${lineNumber}: duplicate key ${key}`);
        result.set(key, value);
    }
    return result;
}
const catalogs = new Map(['ko', 'en', 'ja', 'zh_CN'].map(code => [code, properties(path.join(resources, code === 'ko' ? 'messages.properties' : `messages_${code}.properties`))]));
const source = catalogs.get('ko');
counts.keys = source.size;
const placeholders = text => [...text.matchAll(/\{\d+\}/g)].map(match => match[0]).sort();
for (const [code, catalog] of catalogs) {
    for (const key of source.keys()) if (!catalog.has(key)) failures.push(`${code}: missing ${key}`);
    for (const [key, value] of catalog) {
        if (!source.has(key)) failures.push(`${code}: extra ${key}`);
        if (!value.trim() && !(key === 'common.ui0043' && ['en', 'zh_CN'].includes(code))) failures.push(`${code}: empty ${key}`);
        if (source.has(key) && JSON.stringify(placeholders(value)) !== JSON.stringify(placeholders(source.get(key)))) failures.push(`${code}: placeholder count/set mismatch ${key}`);
    }
}
function reference(key, file, text, offset, kind) {
    if (kind === 'mustache') { counts.mustacheReferences++; if (!key.includes('.')) key = 'common.' + key; }
    else counts.javascriptReferences++;
    if (!source.has(key)) failures.push(`${location(file, text, offset)}: missing ${kind} key ${key}`);
}
function classify(file, text, offset, classification, value) {
    if (hangul.test(value)) remaining.push({location: location(file, text, offset), classification, text: value.slice(0, 240)});
}
function javascript(code, file, fullText = code, offset = 0) {
    const prepared = code.replace(/\{\{\{[^]*?\}\}\}/g, 'null').replace(/\{\{[#/^!][^]*?\}\}/g, '').replace(/\{\{[^]*?\}\}/g, 'null');
    try { new vm.Script(prepared, {filename: path.relative(root, file)}); counts.javascriptSyntaxChecks++; }
    catch (error) { failures.push(`${location(file, fullText, offset)}: JavaScript syntax: ${error.message}`); return; }
    for (const match of code.matchAll(/\bBusanI18n\.t\(\s*(['"])([^'"\r\n]+)\1/g)) reference(match[2], file, fullText, offset + match.index, 'javascript');
    if (babel) {
        let ast;
        try { ast = babel.parse(prepared, {sourceType: 'unambiguous'}); } catch (_) { return; }
        for (const comment of ast.comments || []) classify(file, fullText, offset + comment.start, 'comment', comment.value);
        function visit(node, parent) {
            if (!node || typeof node !== 'object') return;
            if (node.type === 'RegExpLiteral') classify(file, fullText, offset + node.start, 'regex', node.pattern);
            if (node.type === 'StringLiteral' || node.type === 'TemplateElement') {
                const value = node.type === 'StringLiteral' ? node.value : node.value.raw;
                const called = parent?.type === 'CallExpression' ? (parent.callee.property?.name || parent.callee.name) : '';
                const intentional = parent?.type === 'BinaryExpression' || ['includes', 'startsWith', 'endsWith', 'match', 'replace', 'replaceAll', 'test', 'querySelector', 'querySelectorAll', 'matches', 'closest', 'getElementById', 'getElementsByClassName'].includes(called);
                const category = intentional ? 'predicate-or-selector' : 'javascript-visible-literal';
                classify(file, fullText, offset + node.start, category, value);
            }
            for (const [key, child] of Object.entries(node)) {
                if (['loc', 'comments', 'leadingComments', 'trailingComments', 'innerComments', 'tokens'].includes(key)) continue;
                if (Array.isArray(child)) child.forEach(item => visit(item, node));
                else if (child && typeof child === 'object') visit(child, node);
            }
        }
        visit(ast, null);
    } else {
        // Conservative fallback reports all candidate lines for manual review, without claiming AST accuracy.
        code.split('\n').forEach((line, index) => classify(file, fullText, offset, /^\s*\/\//.test(line) ? 'comment' : 'javascript-review-without-parser', `${index + 1}: ${line}`));
    }
}
for (const file of files(path.join(resources, 'static/js'), '.js')) javascript(fs.readFileSync(file, 'utf8'), file);
for (const file of files(path.join(resources, 'templates'), '.mustache')) {
    const text = fs.readFileSync(file, 'utf8');
    for (const match of text.matchAll(/\{\{\{?\s*[#^/]?\s*i18n\.([\w.]+)\s*\}\}\}?/g)) reference(match[1], file, text, match.index, 'mustache');
    const excluded = [];
    for (const match of text.matchAll(/<!--[^]*?-->|\{\{![^]*?\}\}|<style\b[^>]*>[^]*?<\/style\s*>|<script\b([^>]*)>([^]*?)<\/script\s*>/gi)) {
        excluded.push([match.index, match.index + match[0].length]);
        if (/^<script/i.test(match[0]) && !/\bsrc\s*=|\btype\s*=\s*['"](?:application\/json|application\/ld\+json)/i.test(match[1])) javascript(match[2], file, text, match.index + match[0].indexOf('>') + 1);
        else if (/^<!--|^\{\{!/.test(match[0])) classify(file, text, match.index, 'comment', match[0]);
    }
    const markup = text.split('').map((character, index) => excluded.some(([start,end]) => index >= start && index < end) ? (character === '\n' ? '\n' : ' ') : character).join('');
    for (const match of markup.matchAll(/>([^<]+)</g)) {
        const visible = match[1].replace(/\{\{\{?[^]*?\}\}\}?/g, '').trim();
        classify(file, text, match.index, 'html-visible-text', visible);
    }
    for (const match of markup.matchAll(/\b(title|alt|placeholder|aria-label|aria-description)\s*=\s*(['"])([^]*?)\2/gi)) classify(file, text, match.index, 'html-visible-attribute', match[3].replace(/\{\{\{?[^]*?\}\}\}?/g, ''));
}
for (const item of remaining) if (['html-visible-text', 'html-visible-attribute', 'javascript-visible-literal'].includes(item.classification)) failures.push(`${item.location}: untranslated ${item.classification}: ${item.text}`);
const summary = {...counts, parser: babel ? 'Babel (optional build tooling)' : 'built-ins (manual JS literal review required)', remainingHangul: remaining.length, classifications: Object.fromEntries([...new Set(remaining.map(item => item.classification))].map(category => [category, remaining.filter(item => item.classification === category).length])), failures: failures.length};
const output = path.join(root, 'build/i18n1b');
fs.mkdirSync(output, {recursive: true});
fs.writeFileSync(path.join(output, 'validation-report.json'), JSON.stringify({summary, failures, remainingHangul: remaining}, null, 2));
console.log(JSON.stringify(summary, null, 2));
failures.slice(0, 40).forEach(failure => console.error(failure));
if (failures.length > 40) console.error(`… ${failures.length - 40} more; see build/i18n1b/validation-report.json`);
process.exitCode = failures.length ? 1 : 0;
