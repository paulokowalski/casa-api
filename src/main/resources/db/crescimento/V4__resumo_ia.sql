-- Módulo crescimento, passo 7: resumo do mês de vida gerado por IA (só quando alguém pede).
-- Um por criança e mês de vida (mes = 0 é o 1º mês); gerar de novo substitui.
-- Aplicar à mão:
--   docker exec -i postgresql18-postgresql18-1 psql -U postgres -d casa -v ON_ERROR_STOP=1 < src/main/resources/db/crescimento/V4__resumo_ia.sql

CREATE TABLE IF NOT EXISTS crescimento.resumo_mes (
    id          uuid PRIMARY KEY,
    crianca_id  uuid NOT NULL REFERENCES crescimento.crianca (id) ON DELETE CASCADE,
    mes         smallint NOT NULL CHECK (mes >= 0),
    inicio      date NOT NULL,
    fim         date NOT NULL,
    texto       varchar(4000) NOT NULL,
    -- uma pergunta por linha
    perguntas   varchar(4000) NOT NULL DEFAULT '',
    modelo      varchar(60),
    gerado_por  bigint,
    gerado_em   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_resumo_mes UNIQUE (crianca_id, mes)
);
