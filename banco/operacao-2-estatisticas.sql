\set ON_ERROR_STOP on

CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

SELECT extversion AS pg_stat_statements FROM pg_extension WHERE extname = 'pg_stat_statements';
