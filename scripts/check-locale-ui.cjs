// Install the optional browser test tool under build/: npm install --prefix build/locale-browser --no-save playwright
// Run against an isolated H2 server: node scripts/check-locale-ui.cjs http://localhost:18081
const assert = require('node:assert/strict');
const {chromium} = require('../build/locale-browser/node_modules/playwright');
const base = process.argv[2] || 'http://localhost:18081';

(async () => {
    const browser = await chromium.launch({channel: 'msedge', headless: true});
    try {
        for (const [code, label] of [['ko', '언어'], ['en', 'Language'], ['ja', '言語'], ['zh-CN', '语言']]) {
            const context = await browser.newContext();
            const page = await context.newPage();
            const errors = [];
            page.on('pageerror', error => errors.push(error.message));
            await page.goto(base + '/login');
            await page.waitForFunction(() => document.querySelector('#locale-modal')?.open);
            assert.equal(await page.locator('#login-modal').evaluate(node => node.open), false, 'first language dialog has priority');
            await page.locator(`input[name="locale"][value="${code}"]`).check();
            await Promise.all([page.waitForURL(base + '/login'), page.locator('.bp-locale-apply').click()]);
            await page.waitForFunction(() => document.querySelector('#login-modal')?.open);
            assert.equal(await page.locator('#locale-modal').evaluate(node => node.open), false);
            assert.equal(await page.locator('html').getAttribute('lang'), code);
            const cookie = (await context.cookies()).find(item => item.name === 'BUSAN_PLAY_LOCALE');
            assert.equal(cookie.value, code); assert.equal(cookie.httpOnly, true);
            assert.equal(cookie.sameSite, 'Lax'); assert.equal(cookie.path, '/'); assert.ok(cookie.expires > Date.now() / 1000 + 30000000);
            await page.goto(base + '/signup');
            await page.waitForSelector('.bp-locale-trigger');
            assert.ok((await page.locator('.bp-locale-trigger').textContent()).includes(label));
            await page.locator('#email').fill('preserved@example.test');
            await page.locator('[data-locale-open]').click();
            await page.locator('[data-locale-close]').click();
            assert.equal(await page.locator('#email').inputValue(), 'preserved@example.test', 'language dialog leaves signup fields intact');
            const state = await context.storageState();
            const revisit = await browser.newContext({storageState: state});
            const returnPage = await revisit.newPage();
            await returnPage.goto(base + '/signup');
            assert.equal(await returnPage.locator('.bp-locale').getAttribute('data-first-visit'), 'false');
            assert.equal(await returnPage.locator('#locale-modal').evaluate(node => node.open), false);
            await returnPage.goto(base + '/locale-browser-missing-page');
            assert.equal(await returnPage.locator('[data-locale-open]').count(), 1, 'actual 404 page includes language switch');
            for (const path of ['/css/locale-missing.css', '/api/locale-missing']) {
                const response = await returnPage.request.get(base + path, {headers: {Accept: 'text/html'}});
                assert.equal(response.status(), 404);
                assert.equal((await response.text()).includes('id="locale-modal"'), false, 'resource/API errors contain no language modal');
            }
            assert.deepEqual(errors, []);
            await revisit.close(); await context.close();
        }
        const mobile = await browser.newContext({viewport: {width: 390, height: 844}});
        const mobilePage = await mobile.newPage();
        await mobilePage.goto(base + '/signup');
        await mobilePage.waitForFunction(() => document.querySelector('#locale-modal')?.open);
        assert.ok(await mobilePage.locator('#locale-modal').evaluate(node => node.getBoundingClientRect().width <= innerWidth));
        await mobilePage.screenshot({path: 'build/locale-mobile.png'});
        await mobile.close();
        console.log('PASS: 4 languages, initial/login dialog order, form preservation, persistent cookies/revisit, 404 and mobile layout');
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
