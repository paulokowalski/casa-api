\set ON_ERROR_STOP on

ALTER SYSTEM SET log_min_duration_statement = '500ms';
ALTER SYSTEM SET log_parameter_max_length = 0;
ALTER SYSTEM SET log_parameter_max_length_on_error = 0;
ALTER SYSTEM SET log_lock_waits = on;
ALTER SYSTEM SET pg_stat_statements.track = 'top';

SELECT CASE
           WHEN current_setting('shared_preload_libraries') ~ '(^|,)\s*pg_stat_statements\s*(,|$)'
               THEN current_setting('shared_preload_libraries')
           WHEN current_setting('shared_preload_libraries') = '' THEN 'pg_stat_statements'
           ELSE current_setting('shared_preload_libraries') || ',pg_stat_statements'
       END AS bibliotecas
\gset
ALTER SYSTEM SET shared_preload_libraries = :'bibliotecas';

SELECT pg_reload_conf();
SELECT name, setting, pending_restart FROM pg_settings
WHERE name IN ('log_min_duration_statement', 'log_parameter_max_length', 'log_parameter_max_length_on_error',
               'log_lock_waits', 'shared_preload_libraries')
ORDER BY name;
