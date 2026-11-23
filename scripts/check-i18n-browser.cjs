// Run only against an isolated seeded H2 server; this creates test events/registrations with internal points.
// No external payment, email or business-verification requests are made.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {chromium, request} = require(process.env.PLAYWRIGHT_MODULE || path.resolve('build/locale-browser/node_modules/playwright'));
const base = process.argv[2] || 'http://127.0.0.1:18110';
if (!/^http:\/\/(127\.0\.0\.1|localhost):18110$/.test(base)) throw new Error('Use the dedicated isolated H2 audit server on port 18110.');
const result = {pages:[], issues:[], javascriptErrors:[], preexistingLayoutIssues:[]};
const title = '다국어 원본 행사 ' + Date.now();
const summary = '번역하지 않는 원본 소개';
const description = '번역하지 않는 원본 행사 설명';
async function login(client, role) {
    await client.get('/');
    const response = await client.post(role === 'company' ? '/login/company' : '/login', {
        form:{email:role === 'company' ? 'company@example.test' : role === 'admin' ? 'admin@example.test' : 'member@example.test', password:'1234'},
        maxRedirects:0
    });
    assert.equal(response.status(),302,'seeded '+role+' login');
}
async function selectLanguage(client, code, returnTo = '/') {
    const before = await client.get(returnTo);
    const token = (await before.text()).match(/name="_localeCsrf" value="([^"]+)"/);
    assert.ok(token, 'locale selection CSRF token exists');
    const response = await client.post('/locale',{form:{locale:code,_localeCsrf:token[1],returnTo},maxRedirects:0});
    assert.equal(response.status(),303);assert.equal(response.headers().location,returnTo);
}
(async()=>{
    fs.mkdirSync('build/i18n1b',{recursive:true});
    const company = await request.newContext({baseURL:base});
    const admin = await request.newContext({baseURL:base});
    const member = await request.newContext({baseURL:base});
    let browser;
    try {
        await login(company,'company');await login(admin,'admin');await login(member,'member');
        const applicationOpens = new Date(Date.now()+4000).toLocaleString('sv-SE',{timeZone:'Asia/Seoul'}).replace(' ','T');
        const saved=await company.post('/events/save',{maxRedirects:0,multipart:{title,summary,description,address:'부산광역시 해운대구 APEC로 55',postalCode:'48058',categoryId:'1',hasSessions:'false',pricingType:'FREE',fee:'0',startDate:'2030-12-20',endDate:'2030-12-21',applyStartAt:applicationOpens,applyEndAt:'2030-12-18T18:00','sessions[0].startsAt':'2030-12-20T09:30','sessions[0].endsAt':'2030-12-21T18:15','sessions[0].capacity':'17',file:{name:'i18n.png',mimeType:'image/png',buffer:Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/9WQAAAAASUVORK5CYII=','base64')}}});
        if(saved.status()!==302){const body=await saved.text();fs.writeFileSync('build/i18n1b/create-event-error.html',body);console.log('Event creation response:',(body.match(/<p\b[^>]*>[\s\S]*?<\/p>/g)||[]).map(value=>value.replace(/<[^>]*>/g,'').trim()));}
        assert.equal(saved.status(),302,'create original-language event');
        const companyHtml=await (await company.get('/events')).text();
        const ids=[...companyHtml.matchAll(/href="\/events\/sessions\/list\/(\d+)"/g)].map(m=>Number(m[1]));
        const eventId=Math.max(...ids);assert.ok(Number.isFinite(eventId));
        assert.equal((await admin.post('/admin/events/'+eventId+'/approve',{maxRedirects:0})).status(),302);
        const sessionsHtml=await (await company.get('/events/sessions/list/'+eventId)).text();
        const sessionIds=[...sessionsHtml.matchAll(/href="\/event-sessions\/(\d+)"/g)].map(m=>Number(m[1]));
        const sessionId=Math.max(...sessionIds);assert.ok(Number.isFinite(sessionId));
        assert.equal((await company.post('/event/deposit/'+sessionId,{maxRedirects:0})).status(),302);
        await new Promise(resolve=>setTimeout(resolve,5000));
        assert.equal((await member.post('/api/registrations/apply/'+sessionId)).status(),200);
        result.eventId=eventId;result.sessionId=sessionId;
        browser=await chromium.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});
        const pages={
            guest:['/','/events/list?keyword='+encodeURIComponent(title),'/event/'+eventId,'/login','/signup','/signup/company','/missing-i18n-page'],
            member:['/mypage','/myhistory','/mypoint','/charge','/community','/community/'+sessionId],
            company:['/company/detail','/company/update','/company/points','/events','/events/save','/event-sessions/'+sessionId,'/attendance/qr/'+sessionId],
            admin:['/admin/dashboard','/admin/members','/admin/companies','/admin/events','/admin/sessions','/admin/event-cancels','/admin/categories']
        };
        for(const code of ['ko','en','ja','zh-CN'])for(const role of Object.keys(pages)){
            const context=await browser.newContext({baseURL:base,ignoreHTTPSErrors:true});
            if(role!=='guest')await login(context.request,role);
            await selectLanguage(context.request,code,'/events/list?keyword=retained&page=1');
            const page=await context.newPage();
            page.on('pageerror',error=>result.javascriptErrors.push({code,role,error:error.message}));
            await context.route('**/*',route=>{
                const url=new URL(route.request().url());
                if(url.origin===base || /(?:sockjs|stomp|qrcode)/i.test(url.pathname))return route.continue();
                return route.fulfill({body:'',contentType:'text/javascript'});
            });
            for(const route of pages[role]){
                const response=await page.goto(base+route);await page.waitForTimeout(100);
                const status=response.status();
                if(status!==(route==='/missing-i18n-page'?404:200))result.issues.push({code,role,route,status});
                const lang=await page.locator('html').getAttribute('lang');
                if(lang!==code)result.issues.push({code,role,route,lang});
                const texts=await page.evaluate(()=>{
                    const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT),values=[];let node;
                    while((node=walker.nextNode()))if(node.textContent.trim()&&!node.parentElement.closest('script,style,template,noscript')&&node.parentElement.getClientRects().length)values.push(node.textContent.trim());
                    return values;
                });
                const visible=texts.join(' ');
                if(/\b(?:common|auth|admin|event|payment|company|application)\.ui\d{4}\b/.test(visible))result.issues.push({code,role,route,unresolvedKey:true});
                let hangul=[];
                if(code!=='ko'){
                    const preserved=[title,summary,description,'테스트일반회원','테스트운영자','테스트기업','테스트대표','테스트담당자','한국어','전시','공연','축제','체험','기본 회차','부산광역시 해운대구 APEC로 55'];
                    hangul=texts.filter(text=>{
                        text=text.replace(/다국어 원본 행사 \d+/g,''); // Original DB titles from previous runs are intentionally unchanged.
                        for(const raw of preserved)text=text.replaceAll(raw,'');
                        return /[가-힣]/.test(text);
                    });
                    if(hangul.length)result.issues.push({code,role,route,remainingHangul:hangul});
                }
                if(route.startsWith('/event/'))assert.ok(visible.includes(title),'DB event title must remain original');
                await page.setViewportSize({width:390,height:1000});
                const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth);
                if(overflow){
                    const entry={code,role,route,mobileOverflow:true};
                    // The common error footer already overflows in Korean; keep this separate from translation regressions.
                    if(route==='/missing-i18n-page')result.preexistingLayoutIssues.push(entry);
                    else result.issues.push(entry);
                }
                await page.setViewportSize({width:1280,height:900});
                result.pages.push({code,role,route,status,lang,remainingHangul:hangul});
            }
            await context.close();
        }
        console.log('Live four-language pages:',result.pages.length,'issues:',result.issues.length,'JS errors:',result.javascriptErrors.length);
        if(result.issues.length)console.log(JSON.stringify(result.issues,null,2));
        if(result.javascriptErrors.length)console.log(JSON.stringify(result.javascriptErrors,null,2));
        assert.deepEqual(result.issues,[]);assert.deepEqual(result.javascriptErrors,[]);
    }finally{
        fs.writeFileSync('build/i18n1b/browser-results.json',JSON.stringify(result,null,2));
        if(browser)await browser.close();await Promise.all([company.dispose(),admin.dispose(),member.dispose()]);
    }
})().catch(error=>{console.error(error);process.exitCode=1;});
