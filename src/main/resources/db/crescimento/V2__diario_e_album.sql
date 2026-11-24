CREATE TABLE IF NOT EXISTS crescimento.registro (
    id               uuid PRIMARY KEY,
    crianca_id       uuid NOT NULL REFERENCES crescimento.crianca (id) ON DELETE CASCADE,
    data             date NOT NULL,
    tipo             varchar(10) NOT NULL CHECK (tipo IN ('PESAGEM', 'MOMENTO', 'CONSULTA')),
    texto            varchar(3000),
    profissional     varchar(80),
    proxima_consulta date,
    album_chave      varchar(40),
    album_titulo     varchar(50),
    medicao_id       uuid REFERENCES crescimento.medicao (id) ON DELETE SET NULL,
    criado_por       bigint,
    criado_em        timestamptz NOT NULL DEFAULT now(),
    atualizado_em    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_registro_crianca_data ON crescimento.registro (crianca_id, data);
CREATE UNIQUE INDEX IF NOT EXISTS uq_registro_album ON crescimento.registro (crianca_id, album_chave) WHERE album_chave IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_registro_proxima_consulta ON crescimento.registro (crianca_id, proxima_consulta) WHERE proxima_consulta IS NOT NULL;

ALTER TABLE crescimento.foto ADD COLUMN IF NOT EXISTS registro_id uuid REFERENCES crescimento.registro (id) ON DELETE SET NULL;
ALTER TABLE crescimento.foto ADD COLUMN IF NOT EXISTS ordem smallint;
CREATE INDEX IF NOT EXISTS ix_foto_registro ON crescimento.foto (registro_id, ordem);
