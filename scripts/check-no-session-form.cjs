// Render scripts/FinalPolishRender.java first. Tests real form JS with a mocked address lookup.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {chromium} = require(process.env.PLAYWRIGHT_MODULE || path.resolve('build/locale-browser/node_modules/playwright'));

(async () => {
    const browser = await chromium.launch({executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true});
    try {
        const page = await browser.newPage();
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        await page.route('**/*', route => {
            const url = new URL(route.request().url());
            if (url.origin === 'http://form.test' && url.pathname.startsWith('/js/'))
                return route.fulfill({body: fs.readFileSync(path.resolve('src/main/resources/static' + url.pathname)), contentType:'text/javascript'});
            return route.fulfill({body: route.request().url() === 'http://form.test/events/save'
                ? fs.readFileSync('build/final-polish/event-save.html') : '', contentType: 'text/html; charset=utf-8'});
        });
        await page.addInitScript(() => {
            window.alerts = [];
            window.alert = message => alerts.push(message);
            window.requestedSave = false;
            window.fetch = async () => {
                window.requestedSave = true;
                return {ok:false,status:400,json:async()=>({message:'Mock validation response'})};
            };
            window.daum = {Postcode: class {
                constructor(options) { this.options = options; }
                open() { this.options.oncomplete({roadAddress: 'Busan Haeundae', zonecode: '48058', buildingName: 'Audit venue'}); }
            }};
        });
        await page.goto('http://form.test/events/save');
        await page.locator('[name="hasSessions"][value="false"]').check();
        for (const id of ['startTime', 'endTime', 'capacity']) {
            assert.equal(await page.locator('#' + id).count(), 1, id + ' input exists');
            assert.equal(await page.locator('#' + id).isEnabled(), true);
            assert.equal(await page.locator('#' + id).getAttribute('required'), '');
        }
        await page.locator('[name="categoryId"]').selectOption('1');
        for (const [name, value] of Object.entries({title: 'Audit event', summary: 'Audit summary', description: 'Audit description'})) await page.locator('[name="' + name + '"]').fill(value);
        await page.locator('#address-search-button').click();
        await page.locator('[name="file"]').setInputFiles({name: 'audit.png', mimeType: 'image/png', buffer: Buffer.from('audit')});
        for (const [id, value] of Object.entries({startDate: '2030-12-20', startTime: '09:30', endDate: '2030-12-20', endTime: '18:15', capacity: '17', applyStartDate: '2030-12-01', applyStartTime: '09:00', applyEndDate: '2030-12-18', applyEndTime: '18:00'})) await page.locator('#' + id).fill(value);
        await page.locator('#pricingType').selectOption('FREE');
        // Cancel navigation after the real submit listeners run, then inspect native FormData.
        await page.evaluate(() => document.getElementById('event-save-form').addEventListener('submit', event => {
            window.formBlocked = event.defaultPrevented;
            window.submitted = [...new FormData(event.target).entries()].filter(([key]) => key !== 'file');
            event.preventDefault();
        }));
        await page.locator('[type="submit"]').filter({hasText: '주최 신청하기'}).click();
        assert.equal(await page.evaluate(() => window.requestedSave), true, 'valid form uses Fetch submission');
        let values = Object.fromEntries(await page.evaluate(() => window.submitted));
        assert.equal(values['sessions[0].capacity'], '17');
        assert.equal(values['sessions[0].startsAt'], '2030-12-20T09:30');
        assert.equal(values['sessions[0].endsAt'], '2030-12-20T18:15');
        await page.locator('#endTime').fill('08:00');
        await page.evaluate(() => { window.requestedSave = false; });
        await page.locator('[type="submit"]').filter({hasText: '주최 신청하기'}).click();
        assert.equal(await page.evaluate(() => window.formBlocked), true, 'reversed times are blocked');
        assert.equal(await page.evaluate(() => window.requestedSave), false, 'invalid times never reach Fetch');
        await page.locator('[name="hasSessions"][value="true"]').check();
        values = Object.fromEntries(await page.evaluate(() => [...new FormData(document.getElementById('event-save-form')).entries()]));
        assert.equal(await page.locator('#capacity').isEnabled(), false);
        assert.equal(await page.locator('#startTime').isEnabled(), false);
        assert.notEqual(values['sessions[0].capacity'], '17', 'disabled default-session values do not leak into multi-session mode');
        await page.locator('[name="hasSessions"][value="false"]').check();
        assert.equal(await page.locator('#capacity').inputValue(), '17', 'switching back retains input');
        assert.equal(await page.locator('#startTime').isEnabled(), true);
        for (const width of [1440, 768, 390, 360]) {
            await page.setViewportSize({width, height: 1000});
            assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'form overflows at ' + width);
        }
        assert.deepEqual(errors, []);
        console.log('PASS no-session capacity/time fields, DTO submission, reversed time, tab switching and 4 responsive widths');
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
