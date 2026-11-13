-- Futtasd, helyezd át a tárgyat a böngészőben, majd futtasd ismét.
SELECT i.id, i.name AS targy, l.name AS helyszin,
       i.version AS verzio, i.status AS allapot, i.asking AS iranyar,
       i.actual AS eladasi_ar, i.sold_date AS eladas_datuma
FROM item AS i
JOIN location AS l ON l.id = i.location_id
WHERE i.trashed = FALSE
ORDER BY i.name, i.id;
