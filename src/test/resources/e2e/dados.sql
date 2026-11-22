-- Dados mínimos dos testes E2E (banco de teste descartável, criado do zero a cada execução).
-- A pessoa abaixo vira ADMIN na subida da API (AdminInicial, com AUTH_ADMIN_EMAIL/AUTH_ADMIN_SENHA do e2e/rodar.sh).
INSERT INTO public.pessoa (nome, email) VALUES ('ADMIN E2E', 'admin@e2e.local');

-- Estado do robô de passagens: sem ele a tela de Passagens responde "o robô ainda não enviou dados" e não lista rotas.
INSERT INTO passagens.robo (id, sincronizado_em, intervalo_horas, moeda) VALUES (1, now(), 6, 'BRL');
