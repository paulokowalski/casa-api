\set ON_ERROR_STOP on
\pset footer off
BEGIN TRANSACTION READ ONLY;

SELECT round(total_exec_time::numeric, 1) AS total_ms,
       calls AS chamadas,
       round(mean_exec_time::numeric, 2) AS media_ms,
       round(100 * shared_blks_hit::numeric / nullif(shared_blks_hit + shared_blks_read, 0), 1) AS acerto_cache_pct,
       left(regexp_replace(query, '\s+', ' ', 'g'), 160) AS consulta
FROM pg_stat_statements
WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
ORDER BY total_exec_time DESC
LIMIT 15;

ROLLBACK;
