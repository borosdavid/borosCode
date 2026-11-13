'use strict';
const content = document.querySelector('#content');
const message = document.querySelector('#message');
const page = location.pathname.split('/')[2] || 'targyak';
const moving = page === 'koltoztetes';
const sale = page === 'eladas', trash = page === 'lomtar';
const statuses = {KEEP:'Megtartjuk', WAIT:'Meghirdetésre vár', LISTED:'Meghirdetve', SOLD:'Elkelt'};
let actions = [], places = [], dirty = () => false;
function warn(e) {message.textContent=e.message || String(e); message.hidden=false; message.scrollIntoView({block:'nearest'});}
function esc(v) {return String(v ?? '').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
async function api(path, options) {
    const res=await fetch('/api/'+path, options);
    const raw=await res.text();
    if(!res.ok) {
        let detail;
        try { detail=JSON.parse(raw).detail; } catch {}
        const error=new Error(detail || 'A művelet sikertelen ('+res.status+'). Próbáld újra.');
        error.status=res.status;
        throw error;
    }
    return raw ? JSON.parse(raw) : null;
}
function options(){return places.map(p=>'<option value="'+esc(p.id)+'">'+esc(p.name)+'</option>').join('');}
window.addEventListener('beforeunload',e=>{if(dirty()){e.preventDefault();e.returnValue=true;}});
function actionLabel(row) {
    const action=actions.find(a=>a.id===row.action_id);
    if(action?.kind==='MOVE')return action.name+' → '+(places.find(p=>p.id===row.destination_id)?.name||'');
    return action?.kind==='SALE'?action.name+' · '+statuses[row.status]:action?.name||'Nincs kijelölve';
}
function showActionSettings() {
    const host=document.querySelector('#actionSettings');
    host.innerHTML='<h2>Tárgyműveletek</h2><form class="actionform"><label>Új művelet neve<input maxlength="100" required></label><label>Típus<select><option value="CUSTOM">Egyszerű jelölés</option><option value="MOVE">Költöztetés célhelyszínnel</option></select></label><button>Hozzáadás</button></form><div class="actionlist"></div>';
    const create=host.querySelector('form');
    const post=body=>api('actions',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
    function addRow(a){
        const f=document.createElement('form');
        f.className='place';
        f.innerHTML='<input required maxlength="100" aria-label="Művelet neve" value="'+esc(a.name)+'"><button>Mentés</button> <button type="button"></button>';
        const input=f.elements[0],toggle=f.elements[2];
        const render=()=>{toggle.textContent=a.active?'Kikapcsolás':'Visszakapcsolás';f.classList.toggle('inactive',!a.active);};
        async function save(enabled,name){
            try{await post({...a,name,active:enabled});a.active=enabled;a.name=name;input.defaultValue=name;render();message.textContent='Művelet mentve.';message.hidden=false;}catch(e){warn(e);}
        }
        toggle.onclick=()=>save(!a.active,a.name);
        f.onsubmit=e=>{e.preventDefault();save(a.active,input.value.trim());};
        host.querySelector('.actionlist').append(f);render();
    }
    actions.forEach(addRow);
    create.onsubmit=async e=>{
        e.preventDefault();
        try{const name=create.elements[0].value.trim();await post({name,kind:create.elements[1].value,active:true});
            actions=await api('actions');addRow(actions.find(a=>a.name===name));create.reset();message.textContent='Művelet hozzáadva.';message.hidden=false;
        }catch(err){warn(err);}
    };
}
function placeForm(p) {
    return '<form class="place" data-id="'+esc(p.id)+'"><input aria-label="Helyszín neve" required maxlength="180" value="'+esc(p.name)+'"><button>Mentés</button></form>';
}
function showPlaces() {
    content.innerHTML='<h1>Beállítások</h1><section id="places"><h2>Új helyszín</h2>'+placeForm({})+'<h2>Helyszínek átnevezése</h2>'+places.map(placeForm).join('')+'</section><section id="actionSettings"><h2>Tárgyműveletek</h2></section>';
    showActionSettings();
    const forms=[...content.querySelectorAll('#places form')];
    dirty=()=>[...content.querySelectorAll('input, select')].some(el=>el.tagName==='INPUT'?el.value!==el.defaultValue:el.value!==el.options[0].value);
    for(const form of forms) form.onsubmit=async function(e){
        const f=e.currentTarget;
        e.preventDefault();
        try {
            const name=f.elements[0].value.trim();
            await api('places',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({id:f.dataset.id||null,name})});
            if(f.dataset.id) f.elements[0].defaultValue=name;
            else {f.elements[0].value='';}
            places=await api('places');
            if(!f.dataset.id) {const added=places.find(p=>p.name===name); const saved=document.createElement('div'); saved.innerHTML=placeForm(added); const extra=saved.firstElementChild; content.querySelector('#places').append(extra);extra.onsubmit=f.onsubmit;}
            message.textContent='Helyszín mentve.';message.hidden=false;
        } catch(err){warn(err);}
    };
}
async function list(selectedPlace='') {
    const rows=await api('items?trash='+trash);
    const sourceQuery=(sale||moving)?'?from='+page:'';
    const title=moving?'Költöztetés':trash?'Lomtár':sale?'Eladás':'Tárgyak';
    content.innerHTML='<div class="heading"><h1>'+title+'</h1>'+(trash||moving?'':'<a class="button" href="'+(places.length?'/leltar/uj'+sourceQuery:'/leltar/beallitasok')+'">'+(places.length?'Új tárgy':'Első helyszín felvétele')+'</a>')+'</div><div class="bar"><label>Helyszín<select id="locationFilter"><option value="">Minden helyszín</option>'+options()+'</select></label>'+(!sale&&!trash&&!moving?'<label><span><input type="checkbox" id="saleFilter"> Csak eladó tárgyak</span></label>':'')+'</div><div class="cards"></div>';
    const filter=content.querySelector('#locationFilter');
    filter.value=selectedPlace;
    const draw=()=>{
        const cards=content.querySelector('.cards');
        const visible=rows.filter(r=>(!moving || actions.some(a=>a.id===r.action_id && a.kind==='MOVE')) && (!filter.value||r.location_id===filter.value)&&(!(sale||document.querySelector('#saleFilter')?.checked)||r.status!=='KEEP'));
        cards.innerHTML=visible.map(r=>'<article class="card '+(r.status==='SOLD'?'sold':'')+'">'+(r.cover?'<img alt="" src="/api/photos/'+esc(r.cover)+'/small">':'')+'<span class="tag">'+esc(moving ? actions.find(a=>a.id===r.action_id)?.name : actionLabel(r))+'</span><h2>'+esc(r.name)+'</h2><p>'+esc(r.place)+(moving?' → '+esc(places.find(p=>p.id===r.destination_id)?.name):'')+'</p>'+(trash?'<button data-action="restore">Visszaállítás</button> <button data-action="purge">Végleges törlés</button>':'<a href="/leltar/'+esc(r.id)+sourceQuery+'">Adatlap</a> '+(moving?'<button class="primary" data-action="move">Költöztetés végrehajtása</button>':'<button data-action="trash">Lomtárba</button>'))+'</article>').join('');
        if(!visible.length) cards.innerHTML='<p class="empty">'+(moving?'Nincs költöztetésre kijelölt tárgy ezen a listán.':trash?'A Lomtár üres.':'Nincs megjeleníthető tárgy.')+'</p>';
        [...cards.querySelectorAll('article')].forEach((card,i)=>{
            for(const b of card.querySelectorAll('button')) b.onclick=async()=>{
                const action=b.dataset.action,r=visible[i];
                if(action==='purge'&&!confirm('Végleg törlöd a következő tárgyat és az összes fotóját? '+r.name))return;
                b.disabled=true;
                try {
                    await api('items/'+r.id+'/'+action+'?version='+r.version,{method:'POST'});
                    await list(filter.value);
                    if(action==='move'){
                        message.textContent=r.name+' áthelyezve: '+places.find(p=>p.id===r.destination_id)?.name;
                        message.hidden=false;
                    }
                } catch(e){b.disabled=false;warn(e);}
            };
        });
    };
    content.querySelector('.bar').onchange=draw;draw();
}
function editor(item) {
    content.replaceChildren(document.querySelector('#editor').content.cloneNode(true));
    const form=content.querySelector('form'), fields=form.elements;
    const source=new URLSearchParams(location.search).get('from');
    const returnPath=['koltoztetes','eladas'].includes(source)?'/leltar/'+source:'/leltar';
    const id=item.id || crypto.randomUUID();
    let version=item.version||0, photos=item.photos||[], pending=[], cover=item.cover||'', baseline;
    const keys=['actionId','destination','name','description','location','status','asking','link','actual','soldDate'];
    const snapshot=()=>JSON.stringify([...keys.map(k=>fields[k].value),cover,pending.map(p=>p.key)]);
    const values=r=>({...r,status:r.status==='KEEP'?'WAIT':r.status,actionId:r.action_id, destination:r.destination_id,location:r.location_id,soldDate:r.sold_date?.slice(0,10)});
    fields.location.innerHTML='<option value="">Válassz helyszínt</option>'+options();
    fields.actionId.innerHTML='<option value="">Nincs kijelölve</option>'+actions.filter(a=>a.active||a.id===item.action_id).map(a=>'<option value="'+esc(a.id)+'">'+esc(a.name)+'</option>').join('');
    fields.destination.innerHTML='<option value="">Válassz célhelyszínt</option>'+options();
    function populate(row) {
        const v=values(row);
        for(const key of keys) fields[key].value=key==='status' && (!v.status||v.status==='KEEP')?'WAIT':v[key]??(key==='actionId'&&!item.id&&actions.some(a=>a.id==='KEEP'&&a.active)?'KEEP':'');
    }
    function visibility(){
        const kind=actions.find(a=>a.id===fields.actionId.value)?.kind;
        form.querySelectorAll('[data-sale]').forEach(el=>el.hidden=kind!=='SALE');
        form.querySelector('[data-move]').hidden=kind!=='MOVE';
        fields.destination.required=kind==='MOVE';
        for(const option of fields.destination.options)option.disabled=!!option.value&&option.value===fields.location.value;
        form.querySelector('[data-complete]').hidden=kind!=='MOVE'||!fields.destination.value||fields.destination.value===fields.location.value;
        form.querySelectorAll('[data-sold]').forEach(el=>el.hidden=kind!=='SALE'||fields.status.value!=='SOLD');
    }
    function gallery(){
        const rows=[...photos.map(p=>({key:p.id,label:p.filename,src:'/api/photos/'+p.id+'/small'})),...pending];
        if(!rows.some(r=>r.key===cover)) cover=rows[0]?.key||'';
        const host=form.querySelector('#gallery');host.replaceChildren();
        for(const r of rows){
            const figure=document.createElement('figure');
            figure.innerHTML=(r.file?'':'<a target="_blank" rel="noopener" href="/api/photos/'+esc(r.key)+'/original">')+'<img alt="'+esc(r.label)+'" src="'+esc(r.src)+'">'+(r.file?'':'</a>')+'<figcaption>'+esc(r.label)+'</figcaption><button type="button" class="cover">'+(r.key===cover?'Borítókép':'Legyen borítókép')+'</button>'+(r.file?' <button class="remove" type="button">Kivétel</button>':'');
            figure.querySelector('.cover').onclick=()=>{cover=r.key;gallery();};
            if(r.file) figure.querySelector('.remove').onclick=()=>{pending=pending.filter(p=>p!==r);URL.revokeObjectURL(r.src);gallery();};
            host.append(figure);
        }
    }
    populate(item);visibility();gallery();baseline=snapshot();dirty=()=>snapshot()!==baseline;
    fields.status.onchange=visibility;
    fields.actionId.onchange=()=>{fields.destination.value='';visibility();};
    fields.location.onchange=()=>{if(fields.destination.value===fields.location.value)fields.destination.value='';visibility();};
    fields.destination.onchange=visibility;
    form.querySelector('[data-complete]').onclick=()=>{fields.location.value=fields.destination.value;fields.destination.value='';fields.actionId.value='';visibility();};
    form.querySelector('#picker').onchange=e=>{
        for(const file of e.target.files) pending.push({key:crypto.randomUUID(),file,label:file.name,src:URL.createObjectURL(file)});
        e.target.value='';gallery();
    };
    async function conflict() {
        const fresh=await api('items/'+id);
        const panel=form.querySelector('#conflict');panel.hidden=false;
        if(fresh.trashed){panel.textContent='A tárgy közben a Lomtárba került. Visszaállítás után menthető.';return;}
        const v=values(fresh), labels={actionId:'Művelet',destination:'Célhelyszín',name:'Név',description:'Leírás',location:'Helyszín',status:'Állapot',asking:'Irányár',link:'Link',actual:'Tényleges ár',soldDate:'Eladás dátuma'};
        const display=(k,x)=>k==='actionId'?actions.find(a=>a.id===x)?.name||'Nincs kijelölve':['location','destination'].includes(k)?places.find(p=>p.id===x)?.name||x:k==='status'?statuses[x]:x;
        panel.innerHTML='<h2>Az adatlap közben megváltozott</h2><p>Válaszd ki a megtartandó értékeket, majd ments újra.</p>';
        for(const k of keys){
            const current=String(v[k]??'');
            if(current===fields[k].value)continue;
            const label=document.createElement('label');label.textContent=labels[k];
            const select=document.createElement('select');
            select.add(new Option('Saját: '+display(k,fields[k].value),fields[k].value));
            select.add(new Option('Mentett: '+display(k,current),current));
            select.onchange=()=>{fields[k].value=select.value;visibility();};
            label.append(select);panel.append(label);
        }
        photos=fresh.photos;gallery();
        const info=document.createElement('p');info.textContent='Frissen mentett borítókép: '+(photos.find(p=>p.id===fresh.cover)?.filename||'nincs')+'. A fotóknál választhatsz borítóképet.';panel.append(info);
        version=fresh.version;panel.scrollIntoView();
    }
    form.onsubmit=async e=>{
        e.preventDefault();message.hidden=true;
        const draft=Object.fromEntries(keys.map(k=>[k,fields[k].value]));
        Object.assign(draft,{id,version,cover:pending.some(p=>p.key===cover)?'new:'+pending.findIndex(p=>p.key===cover):cover});
        for(const k of ['asking','actual','soldDate'])if(!draft[k])draft[k]=null;
        const kind=actions.find(a=>a.id===draft.actionId)?.kind;
        if(kind!=='MOVE') draft.destination=null;
        if(kind!=='SALE') draft.status='KEEP';
        if(draft.status==='KEEP')Object.assign(draft,{link:'',asking:null});
        if(draft.status!=='SOLD')Object.assign(draft,{actual:null,soldDate:null});
        const data=new FormData();data.append('draft',new Blob([JSON.stringify(draft)],{type:'application/json'}));
        pending.forEach(p=>data.append('files',p.file));
        form.inert=true;
        try{
            await api('items',{method:'POST',body:data});dirty=()=>false;location.replace(returnPath);
        }catch(err){
            warn(err);
            if(err.status===409)try{await conflict();}catch(other){warn(other);}
        }finally{form.inert=false;}
    };
}
(async()=>{
    [places,actions]=await Promise.all([api('places'),api('actions')]);
    document.querySelectorAll('.tabs a').forEach(a=>{if(a.pathname===location.pathname)a.setAttribute('aria-current','page');});
    if(['helyszinek','beallitasok'].includes(page))showPlaces();
    else if(['targyak','eladas','koltoztetes','lomtar'].includes(page))await list();
    else editor(page==='uj'?{}:await api('items/'+page));
})().catch(warn);
