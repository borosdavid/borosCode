'use strict';
const content = document.querySelector('#content');
const message = document.querySelector('#message');
const page = location.pathname.split('/')[2] || 'targyak';
const sale = page === 'eladas', trash = page === 'lomtar';
const statuses = {KEEP:'Megtartjuk', WAIT:'Meghirdetésre vár', LISTED:'Meghirdetve', SOLD:'Elkelt'};
let places = [], dirty = () => false;
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
function placeForm(p) {
    return '<form class="place" data-id="'+esc(p.id)+'"><input aria-label="Helyszín neve" required maxlength="180" value="'+esc(p.name)+'"><button>Mentés</button></form>';
}
function showPlaces() {
    content.innerHTML='<h1>Helyszínek</h1><h2>Új helyszín</h2>'+placeForm({})+'<h2>Helyszínek átnevezése</h2>'+places.map(placeForm).join('');
    const forms=[...content.querySelectorAll('form')];
    dirty=()=>forms.some(f=>f.elements[0].value!==f.elements[0].defaultValue);
    for(const f of forms) f.onsubmit=async e=>{
        e.preventDefault();
        try {
            const name=f.elements[0].value.trim();
            await api('places',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({id:f.dataset.id||null,name})});
            if(f.dataset.id) f.elements[0].defaultValue=name;
            else {f.elements[0].value='';}
            const drafts=new Map(forms.filter(x=>x!==f && x.elements[0].value!==x.elements[0].defaultValue).map(x=>[x.dataset.id,x.elements[0].value]));
            places=await api('places');showPlaces();
            for(const form of content.querySelectorAll('form')) if(drafts.has(form.dataset.id)) form.elements[0].value=drafts.get(form.dataset.id);
            message.textContent='Helyszín mentve.';message.hidden=false;
        } catch(err){warn(err);}
    };
}
async function list() {
    const rows=await api('items?trash='+trash);
    const title=trash?'Lomtár':sale?'Eladás':'Tárgyak';
    content.innerHTML='<div class="heading"><h1>'+title+'</h1>'+(trash?'':'<a class="button" href="'+(places.length?'/leltar/uj':'/leltar/helyszinek')+'">'+(places.length?'Új tárgy':'Első helyszín felvétele')+'</a>')+'</div><div class="bar"><label>Helyszín<select id="locationFilter"><option value="">Minden helyszín</option>'+options()+'</select></label>'+(!sale&&!trash?'<label><span><input type="checkbox" id="saleFilter"> Csak eladó tárgyak</span></label>':'')+'</div><div class="cards"></div>';
    const filter=content.querySelector('#locationFilter');
    const draw=()=>{
        const cards=content.querySelector('.cards');
        const visible=rows.filter(r=>(!filter.value||r.location_id===filter.value)&&(!(sale||document.querySelector('#saleFilter')?.checked)||r.status!=='KEEP'));
        cards.innerHTML=visible.map(r=>'<article class="card '+(r.status==='SOLD'?'sold':'')+'">'+(r.cover?'<img alt="" src="/api/photos/'+esc(r.cover)+'/small">':'')+'<span class="tag">'+esc(statuses[r.status])+'</span><h2>'+esc(r.name)+'</h2><p>'+esc(r.place)+'</p>'+(trash?'<button data-action="restore">Visszaállítás</button> <button data-action="purge">Végleges törlés</button>':'<a href="/leltar/'+esc(r.id)+'">Adatlap</a> <button data-action="trash">Lomtárba</button>')+'</article>').join('');
        if(!visible.length) cards.innerHTML='<p class="empty">'+(trash?'A Lomtár üres.':'Nincs megjeleníthető tárgy.')+'</p>';
        [...cards.querySelectorAll('article')].forEach((card,i)=>{
            for(const b of card.querySelectorAll('button')) b.onclick=async()=>{
                const action=b.dataset.action,r=visible[i];
                if(action==='purge'&&!confirm('Végleg törlöd a következő tárgyat és az összes fotóját? '+r.name))return;
                try{await api('items/'+r.id+'/'+action+'?version='+r.version,{method:'POST'});await list();}catch(e){warn(e);}
            };
        });
    };
    content.querySelector('.bar').onchange=draw;draw();
}
function editor(item) {
    content.replaceChildren(document.querySelector('#editor').content.cloneNode(true));
    const form=content.querySelector('form'), fields=form.elements;
    const id=item.id || crypto.randomUUID();
    let version=item.version||0, photos=item.photos||[], pending=[], cover=item.cover||'', baseline;
    const keys=['name','description','location','status','asking','link','actual','soldDate'];
    const snapshot=()=>JSON.stringify([...keys.map(k=>fields[k].value),cover,pending.map(p=>p.key)]);
    const values=r=>({...r,location:r.location_id,soldDate:r.sold_date?.slice(0,10)});
    fields.location.innerHTML='<option value="">Válassz helyszínt</option>'+options();
    function populate(row) {
        const v=values(row);
        for(const key of keys) fields[key].value=v[key]??(key==='status'?'KEEP':'');
    }
    function visibility(){
        form.querySelectorAll('[data-sale]').forEach(el=>el.hidden=fields.status.value==='KEEP');
        form.querySelectorAll('[data-sold]').forEach(el=>el.hidden=fields.status.value!=='SOLD');
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
    form.querySelector('#picker').onchange=e=>{
        for(const file of e.target.files) pending.push({key:crypto.randomUUID(),file,label:file.name,src:URL.createObjectURL(file)});
        e.target.value='';gallery();
    };
    async function conflict() {
        const fresh=await api('items/'+id);
        const panel=form.querySelector('#conflict');panel.hidden=false;
        if(fresh.trashed){panel.textContent='A tárgy közben a Lomtárba került. Visszaállítás után menthető.';return;}
        const v=values(fresh), labels={name:'Név',description:'Leírás',location:'Helyszín',status:'Állapot',asking:'Irányár',link:'Link',actual:'Tényleges ár',soldDate:'Eladás dátuma'};
        const display=(k,x)=>k==='location'?places.find(p=>p.id===x)?.name||x:k==='status'?statuses[x]:x;
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
        if(draft.status==='KEEP')Object.assign(draft,{link:'',asking:null});
        if(draft.status!=='SOLD')Object.assign(draft,{actual:null,soldDate:null});
        const data=new FormData();data.append('draft',new Blob([JSON.stringify(draft)],{type:'application/json'}));
        pending.forEach(p=>data.append('files',p.file));
        form.inert=true;
        try{
            await api('items',{method:'POST',body:data});dirty=()=>false;location.assign('/leltar/'+id);
        }catch(err){
            warn(err);
            if(err.status===409)try{await conflict();}catch(other){warn(other);}
        }finally{form.inert=false;}
    };
}
(async()=>{
    places=await api('places');
    document.querySelectorAll('.tabs a').forEach(a=>{if(a.pathname===location.pathname)a.setAttribute('aria-current','page');});
    if(page==='helyszinek')showPlaces();
    else if(['targyak','eladas','lomtar'].includes(page))await list();
    else editor(page==='uj'?{}:await api('items/'+page));
})().catch(warn);
