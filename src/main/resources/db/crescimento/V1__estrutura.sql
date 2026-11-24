CREATE SCHEMA IF NOT EXISTS crescimento;

CREATE TABLE IF NOT EXISTS crescimento.familia (
    id         uuid PRIMARY KEY,
    nome       varchar(80) NOT NULL,
    criado_em  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS crescimento.familia_membro (
    id         uuid PRIMARY KEY,
    familia_id uuid NOT NULL REFERENCES crescimento.familia (id) ON DELETE CASCADE,
    usuario_id bigint NOT NULL,
    papel      varchar(12) NOT NULL CHECK (papel IN ('RESPONSAVEL', 'LEITOR')),
    criado_em  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_familia_membro UNIQUE (familia_id, usuario_id)
);
CREATE INDEX IF NOT EXISTS ix_familia_membro_usuario ON crescimento.familia_membro (usuario_id);

CREATE TABLE IF NOT EXISTS crescimento.crianca (
    id                        uuid PRIMARY KEY,
    familia_id                uuid NOT NULL REFERENCES crescimento.familia (id) ON DELETE CASCADE,
    nome                      varchar(60) NOT NULL,
    nascimento                date NOT NULL,
    sexo                      char(1) NOT NULL CHECK (sexo IN ('M', 'F')),
    idade_gestacional_semanas numeric(3, 1) CHECK (idade_gestacional_semanas BETWEEN 22 AND 44),
    peso_nascer_kg            numeric(5, 3),
    comprimento_nascer_cm     numeric(4, 1),
    perimetro_nascer_cm       numeric(4, 1),
    foto_id                   uuid,
    criado_em                 timestamptz NOT NULL DEFAULT now(),
    atualizado_em             timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_crianca_familia ON crescimento.crianca (familia_id);

CREATE TABLE IF NOT EXISTS crescimento.medicao (
    id             uuid PRIMARY KEY,
    crianca_id     uuid NOT NULL REFERENCES crescimento.crianca (id) ON DELETE CASCADE,
    data           date NOT NULL,
    peso_kg        numeric(6, 3),
    comprimento_cm numeric(5, 1),
    perimetro_cm   numeric(4, 1),
    observacao     varchar(1000),
    criado_por     bigint,
    criado_em      timestamptz NOT NULL DEFAULT now(),
    atualizado_em  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_medicao_alguma_medida CHECK (peso_kg IS NOT NULL OR comprimento_cm IS NOT NULL OR perimetro_cm IS NOT NULL)
);
CREATE INDEX IF NOT EXISTS ix_medicao_crianca_data ON crescimento.medicao (crianca_id, data);

CREATE TABLE IF NOT EXISTS crescimento.foto (
    id          uuid PRIMARY KEY,
    familia_id  uuid NOT NULL REFERENCES crescimento.familia (id) ON DELETE CASCADE,
    crianca_id  uuid NOT NULL REFERENCES crescimento.crianca (id) ON DELETE CASCADE,
    chave       varchar(200) NOT NULL UNIQUE,
    largura     integer NOT NULL,
    altura      integer NOT NULL,
    bytes       bigint NOT NULL,
    sha256      char(64) NOT NULL,
    tirada_em   timestamp,
    enviada_por bigint,
    criado_em   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_foto_crianca_sha UNIQUE (crianca_id, sha256)
);
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'crescimento' AND table_name = 'foto' AND column_name = 'criado_em') THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS ix_foto_crianca ON crescimento.foto (crianca_id, criado_em)';
    ELSIF EXISTS (SELECT 1 FROM information_schema.columns
                  WHERE table_schema = 'crescimento' AND table_name = 'foto' AND column_name = 'criada_em') THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS ix_foto_crianca ON crescimento.foto (crianca_id, criada_em)';
    END IF;
END $$;

DO $$ BEGIN
    ALTER TABLE crescimento.crianca
        ADD CONSTRAINT fk_crianca_foto FOREIGN KEY (foto_id) REFERENCES crescimento.foto (id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
