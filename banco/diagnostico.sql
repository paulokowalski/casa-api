\set ON_ERROR_STOP on
\pset footer off
BEGIN TRANSACTION READ ONLY;

\echo
\echo '== Inconsistências (só quantidades) =='
SELECT verificacao, quantidade FROM (
    SELECT 1 AS ordem, 'compras sem cartao_id' AS verificacao,
           (SELECT count(*) FROM compra WHERE cartao_id IS NULL) AS quantidade
    UNION ALL SELECT 2, 'compras com cartao_id fora de tipo_lancamento',
           (SELECT count(*) FROM compra c WHERE c.cartao_id IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM tipo_lancamento t WHERE t.id = c.cartao_id))
    UNION ALL SELECT 3, 'tipos de lançamento sem o mesmo id em financa.cartao',
           (SELECT count(*) FROM tipo_lancamento t WHERE NOT EXISTS (SELECT 1 FROM financa.cartao f WHERE f.id = t.id))
    UNION ALL SELECT 4, 'cartões de financa.cartao sem o mesmo id em tipo_lancamento',
           (SELECT count(*) FROM financa.cartao f WHERE NOT EXISTS (SELECT 1 FROM tipo_lancamento t WHERE t.id = f.id))
    UNION ALL SELECT 5, 'compras com nm_cartao diferente do nome do tipo de lançamento',
           (SELECT count(*) FROM compra c JOIN tipo_lancamento t ON t.id = c.cartao_id
             WHERE upper(trim(c.nm_cartao)) <> upper(trim(t.nome)))
    UNION ALL SELECT 6, 'nomes de tipo de lançamento repetidos (grupos)',
           (SELECT count(*) FROM (SELECT 1 FROM tipo_lancamento GROUP BY upper(trim(nome)) HAVING count(*) > 1) g)
    UNION ALL SELECT 7, 'compras cujo nome não bate com nenhuma pessoa',
           (SELECT count(*) FROM compra c
             WHERE NOT EXISTS (SELECT 1 FROM pessoa p WHERE upper(trim(p.nome)) = upper(trim(c.nm_pessoa_compra))))
    UNION ALL SELECT 8, 'nomes distintos de compra sem pessoa cadastrada',
           (SELECT count(DISTINCT upper(trim(c.nm_pessoa_compra))) FROM compra c
             WHERE NOT EXISTS (SELECT 1 FROM pessoa p WHERE upper(trim(p.nome)) = upper(trim(c.nm_pessoa_compra))))
    UNION ALL SELECT 9, 'compras cujo nome bate com mais de uma pessoa',
           (SELECT count(*) FROM compra c
             WHERE (SELECT count(*) FROM pessoa p WHERE upper(trim(p.nome)) = upper(trim(c.nm_pessoa_compra))) > 1)
    UNION ALL SELECT 10, 'nomes de pessoa repetidos (grupos)',
           (SELECT count(*) FROM (SELECT 1 FROM pessoa GROUP BY upper(trim(nome)) HAVING count(*) > 1) g)
    UNION ALL SELECT 11, 'compras com nr_parcelas menor que 1',
           (SELECT count(*) FROM compra WHERE nr_parcelas < 1)
    UNION ALL SELECT 12, 'compras sem nenhuma parcela',
           (SELECT count(*) FROM compra c WHERE NOT EXISTS (SELECT 1 FROM compra_parcela p WHERE p.compra_id = c.id))
    UNION ALL SELECT 13, 'compras com quantidade de parcelas diferente de nr_parcelas',
           (SELECT count(*) FROM compra c
             WHERE c.nr_parcelas <> (SELECT count(*) FROM compra_parcela p WHERE p.compra_id = c.id))
    UNION ALL SELECT 14, 'compras com soma das parcelas diferente do total (mais de 1 centavo por parcela)',
           (SELECT count(*) FROM compra c
             WHERE abs(c.vl_produto - coalesce((SELECT sum(p.vl_parcela) FROM compra_parcela p WHERE p.compra_id = c.id), 0))
                   > 0.01 * greatest(c.nr_parcelas, 1))
    UNION ALL SELECT 15, 'compras com valor que não cabe em numeric(12,2)',
           (SELECT count(*) FROM compra WHERE abs(vl_produto) >= 10000000000)
    UNION ALL SELECT 16, 'parcelas com número repetido na mesma compra (grupos)',
           (SELECT count(*) FROM (SELECT 1 FROM compra_parcela GROUP BY compra_id, nr_parcela HAVING count(*) > 1) g)
    UNION ALL SELECT 17, 'parcelas com nr_parcela menor que 1',
           (SELECT count(*) FROM compra_parcela WHERE nr_parcela < 1)
    UNION ALL SELECT 18, 'renda_mensal com competência fora do dia 1',
           (SELECT count(*) FROM renda_mensal WHERE extract(day FROM competencia) <> 1)
    UNION ALL SELECT 19, 'fatura_paga com competência fora do dia 1',
           (SELECT count(*) FROM fatura_paga WHERE extract(day FROM competencia) <> 1)
    UNION ALL SELECT 20, 'contas com início ou fim fora do dia 1',
           (SELECT count(*) FROM conta
             WHERE extract(day FROM competencia_inicio) <> 1 OR extract(day FROM competencia_fim) <> 1)
    UNION ALL SELECT 21, 'lançamentos com competência fora do dia 1',
           (SELECT count(*) FROM conta_lancamento WHERE extract(day FROM competencia) <> 1)
    UNION ALL SELECT 22, 'lançamentos pagos sem data de pagamento',
           (SELECT count(*) FROM conta_lancamento WHERE paga AND data_pagamento IS NULL)
    UNION ALL SELECT 23, 'lançamentos não pagos com data de pagamento',
           (SELECT count(*) FROM conta_lancamento WHERE NOT paga AND data_pagamento IS NOT NULL)
    UNION ALL SELECT 24, 'lançamentos com numero_parcela menor que 1',
           (SELECT count(*) FROM conta_lancamento WHERE numero_parcela < 1)
    UNION ALL SELECT 25, 'categorias com ativo nulo',
           (SELECT count(*) FROM categorias WHERE ativo IS NULL)
    UNION ALL SELECT 26, 'categorias com cor fora do formato #RRGGBB',
           (SELECT count(*) FROM categorias WHERE cor IS NOT NULL AND cor !~ '^#[0-9A-Fa-f]{6}$')
    UNION ALL SELECT 27, 'nomes de categoria repetidos ignorando maiúsculas (grupos)',
           (SELECT count(*) FROM (SELECT 1 FROM categorias GROUP BY upper(trim(nome)) HAVING count(*) > 1) g)
    UNION ALL SELECT 28, 'categorias em uso por compras e por contas',
           (SELECT count(*) FROM categorias k
             WHERE EXISTS (SELECT 1 FROM compra c WHERE c.categoria_id = k.id)
               AND EXISTS (SELECT 1 FROM conta c WHERE c.categoria_id = k.id))
    UNION ALL SELECT 29, 'carros com id repetido',
           (SELECT count(*) FROM (SELECT 1 FROM monitoramento.carro GROUP BY id HAVING count(*) > 1) g)
    UNION ALL SELECT 30, 'carros com mes_referencia fora do formato MM/AAAA',
           (SELECT count(*) FROM monitoramento.carro WHERE mes_referencia !~ '^(0[1-9]|1[0-2])/[0-9]{4}$')
    UNION ALL SELECT 31, 'carros com ano que não é número',
           (SELECT count(*) FROM monitoramento.carro WHERE ano !~ '^[0-9]{4}$')
    UNION ALL SELECT 32, 'dias com leituras de geração e sem total diário',
           (SELECT count(*) FROM (SELECT DISTINCT g.data::date AS dia FROM geracao g) d
             WHERE NOT EXISTS (SELECT 1 FROM geracao_diaria x WHERE x.data = d.dia))
    UNION ALL SELECT 33, 'leituras de geração com mais de 12 meses',
           (SELECT count(*) FROM geracao WHERE data < date_trunc('month', now()) - interval '12 months')
    UNION ALL SELECT 34, 'fotos cuja família difere da família da criança',
           (SELECT count(*) FROM crescimento.foto f JOIN crescimento.crianca c ON c.id = f.crianca_id
             WHERE f.familia_id <> c.familia_id)
    UNION ALL SELECT 35, 'exames cuja família difere da família da criança',
           (SELECT count(*) FROM crescimento.exame e JOIN crescimento.crianca c ON c.id = e.crianca_id
             WHERE e.familia_id <> c.familia_id)
    UNION ALL SELECT 36, 'fotos e exames com sha256 fora do formato hexadecimal',
           (SELECT (SELECT count(*) FROM crescimento.foto WHERE sha256 !~ '^[0-9a-f]{64}$')
                 + (SELECT count(*) FROM crescimento.exame WHERE sha256 !~ '^[0-9a-f]{64}$'))
) v
ORDER BY ordem;

\echo
\echo '== Tabelas que seriam arquivadas (só quantidade de linhas) =='
SELECT tabela, linhas FROM (
              SELECT 'public.transacao' AS tabela, (SELECT count(*) FROM public.transacao) AS linhas
    UNION ALL SELECT 'public.lista_compra', (SELECT count(*) FROM public.lista_compra)
    UNION ALL SELECT 'public.rastreamento', (SELECT count(*) FROM public.rastreamento)
    UNION ALL SELECT 'public.rastreamento_detalhe', (SELECT count(*) FROM public.rastreamento_detalhe)
    UNION ALL SELECT 'public.geracao_mensal', (SELECT count(*) FROM public.geracao_mensal)
    UNION ALL SELECT 'financa.banco', (SELECT count(*) FROM financa.banco)
    UNION ALL SELECT 'financa.cartao', (SELECT count(*) FROM financa.cartao)
    UNION ALL SELECT 'investimento.tb_carteira', (SELECT count(*) FROM investimento.tb_carteira)
    UNION ALL SELECT 'investimento.tb_investimento_renda_fixa', (SELECT count(*) FROM investimento.tb_investimento_renda_fixa)
) t;

\echo
\echo '== Tabelas: tamanho e forma de leitura =='
SELECT schemaname || '.' || relname AS tabela,
       n_live_tup AS linhas,
       pg_size_pretty(pg_total_relation_size(relid)) AS tamanho_total,
       pg_size_pretty(pg_indexes_size(relid)) AS tamanho_indices,
       seq_scan AS leituras_sem_indice,
       idx_scan AS leituras_com_indice,
       n_dead_tup AS linhas_mortas,
       last_autovacuum::date AS ultimo_autovacuum,
       last_autoanalyze::date AS ultimo_autoanalyze
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(relid) DESC;

\echo
\echo '== Índices: uso e tamanho =='
SELECT s.schemaname || '.' || s.relname AS tabela,
       s.indexrelname AS indice,
       s.idx_scan AS usos,
       pg_size_pretty(pg_relation_size(s.indexrelid)) AS tamanho,
       i.indisunique AS unico,
       i.indisvalid AS valido
FROM pg_stat_user_indexes s
JOIN pg_index i ON i.indexrelid = s.indexrelid
ORDER BY s.idx_scan, pg_relation_size(s.indexrelid) DESC;

\echo
\echo '== Janela das estatísticas e versão =='
SELECT (SELECT stats_reset::date FROM pg_stat_database WHERE datname = current_database()) AS estatisticas_desde,
       pg_postmaster_start_time()::date AS banco_no_ar_desde,
       current_setting('server_version') AS versao,
       current_user AS usuario,
       (SELECT count(*) FROM pg_extension WHERE extname = 'pg_stat_statements') AS tem_pg_stat_statements;

\echo
\echo '== Configuração atual =='
SELECT name, setting, unit
FROM pg_settings
WHERE name IN ('shared_buffers', 'effective_cache_size', 'work_mem', 'maintenance_work_mem', 'max_connections',
               'random_page_cost', 'io_method', 'autovacuum', 'log_min_duration_statement',
               'idle_in_transaction_session_timeout', 'statement_timeout', 'shared_preload_libraries')
ORDER BY name;

ROLLBACK;
