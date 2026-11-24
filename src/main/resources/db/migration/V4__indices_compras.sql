-- Índices para as consultas de compras/parcelas.

-- Busca das parcelas de uma compra (FK sem índice: cada busca lia a tabela inteira).
CREATE INDEX IF NOT EXISTS idx_compra_parcela_compra_id ON compra_parcela (compra_id);

-- Filtros por data da parcela.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'compra_parcela' AND column_name = 'dt_parcela') THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_compra_parcela_dt_parcela ON compra_parcela (dt_parcela)';
    END IF;
END $$;

-- Filtros por pessoa (as consultas comparam com upper()).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'compra' AND column_name = 'nm_pessoa_compra') THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_compra_pessoa ON compra (upper(nm_pessoa_compra))';
    END IF;
END $$;

ANALYZE compra;
ANALYZE compra_parcela;
