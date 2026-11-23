const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const {properties} = require('./i18n-test-helpers.cjs');
const originals = properties('src/main/resources/messages.properties');
for (const [locale,suffix] of [['ko',''],['en','_en'],['ja','_ja'],['zh-CN','_zh_CN']]) {
    const messages = properties('src/main/resources/messages'+suffix+'.properties');
    const document = {documentElement:{},getElementById:()=>({textContent:JSON.stringify({locale,messages,originals})})};
    const window = {};
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/i18n.js','utf8'),{window,document});
    const api=window.BusanI18n;
    assert.equal(api.locale,locale);assert.equal(document.documentElement.lang,locale);
    assert.equal(api.t('common.search'),messages['common.search']);
    const refund=api.t('server.history.cancelRefund',['10000P','5000P']);
    assert.ok(refund.includes('10000P')&&refund.includes('5000P')&&!/\{\d\}/.test(refund));
    assert.equal(api.translateSource('Sample user님이 입장했습니다.'),api.t('server.display.communityJoined',['Sample user']));
    assert.equal(api.translateSource('한글 회사 (주최)'),api.t('server.display.hostName',['한글 회사']));
    const content='사용자가 작성한 원본 행사 제목 <img onerror="alert(1)">';
    assert.equal(api.translateSource(content),content,'unknown user-authored text is unchanged');
    assert.equal(api.source(messages['common.search']),originals['common.search']);
    assert.equal(api.t('common.ui0043'),messages['common.ui0043'],'intentional empty honorific does not fall back to Korean');
}
console.log('PASS runtime message formatting, source/status mapping, multilingual shared system notices and original user content');
