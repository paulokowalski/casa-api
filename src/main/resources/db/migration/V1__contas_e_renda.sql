-- Gestão Financeira: contas fora do cartão (financiamentos, prestações, plano de saúde...) e renda mensal.
-- Requer PostgreSQL 13+ (gen_random_uuid nativo). Pode ser executado mais de uma vez.

CREATE TABLE IF NOT EXISTS conta (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    pessoa_id          bigint        NOT NULL REFERENCES pessoa (id),
    categoria_id       uuid          REFERENCES categorias (id),
    descricao          varchar(120)  NOT NULL,
    tipo               varchar(10)   NOT NULL CHECK (tipo IN ('RECORRENTE', 'PARCELADA', 'UNICA')),
    valor              numeric(12,2) NOT NULL CHECK (valor > 0),
    dia_vencimento     integer       NOT NULL CHECK (dia_vencimento BETWEEN 1 AND 31),
    competencia_inicio date          NOT NULL,                 -- sempre dia 1 do mês
    competencia_fim    date,                                   -- null = recorrente sem fim
    parcela_inicial    integer,                                -- ex.: 24 (financiamento já em andamento)
    total_parcelas     integer,                                -- ex.: 48
    juros_mes          numeric(7,4),                           -- opcional, em %: 1.4900 = 1,49% a.m.
    valor_variavel     boolean       NOT NULL DEFAULT false,   -- água/luz: valor é estimativa
    debito_automatico  boolean       NOT NULL DEFAULT false,
    dt_cadastro        timestamp     NOT NULL DEFAULT now(),
    CONSTRAINT ck_conta_parcelas CHECK (
        (tipo = 'PARCELADA') = (total_parcelas IS NOT NULL)
        AND (parcela_inicial IS NULL OR parcela_inicial BETWEEN 1 AND total_parcelas)
    ),
    CONSTRAINT ck_conta_periodo CHECK (competencia_fim IS NULL OR competencia_fim >= competencia_inicio)
);
CREATE INDEX IF NOT EXISTS ix_conta_pessoa ON conta (pessoa_id);

CREATE TABLE IF NOT EXISTS conta_lancamento (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    conta_id        uuid          NOT NULL REFERENCES conta (id) ON DELETE CASCADE,
    competencia     date          NOT NULL,                    -- dia 1 do mês de referência
    numero_parcela  integer,                                   -- só PARCELADA
    vencimento      date          NOT NULL,
    valor           numeric(12,2) NOT NULL,
    valor_estimado  boolean       NOT NULL DEFAULT false,
    paga            boolean       NOT NULL DEFAULT false,
    data_pagamento  date,
    CONSTRAINT uq_lancamento_mes UNIQUE (conta_id, competencia)
);
CREATE INDEX IF NOT EXISTS ix_lancamento_competencia ON conta_lancamento (competencia);

CREATE TABLE IF NOT EXISTS renda_mensal (
    pessoa_id   bigint        NOT NULL REFERENCES pessoa (id),
    competencia date          NOT NULL,
    valor       numeric(12,2) NOT NULL CHECK (valor >= 0),
    PRIMARY KEY (pessoa_id, competencia)
);

-- Categorias usadas pelas contas fixas (as que já existirem não são duplicadas)
INSERT INTO categorias (id, nome)
SELECT gen_random_uuid(), c.nome
FROM (VALUES ('Moradia'), ('Transporte'), ('Saúde'), ('Educação'), ('Seguros'),
             ('Contas de consumo'), ('Impostos'), ('Assinaturas'), ('Outros')) AS c (nome)
WHERE NOT EXISTS (SELECT 1 FROM categorias x WHERE upper(x.nome) = upper(c.nome));
