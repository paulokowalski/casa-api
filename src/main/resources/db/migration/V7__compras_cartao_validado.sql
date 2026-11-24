BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE public.compra VALIDATE CONSTRAINT compra_cartao_id_not_null;
COMMIT;
