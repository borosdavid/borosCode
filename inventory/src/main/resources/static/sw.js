/* Bump VERSION whenever a shell asset changes. Never cache inventory or photos. */
'use strict';
const VERSION = 'otthontar-shell-v2';
const ASSETS = ['/offline.html','/inventory.css','/inventory.js','/auth.js','/photo-input.js','/pwa.js',
    '/manifest.webmanifest','/icons/icon-180.png','/icons/icon-192.png','/icons/icon-512.png'];
self.addEventListener('install', event => {
    // addAll is atomic; a failed installation leaves the active worker/cache intact.
    // No skipWaiting: an open editor must not switch application versions mid-save.
    event.waitUntil(caches.open(VERSION).then(cache => cache.addAll(ASSETS)));
});
self.addEventListener('activate', event => {
    event.waitUntil((async () => {
        for (const key of await caches.keys()) {
            if (key.startsWith('otthontar-shell-') && key !== VERSION) await caches.delete(key);
        }
        await self.clients.claim();
    })());
});
self.addEventListener('fetch', event => {
    const request = event.request, url = new URL(request.url);
    if (request.method !== 'GET' || url.origin !== self.location.origin) return;
    // API (including photographs), auth and all unknown resources are network-only.
    if (url.pathname === '/api' || url.pathname.startsWith('/api/')) return;
    if (request.mode === 'navigate' && (url.pathname === '/leltar' || url.pathname.startsWith('/leltar/'))) {
        event.respondWith(fetch(request).catch(() => caches.open(VERSION).then(cache => cache.match('/offline.html'))));
    } else if (!url.search && ASSETS.includes(url.pathname)) {
        event.respondWith(caches.open(VERSION).then(async cache => (await cache.match(url.pathname)) || fetch(request)));
    }
});
