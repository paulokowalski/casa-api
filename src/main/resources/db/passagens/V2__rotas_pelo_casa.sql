ALTER TABLE passagens.rota ADD COLUMN IF NOT EXISTS id bigserial;
ALTER TABLE passagens.rota ADD COLUMN IF NOT EXISTS criado_em timestamptz NOT NULL DEFAULT now();
CREATE UNIQUE INDEX IF NOT EXISTS ux_rota_id ON passagens.rota (id);

UPDATE passagens.rota
SET config = jsonb_set(config, '{janela}',
        ((config -> 'janela') - 'passo_dias' - 'dias_semana')
        || jsonb_strip_nulls(jsonb_build_object('passoDias', config -> 'janela' -> 'passo_dias',
                                                'diasSemana', config -> 'janela' -> 'dias_semana')))
WHERE jsonb_typeof(config -> 'janela') = 'object'
  AND (config -> 'janela' ? 'passo_dias' OR config -> 'janela' ? 'dias_semana');

UPDATE passagens.rota SET config = config || jsonb_build_object('id', id) WHERE NOT config ? 'id';

REVOKE INSERT, UPDATE, DELETE ON passagens.rota FROM robo_passagens;
GRANT SELECT ON passagens.rota TO robo_passagens;
