(() => {
    if (window.BusanI18n) return;
    const node = document.getElementById('busan-i18n-messages');
    const data = node ? JSON.parse(node.textContent) : {locale:'ko', messages:{}, originals:{}};
    const originalKeys = new Map(), reverse = new Map();
    Object.entries(data.originals).forEach(([key, value]) => {
        if (!originalKeys.has(value)) originalKeys.set(value, key);
        reverse.set(data.messages[key], value);
    });
    const format = (value, args = []) => value.replace(/\{(\d+)\}/g, (match, index) => index < args.length ? String(args[index]) : match);
    const t = (key, args = []) => format(data.messages[key] ?? data.originals[key] ?? key, args);
    const patterns = [];
    Object.entries(data.originals).forEach(([key, value]) => {
        if (!/\{\d+\}/.test(value)) return;
        const indices = [];
        let expression = '^', start = 0;
        for (const match of value.matchAll(/\{(\d+)\}/g)) {
            expression += value.slice(start, match.index).replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '([\\s\\S]*?)';
            indices.push(Number(match[1])); start = match.index + match[0].length;
        }
        expression += value.slice(start).replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$';
        patterns.push({key, indices, pattern:new RegExp(expression)});
    });
    // Explicit static system notices only. CHAT/NOTICE bodies and arbitrary DB text must not use this function.
    function translateSource(value) {
        if (originalKeys.has(value)) return t(originalKeys.get(value));
        for (const entry of patterns) {
            const match = entry.pattern.exec(value);
            if (!match) continue;
            const args = []; entry.indices.forEach((index, group) => { args[index] = match[group + 1]; });
            return t(entry.key, args);
        }
        return value;
    }
    window.BusanI18n = Object.freeze({t, translateSource, source:value => reverse.get(value) ?? value, locale:data.locale});
    document.documentElement.lang = data.locale;
})();
