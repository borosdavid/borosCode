// Isolated browser contract tests. No personal data, database or Windows profile is used.
const {chromium} = require(process.env.PLAYWRIGHT_CORE || 'playwright-core');
const assert = require('node:assert/strict');
const http = require('node:http');
const fs = require('node:fs/promises');
const path = require('node:path');
const root = path.resolve(__dirname,'../inventory/src/main/resources');
const types = {'.js':'text/javascript','.css':'text/css','.html':'text/html','.webmanifest':'application/manifest+json','.png':'image/png'};
let expiredSession = false, fixtureUser = 'account-1';
let failSave = true, savedBody = '', savedHeader = '', nextWorker = false, failAsset = false;
let dropSaveResponse = false, serverUnavailable = false, saveId;
const server = http.createServer(async (req,res) => {
    if(serverUnavailable){req.socket.destroy();return;}
    const url = new URL(req.url,'http://localhost');
    try {
        res.setHeader('Cache-Control','no-store');
        if(url.pathname==='/auth/csrf') {res.setHeader('Content-Type','application/json');res.end(JSON.stringify({headerName:'X-CSRF-TOKEN',token:'fixture-token'}));return;}
        if(url.pathname==='/login'&&req.method==='POST') {expiredSession=false;res.writeHead(204);res.end();return;}
        if(url.pathname.startsWith('/api/')) {
            if(expiredSession){res.writeHead(401,{'Content-Type':'application/json'});res.end(JSON.stringify({detail:'Jelentkezz be újra.'}));return;}
            if(url.pathname==='/api/session'){res.setHeader('Content-Type','application/json');res.end(JSON.stringify({id:fixtureUser,username:'alice',headerName:'X-CSRF-TOKEN',token:'fixture-token'}));return;}

            if(req.method==='POST') {
                savedHeader=req.headers['x-otthontar-request'];
                saveId=req.headers['x-otthontar-save-id'];
                assert.match(saveId,/^[a-f0-9-]{36}$/);
                assert.equal(req.headers['x-csrf-token'],'fixture-token');assert.equal(req.headers['x-otthontar-user'],'account-1');
                for await(const chunk of req) savedBody+=chunk.toString('latin1');
                if(dropSaveResponse){req.socket.destroy();return;}
                res.writeHead(failSave?413:200,{'Content-Type':'application/json'});
                res.end(JSON.stringify(failSave?{detail:'Túl nagy kérés. A piszkozat megmarad.'}:{}));return;
            }
            const data=url.pathname==='/api/places'?[{id:'place-1',name:'Lakás'}]:url.pathname==='/api/actions'?[{id:'KEEP',name:'Megtartjuk',kind:'CUSTOM',active:true}]:[];
            res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify(data));return;
        }
        if(url.pathname==='/leltar'||url.pathname.startsWith('/leltar/')) {
            let html = await fs.readFile(path.join(root,'templates/inventory/index.html'),'utf8');
            html=html.replace(/<header th:replace="[^"]*"><\/header>/,'<header><a class="brand" href="/leltar">Otthontár</a></header>');
            res.writeHead(200,{'Content-Type':'text/html'});res.end(html);return;
        }
        if(failAsset && url.pathname==='/manifest.webmanifest'){res.writeHead(503);res.end();return;}
        const file=path.join(root,'static',url.pathname);
        let data=await fs.readFile(file);
        if(url.pathname==='/sw.js'&&nextWorker)data=Buffer.from(data.toString().replace('otthontar-shell-v3','otthontar-shell-v4'));
        res.writeHead(200,{'Content-Type':types[path.extname(file)]||'application/octet-stream'});res.end(data);
    }catch{res.writeHead(404);res.end();}
});
(async()=>{
    await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
    const origin='http://127.0.0.1:'+server.address().port;
    const executablePath=process.env.BROWSER_EXE || (process.platform==='win32'
        ? 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe' : undefined);
    const browser=await chromium.launch({executablePath,headless:true,args:['--no-sandbox']});
    const context=await browser.newContext({viewport:{width:390,height:844},isMobile:true,hasTouch:true,deviceScaleFactor:3});
    const page=await context.newPage();
    const errors=[];page.on('pageerror',e=>errors.push(e.message));
    try {
        await page.goto(origin+'/leltar');
        await page.getByRole('heading',{name:'Tárgyak',exact:true}).waitFor();
        await page.evaluate(()=>navigator.serviceWorker.ready);
        await page.waitForFunction(()=>navigator.serviceWorker.controller);
        const manifest=await (await fetch(origin+'/manifest.webmanifest')).json();
        assert.equal(manifest.start_url,'/leltar');assert.equal(manifest.scope,'/leltar');
        for(const icon of manifest.icons)assert.equal((await fetch(origin+icon.src)).status,200);
        assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
        await page.goto(origin+'/leltar/uj');
        await page.locator('[name=name]').fill('Mobilos teszt');
        await page.locator('[name=location]').selectOption('place-1');
        assert.equal(await page.locator('#camera').getAttribute('capture'),'environment');
        const png=await page.evaluate(()=>{const c=document.createElement('canvas');c.width=32;c.height=24;return c.toDataURL('image/png').split(',')[1];});
        await page.locator('#picker').setInputFiles({name:'test.png',mimeType:'image/png',buffer:Buffer.from(png,'base64')});
        await page.locator('#gallery figure').waitFor();
        await page.getByRole('button',{name:'Mentés',exact:true}).first().click();
        await page.getByText('Túl nagy kérés. A piszkozat megmarad.',{exact:true}).waitFor();
        assert.equal(await page.locator('[name=name]').inputValue(),'Mobilos teszt');
        assert.equal(await page.locator('#gallery figure').count(),1);
        assert.equal(savedHeader,'1');assert.ok(savedBody.includes('filename="test.jpg"'));assert.ok(savedBody.includes('\xff\xd8\xff'));
        savedBody='';
        await page.locator('#camera').setInputFiles({name:'unsupported.heic',mimeType:'image/heic',buffer:Buffer.from('not-a-supported-image')});
        await page.waitForFunction(()=>document.querySelectorAll('#gallery figure').length===2 && !document.querySelector('form').inert);
        await page.getByRole('button',{name:'Mentés',exact:true}).first().click();
        await page.waitForFunction(()=>document.querySelector('#message').textContent.includes('hibás képet'));
        assert.equal(savedBody,'');assert.equal(await page.locator('[name=name]').inputValue(),'Mobilos teszt');
        await page.locator('.remove').last().click();
        // Validate exact byte limit and aggregate multipart overhead.
        const limits=await page.evaluate(async()=>{
            const blob=new Blob([new Uint8Array(20*1024*1024+1)]);
            let large=false,total=false;
            try{await OtthontarPhotos.prepare(new File([blob],'large.jpg'));}catch{large=true;}
            try{OtthontarPhotos.validateTotal(Array.from({length:10},()=>({size:20*1024*1024})));}catch{total=true;}
            const jpeg=new File([new Uint8Array([255,216,255,217])],'old.jpeg',{type:'image/jpeg'});
            const prepared=await OtthontarPhotos.prepare(jpeg);
            return {large,total,jpeg:Array.from(new Uint8Array(await prepared.arrayBuffer()))};
        });
        assert.equal(limits.large,true);assert.equal(limits.total,true);assert.deepEqual(limits.jpeg,[255,216,255,217]);
        assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
        console.log('OK: mobile layout, manifest, photo conversion, JPEG preservation, HEIC failure and failed-save draft retention, upload limits.');
        expiredSession=true;
        await page.getByRole('button',{name:'Mentés',exact:true}).first().click();
        const reauth=page.getByRole('dialog',{name:'Újbóli bejelentkezés'});
        await reauth.waitFor();
        assert.equal(await page.locator('#itemform [name=name]').inputValue(),'Mobilos teszt');
        assert.equal(await page.locator('#gallery figure').count(),1);
        await reauth.getByLabel('Jelszó',{exact:true}).fill('test-password');
        await reauth.getByRole('button',{name:'Bejelentkezés',exact:true}).click();
        await reauth.waitFor({state:'hidden'});
        assert.equal(await page.locator('#gallery figure').count(),1);
        assert.equal(await page.locator('#itemform [name=name]').inputValue(),'Mobilos teszt');
        assert.equal(savedBody,'');
        fixtureUser='account-2';
        await page.getByRole('button',{name:'Mentés',exact:true}).first().click();
        await reauth.waitFor();
        assert.equal(savedBody,'');
        fixtureUser='account-1';
        await reauth.getByRole('button',{name:'Mégse',exact:true}).click();
        console.log('OK: session expiry and same-account reauthentication preserve draft/photos; changed account cannot upload.');
        failSave=false;dropSaveResponse=true;
        await page.getByRole('button',{name:'Mentés',exact:true}).first().click();
        await page.getByText(/A mentés eredménye még bizonytalan/).waitFor();
        const originalSaveId=saveId;
        const originalDraft=savedBody.match(/\{"actionId"[^\r\n]+/)[0];
        assert.equal(await page.locator('#itemform fieldset[inert]').count(),3);
        dropSaveResponse=false;savedBody='';
        await page.getByRole('button',{name:'Mentés',exact:true}).first().click();
        await page.waitForURL(origin+'/leltar');
        assert.equal(saveId,originalSaveId);
        assert.ok(savedBody.includes(originalDraft));
        assert.ok(savedBody.includes('filename="test.jpg"'));
        await page.getByRole('heading',{name:'Tárgyak',exact:true}).waitFor();
        await page.setViewportSize({width:1280,height:900});
        await page.getByRole('link',{name:'Új tárgy',exact:true}).click();
        await page.locator('[name=name]').waitFor();
        assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
        await page.goto(origin+'/leltar/beallitasok');
        await page.locator('.actionform').waitFor();
        await page.setViewportSize({width:390,height:844});
        assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
        console.log('OK: successful online save redirect, desktop editor and mobile settings layout.');
        // Failed worker update must retain the old shell; IndexedDB and unrelated caches survive.
        await page.evaluate(async()=>{
            await caches.open('unrelated-app');
            await new Promise((resolve,reject)=>{const r=indexedDB.open('otthontar-retention-test',1);r.onupgradeneeded=()=>r.result.createObjectStore('drafts');r.onsuccess=()=>{const db=r.result;const tx=db.transaction('drafts','readwrite');tx.objectStore('drafts').put('keep','draft');tx.oncomplete=()=>{db.close();resolve();};};r.onerror=()=>reject(r.error);});
        });
        nextWorker=true;failAsset=true;
        await page.evaluate(async()=>{
            const registration=await navigator.serviceWorker.getRegistration();
            const done=new Promise(resolve=>registration.addEventListener('updatefound',()=>{const worker=registration.installing;worker.addEventListener('statechange',()=>{if(worker.state==='redundant')resolve();});},{once:true}));
            await registration.update();await done;
        });
        assert.ok((await page.evaluate(()=>caches.keys())).includes('otthontar-shell-v3'));
        failAsset=false;
        await page.evaluate(async()=>{
            const registration=await navigator.serviceWorker.getRegistration();
            const done=new Promise(resolve=>registration.addEventListener('updatefound',()=>{const worker=registration.installing;worker.addEventListener('statechange',()=>{if(worker.state==='installed')resolve();});},{once:true}));
            await registration.update();await done;
        });
        assert.equal(await page.evaluate(async()=>(await navigator.serviceWorker.getRegistration()).waiting?.state),'installed');
        // A waiting worker must not replace the page while an editor is open.
        await page.waitForFunction(()=>!document.querySelector('#pwa-update').hidden);
        page.on('dialog',dialog=>dialog.accept());
        await page.close();
        const fresh=await context.newPage();
        await fresh.goto(origin+'/leltar');
        await fresh.waitForFunction(async()=>(await caches.keys()).includes('otthontar-shell-v4')&&!(await caches.keys()).includes('otthontar-shell-v3'));
        const dbValue=await fresh.evaluate(()=>new Promise(resolve=>{const r=indexedDB.open('otthontar-retention-test');r.onsuccess=()=>{const db=r.result;const tx=db.transaction('drafts');const get=tx.objectStore('drafts').get('draft');get.onsuccess=()=>{resolve(get.result);db.close();};};}));
        assert.equal(dbValue,'keep');
        const cached=await fresh.evaluate(async()=>({keys:await caches.keys(),urls:(await(await caches.open('otthontar-shell-v4')).keys()).map(r=>r.url)}));
        assert.ok(cached.keys.includes('unrelated-app'));assert.ok(!cached.urls.some(u=>u.includes('/api/')||/\/leltar/.test(u)));
        await fresh.waitForFunction(()=>navigator.serviceWorker.controller?.state==='activated');
        // Fail the actual HTTP connection, including fetches made by the service worker.
        serverUnavailable=true;
        await fresh.goto(origin+'/leltar/some-item');
        await fresh.getByRole('heading',{name:'A szerver nem érhető el'}).waitFor();
        assert.equal(await fresh.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
        assert.equal(await fresh.evaluate(()=>fetch('/api/items').then(()=>true,()=>false)),false);
        assert.deepEqual(errors,[]);
        console.log('OK: offline deep-link fallback, API exclusion, atomic failed update, deferred activation, cache and IndexedDB retention.');
    }finally{await browser.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1;server.close();});
