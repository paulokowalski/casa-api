BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE public.compra DROP COLUMN IF EXISTS nm_cartao;
COMMIT;
