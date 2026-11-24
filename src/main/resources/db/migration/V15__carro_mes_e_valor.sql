BEGIN;
SET LOCAL lock_timeout = '5s';

ALTER TABLE monitoramento.carro ADD COLUMN IF NOT EXISTS mes date
    GENERATED ALWAYS AS (
        CASE WHEN mes_referencia ~ '^(0[1-9]|1[0-2])/[0-9]{4}$'
             THEN make_date(substr(mes_referencia, 4, 4)::int, substr(mes_referencia, 1, 2)::int, 1)
        END) STORED;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'monitoramento' AND table_name = 'carro' AND column_name = 'valor'
                 AND (numeric_precision IS DISTINCT FROM 12 OR numeric_scale IS DISTINCT FROM 2)) THEN
        IF EXISTS (SELECT 1 FROM monitoramento.carro WHERE abs(valor) >= 10000000000) THEN
            RAISE EXCEPTION 'Há valor de carro que não cabe em numeric(12,2)';
        END IF;
        ALTER TABLE monitoramento.carro ALTER COLUMN valor TYPE numeric(12,2);
    END IF;
END $$;

COMMIT;

ANALYZE monitoramento.carro;
