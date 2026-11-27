'use strict';
const content = document.querySelector('#content');
const message = document.querySelector('#message');
const confirmation = document.querySelector('#confirmation');
const appUrl = AccountAuth.url;
const page = location.pathname.slice(appUrl('leltar/').length) || 'targyak';
const moving = page === 'koltoztetes';
const sale = page === 'eladas', trash = page === 'lomtar';
const statuses = {KEEP:'Megtartjuk', WAIT:'Meghirdetésre vár', LISTED:'Meghirdetve', SOLD:'Elkelt'};
let actions = [], places = [], dirty = () => false;
function warn(e) {
    confirmation.hidden = true;
    message.textContent = e.message || String(e);
    message.hidden = false;
    message.scrollIntoView({block:'nearest'});
}
function confirmChange(text) {
    message.hidden = true;
    confirmation.textContent = text;
    confirmation.hidden = false;
}
function esc(v) {return String(v ?? '').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
async function api(path, options) {
    const writing = options?.method && !['GET','HEAD','OPTIONS'].includes(options.method.toUpperCase());
    const authHeaders = writing ? await AccountAuth.writeHeaders() : {};
    let res, raw;
    try {
        res=await fetch(appUrl('api/'+path), {...options, cache:'no-store', headers:{...options?.headers, ...authHeaders, 'X-Otthontar-Request':'1'}});
        raw=await res.text();
    } catch (cause) {
        const error = new Error('Megszakadt a kapcsolat. Próbáld újra.', {cause});
        error.outcomeUnknown = writing;
        throw error;
    }
    if(!res.ok) {
        if(res.status===401)AccountAuth.expired();
        let detail;
        try { detail=JSON.parse(raw).detail; } catch {}
        const error=new Error(detail || 'A művelet sikertelen ('+res.status+'). Próbáld újra.');
        error.status=res.status;
        error.outcomeUnknown=writing && (res.status===408 || res.status>=500);
        error.applicationProblem=res.headers.get('Content-Type')?.startsWith('application/problem+json');
        throw error;
    }
    try { return raw ? JSON.parse(raw) : null; }
    catch (error) { error.outcomeUnknown=writing; throw error; }
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
            try{await post({...a,name,active:enabled});a.active=enabled;a.name=name;input.defaultValue=name;render();confirmChange('Művelet mentve.');}catch(e){warn(e);}
        }
        toggle.onclick=()=>save(!a.active,a.name);
        f.onsubmit=e=>{e.preventDefault();save(a.active,input.value.trim());};
        host.querySelector('.actionlist').append(f);render();
    }
    actions.forEach(addRow);
    create.onsubmit=async e=>{
        e.preventDefault();
        try{const name=create.elements[0].value.trim();await post({name,kind:create.elements[1].value,active:true});
            actions=await api('actions');addRow(actions.find(a=>a.name===name));create.reset();confirmChange('Művelet hozzáadva.');
        }catch(err){warn(err);}
    };
}
function placeForm(p) {
    return '<form class="place" data-id="'+esc(p.id)+'"><input aria-label="Helyszín neve" required maxlength="180" value="'+esc(p.name)+'"><button'+(p.id?'':' class="primary"')+'>'+ (p.id?'Mentés':'Hozzáadás')+'</button></form>';
}
function showPlaces() {
    content.innerHTML='<div class="heading"><h1>Beállítások</h1></div><section id="places" class="panel"><h2>Új helyszín</h2>'+placeForm({})+'<h2>Helyszínek átnevezése</h2>'+places.map(placeForm).join('')+'</section><section id="actionSettings" class="panel"></section>';
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
            confirmChange('Helyszín mentve.');
        } catch(err){warn(err);}
    };
}
function showSaleDialog(item,filters) {
    const dialog=document.querySelector('#sale-dialog');
    const form=dialog.querySelector('form');
    const error=form.querySelector('[role=alert]');
    const today=new Date();
    today.setMinutes(today.getMinutes()-today.getTimezoneOffset());
    form.reset();
    form.elements.soldDate.value=today.toISOString().slice(0,10);
    form.querySelector('.item-name').textContent=item.name;
    error.hidden=true;
    form.querySelector('[type=button]').onclick=()=>dialog.close();
    dialog.oncancel=e=>{if(form.inert)e.preventDefault();};
    form.onsubmit=async e=>{
        e.preventDefault();
        form.inert=true;
        try {
            const body={version:item.version,actual:form.elements.actual.value,soldDate:form.elements.soldDate.value};
            await api('items/'+item.id+'/sold',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
        } catch(err){
            error.textContent=err.message;
            error.hidden=false;
            return;
        } finally {form.inert=false;}
        dialog.close();
        try {
            await list(filters);
            confirmChange('Eladás rögzítve.');
            content.focus();
        } catch(err) { warn(err); }
    };
    dialog.showModal();
}
function searchable(value) {
    return String(value ?? '').toLocaleLowerCase('hu').normalize('NFD').replace(/\p{M}/gu, '');
}

function cardMarkup(item, sourceQuery) {
    const place = moving ? `${item.place} → ${places.find(p => p.id === item.destination_id)?.name ?? ''}` : item.place;
    const state = moving ? actions.find(a => a.id === item.action_id)?.name : actionLabel(item);
    const thumb = item.cover
        ? `<img loading="lazy" alt="" src="${appUrl('api/photos/')}${esc(item.cover)}/small">`
        : '<span aria-hidden="true">▧</span>';
    const cardActions = trash
        ? '<button data-action="restore">Visszaállítás</button><button class="quiet danger" data-action="purge">Végleges törlés</button>'
        : `<a class="card-link" href="${appUrl('leltar/')}${esc(item.id)}${sourceQuery}">Adatlap</a>` + (
            moving ? '<button class="primary" data-action="move">Költöztetés végrehajtása</button>'
                : '<button class="quiet danger" data-action="trash">Lomtárba</button>'
        );
    return `<article class="card${item.status === 'SOLD' ? ' sold' : ''}">
        <div class="card-thumb">${thumb}</div>
        <div class="card-details"><h2>${esc(item.name)}</h2><p>${esc(place)}</p><span class="tag">${esc(state)}</span></div>
        <div class="actions card-actions">
            ${sale && item.status !== 'SOLD' ? '<button class="primary" data-action="sold">Eladva</button>' : ''}
            ${cardActions}
        </div>
    </article>`;
}

async function list(filters={}) {
    const rows=await api('items?trash='+trash);
    const sourceQuery=(sale||moving)?'?from='+page:'';
    const title=moving?'Költöztetés':trash?'Lomtár':sale?'Eladás':'Tárgyak';
    const create = !trash && !moving ? `<a class="button" href="${places.length ? appUrl('leltar/uj') + sourceQuery : appUrl('leltar/beallitasok')}">
        <span aria-hidden="true">＋</span>${places.length ? 'Új tárgy' : 'Első helyszín felvétele'}</a>` : '';
    const overview = page === 'targyak';
    const summary = overview ? `<dl class="inventory-summary" aria-label="Leltár áttekintése">
        <div><dt>Tárgy a leltárban</dt><dd>${rows.length}</dd></div>
        <div><dt>Helyszín</dt><dd>${places.length}</dd></div>
        <div><dt>Borítóképes tárgy</dt><dd>${rows.filter(row => row.cover).length}</dd></div>
    </dl>` : '';
    const viewPicker = overview ? `<div class="view-picker" role="group" aria-label="Tárgylista nézete">
        <button type="button" data-view="gallery">Fotónézet</button>
        <button type="button" data-view="list">Lista</button>
    </div>` : '';
    content.innerHTML=`<div class="heading${overview ? ' inventory-heading' : ''}"><h1>${title}</h1>${create}</div>
        ${summary}
        <div class="bar" role="group" aria-label="Keresés és szűrés">
            <label>Keresés<input id="itemSearch" type="search" placeholder="Tárgy neve vagy helyszíne" autocomplete="off"></label>
            <label>Helyszín<select id="locationFilter"><option value="">Minden helyszín</option>${options()}</select></label>
            ${!sale && !trash && !moving ? '<label class="checkbox"><input type="checkbox" id="saleFilter"> Csak eladó tárgyak</label>' : ''}
        </div>
        <div class="results-row"><p class="results" role="status"></p>${viewPicker}</div>
        <div class="cards"></div>`;
    const filter = content.querySelector('#locationFilter');
    const search = content.querySelector('#itemSearch');
    const saleFilter = content.querySelector('#saleFilter');
    const results = content.querySelector('.results');
    const cards = content.querySelector('.cards');
    if (overview) {
        const buttons = [...content.querySelectorAll('.view-picker button')];
        const key = appUrl('inventory-view');
        const selectView = view => {
            cards.dataset.view = view;
            for (const button of buttons) {
                button.setAttribute('aria-pressed', String(button.dataset.view === view));
            }
        };
        let savedView;
        try { savedView = sessionStorage.getItem(key); } catch { /* Storage may be unavailable. */ }
        selectView(savedView === 'list' ? 'list' : 'gallery');
        for (const button of buttons) button.addEventListener('click', () => {
            selectView(button.dataset.view);
            try { sessionStorage.setItem(key, button.dataset.view); } catch { /* Keep the current view. */ }
        });
    }
    filter.value = filters.place ?? '';
    search.value = filters.query ?? '';
    if (saleFilter) saleFilter.checked = filters.onlySale ?? false;
    const currentFilters = () => ({place: filter.value, query: search.value, onlySale: saleFilter?.checked});
    const draw=()=>{
        const query = searchable(search.value.trim());
        const visible = rows.filter(row =>
            (!moving || actions.some(action => action.id === row.action_id && action.kind === 'MOVE')) &&
            (!filter.value || row.location_id === filter.value) &&
            (!(sale || saleFilter?.checked) || row.status !== 'KEEP') &&
            (!query || [row.name, row.place].some(value => searchable(value).includes(query)))
        );
        results.textContent = `${visible.length} tárgy`;
        cards.innerHTML = visible.map(row => cardMarkup(row, sourceQuery)).join('');
        if (!visible.length) {
            let empty = 'Nincs megjeleníthető tárgy.';
            if (query || filter.value || saleFilter?.checked) empty = 'A keresés és a szűrők alapján nincs találat.';
            else if (moving) empty = 'Nincs költöztetésre kijelölt tárgy.';
            else if (trash) empty = 'A lomtár üres.';
            cards.innerHTML = `<p class="empty">${empty}</p>`;
        }
        [...cards.querySelectorAll('article')].forEach((card,i)=>{
            for(const b of card.querySelectorAll('button')) b.onclick=async()=>{
                const action=b.dataset.action,r=visible[i];
                if(action==='sold'){showSaleDialog(r,currentFilters());return;}
                if(action==='purge'&&!confirm('Végleg törlöd a következő tárgyat és az összes fotóját? '+r.name))return;
                b.disabled=true;
                try {
                    await api('items/'+r.id+'/'+action+'?version='+r.version,{method:'POST'});
                    await list(currentFilters());
                    const labels = {move: 'Tárgy áthelyezve.', trash: 'Tárgy a lomtárba helyezve.',
                        restore: 'Tárgy visszaállítva.', purge: 'Tárgy végleg törölve.'};
                    confirmChange(labels[action]);
                    content.focus();
                } catch(e){b.disabled=false;warn(e);}
            };
        });
    };
    filter.addEventListener('change', draw);
    saleFilter?.addEventListener('change', draw);
    search.addEventListener('input', draw);
    draw();
}
function editor(item) {
    content.replaceChildren(document.querySelector('#editor').content.cloneNode(true));
    const form=content.querySelector('form'), fields=form.elements;
    const source=new URLSearchParams(location.search).get('from');
    const returnPath=['koltoztetes','eladas'].includes(source)?appUrl('leltar/'+source):appUrl('leltar');
    form.querySelector('.back-link').href = returnPath;
    form.querySelector('h1').textContent = item.id ? item.name : 'Új tárgy';
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
        const rows=[...photos.map(p=>({key:p.id,label:p.filename,src:appUrl('api/photos/'+p.id+'/small')})),...pending];
        if(!rows.some(r=>r.key===cover)) cover=rows[0]?.key||'';
        const host=form.querySelector('#gallery');host.replaceChildren();
        for(const r of rows){
            const figure=document.createElement('figure');
            const selected = r.key === cover;
            const image = `<img alt="${esc(r.label)}" src="${esc(r.src)}">`;
            const preview = r.original ? `<div class="photo-preview">${image}</div>`
                : `<a class="photo-preview" target="_blank" rel="noopener" href="${appUrl('api/photos/')}${esc(r.key)}/original">${image}</a>`;
            figure.innerHTML = preview +
                `<figcaption>${esc(r.label)}</figcaption><div class="actions"><button type="button" class="cover" aria-pressed="${selected}">${selected ? 'Borítókép' : 'Legyen borítókép'}</button>` +
                (r.original ? '<button class="remove quiet danger" type="button">Kivétel</button>' : '') + '</div>';
            if(r.error){const error=document.createElement('p');error.setAttribute('role','alert');error.textContent=r.error;figure.append(error);figure.querySelector('.cover').disabled=true;}
            figure.querySelector('.cover').onclick=()=>{
                cover = r.key;
                for (const [index, button] of [...host.querySelectorAll('.cover')].entries()) {
                    const pressed = rows[index].key === cover;
                    button.setAttribute('aria-pressed', String(pressed));
                    button.textContent = pressed ? 'Borítókép' : 'Legyen borítókép';
                }
            };
            if(r.original) figure.querySelector('.remove').onclick=()=>{
                pending=pending.filter(p=>p!==r);URL.revokeObjectURL(r.src);gallery();
                form.querySelector('#picker').focus();
            };
            host.append(figure);
        }
    }
    populate(item);visibility();gallery();baseline=snapshot();dirty=()=>snapshot()!==baseline;
    fields.status.onchange=visibility;
    fields.actionId.onchange=()=>{fields.destination.value='';visibility();};
    fields.location.onchange=()=>{if(fields.destination.value===fields.location.value)fields.destination.value='';visibility();};
    fields.destination.onchange=visibility;
    form.querySelector('[data-complete]').onclick=()=>{fields.location.value=fields.destination.value;fields.destination.value='';fields.actionId.value='';visibility();};
    let preparing = false, saveAttempt = null;
    async function selectPhotos(e) {
        const files = [...e.target.files];
        preparing = true; form.inert = true;
        try {
            for (const original of files) {
                const entry = {key:crypto.randomUUID(),original,label:original.name,src:URL.createObjectURL(original)};
                pending.push(entry); // retain original even if conversion fails
                try { entry.file = await OtthontarPhotos.prepare(original); }
                catch (err) { entry.error = err.message; warn(err); }
            }
        } finally { e.target.value=''; preparing=false;form.inert=false;gallery(); }
    }
    form.querySelector('#picker').onchange=selectPhotos;
    form.querySelector('#camera').onchange=selectPhotos;
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
        if(preparing||form.inert)return;
        if(!saveAttempt){
            if(pending.some(p=>p.error)){warn(new Error('A hibás képet vedd ki, vagy válassz helyette JPEG-et. A többi mező és fotó megmarad.'));return;}
            const draft=Object.fromEntries(keys.map(k=>[k,fields[k].value]));
            Object.assign(draft,{id,version,cover:pending.some(p=>p.key===cover)?'new:'+pending.findIndex(p=>p.key===cover):cover});
            for(const k of ['asking','actual','soldDate'])if(!draft[k])draft[k]=null;
            const kind=actions.find(a=>a.id===draft.actionId)?.kind;
            if(kind!=='MOVE') draft.destination=null;
            if(kind!=='SALE') draft.status='KEEP';
            if(draft.status==='KEEP')Object.assign(draft,{link:'',asking:null});
            if(draft.status!=='SOLD')Object.assign(draft,{actual:null,soldDate:null});
            try{OtthontarPhotos.validateTotal(pending.map(p=>p.file),new Blob([JSON.stringify(draft)]).size);}catch(err){warn(err);return;}
            const data=new FormData();data.append('draft',new Blob([JSON.stringify(draft)],{type:'application/json'}));
            pending.forEach(p=>data.append('files',p.file));
            saveAttempt={id:crypto.randomUUID(),data,uncertain:false};
        }
        form.inert=true;
        try{
            await api('items',{method:'POST',body:saveAttempt.data,headers:{'X-Otthontar-Save-Id':saveAttempt.id}});
            dirty=()=>false;location.replace(returnPath);
        }catch(err){
            saveAttempt.uncertain ||= err.outcomeUnknown === true;
            // Keep the exact submitted data while its outcome is uncertain. An application-level
            // rejection of that same request can resolve the uncertainty and restore editing.
            const rejected=err.applicationProblem && [404,409,422].includes(err.status);
            if(!saveAttempt.uncertain || rejected)saveAttempt=null;
            warn(saveAttempt ? new Error(err.message+' A mentés eredménye még bizonytalan. A Mentés gombbal ugyanazt a kérést próbálhatod újra.') : err);
            if(!saveAttempt && err.status===409)try{await conflict();}catch(other){warn(other);}
        }finally{
            form.querySelectorAll('fieldset, #conflict').forEach(part=>{part.inert=!!saveAttempt;});
            form.inert=false;
        }
    };
}
(async()=>{
    const source = new URLSearchParams(location.search).get('from');
    const section = ['eladas','koltoztetes','lomtar','beallitasok'].includes(page) ? page
        : page === 'helyszinek' ? 'beallitasok' : ['eladas','koltoztetes'].includes(source) ? source : '';
    const activePath = appUrl(section ? 'leltar/'+section : 'leltar');
    document.querySelectorAll('.tabs a').forEach(link=>{
        if(link.pathname===activePath)link.setAttribute('aria-current','page');
    });
    await AccountAuth.init();
    [places,actions]=await Promise.all([api('places'),api('actions')]);
    if(['helyszinek','beallitasok'].includes(page))showPlaces();
    else if(['targyak','eladas','koltoztetes','lomtar'].includes(page))await list();
    else editor(page==='uj'?{}:await api('items/'+page));
})().catch(error=>{
    warn(error);
    content.innerHTML='<div class="empty"><p>A leltár nem töltődött be.</p><a class="card-link" href="">Újrapróbálás</a></div>';
}).finally(()=>content.removeAttribute('aria-busy'));
document.querySelector('#logout-form')?.addEventListener('submit',event=>{
    if(dirty()&&!confirm('A nyitott, még nem mentett módosítások és fotók elvesznek. Biztosan kijelentkezel?'))event.preventDefault();
});
document.addEventListener('otthontar-reauthenticated',()=>{
    if (content.querySelector('#itemform')) confirmChange('Újra bejelentkeztél. A piszkozat megmaradt. A mentéshez nyomd meg újra a Mentés gombot.');
    else location.reload();
});
