BEGIN;
SET LOCAL lock_timeout = '5s';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'casa_app') THEN
        CREATE ROLE casa_app LOGIN;
    END IF;
END $$;

ALTER ROLE casa_app SET statement_timeout = '30s';
ALTER ROLE casa_app SET lock_timeout = '10s';
ALTER ROLE casa_app SET idle_in_transaction_session_timeout = '60s';

DO $$
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO casa_app', current_database());
END $$;

GRANT USAGE ON SCHEMA public, crescimento, passagens, monitoramento TO casa_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public, crescimento, passagens TO casa_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public, crescimento, passagens TO casa_app;
GRANT SELECT ON ALL TABLES IN SCHEMA monitoramento TO casa_app;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public, crescimento, passagens
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO casa_app;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public, crescimento, passagens
    GRANT USAGE, SELECT ON SEQUENCES TO casa_app;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA monitoramento
    GRANT SELECT ON TABLES TO casa_app;

COMMIT;
