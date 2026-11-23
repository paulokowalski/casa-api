-- Módulo crescimento: remove as tabelas da IA (resumo do mês e perguntas), criadas pelos antigos V4 e V5.
-- Regra da família: nenhuma IA tem acesso aos dados das crianças. Apaga também o que a IA gerou.
-- Aplicar à mão (só o usuário; as IAs não acessam o schema crescimento):
--   docker exec -i postgresql18-postgresql18-1 psql -U postgres -d casa -v ON_ERROR_STOP=1 < src/main/resources/db/crescimento/V7__remove_ia.sql

DROP TABLE IF EXISTS crescimento.pergunta_ia;
DROP TABLE IF EXISTS crescimento.resumo_mes;
