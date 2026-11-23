-- Dados mínimos dos testes E2E (banco de teste descartável, criado do zero a cada execução).
-- A pessoa abaixo vira ADMIN na subida da API (AdminInicial, com AUTH_ADMIN_EMAIL/AUTH_ADMIN_SENHA do e2e/rodar.sh).
INSERT INTO public.pessoa (nome, email) VALUES ('ADMIN E2E', 'admin@e2e.local');

-- Estado do robô de passagens: sem ele a tela de Passagens responde "o robô ainda não enviou dados" e não lista rotas.
INSERT INTO passagens.robo (id, sincronizado_em, intervalo_horas, moeda) VALUES (1, now(), 6, 'BRL');

INSERT INTO monitoramento.carro (codigo_veiculo, mes_referencia, combustivel, marca, modelo, ano, valor) VALUES
    ('E2E-001', '12/2025', 'Gasolina', 'MARCA E2E', 'MODELO E2E', '2020', 51000.50),
    ('E2E-001', '01/2026', 'Gasolina', 'MARCA E2E', 'MODELO E2E', '2020', 50500.00),
    ('E2E-001', '11/2025', 'Gasolina', 'MARCA E2E', 'MODELO E2E', '2020', 52000.00);
