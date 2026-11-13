CREATE TABLE IF NOT EXISTS conta (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    pessoa_id          bigint        NOT NULL REFERENCES pessoa (id),
    categoria_id       uuid          REFERENCES categorias (id),
    descricao          varchar(120)  NOT NULL,
    tipo               varchar(10)   NOT NULL CHECK (tipo IN ('RECORRENTE', 'PARCELADA', 'UNICA')),
    valor              numeric(12,2) NOT NULL CHECK (valor > 0),
    dia_vencimento     integer       NOT NULL CHECK (dia_vencimento BETWEEN 1 AND 31),
    competencia_inicio date          NOT NULL,
    competencia_fim    date,
    parcela_inicial    integer,
    total_parcelas     integer,
    juros_mes          numeric(7,4),
    valor_variavel     boolean       NOT NULL DEFAULT false,
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
    competencia     date          NOT NULL,
    numero_parcela  integer,
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

INSERT INTO categorias (id, nome)
SELECT gen_random_uuid(), c.nome
FROM (VALUES ('Moradia'), ('Transporte'), ('Saúde'), ('Educação'), ('Seguros'),
             ('Contas de consumo'), ('Impostos'), ('Assinaturas'), ('Outros')) AS c (nome)
WHERE NOT EXISTS (SELECT 1 FROM categorias x WHERE upper(x.nome) = upper(c.nome));
