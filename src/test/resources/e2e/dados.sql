-- Dados mínimos dos testes E2E (banco de teste descartável, criado do zero a cada execução).
-- A pessoa abaixo vira ADMIN na subida da API (AdminInicial, com AUTH_ADMIN_EMAIL/AUTH_ADMIN_SENHA do e2e/rodar.sh).
INSERT INTO public.pessoa (nome, email) VALUES ('ADMIN E2E', 'admin@e2e.local');

-- Estado do robô de passagens: sem ele a tela de Passagens responde "o robô ainda não enviou dados" e não lista rotas.
INSERT INTO passagens.robo (id, sincronizado_em, intervalo_horas, moeda) VALUES (1, now(), 6, 'BRL');

INSERT INTO monitoramento.carro (codigo_veiculo, mes_referencia, combustivel, marca, modelo, ano, valor) VALUES
    ('E2E-001', '12/2025', 'Gasolina', 'MARCA E2E', 'MODELO E2E', '2020', 51000.50),
    ('E2E-001', '01/2026', 'Gasolina', 'MARCA E2E', 'MODELO E2E', '2020', 50500.00),
    ('E2E-001', '11/2025', 'Gasolina', 'MARCA E2E', 'MODELO E2E', '2020', 52000.00);

INSERT INTO public.tipo_lancamento (id, nome) VALUES ('00000000-0000-0000-0000-0000000000c1', 'E2E CARTAO');
INSERT INTO public.categorias (id, nome) VALUES ('00000000-0000-0000-0000-0000000000a1', 'E2E CATEGORIA');
INSERT INTO public.compra (id, nm_produto, vl_produto, dt_compra, nr_parcelas, nm_pessoa_compra, nm_cartao, cartao_id, categoria_id) VALUES
    ('00000000-0000-0000-0000-0000000000b1', 'TV E2E', 300.00, '2031-01-15', 3, 'E2E COMPRADOR', 'E2E CARTAO',
     NULL, '00000000-0000-0000-0000-0000000000a1'),
    ('00000000-0000-0000-0000-0000000000b2', 'MERCADO E2E', 50.00, '2031-01-10', 1, 'E2E COMPRADOR', 'E2E CARTAO',
     NULL, NULL);
INSERT INTO public.compra_parcela (compra_id, nr_parcela, vl_parcela, dt_parcela) VALUES
    ('00000000-0000-0000-0000-0000000000b1', 1, 100.00, '2031-02-15'),
    ('00000000-0000-0000-0000-0000000000b1', 2, 100.00, '2031-03-15'),
    ('00000000-0000-0000-0000-0000000000b1', 3, 100.00, '2031-04-15'),
    ('00000000-0000-0000-0000-0000000000b2', 1, 50.00, '2031-02-10');

INSERT INTO passagens.alerta (chave, rota, origem, destino, ida, volta, preco, em) VALUES
    ('E2E ROTA|FOR|GRU|2099-01-10|', 'E2E ROTA', 'FOR', 'GRU', '2099-01-10', NULL, 900, now());
