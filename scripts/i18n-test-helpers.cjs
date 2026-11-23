// Unit-test adapter reads the same MessageSource properties, never a separate translation dictionary.
const fs = require('node:fs');
function properties(file) {
    const values = {};
    for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
        if (!line || /^\s*[#!]/.test(line)) continue;
        const index = line.indexOf('=');
        if (index < 0) continue;
        const decode = value => value.replace(/\\u([0-9a-f]{4})|\\(.)/gi, (_, hex, character) => hex
            ? String.fromCharCode(parseInt(hex, 16)) : ({n:'\n',r:'\r',t:'\t',f:'\f'}[character] ?? character));
        values[decode(line.slice(0,index).trim())] = decode(line.slice(index+1));
    }
    return values;
}
const messages = properties('src/main/resources/messages.properties');
const BusanI18n = {
    locale:'ko',
    t:(key,args=[]) => (messages[key] ?? key).replace(/\{(\d+)\}/g,(match,index)=>index<args.length?String(args[index]):match),
    source:value=>value,
    translateSource:value=>value
};
module.exports = {BusanI18n, properties};
