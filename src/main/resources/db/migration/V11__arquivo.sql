BEGIN;
SET LOCAL lock_timeout = '5s';

CREATE SCHEMA IF NOT EXISTS arquivo;

ALTER TABLE IF EXISTS public.transacao SET SCHEMA arquivo;
ALTER TABLE IF EXISTS public.lista_compra SET SCHEMA arquivo;
ALTER TABLE IF EXISTS public.rastreamento_detalhe SET SCHEMA arquivo;
ALTER TABLE IF EXISTS public.rastreamento SET SCHEMA arquivo;
ALTER TABLE IF EXISTS public.geracao_mensal SET SCHEMA arquivo;
ALTER TABLE IF EXISTS financa.cartao SET SCHEMA arquivo;
ALTER TABLE IF EXISTS financa.banco SET SCHEMA arquivo;
ALTER TABLE IF EXISTS investimento.tb_investimento_renda_fixa SET SCHEMA arquivo;
ALTER TABLE IF EXISTS investimento.tb_carteira SET SCHEMA arquivo;

DO $$
DECLARE
    esquema text;
BEGIN
    FOREACH esquema IN ARRAY ARRAY['financa', 'investimento'] LOOP
        IF EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = esquema)
           AND NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = esquema)
           AND NOT EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace WHERE n.nspname = esquema)
           AND NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE n.nspname = esquema) THEN
            EXECUTE format('DROP SCHEMA %I', esquema);
        END IF;
    END LOOP;
END $$;

COMMIT;
