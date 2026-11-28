// Isolated browser contract tests. No personal data, database or Windows profile is used.
const {chromium} = require(process.env.PLAYWRIGHT_CORE || 'playwright-core');
const assert = require('node:assert/strict');
const http = require('node:http');
const fs = require('node:fs/promises');
const path = require('node:path');
const {calendarFixture, checkCalendar} = require('./calendar.cjs');
const calendar = calendarFixture();
const root = path.resolve(__dirname,'../inventory/src/main/resources');
const types = {'.js':'text/javascript','.css':'text/css','.html':'text/html','.webmanifest':'application/manifest+json','.png':'image/png'};
let expiredSession = false, fixtureUser = 'account-1';
let failSave = true, savedBody = '', savedHeader = '', nextWorker = false, failAsset = false;
let dropSaveResponse = false, serverUnavailable = false, saveId;
let blockItems, itemsUnavailable, testLocationApproval;
const sampleItems = [
    {id:'fixture-1', name:'Állólámpa', place:'Lakás', location_id:'place-1', cover:'photo-1', status:'KEEP', action_id:'MOVE', destination_id:'place-2', version:1},
    {id:'fixture-2', name:'Szék a hosszú nevű tárgyak elrendezésének ellenőrzéséhez', place:'Raktár', location_id:'place-2', status:'LISTED', action_id:'SALE', version:1}
];
let fixtureItems;

function changeFixtureItem(item, operation) {
    if (operation === 'sold') item.status = 'SOLD';
    if (operation === 'trash' || operation === 'restore') item.trashed = operation === 'trash';
    if (operation === 'move') {
        item.location_id = item.destination_id;
        item.place = 'Raktár';
        item.action_id = null;
        item.destination_id = null;
    }
    item.version++;
}

async function fixturePage(filename, prefix) {
    const read = name => fs.readFile(path.join(root, 'templates', name + '.html'), 'utf8');
    let html = await read(filename);
    if (filename === 'inventory/index' || filename === 'calendar/index') {
        const header = (await read('layout')).match(/<header[\s\S]+?<\/header>/)[0];
        html = html.replace(/<header th:replace="[^"]*"><\/header>/, header)
            .replace(/th:attr="data-app-root=@\{\/\}(?:,data-upload-limit-mib=[^"]+)?"/, `data-app-root="${prefix}/" data-upload-limit-mib="200"`);
    }
    if (filename === 'login') html = html.replace(/<p[^>]*th:if="[^"]*"[^>]*>[^<]*<\/p>/g, '');
    return html.replace(/th:(src|href|action)="@\{([^}]+)\}"/g, (_, attribute, value) => `${attribute}="${prefix}${value}"`);
}

const server = http.createServer(async (req,res) => {
    if(serverUnavailable){req.socket.destroy();return;}
    const url = new URL(req.url,'http://localhost');
    const prefix = url.pathname.startsWith('/otthontar/') ? '/otthontar' : '';
    const route = url.pathname.slice(prefix.length);
    try {
        res.setHeader('Cache-Control','no-store');
        if(route==='/auth/csrf') {res.setHeader('Content-Type','application/json');res.end(JSON.stringify({headerName:'X-CSRF-TOKEN',token:'fixture-token'}));return;}
        if(route==='/login'&&req.method==='POST') {expiredSession=false;res.writeHead(204);res.end();return;}
        if (route === '/logout' && req.method === 'POST') {
            expiredSession = true;
            res.writeHead(302, {Location: prefix + '/login'});res.end();return;
        }
        if(route.startsWith('/api/')) {
            if(expiredSession){res.writeHead(401,{'Content-Type':'application/json'});res.end(JSON.stringify({detail:'Jelentkezz be újra.'}));return;}
            if(route==='/api/session'){res.setHeader('Content-Type','application/json');res.end(JSON.stringify({id:fixtureUser,username:'alice',headerName:'X-CSRF-TOKEN',token:'fixture-token'}));return;}
            if (route === '/api/calendar' || route.startsWith('/api/calendar/')) {
                await calendar.handle(req, res, route, url.searchParams);
                return;
            }

            const transition = route.match(/^\/api\/items\/(fixture-\d+)\/(sold|move|trash|restore)$/);
            if (req.method === 'POST' && transition) {
                const item = fixtureItems.find(row => row.id === transition[1]);
                req.resume();
                changeFixtureItem(item, transition[2]);
                res.writeHead(204);res.end();return;
            }
            if(req.method==='POST') {
                savedHeader=req.headers['x-otthontar-request'];
                saveId=req.headers['x-otthontar-save-id'];
                assert.match(saveId,/^[a-f0-9-]{36}$/);
                assert.equal(req.headers['x-csrf-token'],'fixture-token');assert.equal(req.headers['x-otthontar-user'],'account-1');
                let body='';
                for await(const chunk of req) body+=chunk.toString('latin1');
                savedBody+=body;
                if(testLocationApproval){
                    if(!body.includes('name="calendarApproval"')){
                        testLocationApproval.challenges++;
                        res.writeHead(409,{'Content-Type':'application/problem+json'});
                        res.end(JSON.stringify({code:'CALENDAR_LOCATION_APPROVAL',
                            approval:{from:'place-1',to:'place-2',events:{'plan-1':2}},
                            events:[{id:'plan-1',title:'Nyitott szállítás',date:'2026-11-28'}]}));
                    }else{
                        assert.ok(body.includes('"plan-1":2'));
                        testLocationApproval.approved++;
                        res.writeHead(200,{'Content-Type':'application/json'});res.end('{}');
                    }
                    return;
                }
                if(dropSaveResponse){req.socket.destroy();return;}
                res.writeHead(failSave?413:200,{'Content-Type':'application/json'});
                res.end(JSON.stringify(failSave?{detail:'Túl nagy kérés. A piszkozat megmarad.'}:{}));return;
            }
            if(route==='/api/photos/photo-1/small') {
                const icon = await fs.readFile(path.join(root,'static/icons/icon-192.png'));
                res.writeHead(200, {'Content-Type':'image/png'});res.end(icon);return;
            }
            if (route === '/api/items/fixture-1') {
                const item = fixtureItems.find(row => row.id === 'fixture-1');
                res.setHeader('Content-Type', 'application/json');
                res.end(JSON.stringify({...item, photos:[{id:'photo-1',filename:'Mentett kép.jpg'}]}));
                return;
            }
            if (route === '/api/items' && blockItems) await blockItems;
            if (route === '/api/items' && itemsUnavailable) {
                res.writeHead(503, {'Content-Type':'application/json'});
                res.end(JSON.stringify({detail:'A tesztleltár átmenetileg nem elérhető.'}));return;
            }
            const lists = {
                '/api/items':fixtureItems.filter(item => !!item.trashed === (url.searchParams.get('trash') === 'true')),
                '/api/places':[{id:'place-1',name:'Lakás'},{id:'place-2',name:'Raktár'}],
                '/api/actions':[{id:'KEEP',name:'Megtartjuk',kind:'CUSTOM',active:true},
                    {id:'MOVE',name:'Költöztetés',kind:'MOVE',active:true},{id:'SALE',name:'Eladás',kind:'SALE',active:true}]
            };
            const data = lists[route] ?? [];
            res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify(data));return;
        }
        if(/^\/leltar(?:\/[^/]+)?$/.test(route)||route==='/offline.html'||route==='/login') {
            const template = {'/offline.html':'offline', '/login':'login', '/leltar/naptar':'calendar/index'}[route] ?? 'inventory/index';
            const html = await fixturePage(template, prefix);
            res.writeHead(200,{'Content-Type':'text/html; charset=utf-8'});res.end(html);return;
        }
        if(failAsset && route==='/manifest.webmanifest'){res.writeHead(503);res.end();return;}
        const file=path.join(root,'static',route);
        let data=await fs.readFile(file);
        if(route==='/sw.js'&&nextWorker)data=Buffer.from(data.toString().replace(/const VERSION = cachePrefix \+ 'v\d+';/, "const VERSION = cachePrefix + 'test-candidate';"));
        res.writeHead(200,{'Content-Type':types[path.extname(file)]||'application/octet-stream'});res.end(data);
    }catch{res.writeHead(404);res.end();}
});
async function checkPhotoFrame(page, preview, viewport, width, height) {
    await page.setViewportSize(viewport);
    const cover = await page.locator(preview + ' img').evaluate(async (img, size) => {
        const picture = `<svg xmlns="http://www.w3.org/2000/svg" width="${size.width}" height="${size.height}"><rect width="100%" height="100%" fill="green"/></svg>`;
        img.src = 'data:image/svg+xml,' + encodeURIComponent(picture);
        await img.decode();
        const style = getComputedStyle(img);
        const frame = img.parentElement.getBoundingClientRect();
        const image = img.getBoundingClientRect();
        return {
            fit: style.objectFit,
            position: style.objectPosition,
            frameWidth: frame.width,
            frameHeight: frame.height,
            inset: [image.left - frame.left, image.top - frame.top,
                frame.right - image.right, frame.bottom - image.bottom],
        };
    }, {width, height});
    assert.equal(cover.fit, 'contain');
    assert.equal(cover.position, '50% 50%');
    assert.ok(cover.frameWidth > 0 && cover.frameHeight > 0, 'Photo frame must have an area');
    assert.ok(cover.inset.every(edge => Math.abs(edge) < 1), 'Photo must fit its frame');
}

async function checkInstallHelp(browser, origin) {
    const iphoneWebkit = 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko)';
    const iphoneSafari = `${iphoneWebkit} Version/26.0 Mobile/15E148 Safari/604.1`;
    const desktopSafari = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15';
    const scenarios = [
        {name: 'iPhone Safari', userAgent: iphoneSafari, visible: true},
        {name: 'desktop Safari', userAgent: desktopSafari, visible: false},
        {name: 'iPhone Chrome', userAgent: `${iphoneWebkit} CriOS/140.0.7339.122 Mobile/15E148 Safari/604.1`, visible: false},
        {name: 'iPhone Home Screen', userAgent: iphoneSafari, standalone: true, visible: false},
        {name: 'standalone display mode', userAgent: iphoneSafari, standaloneDisplay: true, visible: false},
    ];
    for (const {name, userAgent, visible, standalone, standaloneDisplay} of scenarios) {
        const page = await browser.newPage({userAgent});
        try {
            await page.addInitScript(({standalone, standaloneDisplay}) => {
                if (standalone) Object.defineProperty(navigator, 'standalone', {value: true});
                if (standaloneDisplay) {
                    const matchMedia = window.matchMedia.bind(window);
                    window.matchMedia = query => query === '(display-mode: standalone)' ? {matches: true} : matchMedia(query);
                }
            }, {standalone, standaloneDisplay});
            await page.goto(origin + '/leltar');
            const help = page.getByText('Telepítés iPhone-ra', {exact: true});
            assert.equal(await help.isVisible(), visible, name);
            if (visible) {
                await help.click();
                const instructions = page.getByText(/Megosztás → Hozzáadás a Főképernyőhöz/);
                assert.equal(await instructions.isVisible(), true, name);
            }
        } finally {
            await page.close();
        }
    }
    console.log(`OK: ${new URL(origin).pathname} install help only in iPhone Safari browser mode.`);
}

async function checkLocationApproval(browser, origin) {
    const page = await browser.newPage({viewport:{width:390,height:844}, isMobile:true, hasTouch:true});
    testLocationApproval = {challenges:0, approved:0};
    try {
        await page.goto(origin + '/leltar/fixture-1');
        await page.locator('[name=location]').selectOption('place-2');
        await page.locator('[name=actionId]').selectOption('KEEP');
        await page.locator('[name=description]').fill('A szerkesztőben megmarad');
        const save = page.getByRole('button',{name:'Mentés',exact:true}).first();
        await save.click();
        const dialog = page.getByRole('dialog',{name:'Helyváltoztatás jóváhagyása'});
        await dialog.getByText('Lakás → Raktár').waitFor();
        await dialog.getByText(/Nyitott szállítás/).waitFor();
        await dialog.getByRole('button',{name:'Vissza a szerkesztéshez'}).click();
        await dialog.waitFor({state:'hidden'});
        assert.equal(await page.locator('[name=description]').inputValue(),'A szerkesztőben megmarad');
        assert.equal(await page.locator('[name=location]').inputValue(),'place-2');
        assert.equal(testLocationApproval.approved,0);
        await save.click();
        await dialog.getByRole('button',{name:'Jóváhagyás és mentés'}).click();
        await page.waitForURL(origin + '/leltar');
        assert.deepEqual(testLocationApproval,{challenges:2,approved:1});
        console.log('OK: calendar location approval shows affected plans, preserves the draft when rejected and resubmits when accepted.');
    } finally {
        await page.close();
        testLocationApproval = undefined;
        savedBody = '';
    }
}

async function browserChecks(browser, base, prefix) {
    const origin = base + prefix;
    expiredSession = false; fixtureUser = 'account-1'; failSave = true; savedBody = ''; savedHeader = '';
    nextWorker = false; failAsset = false; dropSaveResponse = false; serverUnavailable = false; itemsUnavailable = false;
    fixtureItems = structuredClone(sampleItems);
    await checkCalendar(browser, origin, calendar, value => { expiredSession = value; });
    await checkInstallHelp(browser, origin);
    await checkLocationApproval(browser, origin);
    const context=await browser.newContext({viewport:{width:390,height:844},isMobile:true,hasTouch:true,deviceScaleFactor:3, serviceWorkers:'allow'});
    const page=await context.newPage();
    const errors=[];page.on('pageerror',e=>errors.push(e.message));
    try {
        await page.goto(origin+'/login');
        await page.getByRole('heading',{name:'Bejelentkezés',exact:true}).waitFor();
        assert.equal(await page.locator('.login-panel form').getAttribute('action'),prefix+'/login');
        assert.equal(await page.locator('.moving-art').isVisible(),true);
        assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
        let releaseItems;
        blockItems = new Promise(resolve => { releaseItems = resolve; });
        try {
            await page.goto(origin+'/leltar');
            await page.getByRole('status').filter({hasText:'A leltár betöltése…'}).waitFor();
        } finally { releaseItems(); blockItems = null; }
        await page.getByRole('heading',{name:'Tárgyak',exact:true}).waitFor();
        await page.evaluate(()=>navigator.serviceWorker.ready);
        await page.waitForFunction(()=>navigator.serviceWorker.controller);
        assert.equal(await page.locator('#pwa-update').isVisible(),false);
        const manifestUrl = origin+'/manifest.webmanifest';
        const manifest=await (await fetch(manifestUrl)).json();
        assert.equal(new URL(manifest.start_url,manifestUrl).href,origin+'/leltar');
        assert.equal(new URL(manifest.scope,manifestUrl).href,origin+'/leltar');
        for(const icon of manifest.icons)assert.equal((await fetch(new URL(icon.src,manifestUrl))).status,200);
        const shellKey = await page.evaluate(async()=>(await caches.keys()).find(key=>key.startsWith('otthontar-')));
        assert.ok(shellKey);
        assert.equal(await page.getByRole('navigation',{name:'Leltár'}).getByRole('link',{name:'Tárgyak',exact:true}).getAttribute('aria-current'),'page');
        await page.locator('.card-thumb img').evaluate(img=>img.decode());
        assert.equal(await page.locator('.card').first().getByRole('link',{name:'Adatlap'}).getAttribute('href'),prefix+'/leltar/fixture-1');
        assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
        await page.keyboard.press('Tab');
        assert.equal(await page.evaluate(()=>document.activeElement.getAttribute('href')),'#content');
        await page.keyboard.press('Enter');
        assert.equal(await page.evaluate(()=>document.activeElement.id),'content');
        const view = page.getByRole('group',{name:'Tárgylista nézete'});
        assert.equal(await view.getByRole('button',{name:'Fotónézet'}).getAttribute('aria-pressed'),'true');
        assert.deepEqual(await page.locator('.inventory-summary dd').allTextContents(),['2','2','1']);
        const mobile = {width:390,height:844};
        const desktop = {width:1440,height:900};
        await checkPhotoFrame(page, '.card-thumb', mobile, 900, 300);
        await checkPhotoFrame(page, '.card-thumb', desktop, 300, 900);
        await page.setViewportSize(mobile);
        await view.getByRole('button',{name:'Lista',exact:true}).click();
        assert.equal(await page.locator('.cards').getAttribute('data-view'),'list');
        await checkPhotoFrame(page, '.card-thumb', mobile, 300, 900);
        await checkPhotoFrame(page, '.card-thumb', desktop, 900, 300);
        await page.setViewportSize(mobile);
        await page.reload();
        await view.getByRole('button',{name:'Lista',exact:true,pressed:true}).waitFor();
        await view.getByRole('button',{name:'Fotónézet'}).click();
        assert.equal(await page.locator('.cards').getAttribute('data-view'),'gallery');
        const search = page.getByRole('searchbox',{name:'Keresés'});
        await search.fill('allolampa');
        assert.equal(await page.locator('.card').count(),1);
        await page.locator('#locationFilter').selectOption('place-2');
        await page.getByText('A keresés és a szűrők alapján nincs találat.').waitFor();
        await search.fill('');
        assert.equal(await page.locator('.card').count(),1);
        await page.getByLabel('Csak eladó tárgyak').check();
        await page.locator('#locationFilter').selectOption('');
        assert.equal(await page.locator('.card').count(),1);
        await page.getByLabel('Csak eladó tárgyak').uncheck();
        await page.addStyleTag({content:'html { font-size: 125%; }'});
        assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
        await page.getByRole('link',{name:'Lomtár',exact:true}).click();
        await page.getByText('A lomtár üres.').waitFor();
        itemsUnavailable = true;
        await page.goto(origin+'/leltar');
        await page.getByRole('alert').filter({hasText:'A tesztleltár átmenetileg nem elérhető.'}).waitFor();
        await page.getByText('A leltár nem töltődött be.').waitFor();
        itemsUnavailable = false;
        await page.getByRole('link',{name:'Újrapróbálás'}).click();
        await page.getByRole('heading',{name:'Tárgyak',exact:true}).waitFor();
        await page.getByRole('link',{name:'Költöztetés',exact:true}).click();
        await page.getByRole('button',{name:'Költöztetés végrehajtása'}).click();
        await page.getByText('Tárgy áthelyezve.',{exact:true}).waitFor();
        await page.getByText('Nincs költöztetésre kijelölt tárgy.').waitFor();
        await page.getByRole('link',{name:'Eladás',exact:true}).click();
        await page.locator('#locationFilter').selectOption('place-2');
        await page.getByRole('button',{name:'Eladva'}).click();
        const saleDialog = page.getByRole('dialog',{name:'Eladás rögzítése'});
        await saleDialog.getByRole('spinbutton').fill('3500');
        await saleDialog.getByRole('button',{name:'Mentés',exact:true}).click();
        await page.getByText('Eladás · Elkelt',{exact:true}).waitFor();
        assert.equal(await page.locator('#locationFilter').inputValue(),'place-2');
        await page.getByRole('button',{name:'Lomtárba'}).click();
        await page.getByText('Tárgy a lomtárba helyezve.',{exact:true}).waitFor();
        await page.getByRole('link',{name:'Lomtár',exact:true}).click();
        await page.getByRole('button',{name:'Visszaállítás'}).click();
        await page.getByText('Tárgy visszaállítva.',{exact:true}).waitFor();
        console.log(`OK: ${prefix || '/'} search, loading, errors, sale, move, trash and restore.`);
        await page.goto(origin+'/leltar/fixture-1');
        const savedPhoto = '#gallery a.photo-preview';
        await page.locator('#gallery img').waitFor();
        assert.equal(await page.locator('#gallery figure a').getAttribute('href'),prefix+'/api/photos/photo-1/original');
        assert.equal(await page.locator(savedPhoto).count(), 1);
        await checkPhotoFrame(page, savedPhoto, mobile, 300, 900);
        await checkPhotoFrame(page, savedPhoto, desktop, 900, 300);
        await page.setViewportSize(mobile);
        await page.goto(origin+'/leltar/uj');
        await page.locator('[name=name]').fill('Mobilos teszt');
        await page.locator('[name=location]').selectOption('place-1');
        assert.equal(await page.locator('#camera').getAttribute('capture'),'environment');
        const png=await page.evaluate(()=>{const c=document.createElement('canvas');c.width=32;c.height=24;return c.toDataURL('image/png').split(',')[1];});
        await page.locator('#picker').setInputFiles({name:'test.png',mimeType:'image/png',buffer:Buffer.from(png,'base64')});
        await page.locator('#gallery figure').waitFor();
        await checkPhotoFrame(page, '#gallery .photo-preview', mobile, 900, 300);
        await checkPhotoFrame(page, '#gallery .photo-preview', desktop, 300, 900);
        await page.setViewportSize(mobile);
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
        assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
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
        assert.ok((await page.evaluate(()=>caches.keys())).includes(shellKey));
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
        const nextKey = shellKey.replace(/v\d+$/, 'test-candidate');
        await fresh.waitForFunction(async({nextKey,shellKey})=>(await caches.keys()).includes(nextKey)&&!(await caches.keys()).includes(shellKey),{nextKey,shellKey});
        const dbValue=await fresh.evaluate(()=>new Promise(resolve=>{const r=indexedDB.open('otthontar-retention-test');r.onsuccess=()=>{const db=r.result;const tx=db.transaction('drafts');const get=tx.objectStore('drafts').get('draft');get.onsuccess=()=>{resolve(get.result);db.close();};};}));
        assert.equal(dbValue,'keep');
        const cached=await fresh.evaluate(async key=>({keys:await caches.keys(),urls:(await(await caches.open(key)).keys()).map(r=>r.url)}),nextKey);
        assert.ok(cached.keys.includes('unrelated-app'));assert.ok(!cached.urls.some(u=>u.includes('/api/')||/\/leltar/.test(u)));
        await fresh.waitForFunction(()=>navigator.serviceWorker.controller?.state==='activated');
        // Fail the actual HTTP connection, including fetches made by the service worker.
        serverUnavailable=true;
        await fresh.goto(origin+'/leltar/some-item');
        await fresh.getByRole('heading',{name:'A szerver nem érhető el'}).waitFor();
        assert.equal(await fresh.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
        assert.equal(await fresh.evaluate(url=>fetch(url).then(()=>true,()=>false),origin+'/api/items'),false);
        assert.ok(await fresh.locator('.entry-panel').evaluate(el=>getComputedStyle(el).borderRadius!=='0px'));
        assert.deepEqual(errors,[]);
        console.log(`OK: ${prefix || '/'} offline deep-link, themed shell, API exclusion, atomic update and cache retention.`);
    }finally{await context.close();}
}

(async()=>{
    await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
    const origin='http://127.0.0.1:'+server.address().port;
    const executablePath=process.env.BROWSER_EXE || (process.platform==='win32'
        ? 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe' : undefined);
    const browser=await chromium.launch({executablePath,headless:true,args:['--no-sandbox']});
    try {
        await browserChecks(browser, origin, '');
        await browserChecks(browser, origin, '/otthontar');
    } finally { await browser.close(); await new Promise(resolve=>server.close(resolve)); }
})().catch(error=>{console.error(error);process.exitCode=1;server.close();});
