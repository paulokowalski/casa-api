-- As rotas passam a ser cadastradas pelo casa (aba Passagens) em vez do config.yaml do robô.
-- O robô só lê esta tabela (e guarda uma cópia local para continuar buscando com o Postgres fora do ar).
-- Aplicar à mão:
--   docker exec -i postgresql18-postgresql18-1 psql -U postgres -d casa -v ON_ERROR_STOP=1 < src/main/resources/db/passagens/V2__rotas_pelo_casa.sql

ALTER TABLE passagens.rota ADD COLUMN IF NOT EXISTS id bigserial;
ALTER TABLE passagens.rota ADD COLUMN IF NOT EXISTS criado_em timestamptz NOT NULL DEFAULT now();
CREATE UNIQUE INDEX IF NOT EXISTS ux_rota_id ON passagens.rota (id);

-- janela flexível em camelCase, como o resto da configuração (passo_dias -> passoDias, dias_semana -> diasSemana)
UPDATE passagens.rota
SET config = jsonb_set(config, '{janela}',
        ((config -> 'janela') - 'passo_dias' - 'dias_semana')
        || jsonb_strip_nulls(jsonb_build_object('passoDias', config -> 'janela' -> 'passo_dias',
                                                'diasSemana', config -> 'janela' -> 'dias_semana')))
WHERE jsonb_typeof(config -> 'janela') = 'object'
  AND (config -> 'janela' ? 'passo_dias' OR config -> 'janela' ? 'dias_semana');

-- o id também dentro do config, para a tela
UPDATE passagens.rota SET config = config || jsonb_build_object('id', id) WHERE NOT config ? 'id';

-- o robô não escreve mais as rotas
REVOKE INSERT, UPDATE, DELETE ON passagens.rota FROM robo_passagens;
GRANT SELECT ON passagens.rota TO robo_passagens;
