/* Originals stay in the editor's pending list until the server confirms saving. */
'use strict';
window.OtthontarPhotos = (() => {
    const MAX_FILE = 40 * 1024 * 1024;
    const WIFI_ONLY_BYTES = 100_000_000;
    const PROXY_REQUEST_BUDGET = 90_000_000;
    const requestLimitMiB = Number(document.body.dataset.uploadLimitMib || 200);
    const batchLimitMiB = Number(document.body.dataset.photoBatchLimitMib || 400);
    const maxBatch = batchLimitMiB * 1024 * 1024;
    async function prepare(original) {
        if (!original.size || original.size > MAX_FILE) throw Error('A fotó legfeljebb 40 MiB lehet. Az eredeti fájl megmarad; válassz kisebb képet.');
        const bytes = new Uint8Array(await original.slice(0,3).arrayBuffer());
        const jpeg = bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff;
        // Preserve existing JPEG bytes, including EXIF. Browser previews honor orientation;
        // the imaging service normalizes the generated thumbnail separately.
        if (jpeg) return new File([original], original.name.replace(/\.[^.]+$/, '').slice(0,240)+'.jpg', {type:'image/jpeg'});
        let bitmap;
        try {
            bitmap = await createImageBitmap(original, {imageOrientation:'from-image'});
            if (bitmap.width * bitmap.height > 40_000_000) throw Error('Túl nagy képfelbontás.');
            const canvas = document.createElement('canvas');
            canvas.width = bitmap.width; canvas.height = bitmap.height;
            const ctx = canvas.getContext('2d');
            ctx.fillStyle = '#fff'; ctx.fillRect(0,0,canvas.width,canvas.height);
            ctx.drawImage(bitmap,0,0);
            const converted = await new Promise(resolve => canvas.toBlob(resolve,'image/jpeg',0.92));
            if (!converted || converted.type !== 'image/jpeg' || converted.size > MAX_FILE) throw Error('A JPEG nem fér el a méretkorlátban.');
            return new File([converted],original.name.replace(/\.[^.]+$/, '').slice(0,240)+'.jpg',{type:'image/jpeg'});
        } catch {
            throw Error('Ez a kép nem alakítható JPEG-gé ezen a böngészőn (például HEIC/HEIF). Az eredetit megtartottuk ezen a lapon. Válassz JPEG-et vagy állítsd az iPhone kamerát a Leginkább kompatibilis formátumra.');
        } finally { bitmap?.close(); }
    }
    function uploadPlan(files, draftBytes = 0) {
        const bytes = files.reduce((sum,file)=>sum+file.size, 0);
        if (bytes > maxBatch)
            throw Error(`Egy mentésben legfeljebb ${batchLimitMiB} MiB új fotó küldhető. Vegyél ki néhány képet; a piszkozat megmarad.`);
        // Keep legacy multipart requests comfortably below the proxy and multipart limits.
        const wireBytes = bytes + draftBytes + 1024 * (files.length + 2);
        return {wifiOnly: bytes > WIFI_ONLY_BYTES,
            staged: files.length > 20 || wireBytes > Math.min(PROXY_REQUEST_BUDGET, requestLimitMiB * 1024 * 1024)};
    }

    async function wifiUpload(confirmSettings) {
        const network = navigator.connection;
        const state = () => {
            if (!navigator.onLine) return 'blocked';
            if (network?.type === 'wifi') return 'wifi';
            return !network?.type || ['other', 'unknown'].includes(network.type) ? 'unverified' : 'blocked';
        };
        const reason = '100 MB feletti fotófeltöltéshez Wi-Fi kell. A képek megmaradnak ezen a lapon.';
        const manual = state() === 'unverified' && await confirmSettings();
        if (state() !== 'wifi' && !manual) throw Error(reason);
        const controller = new AbortController();
        const allowed = () => state() === 'wifi' || (manual && state() === 'unverified');
        const changed = () => { if (!allowed()) controller.abort(); };
        const check = () => { if (controller.signal.aborted || !allowed()) throw Error(reason); };
        network?.addEventListener('change', changed);
        window.addEventListener('offline', changed);
        return {
            signal: controller.signal, check,
            close() {
                network?.removeEventListener('change', changed);
                window.removeEventListener('offline', changed);
            }
        };
    }

    return {prepare, uploadPlan, wifiUpload};
})();
