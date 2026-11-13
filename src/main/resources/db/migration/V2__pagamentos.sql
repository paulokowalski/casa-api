ALTER TABLE conta_lancamento ADD COLUMN IF NOT EXISTS baixa_manual boolean NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS fatura_paga (
    pessoa_id      bigint NOT NULL REFERENCES pessoa (id),
    competencia    date   NOT NULL,
    data_pagamento date   NOT NULL,
    PRIMARY KEY (pessoa_id, competencia)
);
