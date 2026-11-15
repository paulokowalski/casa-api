-- Índices para as consultas de compras/parcelas.

-- Busca das parcelas de uma compra (FK sem índice: cada busca lia a tabela inteira).
CREATE INDEX IF NOT EXISTS idx_compra_parcela_compra_id ON compra_parcela (compra_id);

-- Filtros por data da parcela.
CREATE INDEX IF NOT EXISTS idx_compra_parcela_dt_parcela ON compra_parcela (dt_parcela);

-- Filtros por pessoa (as consultas comparam com upper()).
CREATE INDEX IF NOT EXISTS idx_compra_pessoa ON compra (upper(nm_pessoa_compra));

ANALYZE compra;
ANALYZE compra_parcela;
