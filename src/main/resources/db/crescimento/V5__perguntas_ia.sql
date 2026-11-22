-- Módulo crescimento: perguntas dos pais à IA sobre um mês de vida (abaixo do resumo do mês) e as respostas.
-- Aplicar à mão:
--   docker exec -i postgresql18-postgresql18-1 psql -U postgres -d casa -v ON_ERROR_STOP=1 < src/main/resources/db/crescimento/V5__perguntas_ia.sql

CREATE TABLE IF NOT EXISTS crescimento.pergunta_ia (
    id          uuid PRIMARY KEY,
    crianca_id  uuid NOT NULL REFERENCES crescimento.crianca (id) ON DELETE CASCADE,
    mes         smallint NOT NULL CHECK (mes >= 0),
    pergunta    varchar(500) NOT NULL,
    resposta    varchar(3000) NOT NULL,
    modelo      varchar(60),
    feita_por   bigint,
    feita_em    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_pergunta_ia_crianca_mes ON crescimento.pergunta_ia (crianca_id, mes, feita_em);
