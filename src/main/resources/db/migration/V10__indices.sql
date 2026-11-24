SET lock_timeout = '5s';

CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_geracao_data ON public.geracao (data);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_compra_cartao_id ON public.compra (cartao_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_conta_categoria_id ON public.conta (categoria_id);

DROP INDEX CONCURRENTLY IF EXISTS public.idx_categorias_nome;
DROP INDEX CONCURRENTLY IF EXISTS public.idx_categorias_ativo;
ALTER TABLE public.pessoa DROP CONSTRAINT IF EXISTS pessoa_email_key;

DO $$
DECLARE
    invalidos text;
BEGIN
    SELECT string_agg(c.relname, ', ') INTO invalidos
    FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid
    WHERE NOT i.indisvalid AND c.relname IN ('ix_geracao_data', 'ix_compra_cartao_id', 'ix_conta_categoria_id');
    IF invalidos IS NOT NULL THEN
        RAISE EXCEPTION 'Índices que ficaram inválidos: %. Apague cada um com DROP INDEX CONCURRENTLY e rode o script de novo', invalidos;
    END IF;
END $$;

ANALYZE public.geracao;
ANALYZE public.compra;
ANALYZE public.conta;
