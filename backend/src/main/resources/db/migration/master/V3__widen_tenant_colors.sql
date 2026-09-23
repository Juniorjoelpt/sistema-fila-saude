-- cor_primaria/cor_secundaria estavam VARCHAR(7) (só cabia "#rrggbb" exato). Texto digitado
-- livremente (com espaço, sem "#", nome de cor, etc.) estourava e quebrava o salvamento da
-- identidade visual (item 3.6) com "Data too long for column 'cor_primaria'".
ALTER TABLE tenants
    MODIFY COLUMN cor_primaria VARCHAR(30) NULL,
    MODIFY COLUMN cor_secundaria VARCHAR(30) NULL;
