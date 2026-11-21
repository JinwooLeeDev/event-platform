// Browser regressions use real rendered templates; APIs/SDKs are mocked, no real payments.
// First run scripts/FinalPolishRender.java. Set PLAYWRIGHT_MODULE if Playwright is not installed locally.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || path.resolve('build/locale-browser/node_modules/playwright'));
const root = path.resolve('build/final-polish');
const errors = [];
let browser;
const html = name => fs.readFileSync(path.join(root, name + '.html'), 'utf8');

async function setup(page) {
    page.on('pageerror', error => errors.push(error.message));
    await page.route('**/*', async route => {
        const url = new URL(route.request().url());
        if (url.hostname !== 'polish.test') return route.fulfill({body: '', contentType: 'text/javascript'});
        if (url.pathname.startsWith('/js/') || url.pathname.startsWith('/css/')) {
            const file = path.resolve('src/main/resources/static' + url.pathname);
            return route.fulfill({body: fs.readFileSync(file), contentType: url.pathname.endsWith('.js') ? 'text/javascript' : 'text/css'});
        }
        if (url.pathname.startsWith('/events/favorite/')) return route.fulfill({json: {favorite: route.request().method() === 'POST', favoriteCount: 3}});
        if (url.pathname.startsWith('/event-files/')) return route.fulfill({body: '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300"><rect width="400" height="300" fill="#174bc1"/></svg>', contentType: 'image/svg+xml'});
        let fixture = url.pathname.slice(1) || 'main';
        if (url.pathname === '/events/list') fixture = 'list-' + (url.searchParams.get('categoryId') || 'all') + '-' + (url.searchParams.get('page') || '1');
        if (!fs.existsSync(path.join(root, fixture + '.html'))) return route.fulfill({status: 404, body: ''});
        return route.fulfill({body: html(fixture), contentType: 'text/html; charset=utf-8'});
    });
    await page.addInitScript(() => {
        window.alerts = [];
        window.alert = text => alerts.push(text);
        window.SockJS = function () {};
        window.StompJs = {Client: class {
            constructor() { this.connected = true; }
            activate() { this.onConnect(); }
            subscribe() {} publish() {} deactivate() {}
        }};
        window.QRCode = function (box) { const canvas = document.createElement('canvas'); canvas.width = 260; canvas.height = 260; box.append(canvas); };
        window.QRCode.CorrectLevel = {M: 0};
    });
}

async function payment(page) {
    const requests = [];
    await page.route('**/api/*/payment/*', async route => {
        requests.push({url: route.request().url(), body: route.request().postDataJSON()});
        const scenario = await page.evaluate(() => window.paymentScenario);
        if (scenario === 'network') return route.abort();
        if (scenario === 'prepare-error' && route.request().url().endsWith('prepare')) return route.fulfill({status: 500, json: {}});
        if (scenario === 'complete-error' && route.request().url().endsWith('complete')) return route.fulfill({status: 500, json: {}});
        return route.fulfill({json: route.request().url().endsWith('prepare') ?
            {storeId: 'test', channelKey: route.request().url().includes('/naver/') ? 'naver-channel' : 'kakao-channel', paymentId: 'test-payment', amount: 30000} :
            {currentPoint: 58500, redirectUrl: '#complete'}});
    });
    await page.addInitScript(() => {
        window.paymentScenario = 'success'; window.sdkCalls = [];
        window.PortOne = {requestPayment: async options => {
            sdkCalls.push(options);
            if (paymentScenario === 'pending') await new Promise(resolve => { window.releasePayment = resolve; });
            if (paymentScenario === 'cancel') return {code: 'PAY_PROCESS_CANCELED'};
            if (paymentScenario === 'sdk-error') throw new Error('SDK failure');
            return {paymentId: options.paymentId};
        }};
    });
    for (const role of ['personal', 'company']) {
        requests.length = 0;
        await page.goto('http://polish.test/charge-' + role);
        assert.equal(await page.locator('.charge-payment-buttons button').count(), 1, 'one common payment button');
        const pay = page.locator('#chargePayBtn');
        for (const invalid of ['', '0', '-1', '1.5']) {
            await page.locator('#chargeAmount').fill(invalid);
            await pay.click();
        }
        assert.equal(requests.length, 0, 'invalid amount does not prepare a payment');
        await page.locator('[data-amount="30000"]').click();
        for (const provider of ['naver', 'kakao']) {
            requests.length = 0;
            await page.locator('[data-payment-provider="' + provider + '"]').click();
            assert.equal(await page.locator('[data-payment-provider][aria-pressed="true"]').count(), 1);
            assert.equal(await page.locator('#selectedPaymentMethod').textContent(), provider === 'naver' ? '네이버페이' : '카카오페이');
            await pay.click();
            await page.waitForURL(url => url.hash === '#complete');
            assert.equal(new URL(requests[0].url).pathname, '/api/' + provider + '/payment/prepare');
            assert.equal(requests[0].body.amount, 30000);
            assert.equal(new URL(requests[1].url).pathname, '/api/' + provider + '/payment/complete');
            assert.equal(requests[1].body.paymentId, 'test-payment');
            assert.equal(await page.evaluate(() => sdkCalls.at(-1).channelKey), provider + '-channel');
            await page.evaluate(() => history.replaceState(null, '', location.pathname));
        }
        for (const scenario of ['cancel', 'prepare-error', 'complete-error', 'sdk-error', 'network']) {
            requests.length = 0;
            await page.evaluate(value => { window.paymentScenario = value; }, scenario);
            await pay.click();
            await page.waitForFunction(() => !document.getElementById('chargePayBtn').disabled);
            assert.equal(requests.length, scenario === 'complete-error' ? 2 : 1, scenario);
            assert.equal(await page.locator('[data-payment-provider]:disabled').count(), 0);
        }
        requests.length = 0;
        await page.evaluate(() => { window.paymentScenario = 'pending'; });
        await pay.click();
        await page.waitForFunction(() => typeof window.releasePayment === 'function');
        assert.equal(await page.locator('[data-payment-provider]:disabled').count(), 2);
        await page.evaluate(() => { document.getElementById('chargePayBtn').click(); window.releasePayment(); });
        await page.waitForFunction(() => !document.getElementById('chargePayBtn').disabled);
        assert.equal(requests.length, 2, 'duplicate clicks create only one prepare/complete');
    }
    console.log('PASS provider selection, payment routing, cancellation, failures and duplicate prevention');
}

async function list(page) {
    await page.goto('http://polish.test/events/list');
    await page.evaluate(() => { window.documentMarker = 'same-document'; });
    await page.locator('.list-category[value="1"]').click();
    await page.waitForURL('**/*categoryId=1*');
    assert.equal(await page.evaluate(() => window.documentMarker), 'same-document', 'category does not reload the document');
    await page.locator('.event-card__title').filter({hasText: '행사 1 페이지 1'}).waitFor();
    assert.equal(await page.locator('.list-category[aria-current="true"]').getAttribute('value'), '1');
    await page.locator('#list-pagination a').filter({hasText: /^2$/}).click();
    await page.locator('.event-card__title').filter({hasText: '페이지 2'}).waitFor();
    assert.equal(new URL(page.url()).searchParams.get('categoryId'), '1');
    await page.goBack();
    await page.locator('.event-card__title').filter({hasText: '페이지 1'}).waitFor();
    await page.locator('.header-search input[name="keyword"]').fill('부산 & 전시');
    await page.locator('.header-search button').click();
    await page.waitForFunction(() => new URL(location.href).searchParams.get('keyword') === '부산 & 전시');
    assert.equal(new URL(page.url()).searchParams.get('categoryId'), '1');
    const favorite = page.locator('[data-favorite-event]');
    await favorite.click();
    await page.locator('[data-favorite-event][aria-pressed="true"]').waitFor();
    await page.locator('.list-category[value="empty"]').click();
    await page.locator('.main-events__empty').waitFor();
    await page.goBack();
    await page.locator('.event-card').waitFor();
    const oldURL = page.url();
    await page.route('**/events/list?**categoryId=2**', route => route.fulfill({status: 500, body: 'failed'}));
    await page.locator('.list-category[value="2"]').click();
    await page.locator('.list-content[aria-busy="false"]').waitFor();
    assert.match(await page.locator('#list-feedback').textContent(), /불러오지 못했습니다/);
    assert.equal(page.url(), oldURL, 'failed filter retains current URL and content');
    assert.equal(await page.locator('.event-card').count(), 1);
    await page.unroute('**/events/list?**categoryId=2**');
    let releaseFirst;
    const firstArrived = new Promise(resolve => { releaseFirst = resolve; });
    let finishFirst;
    const delay = new Promise(resolve => { finishFirst = resolve; });
    await page.route('**/events/list?**', async route => {
        const url = new URL(route.request().url());
        const category = url.searchParams.get('categoryId');
        if (category === '2') {
            releaseFirst();
            await delay;
        }
        await route.fulfill({body: html('list-' + (category || 'all') + '-1'), contentType: 'text/html; charset=utf-8'}).catch(() => {});
    });
    await page.locator('.list-category[value="2"]').click();
    await firstArrived;
    await page.locator('.list-category[value="1"]').click();
    await page.locator('.list-content[aria-busy="false"]').waitFor();
    finishFirst();
    // Let the delayed response arrive after the latest category has already rendered.
    await page.waitForTimeout(100);
    assert.equal(new URL(page.url()).searchParams.get('categoryId'), '1');
    assert.equal(await page.locator('.list-category[aria-current="true"]').getAttribute('value'), '1');
    assert.match(await page.locator('.event-card__title').textContent(), /행사 1 페이지 1/);
    await page.locator('[data-favorite-event]').click();
    await page.locator('[data-favorite-event][aria-pressed="true"]').waitFor();
    console.log('PASS asynchronous filters, paging, history, search, favorites, empty and failed results');
    console.log('PASS delayed earlier category response cannot replace latest cards, URL, selection or favorites');
}

(async () => {
    browser = await chromium.launch({executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true});
    const page = await browser.newPage();
    await setup(page);
    await payment(page);
    await list(page);
    for (const width of [1440, 768, 390, 360]) {
        await page.setViewportSize({width, height: 1000});
        for (const fixture of ['community-host', 'community-member', 'qr', 'main', 'list-all-1', 'charge-personal', 'charge-company']) {
            await page.goto('http://polish.test/' + fixture);
            if (fixture === 'community-host') await page.locator('.community-notice-editor summary').click();
            assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), fixture + ' overflows at ' + width);
            assert.equal(await page.locator('.user-name .arrow-down').count(), 0);
            if (fixture === 'main' || fixture === 'list-all-1') {
                assert.equal(await page.locator('.event-card__visual').count(), await page.locator('.event-card').count(), 'one image area per event');
                assert.equal(await page.locator('.event-card img').count(), await page.locator('.event-card').count(), 'one representative image per event');
                const image = page.locator('.event-card__image');
                assert.equal(await image.getAttribute('src'), '/event-files/101');
                await image.scrollIntoViewIfNeeded();
                await page.waitForFunction(() => document.querySelector('.event-card__image').naturalWidth > 0);
                if (width === 1440 || width === 390) await page.screenshot({path: path.join(root, fixture + '-' + width + '.png'), fullPage: true});
                await image.evaluate(img => img.dispatchEvent(new Event('error')));
                assert.equal(await image.isVisible(), false, 'broken image shows fallback');
            } else if (width === 1440 || width === 390) await page.screenshot({path: path.join(root, fixture + '-' + width + '.png'), fullPage: true});
        }
    }
    assert.deepEqual(errors, [], 'no uncaught browser errors');
    console.log('PASS 28 responsive layouts, event images/fallback and nickname arrow removal');
})().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => { if (browser) await browser.close(); });
