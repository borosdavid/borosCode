/* Originals stay in the editor's pending list until the server confirms saving. */
'use strict';
window.OtthontarPhotos = (() => {
    const MAX_FILE = 20 * 1024 * 1024;
    const requestLimitMiB = Number(document.body.dataset.uploadLimitMib || 200);
    const MAX_REQUEST = requestLimitMiB * 1024 * 1024;
    async function prepare(original) {
        if (!original.size || original.size > MAX_FILE) throw Error('A fotó legfeljebb 20 MiB lehet. Az eredeti fájl megmarad; válassz kisebb képet.');
        const bytes = new Uint8Array(await original.arrayBuffer());
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
    function validateTotal(files, draftBytes = 0) {
        // Leave room for multipart field headers and boundaries, not only photo bytes.
        if (files.reduce((sum,file)=>sum+file.size, draftBytes) + 1024 * (files.length+2) > MAX_REQUEST)
            throw Error(`A mentés legfeljebb ${requestLimitMiB} MiB lehet a teljes űrlappal együtt. Vegyél ki néhány képet; a piszkozat megmarad.`);
    }
    return {prepare,validateTotal};
})();
