const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const template = fs.readFileSync('src/main/resources/templates/event/detail.mustache', 'utf8');
assert.match(template, /<select[^>]*name="sessionId"/);
assert.match(template, /class="[^"]*reg-apply-btn/);
assert.match(template, /\{\{> registration\/apply-button-script\}\}/);
const source = fs.readFileSync('src/main/resources/templates/registration/apply-button-script.mustache', 'utf8')
    .replace(/^<script>\s*/, '').replace(/\s*<\/script>\s*$/, '');

async function scenario({ picked = true, status = 200, check = {}, confirm = true, networkError = false } = {}) {
    let handler;
    const calls = [], alerts = [], confirmations = [];
    const button = { dataset: {}, disabled: false };
    const location = { href: '' };
    const context = {
        document: {
            addEventListener: (_, callback) => { handler = callback; },
            querySelector: selector => selector.includes('select[name="sessionId"]') && picked
                ? { value: '42', dataset: { sessionName: '저녁 회차' } } : null
        },
        location,
        alert: message => alerts.push(message),
        confirm: message => { confirmations.push(message); return confirm; },
        fetch: async url => {
            calls.push(url);
            assert.equal(button.disabled, true);
            if (networkError) throw new Error('offline');
            return { status, ok: status === 200, json: async () => check };
        }
    };
    vm.runInNewContext(source, context);
    await handler({ target: { closest: () => button }, preventDefault() {} });
    assert.equal(button.disabled, false);
    return { calls, alerts, confirmations, location };
}

(async () => {
    let result = await scenario({ picked: false });
    assert.match(result.alerts[0], /회차를 선택/);
    assert.equal(result.calls.length, 0);
    result = await scenario({ check: { canApply: true, enoughPoint: true, fee: 25000 } });
    assert.equal(result.calls[0], '/api/registrations/check/42');
    assert.equal(result.location.href, '/event/purchase/42');
    assert.match(result.confirmations[0], /저녁 회차/);
    assert.match(result.confirmations[0], /25,000P/);
    result = await scenario({ check: { canApply: true, enoughPoint: true, fee: 0, waitlist: true } });
    assert.match(result.confirmations[0], /대기 신청/);
    assert.equal(result.location.href, '/event/purchase/42');
    result = await scenario({ status: 401 });
    assert.equal(result.location.href, '/login');
    result = await scenario({ status: 403, check: { message: '개인 회원만 가능' } });
    assert.equal(result.alerts[0], '개인 회원만 가능');
    assert.equal(result.location.href, '');
    result = await scenario({ check: { canApply: false, message: '이미 신청한 회차입니다' } });
    assert.equal(result.alerts[0], '이미 신청한 회차입니다');
    assert.equal(result.confirmations.length, 0);
    result = await scenario({ check: { canApply: true, enoughPoint: false, fee: 3000, currentPoint: 1000 } });
    assert.equal(result.location.href, '/charge');
    result = await scenario({ check: { canApply: true, enoughPoint: true, fee: 3000 }, confirm: false });
    assert.equal(result.location.href, '');
    result = await scenario({ networkError: true });
    assert.match(result.alerts[0], /네트워크 오류/);
    console.log('PASS: selected session, checkout routing, free/waitlist, login, forbidden, duplicate, balance, cancellation, network error');
})().catch(error => { console.error(error); process.exitCode = 1; });
