/* PWA lifecycle is independent of the inventory API and persistence layer. */
'use strict';
(() => {
    const status = document.querySelector('#connection-status');
    const install = document.querySelector('#pwa-help');
    const note = document.querySelector('#pwa-update');
    const connectivity = () => {
        status.textContent = navigator.onLine
            ? 'Online mentéshez elérhető szerver szükséges.'
            : 'Nincs hálózat. A piszkozatot tartsd nyitva; offline mentés még nincs.';
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
            const registration = await navigator.serviceWorker.register('/sw.js', {scope: '/leltar', updateViaCache: 'none'});
            const waiting = () => {
                if (registration.waiting) {
                    note.hidden = false;
                    note.textContent = 'Frissítés érhető el. Mentsd az adatlapot, majd zárd be az összes Otthontár-lapot és nyisd meg újra.';
                }
            };
            waiting();
            registration.addEventListener('updatefound', () => {
                registration.installing?.addEventListener('statechange', waiting);
            });
        } catch {
            note.hidden = false;
            note.textContent = 'Az offline alapfelület előkészítése nem sikerült. Online tovább használhatod az alkalmazást; később nyisd meg újra.';
        }
    });
})();
