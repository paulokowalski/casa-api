CREATE SCHEMA IF NOT EXISTS passagens;

CREATE TABLE IF NOT EXISTS passagens.rota (
    nome          varchar(120) PRIMARY KEY,
    ordem         smallint     NOT NULL,
    ativa         boolean      NOT NULL,
    config        jsonb        NOT NULL,
    atualizado_em timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS passagens.preco (
    id            bigserial PRIMARY KEY,
    origem_id     bigint       NOT NULL UNIQUE,
    rota          varchar(120) NOT NULL,
    origem        varchar(4)   NOT NULL,
    destino       varchar(4)   NOT NULL,
    ida           date         NOT NULL,
    volta         date,
    preco         integer      NOT NULL CHECK (preco > 0),
    cias          varchar(200),
    escalas       smallint,
    url           text,
    consultado_em timestamptz  NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_preco_rota_em ON passagens.preco (rota, consultado_em);
CREATE INDEX IF NOT EXISTS ix_preco_trecho ON passagens.preco (rota, origem, destino, ida, volta, consultado_em);

CREATE TABLE IF NOT EXISTS passagens.alerta (
    chave   varchar(300) PRIMARY KEY,
    rota    varchar(120) NOT NULL,
    origem  varchar(4)   NOT NULL,
    destino varchar(4)   NOT NULL,
    ida     date         NOT NULL,
    volta   date,
    preco   integer      NOT NULL,
    criado_em timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS passagens.robo (
    id              smallint     PRIMARY KEY CHECK (id = 1),
    sincronizado_em timestamptz  NOT NULL,
    intervalo_horas numeric(5,2) NOT NULL,
    moeda           varchar(3)   NOT NULL DEFAULT 'BRL'
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'robo_passagens') THEN
        CREATE ROLE robo_passagens LOGIN;
    END IF;
END $$;
GRANT CONNECT ON DATABASE casa TO robo_passagens;
GRANT USAGE ON SCHEMA passagens TO robo_passagens;
GRANT SELECT, INSERT ON passagens.preco TO robo_passagens;
GRANT USAGE ON SEQUENCE passagens.preco_id_seq TO robo_passagens;
GRANT SELECT, INSERT, UPDATE, DELETE ON passagens.rota, passagens.alerta, passagens.robo TO robo_passagens;
REVOKE CREATE ON SCHEMA public FROM robo_passagens;
