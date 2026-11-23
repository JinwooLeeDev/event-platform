const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const {BusanI18n} = require('./i18n-test-helpers.cjs');
const source = fs.readFileSync('src/main/resources/static/js/admin-event-cancels.js', 'utf8');
const tick = () => new Promise(setImmediate);

function setup() {
    const requests = [], history = [], handlers = {}, clicks = {};
    const list = {attributes:{}, children:['initial'], setAttribute(k,v){this.attributes[k]=v;},
        replaceChildren(...nodes){this.children=nodes;}, querySelectorAll(){return [];}};
    const loading = {hidden:true}, error = {hidden:true}, errorMessage = {};
    const tabs = ['PENDING','APPROVED','REJECTED'].map(status => ({
        dataset:{cancelStatus:status}, href:`http://localhost/admin/event-cancels?status=${status}`,
        attributes:status==='PENDING'?{'aria-current':'page'}:{},
        setAttribute(k,v){this.attributes[k]=v;}, removeAttribute(k){delete this.attributes[k];}
    }));
    const retry = {addEventListener(type, callback){clicks.retry=callback;}};
    const elements = {'[data-admin-cancel-list]':list, '[data-cancel-loading]':loading,
        '[data-cancel-load-error]':error, '[data-cancel-error-message]':errorMessage, '[data-cancel-retry]':retry};
    const browser = {querySelector:s=>elements[s], querySelectorAll:()=>tabs,
        addEventListener(type, callback){clicks[type]=callback;}};
    const window = {location:{href:'http://localhost/admin/event-cancels'},
        history:{pushState(state,title,url){history.push(url); window.location.href=url;}},
        addEventListener(type,callback){handlers[type]=callback;}};
    vm.runInNewContext(source, {BusanI18n, URL, AbortController, window,
        document:{querySelector:()=>browser},
        fetch:(url, options)=>new Promise((resolve,reject)=>requests.push({url,options,resolve,reject})),
        DOMParser:class {parseFromString(html){return {querySelector:()=>html==='missing'?null:{childNodes:[html]}};}}
    });
    function click(index, overrides={}) {
        const event = {target:{closest:()=>tabs[index]}, button:0, preventDefault(){this.prevented=true;}, ...overrides};
        clicks.click(event); return event;
    }
    function respond(index, body, status=200) {
        requests[index].resolve({ok:status===200,status,text:async()=>body});
        return tick();
    }
    return {requests,history,handlers,clicks,list,loading,error,errorMessage,tabs,window,click,respond};
}

(async () => {
    {
        const s=setup();
        assert.equal(s.click(1).prevented,true);
        assert.equal(s.loading.hidden,false);
        await s.respond(0,'approved list');
        assert.deepEqual(s.list.children,['approved list']);
        assert.equal(s.tabs[1].attributes['aria-current'],'page');
        assert.equal(s.tabs[0].attributes['aria-current'],undefined);
        assert.equal(s.history.length,1);
        assert.equal(s.loading.hidden,true);
        assert.equal(s.list.attributes['aria-busy'],'false');
    }
    {
        const s=setup(); s.click(1); s.click(2);
        assert.equal(s.requests[0].options.signal.aborted,true);
        await s.respond(1,'rejected list'); await s.respond(0,'stale approved list');
        assert.deepEqual(s.list.children,['rejected list']);
        assert.equal(s.history.length,1);
    }
    {
        const s=setup(); s.click(1); await s.respond(0,'server error',500);
        assert.deepEqual(s.list.children,['initial']);
        assert.equal(s.history.length,0);
        assert.equal(s.error.hidden,false);
        s.clicks.retry(); await s.respond(1,'recovered list');
        assert.deepEqual(s.list.children,['recovered list']);
        assert.equal(s.error.hidden,true);
    }
    {
        const s=setup(); s.click(1); await s.respond(0,'missing');
        assert.equal(s.error.hidden,false);
        assert.deepEqual(s.list.children,['initial']);
    }
    {
        const s=setup();
        assert.equal(s.click(1,{ctrlKey:true}).prevented,undefined);
        assert.equal(s.requests.length,0);
        s.window.location.href='http://localhost/admin/event-cancels?status=REJECTED';
        s.handlers.popstate(); await s.respond(0,'history list');
        assert.equal(s.history.length,0);
        assert.equal(s.tabs[2].attributes['aria-current'],'page');
    }
    console.log('Admin cancellation async navigation: 5 scenarios passed.');
})().catch(error => {console.error(error);process.exitCode=1;});
