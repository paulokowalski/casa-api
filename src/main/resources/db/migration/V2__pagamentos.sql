-- Gestão Financeira: marcar/desmarcar como paga.
-- Pode ser executado mais de uma vez.

-- Quando a pessoa marca ou desmarca uma conta, o débito automático não muda mais a situação daquele mês
ALTER TABLE conta_lancamento ADD COLUMN IF NOT EXISTS baixa_manual boolean NOT NULL DEFAULT false;

-- Fatura do cartão paga no mês (a linha existir = paga)
CREATE TABLE IF NOT EXISTS fatura_paga (
    pessoa_id      bigint NOT NULL REFERENCES pessoa (id),
    competencia    date   NOT NULL,              -- dia 1 do mês da fatura
    data_pagamento date   NOT NULL,
    PRIMARY KEY (pessoa_id, competencia)
);
