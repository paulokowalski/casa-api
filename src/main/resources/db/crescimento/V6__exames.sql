CREATE TABLE IF NOT EXISTS crescimento.exame (
    id            uuid PRIMARY KEY,
    familia_id    uuid NOT NULL REFERENCES crescimento.familia (id) ON DELETE CASCADE,
    crianca_id    uuid NOT NULL REFERENCES crescimento.crianca (id) ON DELETE CASCADE,
    titulo        varchar(120) NOT NULL,
    data          date NOT NULL,
    observacao    varchar(1000),
    nome_arquivo  varchar(200) NOT NULL,
    tipo          varchar(4) NOT NULL CHECK (tipo IN ('PDF', 'JPEG')),
    chave         varchar(200) NOT NULL,
    bytes         bigint NOT NULL,
    sha256        varchar(64) NOT NULL,
    enviado_por   bigint,
    criado_em     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_exame_crianca_sha UNIQUE (crianca_id, sha256)
);
CREATE INDEX IF NOT EXISTS ix_exame_crianca_data ON crescimento.exame (crianca_id, data DESC);
