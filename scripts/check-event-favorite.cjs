const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const {BusanI18n} = require('./i18n-test-helpers.cjs');
const source = fs.readFileSync('src/main/resources/static/js/event-favorite.js', 'utf8');
const tick = () => new Promise(setImmediate);
const response = (favorite, count, status = 200) => ({status, ok:status === 200, json:async()=>({favorite, favoriteCount:count})});

async function scenario(replies, {company = false, favoritePage = false, duplicates = 1, confirm = false, loginModal = false} = {}) {
    const calls = [], redirects = [], messages = [], items = [];
    const modalOpens = [];
    const total = {textContent:'1'}, empty = {hidden:true};
    const buttons = Array.from({length:duplicates}, () => {
        const message = {textContent:'',hidden:true}, count = {textContent:'—'};
        const item = {removed:false, remove(){this.removed=true;}}; items.push(item); messages.push(message);
        return {dataset:{favoriteEvent:'7',favoriteCompany:String(company)},disabled:true,attributes:{},count,
            setAttribute(key,value){this.attributes[key]=value;},
            querySelector(){return count;},
            closest(selector){return selector==='[data-favorite-control]'?{querySelector:()=>message}:item;},
            addEventListener(_,callback){this.click=callback;}};
    });
    const page = {querySelectorAll:()=>items.filter(item=>!item.removed),
        querySelector:selector=>selector==='[data-favorite-total]'?total:empty};
    vm.runInNewContext(source, {BusanI18n,
        document:{querySelector:()=>favoritePage?page:null,querySelectorAll:selector=>selector==='[data-favorite-event]'?buttons:[]},
        window:{addEventListener(){},BusanLogin:loginModal?{open:()=>modalOpens.push(true)}:undefined}, location:{assign:url=>redirects.push(url)}, confirm:()=>confirm,
        fetch:async(url,options)=>{calls.push({url,method:options.method});const reply=replies.shift();if(reply instanceof Error)throw reply;return await reply;}
    });
    await tick();
    return {buttons,calls,redirects,messages,items,total,empty,modalOpens};
}
const click = button => button.click({preventDefault(){},stopPropagation(){}});

(async () => {
    const toggle = await scenario([response(false,3), response(true,4), response(false,3)], {duplicates:2});
    assert.equal(toggle.calls.length,1, 'duplicate event cards share one state request');
    await click(toggle.buttons[0]);
    assert.equal(toggle.calls[1].method,'POST');
    assert.ok(toggle.buttons.every(button=>button.attributes['aria-pressed']==='true'));
    await click(toggle.buttons[1]);
    assert.equal(toggle.calls[2].method,'DELETE');
    assert.ok(toggle.buttons.every(button=>button.attributes['aria-pressed']==='false'));

    const anonymous = await scenario([response(false,3), response(false,3,401)],{confirm:true});
    await click(anonymous.buttons[0]); assert.deepEqual(anonymous.redirects,['/login']);
    assert.equal(anonymous.buttons[0].attributes['aria-pressed'],'false');
    const modal = await scenario([response(false,3),response(false,3,401)],{loginModal:true});
    await click(modal.buttons[0]); assert.equal(modal.modalOpens.length,1); assert.deepEqual(modal.redirects,[]);
    const corporate = await scenario([response(false,3)],{company:true});
    await click(corporate.buttons[0]); assert.equal(corporate.calls.length,1); assert.equal(corporate.messages[0].hidden,false);

    const failure = await scenario([response(false,3), response(false,3,500)]);
    await click(failure.buttons[0]); assert.equal(failure.buttons[0].attributes['aria-pressed'],'false');
    assert.equal(failure.buttons[0].disabled,false); assert.equal(failure.messages[0].hidden,false);

    let release;
    const pending = new Promise(resolve=>{release=resolve;});
    const repeated = await scenario([response(false,3),pending]);
    const firstClick = click(repeated.buttons[0]);
    await click(repeated.buttons[0]); assert.equal(repeated.calls.length,2);
    release(response(true,4)); await firstClick; assert.equal(repeated.buttons[0].disabled,false);

    const list = await scenario([response(true,1),response(false,0)],{favoritePage:true});
    await click(list.buttons[0]); assert.equal(list.items[0].removed,true);
    assert.equal(list.total.textContent,'0'); assert.equal(list.empty.hidden,false);

    const stale = await scenario([response(false,3),response(false,3,400),response(true,4)]);
    await click(stale.buttons[0]); assert.equal(stale.buttons[0].attributes['aria-pressed'],'true');
    const retry = await scenario([new Error('offline'),response(false,3),response(true,4)]);
    await click(retry.buttons[0]); assert.equal(retry.buttons[0].attributes['aria-pressed'],'true');
    console.log('PASS: favorite toggle, duplicate cards, login/company handling, failure recovery, rapid clicks, last favorite removal, stale state reconciliation');
})().catch(error => {console.error(error);process.exitCode=1;});
