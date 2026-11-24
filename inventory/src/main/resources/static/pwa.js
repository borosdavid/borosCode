/* PWA lifecycle is independent of the inventory API and persistence layer. */
'use strict';
(() => {
    const status = document.querySelector('#connection-status');
    const install = document.querySelector('#pwa-help');
    const note = document.querySelector('#pwa-update');
    const connectivity = () => {
        status.hidden = navigator.onLine;
        status.textContent = navigator.onLine ? '' : 'Nincs hálózat. A piszkozatot hagyd nyitva, amíg újra elérhető a szerver.';
    };
    connectivity();
    window.addEventListener('online', connectivity);
    window.addEventListener('offline', connectivity);
    if (!window.isSecureContext || !('serviceWorker' in navigator)) {
        install.textContent = 'A telepíthető PWA-hoz és a Service Workerhez megbízható HTTPS szükséges. A számítógép localhost címe csak helyi fejlesztési kivétel.';
        return;
    }
    install.textContent = 'iPhone Safari: Megosztás → Hozzáadás a Főképernyőhöz. Telefonon előbb beállított, megbízható HTTPS-elérés szükséges.';
    window.addEventListener('load', async () => {
        try {
            const registration = await navigator.serviceWorker.register(AccountAuth.url('sw.js'), {scope: AccountAuth.url('leltar'), updateViaCache: 'none'});
            const waiting = () => {
                const updateReady = Boolean(registration.active && registration.waiting);
                note.hidden = !updateReady;
                if (updateReady) note.textContent = 'Frissítés érhető el. Mentsd az adatlapot, majd zárd be az Otthontár-lapokat, és nyisd meg újra.';
            };
            waiting();
            navigator.serviceWorker.addEventListener('controllerchange', waiting);
            registration.addEventListener('updatefound', () => {
                registration.installing?.addEventListener('statechange', waiting);
            });
        } catch {
            note.hidden = false;
            note.textContent = 'Az offline alapfelület előkészítése nem sikerült. Online tovább használhatod az alkalmazást; később nyisd meg újra.';
        }
    });
})();
