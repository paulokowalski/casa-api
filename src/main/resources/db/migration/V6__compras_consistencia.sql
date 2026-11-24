BEGIN;
SET LOCAL lock_timeout = '5s';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint
               WHERE conname = 'compra_cartao_id_fkey' AND conrelid = 'public.compra'::regclass
                 AND confrelid <> 'public.tipo_lancamento'::regclass) THEN
        ALTER TABLE public.compra DROP CONSTRAINT compra_cartao_id_fkey;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'compra_cartao_id_fkey' AND conrelid = 'public.compra'::regclass) THEN
        ALTER TABLE public.compra ADD CONSTRAINT compra_cartao_id_fkey
            FOREIGN KEY (cartao_id) REFERENCES public.tipo_lancamento (id) NOT VALID;
    END IF;
END $$;

UPDATE public.compra c
SET cartao_id = t.id
FROM public.tipo_lancamento t
WHERE c.cartao_id IS NULL
  AND upper(trim(t.nome)) = upper(trim(c.nm_cartao));

ALTER TABLE public.compra VALIDATE CONSTRAINT compra_cartao_id_fkey;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'compra_cartao_id_not_null' AND conrelid = 'public.compra'::regclass) THEN
        ALTER TABLE public.compra ADD CONSTRAINT compra_cartao_id_not_null NOT NULL cartao_id NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.pessoa GROUP BY upper(trim(nome)) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Há pessoas com o mesmo nome; deixe os nomes únicos antes de aplicar este script';
    END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS uq_pessoa_nome ON public.pessoa (upper(trim(nome)));

ALTER TABLE public.compra ADD COLUMN IF NOT EXISTS pessoa_id bigint;

CREATE OR REPLACE FUNCTION public.compra_preencher_pessoa() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.nm_pessoa_compra IS NOT NULL THEN
        NEW.pessoa_id := (SELECT p.id FROM public.pessoa p
                          WHERE upper(trim(p.nome)) = upper(trim(NEW.nm_pessoa_compra)));
        IF NEW.pessoa_id IS NULL THEN
            RAISE EXCEPTION 'Não existe pessoa cadastrada com o nome %', NEW.nm_pessoa_compra
                USING ERRCODE = 'foreign_key_violation';
        END IF;
    END IF;
    RETURN NEW;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'compra' AND column_name = 'nm_pessoa_compra') THEN
        EXECUTE 'UPDATE public.compra c SET pessoa_id = p.id FROM public.pessoa p '
             || 'WHERE c.pessoa_id IS NULL AND upper(trim(p.nome)) = upper(trim(c.nm_pessoa_compra))';
        EXECUTE 'ALTER TABLE public.compra ALTER COLUMN nm_pessoa_compra DROP NOT NULL';
        EXECUTE 'CREATE OR REPLACE TRIGGER compra_preencher_pessoa '
             || 'BEFORE INSERT OR UPDATE OF nm_pessoa_compra ON public.compra '
             || 'FOR EACH ROW EXECUTE FUNCTION public.compra_preencher_pessoa()';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'compra_pessoa_id_fkey' AND conrelid = 'public.compra'::regclass) THEN
        ALTER TABLE public.compra ADD CONSTRAINT compra_pessoa_id_fkey
            FOREIGN KEY (pessoa_id) REFERENCES public.pessoa (id) NOT VALID;
    END IF;
END $$;
ALTER TABLE public.compra VALIDATE CONSTRAINT compra_pessoa_id_fkey;
ALTER TABLE public.compra ALTER COLUMN pessoa_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_compra_pessoa_id ON public.compra (pessoa_id);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'uq_compra_parcela_numero' AND conrelid = 'public.compra_parcela'::regclass) THEN
        ALTER TABLE public.compra_parcela ADD CONSTRAINT uq_compra_parcela_numero UNIQUE (compra_id, nr_parcela);
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint
               WHERE conname = 'compra_parcela_compra_id_fkey' AND conrelid = 'public.compra_parcela'::regclass
                 AND confdeltype <> 'c') THEN
        ALTER TABLE public.compra_parcela DROP CONSTRAINT compra_parcela_compra_id_fkey;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'compra_parcela_compra_id_fkey' AND conrelid = 'public.compra_parcela'::regclass) THEN
        ALTER TABLE public.compra_parcela ADD CONSTRAINT compra_parcela_compra_id_fkey
            FOREIGN KEY (compra_id) REFERENCES public.compra (id) ON DELETE CASCADE NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'ck_compra_parcela_numero' AND conrelid = 'public.compra_parcela'::regclass) THEN
        ALTER TABLE public.compra_parcela ADD CONSTRAINT ck_compra_parcela_numero CHECK (nr_parcela >= 1) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'ck_compra_numero_parcelas' AND conrelid = 'public.compra'::regclass) THEN
        ALTER TABLE public.compra ADD CONSTRAINT ck_compra_numero_parcelas CHECK (nr_parcelas >= 1) NOT VALID;
    END IF;
END $$;
ALTER TABLE public.compra_parcela VALIDATE CONSTRAINT compra_parcela_compra_id_fkey;
ALTER TABLE public.compra_parcela VALIDATE CONSTRAINT ck_compra_parcela_numero;
ALTER TABLE public.compra VALIDATE CONSTRAINT ck_compra_numero_parcelas;

DROP INDEX IF EXISTS public.idx_compra_parcela_compra_id;

COMMIT;

ANALYZE public.compra;
ANALYZE public.compra_parcela;
