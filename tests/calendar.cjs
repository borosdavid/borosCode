// Browser cases against the calendar API contract. The packaged-JAR smoke test checks real routing.
const assert = require('node:assert/strict');

function calendarFixture() {
    const events = new Map();
    const factId = 'executed-move';
    let fact;
    const factEntry = () => normalize({
        id: fact.id, title: 'Költöztetve: ' + fact.itemName, source: 'INVENTORY', type: fact.type,
        status: 'DONE', actualStart: fact.date, locationId: fact.destinationLocationId,
        items: [{itemId: fact.itemId}], facts: [fact]
    });
    let failSave = false;
    let unavailable = false;
    let endLoad;
    const normalize = event => ({
        version: 0, notes: null, locationId: null, plannedStart: null, actualStart: null, items: [],
        ...event,
        plannedEnd: event.plannedEnd || event.plannedStart || null,
        actualEnd: event.actualEnd || event.actualStart || null,
        overdue: event.status === 'PLANNED' && (event.plannedEnd || event.plannedStart) < '2026-11-27'
    });
    return {
        reset() {
            events.clear();
            fact = {id: factId, itemId: 'fixture-1', itemName: 'Állólámpa', version: 0, type: 'MOVE',
                date: '2026-11-29', originLocationId: 'place-1', destinationLocationId: 'place-2',
                eventId: null, withdrawn: false};
            failSave = unavailable = false;
            endLoad = null;
            for (const event of [
                {id: 'cross-month-move', title: 'Hónapváltás költözés', type: 'MOVE', status: 'PLANNED',
                    plannedStart: '2026-11-29', plannedEnd: '2026-12-01', items: [{itemId: 'fixture-1', originLocationId: 'place-1'}]},
                {id: 'overdue-move', title: 'Korábbi költözési terv', type: 'MOVE', status: 'PLANNED', plannedStart: '2026-11-05'},
                {id: 'open-sale', title: 'Nyitott állólámpa-eladás', type: 'SALE', status: 'PLANNED',
                    plannedStart: '2026-11-30', items: [{itemId: 'fixture-1', originLocationId: 'place-1'}]}
            ]) events.set(event.id, normalize(event));
        },
        failNextSave() { failSave = true; },
        suspendList() { unavailable = true; },
        resumeList() { unavailable = false; },
        pauseList() {
            let release;
            endLoad = new Promise(resolve => { release = resolve; });
            return () => { release(); endLoad = null; };
        },
        eventNamed(name) { return [...events.values()].find(event => event.title === name); },
        revise(id) { events.get(id).version++; },
        async handle(req, res, route, query) {
            const reply = (status, data) => {
                res.writeHead(status, {'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json'});
                res.end(JSON.stringify(data));
            };
            const id = route.slice('/api/calendar/'.length);
            if (id === 'facts/' + factId) return reply(200, fact);
            if (id === 'facts/' + factId + '/candidates') {
                const plan = events.get('cross-month-move');
                return reply(200, fact.eventId ? [] : [{id: plan.id, title: plan.title,
                    version: plan.version, date: plan.plannedStart}]);
            }
            if (id === 'facts/' + factId + '/assignment') {
                let raw = '';
                for await (const chunk of req) raw += chunk;
                assert.deepEqual(JSON.parse(raw), {eventId: 'cross-month-move', eventVersion: 0, factVersion: 0});
                fact = {...fact, eventId: 'cross-month-move'};
                events.get(fact.eventId).facts = [fact];
                return reply(200, fact);
            }
            const current = events.get(id);
            const warnings = event => {
                if (event.status !== 'PLANNED' || !['SALE', 'MOVE'].includes(event.type)) return [];
                const eventIds = [...events.values()].filter(other =>
                    other.status === 'PLANNED' && other.type === (event.type === 'MOVE' ? 'SALE' : 'MOVE')
                    && other.plannedStart <= event.plannedEnd && other.plannedEnd >= event.plannedStart
                    && other.items.some(link => event.items.some(item => item.itemId === link.itemId)))
                    .map(other => other.id);
                return eventIds.length ? [{code: 'SALE_MOVE_OVERLAP', eventIds}] : [];
            };
            if (req.method === 'GET') {
                if (id) return current ? reply(200, {...current, warnings: warnings(current)}) : reply(404, {detail: 'Az esemény nem található.'});
                if (endLoad) await endLoad;
                if (unavailable) return reply(503, {detail: 'A tesztnaptár átmenetileg nem érhető el.'});
                const visible = [...events.values()];
                if (!fact.eventId) visible.push(factEntry());
                const filtered = visible.filter(event => {
                    const start = event.actualStart || event.plannedStart;
                    const end = event.actualEnd || event.plannedEnd;
                    return start < query.get('until') && end >= query.get('from')
                        && (!query.has('type') || event.type === query.get('type'))
                        && (!query.has('status') || event.status === query.get('status'))
                        && (!query.has('itemId') || event.items.some(link => link.itemId === query.get('itemId')))
                        && (!query.has('locationId') || event.locationId === query.get('locationId')
                            || event.items.some(link => link.originLocationId === query.get('locationId')));
                });
                const day = event => event.actualStart || event.plannedStart;
                filtered.sort((a, b) => day(a).localeCompare(day(b)) || a.id.localeCompare(b.id));
                return reply(200, filtered.map(event => ({...event, warnings: []})));
            }
            assert.equal(req.headers['x-csrf-token'], 'fixture-token');
            assert.equal(req.headers['x-otthontar-user'], 'account-1');
            assert.equal(req.headers['x-otthontar-request'], '1');
            let raw = '';
            for await (const chunk of req) raw += chunk;
            const draft = JSON.parse(raw);
            if (failSave) {
                failSave = false;
                return reply(503, {detail: 'A mentés most nem sikerült.'});
            }
            if (req.method === 'PUT' && (!current || draft.version !== current.version))
                return reply(409, {detail: 'A naptáresemény időközben megváltozott.'});
            const conflict = draft.type === 'SALE' && draft.status === 'PLANNED' && [...events.values()].find(event =>
                event.id !== draft.id && event.type === 'SALE' && event.status === 'PLANNED'
                && event.items.some(link => draft.items.some(item => link.itemId === item.itemId)));
            if (conflict) return reply(409, {detail: 'A tárgynak már van nyitott eladási terve.',
                conflictingEvents: [{id: conflict.id, title: conflict.title}]});
            const saved = normalize({...draft, version: current ? current.version + 1 : 0});
            events.set(saved.id, saved);
            return reply(current ? 200 : 201, {...saved, warnings: warnings(saved)});
        }
    };
}

async function checkCalendar(browser, origin, fixture, expireSession) {
    fixture.reset();
    const desktop = await browser.newContext({viewport: {width: 1440, height: 900}});
    const desktopPage = await desktop.newPage();
    await desktopPage.clock.setFixedTime(new Date('2026-11-27T12:00:00Z'));
    try {
        const release = fixture.pauseList();
        try {
            await desktopPage.goto(origin + '/leltar/naptar');
            await desktopPage.getByText('A naptár betöltése…').waitFor();
        } finally { release(); }
        assert.equal(await desktopPage.getByRole('button', {name: 'Hónapnézet'}).getAttribute('aria-pressed'), 'true');
        const november = desktopPage.getByRole('table');
        await november.waitFor();
        assert.equal(await november.getByRole('button', {name: /Hónapváltás költözés/}).count(), 2);
        await november.getByRole('button', {name: /Korábbi költözési terv.*Lejárt/}).waitFor();
        const nav = desktopPage.getByRole('navigation', {name: 'Leltár'});
        assert.equal(await nav.getByRole('link', {name: 'Naptár'}).getAttribute('aria-current'), 'page');
        await desktopPage.getByRole('button', {name: 'Következő hónap'}).click();
        await desktopPage.getByRole('heading', {name: /december/}).waitFor();
        const december = desktopPage.getByRole('table');
        await december.getByRole('button', {name: /Hónapváltás költözés/}).waitFor();
        assert.equal(await december.getByRole('button', {name: /Hónapváltás költözés/}).count(), 1);
        await desktopPage.getByRole('button', {name: 'Lista', exact: true}).click();
        assert.equal(await desktopPage.getByRole('table').count(), 0);
        assert.equal(await desktopPage.locator('#calendar-events button').count(), 1);
        await desktopPage.getByRole('button', {name: 'Ma', exact: true}).click();
        await desktopPage.getByRole('heading', {name: /november/}).waitFor();
        fixture.suspendList();
        await desktopPage.locator('#calendar-filter-type').selectOption('OTHER');
        await desktopPage.getByText('A tesztnaptár átmenetileg nem érhető el.').waitFor();
        fixture.resumeList();
        await desktopPage.getByRole('button', {name: 'Újrapróbálás'}).click();
        await desktopPage.getByText('Nincs esemény ebben a hónapban a megadott szűrőkkel.').waitFor();
    } finally { await desktop.close(); }

    const mobile = await browser.newContext({viewport: {width: 390, height: 844}, isMobile: true, hasTouch: true});
    const page = await mobile.newPage();
    await page.clock.setFixedTime(new Date('2026-11-27T12:00:00Z'));
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    const sheet = page.getByRole('region', {name: /Új esemény|Költöztetés|Eladás/});
    const title = () => sheet.getByLabel('Cím');
    const save = () => sheet.getByRole('button', {name: 'Mentés', exact: true}).click();
    const waitSaved = () => page.getByText('Esemény mentve.', {exact: true}).waitFor();
    try {
        await page.goto(origin + '/leltar/naptar');
        const factEntry = page.locator('#calendar-events').getByRole('button', {name: /Költöztetve: Állólámpa.*Leltárból/});
        await factEntry.click();
        const actual = page.getByRole('region', {name: 'Költöztetve – Állólámpa'});
        await actual.getByText(/Lakás → Raktár/).waitFor();
        await actual.getByLabel('Naptáresemény').selectOption('cross-month-move');
        await actual.getByRole('button', {name: 'Összekapcsolás'}).click();
        await actual.getByText('A leltári tétel kapcsolása mentve.').waitFor();
        await factEntry.waitFor({state:'hidden'});
        await actual.getByRole('button', {name: 'Kapcsolt esemény megnyitása'}).click();
        const associated = page.getByRole('region', {name: 'Hónapváltás költözés'});
        await associated.getByText(/Állólámpa · 2026. november 29./).waitFor();
        await associated.getByRole('button', {name: 'Megtörtént', exact: true}).waitFor();
        await page.goto(origin + '/leltar/fixture-1');
        await page.getByRole('link', {name: 'Új naptáresemény ehhez a tárgyhoz'}).click();
        await sheet.getByRole('heading', {name: 'Új esemény'}).waitFor();
        assert.equal(await page.getByRole('button', {name: 'Lista', exact: true}).getAttribute('aria-pressed'), 'true');
        assert.equal(await sheet.getByRole('link', {name: 'Állólámpa'}).getAttribute('href'), origin.replace(/^https?:\/\/[^/]+/, '') + '/leltar/fixture-1');
        await sheet.getByLabel('Tárgy hozzáadása').selectOption('fixture-2');
        await sheet.getByRole('button', {name: 'Hozzáadás', exact: true}).click();
        assert.equal(await sheet.getByLabel('Kiindulási hely – Állólámpa').inputValue(), 'place-1');
        assert.equal(await sheet.getByLabel(/Kiindulási hely – Szék/).inputValue(), 'place-2');
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false);
        await title().fill('Költöztetés próbája');
        await sheet.getByRole('combobox', {name: 'Típus', exact: true}).selectOption('MOVE');
        await sheet.getByLabel('Tervezett kezdőnap').fill('2026-11-29');
        await sheet.getByLabel('Tervezett zárónap').fill('2026-12-01');
        fixture.failNextSave();
        await save();
        await sheet.getByText(/A mentés most nem sikerült/).waitFor();
        assert.equal(await title().inputValue(), 'Költöztetés próbája');
        await save();
        await sheet.getByRole('heading', {name: 'Költöztetés próbája'}).waitFor();
        await sheet.getByText(/időpontja átfedésben van/).waitFor();
        const stored = fixture.eventNamed('Költöztetés próbája');
        assert.equal(stored.items.length, 2);

        await sheet.getByLabel('Tervezett zárónap').fill('2026-12-02');
        fixture.revise(stored.id);
        await save();
        await sheet.getByText(/időközben megváltozott/).waitFor();
        assert.equal(await sheet.getByLabel('Tervezett zárónap').inputValue(), '2026-12-02');
        assert.equal(fixture.eventNamed('Költöztetés próbája').plannedEnd, '2026-12-01');
        page.once('dialog', dialog => dialog.accept());
        await sheet.getByRole('button', {name: 'Bezárás'}).click();
        await page.locator('#calendar-events').getByRole('button', {name: /Költöztetés próbája/}).click();
        await sheet.getByLabel('Tervezett zárónap').fill('2026-12-02');
        await save();
        await waitSaved();
        await sheet.getByRole('button', {name: 'Megtörtént', exact: true}).click();
        await sheet.getByLabel('Tényleges kezdőnap').fill('2026-12-02');
        await sheet.getByLabel('Tényleges zárónap').fill('2026-12-04');
        await save();
        await page.getByRole('heading', {name: /december/}).waitFor();
        assert.equal(fixture.eventNamed('Költöztetés próbája').status, 'DONE');
        assert.equal(fixture.eventNamed('Költöztetés próbája').actualEnd, '2026-12-04');
        const linkedItem = sheet.getByRole('link', {name: 'Állólámpa'});
        await linkedItem.click();
        await page.getByRole('heading', {name: 'Állólámpa', exact: true}).waitFor();
        await page.getByRole('navigation', {name: 'Leltár'}).getByRole('link', {name: 'Naptár'}).click();

        await page.getByRole('button', {name: 'Új esemény'}).click();
        await title().fill('Eladás duplikációs próba');
        await sheet.getByRole('combobox', {name: 'Típus', exact: true}).selectOption('SALE');
        await sheet.getByLabel('Tárgy hozzáadása').selectOption('fixture-1');
        await sheet.getByRole('button', {name: 'Hozzáadás', exact: true}).click();
        await save();
        const conflict = sheet.getByRole('link', {name: 'Nyitott állólámpa-eladás'});
        await conflict.waitFor();
        assert.ok((await conflict.getAttribute('href')).endsWith('/leltar/naptar?event=open-sale'));
        await title().fill('Eladás munkamenet próba');
        await sheet.getByRole('button', {name: 'Eltávolítás – Állólámpa'}).click();
        await sheet.getByLabel('Tárgy hozzáadása').selectOption('fixture-2');
        await sheet.getByRole('button', {name: 'Hozzáadás', exact: true}).click();

        expireSession(true);
        await save();
        const login = page.getByRole('dialog', {name: 'Újbóli bejelentkezés'});
        await login.getByLabel('Jelszó').fill('fixture');
        await login.getByRole('button', {name: 'Bejelentkezés', exact: true}).click();
        await login.waitFor({state: 'hidden'});
        assert.equal(await title().inputValue(), 'Eladás munkamenet próba');
        await save();
        await sheet.getByRole('heading', {name: 'Eladás munkamenet próba'}).waitFor();
        await sheet.getByRole('button', {name: 'Esemény lemondása'}).click();
        await save();
        await waitSaved();
        await sheet.getByRole('button', {name: 'Bezárás'}).click();
        await page.locator('#calendar-filter-status').selectOption('CANCELLED');
        await page.locator('#calendar-events').getByRole('button', {name: /Eladás munkamenet próba.*Lemondott/}).waitFor();

        await mobile.setOffline(true);
        await page.getByText(/A naptár online érhető el/).waitFor();
        await mobile.setOffline(false);
        await page.locator('#calendar-events').getByRole('button', {name: /Eladás munkamenet próba/}).waitFor();
        assert.deepEqual(errors, []);
        await page.getByRole('button', {name: 'Kijelentkezés'}).click();
        await page.getByRole('heading', {name: 'Bejelentkezés', exact: true}).waitFor();
    } finally {
        expireSession(false);
        await mobile.close();
    }
    console.log('OK: calendar month and agenda, multi-item editor, filters, conflicts, retries, session and offline states.');
}

module.exports = {calendarFixture, checkCalendar};
