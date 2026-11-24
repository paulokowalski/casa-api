BEGIN;
SET LOCAL lock_timeout = '5s';
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'compra' AND column_name = 'nm_cartao') THEN
        EXECUTE 'ALTER TABLE public.compra ALTER COLUMN nm_cartao DROP NOT NULL';
    END IF;
END $$;
COMMIT;
