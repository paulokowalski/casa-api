BEGIN;
SET LOCAL lock_timeout = '5s';

DO $$
DECLARE
    troca record;
BEGIN
    FOR troca IN
        SELECT * FROM (VALUES
            ('public', 'compra', 'nm_produto', 'produto'),
            ('public', 'compra', 'vl_produto', 'valor'),
            ('public', 'compra', 'dt_compra', 'data_compra'),
            ('public', 'compra', 'nr_parcelas', 'numero_parcelas'),
            ('public', 'compra', 'dt_cadastrato', 'criado_em'),
            ('public', 'compra_parcela', 'nr_parcela', 'numero'),
            ('public', 'compra_parcela', 'vl_parcela', 'valor'),
            ('public', 'compra_parcela', 'dt_parcela', 'data'),
            ('public', 'categorias', 'created_at', 'criado_em'),
            ('public', 'categorias', 'updated_at', 'atualizado_em'),
            ('public', 'conta', 'dt_cadastro', 'criado_em'),
            ('crescimento', 'foto', 'criada_em', 'criado_em'),
            ('passagens', 'alerta', 'em', 'criado_em')
        ) AS t(esquema, tabela, antigo, novo)
    LOOP
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = troca.esquema AND table_name = troca.tabela AND column_name = troca.antigo) THEN
            EXECUTE format('ALTER TABLE %I.%I RENAME COLUMN %I TO %I', troca.esquema, troca.tabela, troca.antigo, troca.novo);
        END IF;
    END LOOP;
END $$;

ALTER INDEX IF EXISTS public.idx_compra_parcela_dt_parcela RENAME TO ix_compra_parcela_data;

COMMIT;
