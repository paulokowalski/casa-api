ALTER TABLE pessoa ADD COLUMN IF NOT EXISTS senha_hash varchar(100);
ALTER TABLE pessoa ADD COLUMN IF NOT EXISTS papel varchar(10) NOT NULL DEFAULT 'USUARIO'
    CHECK (papel IN ('ADMIN', 'USUARIO'));
ALTER TABLE pessoa ADD COLUMN IF NOT EXISTS ativo boolean NOT NULL DEFAULT true;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pessoa_email ON pessoa (lower(email));

SELECT c.nm_pessoa_compra, count(*) AS compras
FROM compra c
WHERE NOT EXISTS (SELECT 1 FROM pessoa p WHERE upper(trim(p.nome)) = upper(trim(c.nm_pessoa_compra)))
GROUP BY c.nm_pessoa_compra;
