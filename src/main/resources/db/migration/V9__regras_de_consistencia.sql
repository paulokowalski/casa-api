BEGIN;
SET LOCAL lock_timeout = '5s';

DO $$
DECLARE
    regra record;
BEGIN
    FOR regra IN
        SELECT * FROM (VALUES
            ('public.renda_mensal', 'ck_renda_mensal_competencia_dia_1', 'CHECK (extract(day FROM competencia) = 1)'),
            ('public.fatura_paga', 'ck_fatura_paga_competencia_dia_1', 'CHECK (extract(day FROM competencia) = 1)'),
            ('public.conta', 'ck_conta_competencias_dia_1',
             'CHECK (extract(day FROM competencia_inicio) = 1 AND (competencia_fim IS NULL OR extract(day FROM competencia_fim) = 1))'),
            ('public.conta_lancamento', 'ck_conta_lancamento_competencia_dia_1', 'CHECK (extract(day FROM competencia) = 1)'),
            ('public.conta_lancamento', 'ck_conta_lancamento_pagamento', 'CHECK (paga = (data_pagamento IS NOT NULL))'),
            ('public.conta_lancamento', 'ck_conta_lancamento_numero_parcela', 'CHECK (numero_parcela IS NULL OR numero_parcela >= 1)'),
            ('public.categorias', 'ck_categorias_cor', 'CHECK (cor IS NULL OR cor ~ ''^#[0-9A-Fa-f]{6}$'')'),
            ('public.categorias', 'categorias_ativo_not_null', 'NOT NULL ativo')
        ) AS r(tabela, nome, definicao)
    LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conname = regra.nome AND conrelid = regra.tabela::regclass) THEN
            EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s NOT VALID', regra.tabela, regra.nome, regra.definicao);
        END IF;
    END LOOP;
END $$;

ALTER TABLE public.renda_mensal VALIDATE CONSTRAINT ck_renda_mensal_competencia_dia_1;
ALTER TABLE public.fatura_paga VALIDATE CONSTRAINT ck_fatura_paga_competencia_dia_1;
ALTER TABLE public.conta VALIDATE CONSTRAINT ck_conta_competencias_dia_1;
ALTER TABLE public.conta_lancamento VALIDATE CONSTRAINT ck_conta_lancamento_competencia_dia_1;
ALTER TABLE public.conta_lancamento VALIDATE CONSTRAINT ck_conta_lancamento_pagamento;
ALTER TABLE public.conta_lancamento VALIDATE CONSTRAINT ck_conta_lancamento_numero_parcela;
ALTER TABLE public.categorias VALIDATE CONSTRAINT ck_categorias_cor;
ALTER TABLE public.categorias VALIDATE CONSTRAINT categorias_ativo_not_null;

CREATE UNIQUE INDEX IF NOT EXISTS uq_carro_id ON monitoramento.carro (id);

COMMIT;
