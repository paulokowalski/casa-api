SET lock_timeout = '5s';

CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_crianca_foto_id ON crescimento.crianca (foto_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_foto_familia_id ON crescimento.foto (familia_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_registro_medicao_id ON crescimento.registro (medicao_id);

DO $$
DECLARE
    invalidos text;
BEGIN
    SELECT string_agg(c.relname, ', ') INTO invalidos
    FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid
    WHERE NOT i.indisvalid AND c.relname IN ('ix_crianca_foto_id', 'ix_foto_familia_id', 'ix_registro_medicao_id');
    IF invalidos IS NOT NULL THEN
        RAISE EXCEPTION 'Índices que ficaram inválidos: %. Apague cada um com DROP INDEX CONCURRENTLY e rode o script de novo', invalidos;
    END IF;
END $$;
