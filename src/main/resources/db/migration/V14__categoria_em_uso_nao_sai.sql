BEGIN;
SET LOCAL lock_timeout = '5s';

ALTER TABLE public.compra DROP CONSTRAINT IF EXISTS fk_compras_categoria;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'compra_categoria_id_fkey' AND conrelid = 'public.compra'::regclass) THEN
        ALTER TABLE public.compra ADD CONSTRAINT compra_categoria_id_fkey
            FOREIGN KEY (categoria_id) REFERENCES public.categorias (id) ON DELETE RESTRICT NOT VALID;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint
               WHERE conname = 'conta_categoria_id_fkey' AND conrelid = 'public.conta'::regclass
                 AND confdeltype <> 'r') THEN
        ALTER TABLE public.conta DROP CONSTRAINT conta_categoria_id_fkey;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'conta_categoria_id_fkey' AND conrelid = 'public.conta'::regclass) THEN
        ALTER TABLE public.conta ADD CONSTRAINT conta_categoria_id_fkey
            FOREIGN KEY (categoria_id) REFERENCES public.categorias (id) ON DELETE RESTRICT NOT VALID;
    END IF;
END $$;

ALTER TABLE public.compra VALIDATE CONSTRAINT compra_categoria_id_fkey;
ALTER TABLE public.conta VALIDATE CONSTRAINT conta_categoria_id_fkey;

COMMIT;
