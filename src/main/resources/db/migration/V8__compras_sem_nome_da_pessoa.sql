BEGIN;
SET LOCAL lock_timeout = '5s';
DROP TRIGGER IF EXISTS compra_preencher_pessoa ON public.compra;
DROP FUNCTION IF EXISTS public.compra_preencher_pessoa();
DROP INDEX IF EXISTS public.idx_compra_pessoa;
ALTER TABLE public.compra DROP COLUMN IF EXISTS nm_pessoa_compra;
COMMIT;
