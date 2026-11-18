const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const tick = () => new Promise(setImmediate);
const source = fs.readFileSync('src/main/resources/static/js/company-participants.js', 'utf8');
class Element {
    constructor(){this.children=[];this.listeners={};this.value='';this.hidden=false;this.disabled=false;this.textContent='';this.attributes={};}
    appendChild(node){this.children.push(node);}
    replaceChildren(...nodes){this.children=nodes;}
    setAttribute(k,v){this.attributes[k]=v;}
    addEventListener(k,v){this.listeners[k]=v;}
    fire(k,event={}){return this.listeners[k]?.(event);}
    focus(){this.focused=true;}
    showModal(){this.open=true;}
    close(){this.open=false;this.fire('close');}
}
function setup() {
    const selectors=['[data-participant-search]','[data-participant-filter]','[data-participant-refresh]',
        '[data-participant-rows]','[data-participant-summary]','[data-participant-error]',
        '[data-participant-login]','table','[data-participant-notice]',
        '[data-reject-target]','[data-reject-confirm]','[data-reject-error]','[data-reject-close]'];
    const elements=Object.fromEntries(selectors.map(s=>[s,new Element()]));
    const dialog=new Element(), occupied=new Element(), requests=[];
    const panel={dataset:{participants:'10'},querySelector:s=>elements[s]};
    dialog.querySelector=s=>elements[s];
    vm.runInNewContext(source,{
        document:{querySelector:s=>s==='[data-participants]'?panel:s==='[data-session-occupied]'?occupied:dialog,createElement:()=>new Element()},
        fetch:(url,options)=>new Promise((resolve,reject)=>requests.push({url,options,resolve,reject}))
    });
    const respond=async(index,data,status=200)=>{requests[index].resolve({ok:status===200,status,json:async()=>data});await tick();};
    const rows=()=>elements['[data-participant-rows]'].children;
    const button=()=>rows()[0].children[4].children[0];
    return {elements,dialog,occupied,requests,respond,rows,button};
}
const person=(id,status,name='Member')=>({registrationId:id,status,displayName:name,createdAt:'2026-11-18T10:00:00'});
(async()=>{
    {
        const s=setup(); await s.respond(0,[person(1,'CONFIRMED','<script>name</script>'),person(2,'WAITING'),person(3,'PENDING'),person(4,'CANCELLED'),person(5,'REJECTED'),person(6,'UNKNOWN')]);
        assert.equal(s.rows().length,3);
        assert.equal(s.occupied.textContent,'2명');
        assert.equal(s.rows()[0].children[1].textContent,'<script>name</script>');
        s.elements['[data-participant-filter]'].value='WAITING'; s.elements['[data-participant-filter]'].fire('change');
        assert.equal(s.rows().length,1);assert.equal(s.rows()[0].children[0].textContent,'2');
        s.elements['[data-participant-search]'].value='absent';s.elements['[data-participant-search]'].fire('input');
        assert.equal(s.rows()[0].children[0].colSpan,5);
    }
    {
        const s=setup();await s.respond(0,[person(1,'CONFIRMED'),person(2,'WAITING')]);
        s.button().fire('click');assert.equal(s.dialog.open,true);
        s.elements['[data-reject-close]'].fire('click');assert.equal(s.requests.length,1);
        s.button().fire('click');
        const submit=s.elements['[data-reject-confirm]'].fire('click');
        s.elements['[data-reject-confirm]'].fire('click');
        assert.equal(s.requests.length,2);assert.equal(s.requests[1].url,'/api/registrations/reject/1');
        assert.equal(s.requests[1].options.method,'POST');
        let prevented=false;s.dialog.fire('cancel',{preventDefault(){prevented=true;}});assert.equal(prevented,true);
        await s.respond(1,{message:'ok'});
        assert.equal(s.requests.length,3); // Refresh the promoted waiting participant.
        await s.respond(2,[person(1,'REJECTED'),person(2,'CONFIRMED')]);await submit;
        assert.equal(s.dialog.open,false);assert.equal(s.rows().length,1);
        assert.equal(s.rows()[0].children[0].textContent,'2');assert.equal(s.occupied.textContent,'1명');
        assert.equal(s.elements['[data-participant-notice]'].hidden,false);
    }
    {
        const s=setup();await s.respond(0,[person(1,'WAITING')]);s.button().fire('click');
        const submit=s.elements['[data-reject-confirm]'].fire('click');await s.respond(1,{message:'Already processed'},400);await submit;
        assert.equal(s.dialog.open,true);assert.equal(s.rows().length,1);
        assert.equal(s.elements['[data-reject-error]'].textContent,'Already processed');
        assert.equal(s.elements['[data-reject-confirm]'].disabled,false);
    }
    {
        const s=setup();await s.respond(0,[person(1,'CONFIRMED')]);s.button().fire('click');
        const submit=s.elements['[data-reject-confirm]'].fire('click');await s.respond(1,{},401);await submit;
        assert.equal(s.elements['[data-participant-login]'].hidden,false);
        assert.equal(s.dialog.open,true);
    }
    {
        const s=setup();await s.respond(0,[person(1,'WAITING')]);s.button().fire('click');
        const submit=s.elements['[data-reject-confirm]'].fire('click');await s.respond(1,{message:'ok'});
        await s.respond(2,{},500);await submit;
        assert.equal(s.elements['[data-participant-error]'].hidden,false);
        assert.equal(s.elements['[data-participant-notice]'].hidden,false);
        assert.equal(s.rows()[0].children[0].colSpan,5);
    }
    console.log('Company participants: 5 scenarios passed (active-only list, filters, reject/reload, duplicates, errors).');
})().catch(error=>{console.error(error);process.exitCode=1;});
