'use strict';
(() => {
    const {api, url} = AccountAuth;
    const byId = id => document.getElementById('calendar-' + id);
    const form = byId('form');
    const fields = form.elements;
    const editor = byId('editor');
    const results = byId('events');
    const types = {MOVE: 'Költözés', SALE: 'Eladás', TRANSPORT: 'Szállítás',
        HANDOVER: 'Átadás', PICKUP: 'Átvétel', OTHER: 'Egyéb'};
    const statuses = {PLANNED: 'Tervezett', DONE: 'Megtörtént', CANCELLED: 'Lemondott'};
    const todayFormat = new Intl.DateTimeFormat('sv-SE', {timeZone: 'Europe/Budapest',
        year: 'numeric', month: '2-digit', day: '2-digit'});
    const monthFormat = new Intl.DateTimeFormat('hu-HU', {timeZone: 'UTC', year: 'numeric', month: 'long'});
    const dayFormat = new Intl.DateTimeFormat('hu-HU', {timeZone: 'UTC', dateStyle: 'long'});
    const collator = new Intl.Collator('hu');
    function today() {
        const parts = todayFormat.formatToParts(new Date());
        return ['year', 'month', 'day'].map(part => parts.find(value => value.type === part).value).join('-');
    }
    const date = value => new Date(value + 'T00:00:00Z');
    const iso = value => value.toISOString().slice(0, 10);
    const monthStart = value => value.slice(0, 7) + '-01';
    function nextMonth(value, step) {
        const day = date(value);
        day.setUTCMonth(day.getUTCMonth() + step);
        return iso(day);
    }

    let month = monthStart(today());
    let mode = matchMedia('(max-width: 41rem)').matches ? 'list' : 'grid';
    let items = new Map(), places = new Map(), events = [];
    let ready = false, initializing = false, listRequest = 0, detailRequest = 0;
    let active = null, links = new Map(), baseline = '', saving = false, opening = false, opener;
    let uncertain = false;
    let initialLink = new URLSearchParams(location.search);

    function node(tag, text, className) {
        const el = document.createElement(tag);
        if (text != null) el.textContent = text;
        if (className) el.className = className;
        return el;
    }
    function populate(select, choices, placeholder) {
        const options = placeholder == null ? [] : [new Option(placeholder, '')];
        for (const [id, name] of choices) options.push(new Option(name, id));
        select.replaceChildren(...options);
    }
    function notice(host, text, related = []) {
        host.replaceChildren();
        host.hidden = !text;
        if (!text) return;
        host.append(node('p', text));
        if (related.length) {
            const list = node('ul');
            for (const event of related) {
                const row = node('li');
                const link = node('a', event.title || 'Kapcsolódó esemény megnyitása');
                link.href = url('leltar/naptar?event=' + encodeURIComponent(event.id));
                link.target = '_blank';
                link.rel = 'noopener';
                row.append(link);
                list.append(row);
            }
            host.append(list);
        }
    }
    function dates(event) {
        return event.status === 'DONE' ? [event.actualStart, event.actualEnd] : [event.plannedStart, event.plannedEnd];
    }
    function dateLabel(start, end) {
        const first = dayFormat.format(date(start));
        return start === end ? first : first + ' – ' + dayFormat.format(date(end));
    }
    function entry(event, agenda) {
        const [start, end] = dates(event);
        const button = node('button', null, 'calendar-entry');
        button.type = 'button';
        button.dataset.status = event.status;
        button.dataset.overdue = String(event.overdue);
        button.append(node('strong', event.title));
        button.append(node('small', [types[event.type], statuses[event.status], event.overdue && 'Lejárt'].filter(Boolean).join(' · ')));
        if (agenda) button.append(node('span', dateLabel(start, end)));
        button.onclick = () => openEvent(event.id, button);
        return button;
    }
    function calendarGrid() {
        const table = node('table', null, 'calendar-table');
        table.setAttribute('aria-labelledby', 'calendar-month');
        const heading = table.createTHead().insertRow();
        for (const label of ['Hétfő', 'Kedd', 'Szerda', 'Csütörtök', 'Péntek', 'Szombat', 'Vasárnap']) {
            const cell = node('th');
            cell.scope = 'col';
            const short = node('abbr', label.slice(0, label === 'Szombat' || label === 'Szerda' ? 3 : 2));
            short.title = label;
            cell.append(short);
            heading.append(cell);
        }
        const body = table.createTBody();
        const day = date(month);
        const followingMonth = nextMonth(month, 1);
        day.setUTCDate(day.getUTCDate() - (day.getUTCDay() + 6) % 7);
        const currentDay = today();
        while (iso(day) < followingMonth) {
            const row = body.insertRow();
            for (let column = 0; column < 7; column++) {
                const key = iso(day);
                const cell = row.insertCell();
                if (key >= month && key < followingMonth) {
                    const label = node('time', day.getUTCDate());
                    label.dateTime = key;
                    cell.append(label);
                    if (key === currentDay) cell.setAttribute('aria-current', 'date');
                    for (const event of events) {
                        const [start, end] = dates(event);
                        if (key >= start && key <= end) cell.append(entry(event, false));
                    }
                }
                day.setUTCDate(day.getUTCDate() + 1);
            }
        }
        return table;
    }
    function draw() {
        const focused = results.contains(document.activeElement);
        if (mode === 'grid') results.replaceChildren(calendarGrid());
        else {
            const list = node('ul', null, 'calendar-agenda');
            for (const event of events) {
                const row = node('li');
                row.append(entry(event, true));
                list.append(row);
            }
            results.replaceChildren(list);
        }
        if (!events.length) results.append(node('p', 'Nincs esemény ebben a hónapban a megadott szűrőkkel.', 'empty'));
        if (focused) byId(mode === 'grid' ? 'grid' : 'list').focus();
    }
    function showLoadError(error) {
        byId('load-message').textContent = navigator.onLine ? error.message :
            'A naptár online érhető el. Az újbóli betöltéshez állítsd helyre a kapcsolatot.';
        byId('load-error').hidden = false;
    }
    async function loadPeriod() {
        if (!ready) return;
        const request = ++listRequest;
        const params = new URLSearchParams({from: month, until: nextMonth(month, 1)});
        const filters = {type: 'type', place: 'locationId', item: 'itemId', status: 'status'};
        for (const [control, name] of Object.entries(filters)) {
            const value = byId('filter-' + control).value;
            if (value) params.set(name, value);
        }
        byId('load-error').hidden = true;
        byId('loading').hidden = false;
        results.setAttribute('aria-busy', 'true');
        results.replaceChildren();
        try {
            const received = await api('calendar?' + params);
            if (request !== listRequest) return;
            events = received.filter(event => params.has('status') || event.status !== 'CANCELLED');
            draw();
        } catch (error) {
            if (request === listRequest) showLoadError(error);
        } finally {
            if (request === listRequest) {
                byId('loading').hidden = true;
                results.setAttribute('aria-busy', 'false');
            }
        }
    }
    function chooseMonth(value) {
        month = value;
        byId('month').textContent = monthFormat.format(date(month));
        loadPeriod();
    }
    function chooseView(value) {
        mode = value;
        byId('grid').setAttribute('aria-pressed', String(mode === 'grid'));
        byId('list').setAttribute('aria-pressed', String(mode === 'list'));
        if (results.firstChild && results.getAttribute('aria-busy') === 'false') draw();
    }

    function drawLinks() {
        const host = byId('item-links');
        host.replaceChildren();
        for (const [id, origin] of links) {
            const row = node('li');
            const item = items.get(id);
            const itemName = item?.name || 'Kapcsolt tárgy';
            const link = node('a', itemName);
            link.href = url('leltar/' + encodeURIComponent(id));
            const label = node('label', 'Kiindulási hely');
            const select = node('select');
            select.setAttribute('aria-label', 'Kiindulási hely – ' + itemName);
            populate(select, places, 'Nincs megadva');
            select.value = origin || '';
            select.onchange = () => links.set(id, select.value || null);
            label.append(select);
            const remove = node('button', 'Eltávolítás');
            remove.type = 'button';
            remove.setAttribute('aria-label', 'Eltávolítás – ' + itemName);
            remove.onclick = () => { links.delete(id); drawLinks(); byId('add-item').focus(); };
            row.append(link, label, remove);
            host.append(row);
        }
        populate(byId('add-item'), [...items].filter(([id, item]) => !links.has(id) && !item.trashed)
            .map(([id, item]) => [id, item.name]), 'Válassz tárgyat');
        byId('attach').disabled = links.size >= 100 || !byId('add-item').value;
    }
    function syncDates() {
        const done = active.status === 'DONE';
        const multiDay = fields.plannedStart.value && fields.plannedEnd.value > fields.plannedStart.value;
        byId('actual').hidden = !done;
        byId('actual').disabled = !done;
        fields.plannedStart.required = !done || !!fields.plannedEnd.value;
        fields.actualStart.required = done;
        fields.actualEnd.required = done && !!multiDay;
        for (const [start, end] of [[fields.plannedStart, fields.plannedEnd], [fields.actualStart, fields.actualEnd]]) {
            end.setCustomValidity(start.value && end.value && end.value < start.value ?
                'A zárónap nem előzheti meg a kezdőnapot.' : '');
        }
    }
    function setStatus(status) {
        active.status = status;
        byId('editor-state').textContent = statuses[status];
        byId('complete').hidden = status !== 'PLANNED';
        byId('cancel-event').hidden = !active.saved || status !== 'PLANNED';
        byId('revert').hidden = active.initialStatus !== 'PLANNED' || status === 'PLANNED';
        syncDates();
    }
    function payload() {
        const result = {id: active.id, status: active.status};
        if (active.saved) result.version = active.version;
        for (const key of ['title', 'type', 'locationId', 'notes', 'plannedStart', 'plannedEnd', 'actualStart', 'actualEnd'])
            result[key] = fields[key].value || null;
        if (active.status !== 'DONE') result.actualStart = result.actualEnd = null;
        result.items = [...links].map(([itemId, originLocationId]) => ({itemId, originLocationId}));
        return result;
    }
    const dirty = () => active && JSON.stringify(payload()) !== baseline;
    const canLeave = () => !saving && (!dirty() || confirm('Elveted az esemény nem mentett módosításait?'));

    function displayWarnings(event) {
        const conflicts = (event.warnings || []).flatMap(warning => warning.eventIds).map(id => ({id}));
        notice(byId('warnings'), conflicts.length ?
            'A tárgy másik eladási vagy költözési tervének időpontja átfedésben van ezzel az eseménnyel.' : '', conflicts);
    }
    function fillEditor(event, trigger) {
        active = {id: event.id || crypto.randomUUID(), version: event.version, saved: !!event.id, initialStatus: event.status};
        opener = trigger;
        uncertain = false;
        links = new Map((event.items || []).map(link => [link.itemId, link.originLocationId]));
        for (const name of ['title', 'type', 'locationId', 'notes', 'plannedStart', 'plannedEnd', 'actualStart', 'actualEnd'])
            fields[name].value = event[name] || '';
        byId('editor-title').textContent = event.title || 'Új esemény';
        setStatus(event.status);
        drawLinks();
        baseline = JSON.stringify(payload());
        notice(byId('error'), '');
        displayWarnings(event);
        editor.hidden = false;
        form.inert = false;
        byId('editor-title').focus();
    }
    async function openEvent(id, trigger) {
        if (opening || !canLeave()) return;
        const request = ++detailRequest;
        opening = true;
        form.inert = true;
        if (trigger) trigger.disabled = true;
        notice(byId('feedback'), 'Az esemény betöltése…');
        try {
            const event = await api('calendar/' + encodeURIComponent(id));
            if (request !== detailRequest) return;
            notice(byId('feedback'), '');
            fillEditor(event, trigger);
        } catch (error) {
            if (request === detailRequest) notice(byId('feedback'), error.message);
        } finally {
            if (trigger) trigger.disabled = false;
            if (request === detailRequest) {
                opening = false;
                form.inert = false;
            }
        }
    }
    function newEvent(trigger, itemId) {
        if (!canLeave()) return;
        detailRequest++;
        opening = false;
        const item = items.get(itemId);
        const selected = item && !item.trashed ? [{itemId, originLocationId: item.location_id || null}] : [];
        notice(byId('feedback'), itemId && !selected.length ? 'A tárgy már nem választható ki.' : '');
        fillEditor({type: 'OTHER', status: 'PLANNED',
            plannedStart: month === monthStart(today()) ? today() : month, items: selected}, trigger);
    }
    function matchesSaved(receipt, draft) {
        const keys = ['id', 'title', 'type', 'status', 'locationId', 'notes', 'plannedStart', 'plannedEnd', 'actualStart', 'actualEnd'];
        const expected = {...draft, title: draft.title.trim(),
            plannedEnd: draft.plannedEnd || draft.plannedStart, actualEnd: draft.actualEnd || draft.actualStart};
        return keys.every(key => receipt[key] === expected[key])
            && receipt.items.length === draft.items.length
            && receipt.items.every(link => draft.items.some(item =>
                item.itemId === link.itemId && item.originLocationId === link.originLocationId));
    }
    async function saveEvent(event) {
        event.preventDefault();
        if (saving) return;
        const draft = payload();
        const route = 'calendar/' + encodeURIComponent(active.id);
        saving = form.inert = true;
        notice(byId('feedback'), '');
        notice(byId('error'), '');
        try {
            let saved;
            try {
                saved = await api(active.saved ? route : 'calendar', {
                    method: active.saved ? 'PUT' : 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(draft)
                });
            } catch (error) {
                uncertain ||= !!error.outcomeUnknown;
                if (!uncertain) throw error;
                const receipt = await api(route).catch(() => null);
                if (!receipt || !matchesSaved(receipt, draft)) throw error;
                saved = receipt;
            }
            fillEditor(saved, opener);
            if (new URLSearchParams(location.search).has('item')) {
                history.replaceState(null, '', url('leltar/naptar?event=' + encodeURIComponent(saved.id)));
            }
            notice(byId('feedback'), 'Esemény mentve.');
            chooseMonth(monthStart(dates(saved)[0]));
        } catch (error) {
            const conflicts = error.problem?.conflictingEvents || (error.status === 409 && active.saved ?
                [{id: active.id, title: 'Aktuális szerverváltozat'}] : []);
            notice(byId('error'), error.message + ' A kitöltött mezők megmaradtak.', conflicts);
        } finally {
            saving = form.inert = false;
        }
    }

    async function initialize() {
        if (initializing) return;
        initializing = true;
        byId('load-error').hidden = true;
        byId('loading').hidden = false;
        try {
            await AccountAuth.init();
            const [locations, current, trashed] = await Promise.all([api('places'), api('items?trash=false'), api('items?trash=true')]);
            places = new Map(locations.map(place => [place.id, place.name]));
            items = new Map([...current, ...trashed].sort((a, b) => collator.compare(a.name, b.name)).map(item => [item.id, item]));
            populate(fields.type, Object.entries(types));
            populate(fields.locationId, places, 'Nincs megadva');
            populate(byId('filter-type'), Object.entries(types), 'Minden típus');
            populate(byId('filter-place'), places, 'Minden helyszín');
            populate(byId('filter-item'), [...items].map(([id, item]) => [id, item.name]), 'Minden tárgy');
            ready = true;
            byId('new').disabled = false;
            const launch = initialLink;
            initialLink = new URLSearchParams();
            if (launch.has('event')) {
                await openEvent(launch.get('event'), byId('new'));
                if (active) {
                    const saved = payload();
                    month = monthStart(dates({...saved, plannedEnd: saved.plannedEnd || saved.plannedStart})[0]);
                }
            } else if (launch.has('item')) newEvent(byId('new'), launch.get('item'));
            chooseMonth(month);
        } catch (error) {
            showLoadError(error);
            byId('loading').hidden = true;
            results.setAttribute('aria-busy', 'false');
        } finally {
            initializing = false;
        }
    }

    document.querySelector('.tabs a[href="' + url('leltar/naptar') + '"]')?.setAttribute('aria-current', 'page');
    byId('month').textContent = monthFormat.format(date(month));
    chooseView(mode);
    byId('previous').onclick = () => chooseMonth(nextMonth(month, -1));
    byId('next').onclick = () => chooseMonth(nextMonth(month, 1));
    byId('today').onclick = () => chooseMonth(monthStart(today()));
    byId('grid').onclick = () => chooseView('grid');
    byId('list').onclick = () => chooseView('list');
    document.querySelectorAll('.calendar-filters select').forEach(control => control.onchange = loadPeriod);
    byId('retry').onclick = () => ready ? loadPeriod() : initialize();
    byId('new').onclick = () => newEvent(byId('new'));
    byId('add-item').onchange = () => { byId('attach').disabled = !byId('add-item').value; };
    byId('attach').onclick = () => {
        const id = byId('add-item').value;
        if (!id) return;
        links.set(id, items.get(id).location_id || null);
        drawLinks();
        byId('add-item').focus();
    };
    byId('complete').onclick = () => {
        fields.actualStart.value ||= today();
        setStatus('DONE');
        fields.actualStart.focus();
    };
    byId('cancel-event').onclick = () => { setStatus('CANCELLED'); form.querySelector('[type=submit]').focus(); };
    byId('revert').onclick = () => { setStatus('PLANNED'); fields.title.focus(); };
    byId('close').onclick = () => {
        if (!canLeave()) return;
        detailRequest++;
        opening = false;
        active = null;
        editor.hidden = true;
        (opener?.isConnected ? opener : byId('new')).focus();
    };
    form.addEventListener('input', syncDates);
    form.addEventListener('submit', saveEvent);
    window.addEventListener('beforeunload', event => {
        if (saving || dirty()) { event.preventDefault(); event.returnValue = true; }
    });
    window.addEventListener('online', () => ready ? loadPeriod() : initialize());
    window.addEventListener('offline', () => {
        listRequest++;
        events = [];
        results.replaceChildren();
        results.setAttribute('aria-busy', 'false');
        byId('loading').hidden = true;
        showLoadError(new Error('A naptárhoz hálózati kapcsolat szükséges.'));
    });
    document.addEventListener('otthontar-reauthenticated', () => {
        if (active) notice(byId('feedback'), 'Újra bejelentkeztél. A kitöltött mezők megmaradtak.');
        if (ready) loadPeriod();
        else initialize();
    });
    initialize();
})();
