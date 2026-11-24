'use strict';
window.AccountAuth = (() => {
    // Never persist a password or session credential in localStorage/IndexedDB.
    const appRoot = document.body.dataset.appRoot || '/';
    const url = path => appRoot + path;
    let account = null;
    const dialog = document.createElement('dialog');
    dialog.setAttribute('aria-labelledby', 'reauth-title');
    dialog.innerHTML = '<form><h2 id="reauth-title">Újbóli bejelentkezés</h2>' +
        '<p>A nyitott piszkozat és a fotók ezen a lapon megmaradnak. Belépés után ismét nyomd meg a Mentés gombot.</p>' +
        '<label>Felhasználónév<input name="username" autocomplete="username" maxlength="64" required></label>' +
        '<label>Jelszó<input name="password" type="password" autocomplete="current-password" required></label>' +
        '<p role="alert" data-error></p><button class="primary">Bejelentkezés</button>' +
        '<button type="button" data-close>Mégse</button></form>';
    document.body.append(dialog);
    const form = dialog.querySelector('form');
    const error = dialog.querySelector('[data-error]');
    dialog.querySelector('[data-close]').onclick = () => dialog.close();
    dialog.addEventListener('close', () => { form.elements.password.value = ''; });

    function expired() {
        error.textContent = '';
        form.elements.username.value = account?.username || '';
        form.elements.username.readOnly = !!account;
        if (!dialog.open) dialog.showModal();
    }
    async function session() {
        const response = await fetch(url('api/session'), {cache:'no-store', credentials:'same-origin'});
        if (response.status === 401) expired();
        if (!response.ok) throw new Error('Jelentkezz be újra. A nyitott piszkozat megmarad.');
        return response.json();
    }
    async function init() {
        const current = await session();
        if (account && current.id !== account.id) throw new Error('Másik fiók van bejelentkezve. A piszkozatot nem töltjük fel.');
        account = current;
        return account;
    }
    async function writeHeaders() {
        const current = await session();
        if (!account || current.id !== account.id) {
            expired();
            throw new Error('A fiók megváltozott. Jelentkezz be az eredeti fiókkal; a piszkozat megmarad.');
        }
        return {[current.headerName]:current.token, 'X-Otthontar-User':account.id};
    }
    form.onsubmit = async event => {
        event.preventDefault();
        error.textContent = '';
        const button = form.querySelector('button');
        button.disabled = true;
        try {
            const tokenResponse = await fetch(url('auth/csrf'), {cache:'no-store', credentials:'same-origin'});
            if (!tokenResponse.ok) throw new Error('A bejelentkezés most nem érhető el.');
            const csrf = await tokenResponse.json();
            const response = await fetch(url('login'), {method:'POST', credentials:'same-origin', cache:'no-store',
                headers:{'Accept':'application/json', [csrf.headerName]:csrf.token},
                body:new URLSearchParams({username:form.elements.username.value, password:form.elements.password.value})});
            if (!response.ok) throw new Error('Sikertelen bejelentkezés. Ellenőrizd az adatokat, vagy várj a próbálkozási korlát feloldásáig.');
            await init();
            dialog.close();
            document.dispatchEvent(new Event('otthontar-reauthenticated'));
        } catch (failure) { error.textContent = failure.message; }
        finally { form.elements.password.value = ''; button.disabled = false; }
    };
    return {init, writeHeaders, expired, url};
})();
