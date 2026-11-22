-- Módulo crescimento, passo 5: marcos do desenvolvimento e vacinas.
-- Os catálogos (quais marcos e quais vacinas existem) ficam no código, versionados; aqui só o que a família registrou,
-- pelo id estável do catálogo (ex.: marco 'm03d', vacina 'penta2') ou 'extra-<id>' para vacinas fora do calendário.
-- Aplicar à mão:
--   docker exec -i postgresql18-postgresql18-1 psql -U postgres -d casa -v ON_ERROR_STOP=1 < src/main/resources/db/crescimento/V3__marcos_e_vacinas.sql

CREATE TABLE IF NOT EXISTS crescimento.marco_atingido (
    id            uuid PRIMARY KEY,
    crianca_id    uuid NOT NULL REFERENCES crescimento.crianca (id) ON DELETE CASCADE,
    marco_id      varchar(20) NOT NULL,
    atingido_em   date NOT NULL,
    nota          varchar(500),
    marcado_por   bigint,
    criado_em     timestamptz NOT NULL DEFAULT now(),
    atualizado_em timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_marco_atingido UNIQUE (crianca_id, marco_id)
);

CREATE TABLE IF NOT EXISTS crescimento.vacina_aplicada (
    id             uuid PRIMARY KEY,
    crianca_id     uuid NOT NULL REFERENCES crescimento.crianca (id) ON DELETE CASCADE,
    vacina_id      varchar(40) NOT NULL,
    -- nome da vacina, só nas que não são do calendário (extra-*)
    nome           varchar(80),
    aplicada_em    date NOT NULL,
    lote           varchar(40),
    local          varchar(80),
    registrado_por bigint,
    criado_em      timestamptz NOT NULL DEFAULT now(),
    atualizado_em  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_vacina_aplicada UNIQUE (crianca_id, vacina_id)
);
