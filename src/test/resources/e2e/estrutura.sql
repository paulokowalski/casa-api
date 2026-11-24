-- Estrutura do banco casa (só tabelas, índices e restrições; SEM dados), para os testes E2E.
-- Gerado com: docker exec postgresql18-postgresql18-1 pg_dump -U postgres -d casa --schema-only --no-owner --no-privileges
-- (sem as linhas \restrict). Atualize quando uma migration nova for aplicada no servidor.
--
-- PostgreSQL database dump
--


-- Dumped from database version 18.7 (Debian 18.7-1.pgdg13+2)
-- Dumped by pg_dump version 18.7 (Debian 18.7-1.pgdg13+2)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: crescimento; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA crescimento;


--
-- Name: financa; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA financa;


--
-- Name: investimento; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA investimento;


--
-- Name: monitoramento; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA monitoramento;


--
-- Name: passagens; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA passagens;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: crianca; Type: TABLE; Schema: crescimento; Owner: -
--

CREATE TABLE crescimento.crianca (
    id uuid NOT NULL,
    familia_id uuid NOT NULL,
    nome character varying(60) NOT NULL,
    nascimento date NOT NULL,
    sexo character(1) NOT NULL,
    idade_gestacional_semanas numeric(3,1),
    peso_nascer_kg numeric(5,3),
    comprimento_nascer_cm numeric(4,1),
    perimetro_nascer_cm numeric(4,1),
    foto_id uuid,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT crianca_idade_gestacional_semanas_check CHECK (((idade_gestacional_semanas >= (22)::numeric) AND (idade_gestacional_semanas <= (44)::numeric))),
    CONSTRAINT crianca_sexo_check CHECK ((sexo = ANY (ARRAY['M'::bpchar, 'F'::bpchar])))
);


--
-- Name: familia; Type: TABLE; Schema: crescimento; Owner: -
--

CREATE TABLE crescimento.familia (
    id uuid NOT NULL,
    nome character varying(80) NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: familia_membro; Type: TABLE; Schema: crescimento; Owner: -
--

CREATE TABLE crescimento.familia_membro (
    id uuid NOT NULL,
    familia_id uuid NOT NULL,
    usuario_id bigint NOT NULL,
    papel character varying(12) NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT familia_membro_papel_check CHECK (((papel)::text = ANY ((ARRAY['RESPONSAVEL'::character varying, 'LEITOR'::character varying])::text[])))
);


--
-- Name: foto; Type: TABLE; Schema: crescimento; Owner: -
--

CREATE TABLE crescimento.foto (
    id uuid NOT NULL,
    familia_id uuid NOT NULL,
    crianca_id uuid NOT NULL,
    chave character varying(200) NOT NULL,
    largura integer NOT NULL,
    altura integer NOT NULL,
    bytes bigint NOT NULL,
    sha256 character(64) NOT NULL,
    tirada_em timestamp without time zone,
    enviada_por bigint,
    criada_em timestamp with time zone DEFAULT now() NOT NULL,
    registro_id uuid,
    ordem smallint
);


--
-- Name: marco_atingido; Type: TABLE; Schema: crescimento; Owner: -
--

CREATE TABLE crescimento.marco_atingido (
    id uuid NOT NULL,
    crianca_id uuid NOT NULL,
    marco_id character varying(20) NOT NULL,
    atingido_em date NOT NULL,
    nota character varying(500),
    marcado_por bigint,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: medicao; Type: TABLE; Schema: crescimento; Owner: -
--

CREATE TABLE crescimento.medicao (
    id uuid NOT NULL,
    crianca_id uuid NOT NULL,
    data date NOT NULL,
    peso_kg numeric(6,3),
    comprimento_cm numeric(5,1),
    perimetro_cm numeric(4,1),
    observacao character varying(1000),
    criado_por bigint,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_medicao_alguma_medida CHECK (((peso_kg IS NOT NULL) OR (comprimento_cm IS NOT NULL) OR (perimetro_cm IS NOT NULL)))
);


--
-- Name: registro; Type: TABLE; Schema: crescimento; Owner: -
--

CREATE TABLE crescimento.registro (
    id uuid NOT NULL,
    crianca_id uuid NOT NULL,
    data date NOT NULL,
    tipo character varying(10) NOT NULL,
    texto character varying(3000),
    profissional character varying(80),
    proxima_consulta date,
    album_chave character varying(40),
    album_titulo character varying(50),
    medicao_id uuid,
    criado_por bigint,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT registro_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['PESAGEM'::character varying, 'MOMENTO'::character varying, 'CONSULTA'::character varying])::text[])))
);


--
-- Name: vacina_aplicada; Type: TABLE; Schema: crescimento; Owner: -
--

CREATE TABLE crescimento.vacina_aplicada (
    id uuid NOT NULL,
    crianca_id uuid NOT NULL,
    vacina_id character varying(40) NOT NULL,
    nome character varying(80),
    aplicada_em date NOT NULL,
    lote character varying(40),
    local character varying(80),
    registrado_por bigint,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: banco; Type: TABLE; Schema: financa; Owner: -
--

CREATE TABLE financa.banco (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nome text NOT NULL
);


--
-- Name: cartao; Type: TABLE; Schema: financa; Owner: -
--

CREATE TABLE financa.cartao (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    banco_id uuid NOT NULL,
    nome text NOT NULL
);


--
-- Name: tb_carteira; Type: TABLE; Schema: investimento; Owner: -
--

CREATE TABLE investimento.tb_carteira (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nome character varying NOT NULL
);


--
-- Name: tb_investimento_renda_fixa; Type: TABLE; Schema: investimento; Owner: -
--

CREATE TABLE investimento.tb_investimento_renda_fixa (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    carteira_id uuid NOT NULL,
    tipo_ativo character varying NOT NULL,
    emissor character varying NOT NULL,
    tipo_titulo character varying NOT NULL,
    forma character varying NOT NULL,
    indexidador character varying NOT NULL,
    data_compra date NOT NULL,
    data_vencimento date NOT NULL,
    taxa numeric NOT NULL,
    valor_compra numeric NOT NULL,
    valor_evolucao numeric NOT NULL
);


--
-- Name: carro; Type: TABLE; Schema: monitoramento; Owner: -
--

CREATE TABLE monitoramento.carro (
    codigo_veiculo character varying NOT NULL,
    mes_referencia character varying NOT NULL,
    combustivel character varying NOT NULL,
    marca character varying NOT NULL,
    modelo character varying NOT NULL,
    ano character varying NOT NULL,
    valor numeric NOT NULL,
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    mes date GENERATED ALWAYS AS (
        CASE WHEN mes_referencia ~ '^(0[1-9]|1[0-2])/[0-9]{4}$'
             THEN make_date(substr(mes_referencia, 4, 4)::int, substr(mes_referencia, 1, 2)::int, 1)
        END) STORED
);


--
-- Name: alerta; Type: TABLE; Schema: passagens; Owner: -
--

CREATE TABLE passagens.alerta (
    chave character varying(300) NOT NULL,
    rota character varying(120) NOT NULL,
    origem character varying(4) NOT NULL,
    destino character varying(4) NOT NULL,
    ida date NOT NULL,
    volta date,
    preco integer NOT NULL,
    em timestamp with time zone NOT NULL
);


--
-- Name: preco; Type: TABLE; Schema: passagens; Owner: -
--

CREATE TABLE passagens.preco (
    id bigint NOT NULL,
    origem_id bigint NOT NULL,
    rota character varying(120) NOT NULL,
    origem character varying(4) NOT NULL,
    destino character varying(4) NOT NULL,
    ida date NOT NULL,
    volta date,
    preco integer NOT NULL,
    cias character varying(200),
    escalas smallint,
    url text,
    consultado_em timestamp with time zone NOT NULL,
    CONSTRAINT preco_preco_check CHECK ((preco > 0))
);


--
-- Name: preco_id_seq; Type: SEQUENCE; Schema: passagens; Owner: -
--

CREATE SEQUENCE passagens.preco_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: preco_id_seq; Type: SEQUENCE OWNED BY; Schema: passagens; Owner: -
--

ALTER SEQUENCE passagens.preco_id_seq OWNED BY passagens.preco.id;


--
-- Name: robo; Type: TABLE; Schema: passagens; Owner: -
--

CREATE TABLE passagens.robo (
    id smallint NOT NULL,
    sincronizado_em timestamp with time zone NOT NULL,
    intervalo_horas numeric(5,2) NOT NULL,
    moeda character varying(3) DEFAULT 'BRL'::character varying NOT NULL,
    CONSTRAINT robo_id_check CHECK ((id = 1))
);


--
-- Name: rota; Type: TABLE; Schema: passagens; Owner: -
--

CREATE TABLE passagens.rota (
    nome character varying(120) NOT NULL,
    ordem smallint NOT NULL,
    ativa boolean NOT NULL,
    config jsonb NOT NULL,
    atualizado_em timestamp with time zone DEFAULT now() NOT NULL,
    id bigint NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: rota_id_seq; Type: SEQUENCE; Schema: passagens; Owner: -
--

CREATE SEQUENCE passagens.rota_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rota_id_seq; Type: SEQUENCE OWNED BY; Schema: passagens; Owner: -
--

ALTER SEQUENCE passagens.rota_id_seq OWNED BY passagens.rota.id;


--
-- Name: categorias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categorias (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nome character varying(100) NOT NULL,
    descricao text,
    cor character varying(7),
    icone character varying(50),
    ativo boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE categorias; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.categorias IS 'Tabela de categorias para classificação de compras';


--
-- Name: COLUMN categorias.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.categorias.id IS 'Identificador único da categoria';


--
-- Name: COLUMN categorias.nome; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.categorias.nome IS 'Nome da categoria';


--
-- Name: COLUMN categorias.descricao; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.categorias.descricao IS 'Descrição detalhada da categoria';


--
-- Name: COLUMN categorias.cor; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.categorias.cor IS 'Código hexadecimal da cor para exibição';


--
-- Name: COLUMN categorias.icone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.categorias.icone IS 'Nome do ícone Material-UI';


--
-- Name: COLUMN categorias.ativo; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.categorias.ativo IS 'Indica se a categoria está ativa';


--
-- Name: compra; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compra (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nm_produto character varying NOT NULL,
    vl_produto numeric(15,2) NOT NULL,
    dt_compra date NOT NULL,
    nr_parcelas integer NOT NULL,
    dt_cadastrato timestamp without time zone DEFAULT now() NOT NULL,
    cartao_id uuid,
    categoria_id uuid,
    pessoa_id bigint
);


--
-- Name: COLUMN compra.categoria_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.compra.categoria_id IS 'Referência para a categoria da compra';


--
-- Name: compra_parcela; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compra_parcela (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    compra_id uuid NOT NULL,
    nr_parcela integer NOT NULL,
    vl_parcela numeric(15,2) NOT NULL,
    dt_parcela date NOT NULL
);


--
-- Name: conta; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.conta (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    pessoa_id bigint NOT NULL,
    categoria_id uuid,
    descricao character varying(120) NOT NULL,
    tipo character varying(10) NOT NULL,
    valor numeric(12,2) NOT NULL,
    dia_vencimento integer NOT NULL,
    competencia_inicio date NOT NULL,
    competencia_fim date,
    parcela_inicial integer,
    total_parcelas integer,
    juros_mes numeric(7,4),
    valor_variavel boolean DEFAULT false NOT NULL,
    debito_automatico boolean DEFAULT false NOT NULL,
    dt_cadastro timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_conta_parcelas CHECK (((((tipo)::text = 'PARCELADA'::text) = (total_parcelas IS NOT NULL)) AND ((parcela_inicial IS NULL) OR ((parcela_inicial >= 1) AND (parcela_inicial <= total_parcelas))))),
    CONSTRAINT ck_conta_periodo CHECK (((competencia_fim IS NULL) OR (competencia_fim >= competencia_inicio))),
    CONSTRAINT conta_dia_vencimento_check CHECK (((dia_vencimento >= 1) AND (dia_vencimento <= 31))),
    CONSTRAINT conta_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['RECORRENTE'::character varying, 'PARCELADA'::character varying, 'UNICA'::character varying])::text[]))),
    CONSTRAINT conta_valor_check CHECK ((valor > (0)::numeric))
);


--
-- Name: conta_lancamento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.conta_lancamento (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    conta_id uuid NOT NULL,
    competencia date NOT NULL,
    numero_parcela integer,
    vencimento date NOT NULL,
    valor numeric(12,2) NOT NULL,
    valor_estimado boolean DEFAULT false NOT NULL,
    paga boolean DEFAULT false NOT NULL,
    data_pagamento date,
    baixa_manual boolean DEFAULT false NOT NULL
);


--
-- Name: fatura_paga; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fatura_paga (
    pessoa_id bigint NOT NULL,
    competencia date NOT NULL,
    data_pagamento date NOT NULL
);


--
-- Name: geracao; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.geracao (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    data timestamp without time zone DEFAULT now() NOT NULL,
    geracao numeric NOT NULL,
    potencia numeric NOT NULL
);


--
-- Name: geracao_diaria; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.geracao_diaria (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    data date DEFAULT now() NOT NULL,
    geracao numeric NOT NULL
);


--
-- Name: geracao_mensal; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.geracao_mensal (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    mes character varying NOT NULL,
    geracao numeric NOT NULL
);


--
-- Name: lista_compra; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lista_compra (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    item text NOT NULL
);


--
-- Name: pessoa; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pessoa (
    id bigint NOT NULL,
    nome character varying(100) NOT NULL,
    email character varying(100) NOT NULL,
    senha_hash character varying(100),
    papel character varying(10) DEFAULT 'USUARIO'::character varying NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    CONSTRAINT pessoa_papel_check CHECK (((papel)::text = ANY ((ARRAY['ADMIN'::character varying, 'USUARIO'::character varying])::text[])))
);


--
-- Name: pessoa_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pessoa_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pessoa_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pessoa_id_seq OWNED BY public.pessoa.id;


--
-- Name: rastreamento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rastreamento (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nome character varying NOT NULL,
    codigo character varying NOT NULL,
    status character varying NOT NULL,
    dt_cadastro timestamp without time zone DEFAULT now() NOT NULL,
    finalizado boolean DEFAULT false
);


--
-- Name: rastreamento_detalhe; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rastreamento_detalhe (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    rastreamento_id uuid NOT NULL,
    status character varying NOT NULL,
    dt_cadastro timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: renda_mensal; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.renda_mensal (
    pessoa_id bigint NOT NULL,
    competencia date NOT NULL,
    valor numeric(12,2) NOT NULL,
    CONSTRAINT renda_mensal_valor_check CHECK ((valor >= (0)::numeric))
);


--
-- Name: tipo_lancamento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipo_lancamento (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nome text NOT NULL
);


--
-- Name: transacao; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transacao (
    id integer NOT NULL,
    tipo character varying(10) NOT NULL,
    descricao character varying(255) NOT NULL,
    valor numeric(12,2) NOT NULL,
    data date NOT NULL,
    fixa boolean NOT NULL,
    pessoa_id integer NOT NULL,
    ano integer NOT NULL,
    mes integer NOT NULL,
    id_serie character varying(100),
    paga boolean DEFAULT false
);


--
-- Name: transacao_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.transacao_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: transacao_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.transacao_id_seq OWNED BY public.transacao.id;


--
-- Name: preco id; Type: DEFAULT; Schema: passagens; Owner: -
--

ALTER TABLE ONLY passagens.preco ALTER COLUMN id SET DEFAULT nextval('passagens.preco_id_seq'::regclass);


--
-- Name: rota id; Type: DEFAULT; Schema: passagens; Owner: -
--

ALTER TABLE ONLY passagens.rota ALTER COLUMN id SET DEFAULT nextval('passagens.rota_id_seq'::regclass);


--
-- Name: pessoa id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pessoa ALTER COLUMN id SET DEFAULT nextval('public.pessoa_id_seq'::regclass);


--
-- Name: transacao id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transacao ALTER COLUMN id SET DEFAULT nextval('public.transacao_id_seq'::regclass);


--
-- Name: crianca crianca_pkey; Type: CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.crianca
    ADD CONSTRAINT crianca_pkey PRIMARY KEY (id);


--
-- Name: familia_membro familia_membro_pkey; Type: CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.familia_membro
    ADD CONSTRAINT familia_membro_pkey PRIMARY KEY (id);


--
-- Name: familia familia_pkey; Type: CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.familia
    ADD CONSTRAINT familia_pkey PRIMARY KEY (id);


--
-- Name: foto foto_chave_key; Type: CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.foto
    ADD CONSTRAINT foto_chave_key UNIQUE (chave);


--
-- Name: foto foto_pkey; Type: CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.foto
    ADD CONSTRAINT foto_pkey PRIMARY KEY (id);


--
-- Name: marco_atingido marco_atingido_pkey; Type: CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.marco_atingido
    ADD CONSTRAINT marco_atingido_pkey PRIMARY KEY (id);


--
-- Name: medicao medicao_pkey; Type: CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.medicao
    ADD CONSTRAINT medicao_pkey PRIMARY KEY (id);


--
-- Name: registro registro_pkey; Type: CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.registro
    ADD CONSTRAINT registro_pkey PRIMARY KEY (id);


--
-- Name: familia_membro uq_familia_membro; Type: CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.familia_membro
    ADD CONSTRAINT uq_familia_membro UNIQUE (familia_id, usuario_id);


--
-- Name: foto uq_foto_crianca_sha; Type: CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.foto
    ADD CONSTRAINT uq_foto_crianca_sha UNIQUE (crianca_id, sha256);


--
-- Name: marco_atingido uq_marco_atingido; Type: CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.marco_atingido
    ADD CONSTRAINT uq_marco_atingido UNIQUE (crianca_id, marco_id);


--
-- Name: vacina_aplicada uq_vacina_aplicada; Type: CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.vacina_aplicada
    ADD CONSTRAINT uq_vacina_aplicada UNIQUE (crianca_id, vacina_id);


--
-- Name: vacina_aplicada vacina_aplicada_pkey; Type: CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.vacina_aplicada
    ADD CONSTRAINT vacina_aplicada_pkey PRIMARY KEY (id);


--
-- Name: banco banco_pkey; Type: CONSTRAINT; Schema: financa; Owner: -
--

ALTER TABLE ONLY financa.banco
    ADD CONSTRAINT banco_pkey PRIMARY KEY (id);


--
-- Name: cartao cartao_pkey; Type: CONSTRAINT; Schema: financa; Owner: -
--

ALTER TABLE ONLY financa.cartao
    ADD CONSTRAINT cartao_pkey PRIMARY KEY (id);


--
-- Name: tb_carteira tb_carteira_pkey; Type: CONSTRAINT; Schema: investimento; Owner: -
--

ALTER TABLE ONLY investimento.tb_carteira
    ADD CONSTRAINT tb_carteira_pkey PRIMARY KEY (id);


--
-- Name: tb_investimento_renda_fixa tb_investimento_renda_fixa_pkey; Type: CONSTRAINT; Schema: investimento; Owner: -
--

ALTER TABLE ONLY investimento.tb_investimento_renda_fixa
    ADD CONSTRAINT tb_investimento_renda_fixa_pkey PRIMARY KEY (id);


--
-- Name: carro carro_pkey; Type: CONSTRAINT; Schema: monitoramento; Owner: -
--

ALTER TABLE ONLY monitoramento.carro
    ADD CONSTRAINT carro_pkey PRIMARY KEY (codigo_veiculo, mes_referencia);


--
-- Name: alerta alerta_pkey; Type: CONSTRAINT; Schema: passagens; Owner: -
--

ALTER TABLE ONLY passagens.alerta
    ADD CONSTRAINT alerta_pkey PRIMARY KEY (chave);


--
-- Name: preco preco_origem_id_key; Type: CONSTRAINT; Schema: passagens; Owner: -
--

ALTER TABLE ONLY passagens.preco
    ADD CONSTRAINT preco_origem_id_key UNIQUE (origem_id);


--
-- Name: preco preco_pkey; Type: CONSTRAINT; Schema: passagens; Owner: -
--

ALTER TABLE ONLY passagens.preco
    ADD CONSTRAINT preco_pkey PRIMARY KEY (id);


--
-- Name: robo robo_pkey; Type: CONSTRAINT; Schema: passagens; Owner: -
--

ALTER TABLE ONLY passagens.robo
    ADD CONSTRAINT robo_pkey PRIMARY KEY (id);


--
-- Name: rota rota_pkey; Type: CONSTRAINT; Schema: passagens; Owner: -
--

ALTER TABLE ONLY passagens.rota
    ADD CONSTRAINT rota_pkey PRIMARY KEY (nome);


--
-- Name: categorias categorias_nome_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categorias
    ADD CONSTRAINT categorias_nome_key UNIQUE (nome);


--
-- Name: categorias categorias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categorias
    ADD CONSTRAINT categorias_pkey PRIMARY KEY (id);


--
-- Name: compra_parcela compra_parcela_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compra_parcela
    ADD CONSTRAINT compra_parcela_pkey PRIMARY KEY (id);


--
-- Name: compra compra_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compra
    ADD CONSTRAINT compra_pkey PRIMARY KEY (id);


--
-- Name: conta_lancamento conta_lancamento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conta_lancamento
    ADD CONSTRAINT conta_lancamento_pkey PRIMARY KEY (id);


--
-- Name: conta conta_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conta
    ADD CONSTRAINT conta_pkey PRIMARY KEY (id);


--
-- Name: fatura_paga fatura_paga_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fatura_paga
    ADD CONSTRAINT fatura_paga_pkey PRIMARY KEY (pessoa_id, competencia);


--
-- Name: geracao_diaria geracao_diaria_data_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.geracao_diaria
    ADD CONSTRAINT geracao_diaria_data_key UNIQUE (data);


--
-- Name: geracao_mensal geracao_mensal_mes_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.geracao_mensal
    ADD CONSTRAINT geracao_mensal_mes_key UNIQUE (mes);


--
-- Name: pessoa pessoa_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pessoa
    ADD CONSTRAINT pessoa_email_key UNIQUE (email);


--
-- Name: pessoa pessoa_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pessoa
    ADD CONSTRAINT pessoa_pkey PRIMARY KEY (id);


--
-- Name: rastreamento rastreamento_codigo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rastreamento
    ADD CONSTRAINT rastreamento_codigo_key UNIQUE (codigo);


--
-- Name: rastreamento_detalhe rastreamento_detalhe_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rastreamento_detalhe
    ADD CONSTRAINT rastreamento_detalhe_pkey PRIMARY KEY (id);


--
-- Name: rastreamento rastreamento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rastreamento
    ADD CONSTRAINT rastreamento_pkey PRIMARY KEY (id);


--
-- Name: renda_mensal renda_mensal_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.renda_mensal
    ADD CONSTRAINT renda_mensal_pkey PRIMARY KEY (pessoa_id, competencia);


--
-- Name: tipo_lancamento tipo_lancamento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_lancamento
    ADD CONSTRAINT tipo_lancamento_pkey PRIMARY KEY (id);


--
-- Name: transacao transacao_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_pkey PRIMARY KEY (id);


--
-- Name: conta_lancamento uq_lancamento_mes; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conta_lancamento
    ADD CONSTRAINT uq_lancamento_mes UNIQUE (conta_id, competencia);


--
-- Name: ix_crianca_familia; Type: INDEX; Schema: crescimento; Owner: -
--

CREATE INDEX ix_crianca_familia ON crescimento.crianca USING btree (familia_id);


--
-- Name: ix_familia_membro_usuario; Type: INDEX; Schema: crescimento; Owner: -
--

CREATE INDEX ix_familia_membro_usuario ON crescimento.familia_membro USING btree (usuario_id);


--
-- Name: ix_foto_crianca; Type: INDEX; Schema: crescimento; Owner: -
--

CREATE INDEX ix_foto_crianca ON crescimento.foto USING btree (crianca_id, criada_em);


--
-- Name: ix_foto_registro; Type: INDEX; Schema: crescimento; Owner: -
--

CREATE INDEX ix_foto_registro ON crescimento.foto USING btree (registro_id, ordem);


--
-- Name: ix_medicao_crianca_data; Type: INDEX; Schema: crescimento; Owner: -
--

CREATE INDEX ix_medicao_crianca_data ON crescimento.medicao USING btree (crianca_id, data);


--
-- Name: ix_registro_crianca_data; Type: INDEX; Schema: crescimento; Owner: -
--

CREATE INDEX ix_registro_crianca_data ON crescimento.registro USING btree (crianca_id, data);


--
-- Name: ix_registro_proxima_consulta; Type: INDEX; Schema: crescimento; Owner: -
--

CREATE INDEX ix_registro_proxima_consulta ON crescimento.registro USING btree (crianca_id, proxima_consulta) WHERE (proxima_consulta IS NOT NULL);


--
-- Name: uq_registro_album; Type: INDEX; Schema: crescimento; Owner: -
--

CREATE UNIQUE INDEX uq_registro_album ON crescimento.registro USING btree (crianca_id, album_chave) WHERE (album_chave IS NOT NULL);


--
-- Name: ix_preco_rota_em; Type: INDEX; Schema: passagens; Owner: -
--

CREATE INDEX ix_preco_rota_em ON passagens.preco USING btree (rota, consultado_em);


--
-- Name: ix_preco_trecho; Type: INDEX; Schema: passagens; Owner: -
--

CREATE INDEX ix_preco_trecho ON passagens.preco USING btree (rota, origem, destino, ida, volta, consultado_em);


--
-- Name: ux_rota_id; Type: INDEX; Schema: passagens; Owner: -
--

CREATE UNIQUE INDEX ux_rota_id ON passagens.rota USING btree (id);


--
-- Name: idx_categorias_ativo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_categorias_ativo ON public.categorias USING btree (ativo);


--
-- Name: idx_categorias_nome; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_categorias_nome ON public.categorias USING btree (nome);


--
-- Name: idx_compra_parcela_compra_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compra_parcela_compra_id ON public.compra_parcela USING btree (compra_id);


--
-- Name: idx_compra_parcela_dt_parcela; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compra_parcela_dt_parcela ON public.compra_parcela USING btree (dt_parcela);


--
-- Name: idx_compra_pessoa; Type: INDEX; Schema: public; Owner: -
--



--
-- Name: idx_compras_categoria_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compras_categoria_id ON public.compra USING btree (categoria_id);


--
-- Name: ix_conta_pessoa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_conta_pessoa ON public.conta USING btree (pessoa_id);


--
-- Name: ix_lancamento_competencia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_lancamento_competencia ON public.conta_lancamento USING btree (competencia);


--
-- Name: uq_pessoa_email; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_pessoa_email ON public.pessoa USING btree (lower((email)::text));


--
-- Name: crianca crianca_familia_id_fkey; Type: FK CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.crianca
    ADD CONSTRAINT crianca_familia_id_fkey FOREIGN KEY (familia_id) REFERENCES crescimento.familia(id) ON DELETE CASCADE;


--
-- Name: familia_membro familia_membro_familia_id_fkey; Type: FK CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.familia_membro
    ADD CONSTRAINT familia_membro_familia_id_fkey FOREIGN KEY (familia_id) REFERENCES crescimento.familia(id) ON DELETE CASCADE;


--
-- Name: crianca fk_crianca_foto; Type: FK CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.crianca
    ADD CONSTRAINT fk_crianca_foto FOREIGN KEY (foto_id) REFERENCES crescimento.foto(id) ON DELETE SET NULL;


--
-- Name: foto foto_crianca_id_fkey; Type: FK CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.foto
    ADD CONSTRAINT foto_crianca_id_fkey FOREIGN KEY (crianca_id) REFERENCES crescimento.crianca(id) ON DELETE CASCADE;


--
-- Name: foto foto_familia_id_fkey; Type: FK CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.foto
    ADD CONSTRAINT foto_familia_id_fkey FOREIGN KEY (familia_id) REFERENCES crescimento.familia(id) ON DELETE CASCADE;


--
-- Name: foto foto_registro_id_fkey; Type: FK CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.foto
    ADD CONSTRAINT foto_registro_id_fkey FOREIGN KEY (registro_id) REFERENCES crescimento.registro(id) ON DELETE SET NULL;


--
-- Name: marco_atingido marco_atingido_crianca_id_fkey; Type: FK CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.marco_atingido
    ADD CONSTRAINT marco_atingido_crianca_id_fkey FOREIGN KEY (crianca_id) REFERENCES crescimento.crianca(id) ON DELETE CASCADE;


--
-- Name: medicao medicao_crianca_id_fkey; Type: FK CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.medicao
    ADD CONSTRAINT medicao_crianca_id_fkey FOREIGN KEY (crianca_id) REFERENCES crescimento.crianca(id) ON DELETE CASCADE;


--
-- Name: registro registro_crianca_id_fkey; Type: FK CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.registro
    ADD CONSTRAINT registro_crianca_id_fkey FOREIGN KEY (crianca_id) REFERENCES crescimento.crianca(id) ON DELETE CASCADE;


--
-- Name: registro registro_medicao_id_fkey; Type: FK CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.registro
    ADD CONSTRAINT registro_medicao_id_fkey FOREIGN KEY (medicao_id) REFERENCES crescimento.medicao(id) ON DELETE SET NULL;


--
-- Name: vacina_aplicada vacina_aplicada_crianca_id_fkey; Type: FK CONSTRAINT; Schema: crescimento; Owner: -
--

ALTER TABLE ONLY crescimento.vacina_aplicada
    ADD CONSTRAINT vacina_aplicada_crianca_id_fkey FOREIGN KEY (crianca_id) REFERENCES crescimento.crianca(id) ON DELETE CASCADE;


--
-- Name: cartao cartao_banco_id_fkey; Type: FK CONSTRAINT; Schema: financa; Owner: -
--

ALTER TABLE ONLY financa.cartao
    ADD CONSTRAINT cartao_banco_id_fkey FOREIGN KEY (banco_id) REFERENCES financa.banco(id);


--
-- Name: tb_investimento_renda_fixa tb_investimento_renda_fixa_carteira_id_fkey; Type: FK CONSTRAINT; Schema: investimento; Owner: -
--

ALTER TABLE ONLY investimento.tb_investimento_renda_fixa
    ADD CONSTRAINT tb_investimento_renda_fixa_carteira_id_fkey FOREIGN KEY (carteira_id) REFERENCES investimento.tb_carteira(id);


--
-- Name: compra compra_cartao_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compra
    ADD CONSTRAINT compra_cartao_id_fkey FOREIGN KEY (cartao_id) REFERENCES financa.cartao(id);


--
-- Name: compra_parcela compra_parcela_compra_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compra_parcela
    ADD CONSTRAINT compra_parcela_compra_id_fkey FOREIGN KEY (compra_id) REFERENCES public.compra(id);


--
-- Name: conta conta_categoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conta
    ADD CONSTRAINT conta_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES public.categorias(id);


--
-- Name: conta_lancamento conta_lancamento_conta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conta_lancamento
    ADD CONSTRAINT conta_lancamento_conta_id_fkey FOREIGN KEY (conta_id) REFERENCES public.conta(id) ON DELETE CASCADE;


--
-- Name: conta conta_pessoa_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conta
    ADD CONSTRAINT conta_pessoa_id_fkey FOREIGN KEY (pessoa_id) REFERENCES public.pessoa(id);


--
-- Name: fatura_paga fatura_paga_pessoa_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fatura_paga
    ADD CONSTRAINT fatura_paga_pessoa_id_fkey FOREIGN KEY (pessoa_id) REFERENCES public.pessoa(id);


--
-- Name: compra fk_compras_categoria; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compra
    ADD CONSTRAINT fk_compras_categoria FOREIGN KEY (categoria_id) REFERENCES public.categorias(id) ON DELETE SET NULL;


--
-- Name: rastreamento_detalhe rastreamento_detalhe_rastreamento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rastreamento_detalhe
    ADD CONSTRAINT rastreamento_detalhe_rastreamento_id_fkey FOREIGN KEY (rastreamento_id) REFERENCES public.rastreamento(id);


--
-- Name: renda_mensal renda_mensal_pessoa_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.renda_mensal
    ADD CONSTRAINT renda_mensal_pessoa_id_fkey FOREIGN KEY (pessoa_id) REFERENCES public.pessoa(id);


--
-- Name: transacao transacao_pessoa_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_pessoa_id_fkey FOREIGN KEY (pessoa_id) REFERENCES public.pessoa(id);


--
-- PostgreSQL database dump complete
--



-- db/crescimento/V6__exames.sql (ainda não aplicada no servidor quando este arquivo foi gerado)
CREATE TABLE IF NOT EXISTS crescimento.exame (
    id            uuid PRIMARY KEY,
    familia_id    uuid NOT NULL REFERENCES crescimento.familia (id) ON DELETE CASCADE,
    crianca_id    uuid NOT NULL REFERENCES crescimento.crianca (id) ON DELETE CASCADE,
    titulo        varchar(120) NOT NULL,
    data          date NOT NULL,
    observacao    varchar(1000),
    nome_arquivo  varchar(200) NOT NULL,
    tipo          varchar(4) NOT NULL CHECK (tipo IN ('PDF', 'JPEG')),
    chave         varchar(200) NOT NULL,
    bytes         bigint NOT NULL,
    sha256        varchar(64) NOT NULL,
    enviado_por   bigint,
    criado_em     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_exame_crianca_sha UNIQUE (crianca_id, sha256)
);
CREATE INDEX IF NOT EXISTS ix_exame_crianca_data ON crescimento.exame (crianca_id, data DESC);

-- db/migration/V6__compras_consistencia.sql, V7__compras_cartao_validado.sql e V8__compras_sem_nome_da_pessoa.sql
BEGIN;
SET LOCAL lock_timeout = '5s';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint
               WHERE conname = 'compra_cartao_id_fkey' AND conrelid = 'public.compra'::regclass
                 AND confrelid <> 'public.tipo_lancamento'::regclass) THEN
        ALTER TABLE public.compra DROP CONSTRAINT compra_cartao_id_fkey;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'compra_cartao_id_fkey' AND conrelid = 'public.compra'::regclass) THEN
        ALTER TABLE public.compra ADD CONSTRAINT compra_cartao_id_fkey
            FOREIGN KEY (cartao_id) REFERENCES public.tipo_lancamento (id) NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'compra' AND column_name = 'nm_cartao') THEN
        EXECUTE 'UPDATE public.compra c SET cartao_id = t.id FROM public.tipo_lancamento t '
             || 'WHERE c.cartao_id IS NULL AND upper(trim(t.nome)) = upper(trim(c.nm_cartao))';
    END IF;
END $$;

ALTER TABLE public.compra VALIDATE CONSTRAINT compra_cartao_id_fkey;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.pessoa GROUP BY upper(trim(nome)) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Há pessoas com o mesmo nome; deixe os nomes únicos antes de aplicar este script';
    END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS uq_pessoa_nome ON public.pessoa (upper(trim(nome)));

ALTER TABLE public.compra ADD COLUMN IF NOT EXISTS pessoa_id bigint;

CREATE OR REPLACE FUNCTION public.compra_preencher_pessoa() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.nm_pessoa_compra IS NOT NULL THEN
        NEW.pessoa_id := (SELECT p.id FROM public.pessoa p
                          WHERE upper(trim(p.nome)) = upper(trim(NEW.nm_pessoa_compra)));
        IF NEW.pessoa_id IS NULL THEN
            RAISE EXCEPTION 'Não existe pessoa cadastrada com o nome %', NEW.nm_pessoa_compra
                USING ERRCODE = 'foreign_key_violation';
        END IF;
    END IF;
    RETURN NEW;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'compra' AND column_name = 'nm_pessoa_compra') THEN
        EXECUTE 'UPDATE public.compra c SET pessoa_id = p.id FROM public.pessoa p '
             || 'WHERE c.pessoa_id IS NULL AND upper(trim(p.nome)) = upper(trim(c.nm_pessoa_compra))';
        EXECUTE 'ALTER TABLE public.compra ALTER COLUMN nm_pessoa_compra DROP NOT NULL';
        EXECUTE 'CREATE OR REPLACE TRIGGER compra_preencher_pessoa '
             || 'BEFORE INSERT OR UPDATE OF nm_pessoa_compra ON public.compra '
             || 'FOR EACH ROW EXECUTE FUNCTION public.compra_preencher_pessoa()';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'compra_pessoa_id_fkey' AND conrelid = 'public.compra'::regclass) THEN
        ALTER TABLE public.compra ADD CONSTRAINT compra_pessoa_id_fkey
            FOREIGN KEY (pessoa_id) REFERENCES public.pessoa (id) NOT VALID;
    END IF;
END $$;
ALTER TABLE public.compra VALIDATE CONSTRAINT compra_pessoa_id_fkey;
ALTER TABLE public.compra ALTER COLUMN pessoa_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_compra_pessoa_id ON public.compra (pessoa_id);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'uq_compra_parcela_numero' AND conrelid = 'public.compra_parcela'::regclass) THEN
        ALTER TABLE public.compra_parcela ADD CONSTRAINT uq_compra_parcela_numero UNIQUE (compra_id, nr_parcela);
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint
               WHERE conname = 'compra_parcela_compra_id_fkey' AND conrelid = 'public.compra_parcela'::regclass
                 AND confdeltype <> 'c') THEN
        ALTER TABLE public.compra_parcela DROP CONSTRAINT compra_parcela_compra_id_fkey;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'compra_parcela_compra_id_fkey' AND conrelid = 'public.compra_parcela'::regclass) THEN
        ALTER TABLE public.compra_parcela ADD CONSTRAINT compra_parcela_compra_id_fkey
            FOREIGN KEY (compra_id) REFERENCES public.compra (id) ON DELETE CASCADE NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'ck_compra_parcela_numero' AND conrelid = 'public.compra_parcela'::regclass) THEN
        ALTER TABLE public.compra_parcela ADD CONSTRAINT ck_compra_parcela_numero CHECK (nr_parcela >= 1) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'ck_compra_numero_parcelas' AND conrelid = 'public.compra'::regclass) THEN
        ALTER TABLE public.compra ADD CONSTRAINT ck_compra_numero_parcelas CHECK (nr_parcelas >= 1) NOT VALID;
    END IF;
END $$;
ALTER TABLE public.compra_parcela VALIDATE CONSTRAINT compra_parcela_compra_id_fkey;
ALTER TABLE public.compra_parcela VALIDATE CONSTRAINT ck_compra_parcela_numero;
ALTER TABLE public.compra VALIDATE CONSTRAINT ck_compra_numero_parcelas;

DROP INDEX IF EXISTS public.idx_compra_parcela_compra_id;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'compra_cartao_id_not_null' AND conrelid = 'public.compra'::regclass) THEN
        ALTER TABLE public.compra ADD CONSTRAINT compra_cartao_id_not_null NOT NULL cartao_id NOT VALID;
    END IF;
END $$;

COMMIT;

ANALYZE public.compra;
ANALYZE public.compra_parcela;

BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE public.compra VALIDATE CONSTRAINT compra_cartao_id_not_null;
COMMIT;

BEGIN;
SET LOCAL lock_timeout = '5s';
DROP TRIGGER IF EXISTS compra_preencher_pessoa ON public.compra;
DROP FUNCTION IF EXISTS public.compra_preencher_pessoa();
DROP INDEX IF EXISTS public.idx_compra_pessoa;
ALTER TABLE public.compra DROP COLUMN IF EXISTS nm_pessoa_compra;
COMMIT;

-- db/migration/V9__regras_de_consistencia.sql
BEGIN;
SET LOCAL lock_timeout = '5s';

DO $$
DECLARE
    regra record;
BEGIN
    FOR regra IN
        SELECT * FROM (VALUES
            ('public.renda_mensal', 'ck_renda_mensal_competencia_dia_1', 'CHECK (extract(day FROM competencia) = 1)'),
            ('public.fatura_paga', 'ck_fatura_paga_competencia_dia_1', 'CHECK (extract(day FROM competencia) = 1)'),
            ('public.conta', 'ck_conta_competencias_dia_1',
             'CHECK (extract(day FROM competencia_inicio) = 1 AND (competencia_fim IS NULL OR extract(day FROM competencia_fim) = 1))'),
            ('public.conta_lancamento', 'ck_conta_lancamento_competencia_dia_1', 'CHECK (extract(day FROM competencia) = 1)'),
            ('public.conta_lancamento', 'ck_conta_lancamento_pagamento', 'CHECK (paga = (data_pagamento IS NOT NULL))'),
            ('public.conta_lancamento', 'ck_conta_lancamento_numero_parcela', 'CHECK (numero_parcela IS NULL OR numero_parcela >= 1)'),
            ('public.categorias', 'ck_categorias_cor', 'CHECK (cor IS NULL OR cor ~ ''^#[0-9A-Fa-f]{6}$'')'),
            ('public.categorias', 'categorias_ativo_not_null', 'NOT NULL ativo')
        ) AS r(tabela, nome, definicao)
    LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conname = regra.nome AND conrelid = regra.tabela::regclass) THEN
            EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s NOT VALID', regra.tabela, regra.nome, regra.definicao);
        END IF;
    END LOOP;
END $$;

ALTER TABLE public.renda_mensal VALIDATE CONSTRAINT ck_renda_mensal_competencia_dia_1;
ALTER TABLE public.fatura_paga VALIDATE CONSTRAINT ck_fatura_paga_competencia_dia_1;
ALTER TABLE public.conta VALIDATE CONSTRAINT ck_conta_competencias_dia_1;
ALTER TABLE public.conta_lancamento VALIDATE CONSTRAINT ck_conta_lancamento_competencia_dia_1;
ALTER TABLE public.conta_lancamento VALIDATE CONSTRAINT ck_conta_lancamento_pagamento;
ALTER TABLE public.conta_lancamento VALIDATE CONSTRAINT ck_conta_lancamento_numero_parcela;
ALTER TABLE public.categorias VALIDATE CONSTRAINT ck_categorias_cor;
ALTER TABLE public.categorias VALIDATE CONSTRAINT categorias_ativo_not_null;

CREATE UNIQUE INDEX IF NOT EXISTS uq_carro_id ON monitoramento.carro (id);

COMMIT;

-- db/migration/V10__indices.sql
SET lock_timeout = '5s';

CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_geracao_data ON public.geracao (data);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_compra_cartao_id ON public.compra (cartao_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_conta_categoria_id ON public.conta (categoria_id);

DROP INDEX CONCURRENTLY IF EXISTS public.idx_categorias_nome;
DROP INDEX CONCURRENTLY IF EXISTS public.idx_categorias_ativo;
ALTER TABLE public.pessoa DROP CONSTRAINT IF EXISTS pessoa_email_key;

DO $$
DECLARE
    invalidos text;
BEGIN
    SELECT string_agg(c.relname, ', ') INTO invalidos
    FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid
    WHERE NOT i.indisvalid AND c.relname IN ('ix_geracao_data', 'ix_compra_cartao_id', 'ix_conta_categoria_id');
    IF invalidos IS NOT NULL THEN
        RAISE EXCEPTION 'Índices que ficaram inválidos: %. Apague cada um com DROP INDEX CONCURRENTLY e rode o script de novo', invalidos;
    END IF;
END $$;

ANALYZE public.geracao;
ANALYZE public.compra;
ANALYZE public.conta;

-- db/crescimento/V8__indices.sql
SET lock_timeout = '5s';

CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_crianca_foto_id ON crescimento.crianca (foto_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_foto_familia_id ON crescimento.foto (familia_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_registro_medicao_id ON crescimento.registro (medicao_id);

DO $$
DECLARE
    invalidos text;
BEGIN
    SELECT string_agg(c.relname, ', ') INTO invalidos
    FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid
    WHERE NOT i.indisvalid AND c.relname IN ('ix_crianca_foto_id', 'ix_foto_familia_id', 'ix_registro_medicao_id');
    IF invalidos IS NOT NULL THEN
        RAISE EXCEPTION 'Índices que ficaram inválidos: %. Apague cada um com DROP INDEX CONCURRENTLY e rode o script de novo', invalidos;
    END IF;
END $$;

-- db/migration/V11__arquivo.sql
BEGIN;
SET LOCAL lock_timeout = '5s';

CREATE SCHEMA IF NOT EXISTS arquivo;

ALTER TABLE IF EXISTS public.transacao SET SCHEMA arquivo;
ALTER TABLE IF EXISTS public.lista_compra SET SCHEMA arquivo;
ALTER TABLE IF EXISTS public.rastreamento_detalhe SET SCHEMA arquivo;
ALTER TABLE IF EXISTS public.rastreamento SET SCHEMA arquivo;
ALTER TABLE IF EXISTS public.geracao_mensal SET SCHEMA arquivo;
ALTER TABLE IF EXISTS financa.cartao SET SCHEMA arquivo;
ALTER TABLE IF EXISTS financa.banco SET SCHEMA arquivo;
ALTER TABLE IF EXISTS investimento.tb_investimento_renda_fixa SET SCHEMA arquivo;
ALTER TABLE IF EXISTS investimento.tb_carteira SET SCHEMA arquivo;

DO $$
DECLARE
    esquema text;
BEGIN
    FOREACH esquema IN ARRAY ARRAY['financa', 'investimento'] LOOP
        IF EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = esquema)
           AND NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = esquema)
           AND NOT EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace WHERE n.nspname = esquema)
           AND NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE n.nspname = esquema) THEN
            EXECUTE format('DROP SCHEMA %I', esquema);
        END IF;
    END LOOP;
END $$;

COMMIT;

-- db/migration/V12__compras_nome_do_cartao_opcional.sql
BEGIN;
SET LOCAL lock_timeout = '5s';
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'compra' AND column_name = 'nm_cartao') THEN
        EXECUTE 'ALTER TABLE public.compra ALTER COLUMN nm_cartao DROP NOT NULL';
    END IF;
END $$;
COMMIT;

-- db/migration/V13__compras_sem_nome_do_cartao.sql
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE public.compra DROP COLUMN IF EXISTS nm_cartao;
COMMIT;

-- db/migration/V14__categoria_em_uso_nao_sai.sql
BEGIN;
SET LOCAL lock_timeout = '5s';

ALTER TABLE public.compra DROP CONSTRAINT IF EXISTS fk_compras_categoria;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'compra_categoria_id_fkey' AND conrelid = 'public.compra'::regclass) THEN
        ALTER TABLE public.compra ADD CONSTRAINT compra_categoria_id_fkey
            FOREIGN KEY (categoria_id) REFERENCES public.categorias (id) ON DELETE RESTRICT NOT VALID;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint
               WHERE conname = 'conta_categoria_id_fkey' AND conrelid = 'public.conta'::regclass
                 AND confdeltype <> 'r') THEN
        ALTER TABLE public.conta DROP CONSTRAINT conta_categoria_id_fkey;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'conta_categoria_id_fkey' AND conrelid = 'public.conta'::regclass) THEN
        ALTER TABLE public.conta ADD CONSTRAINT conta_categoria_id_fkey
            FOREIGN KEY (categoria_id) REFERENCES public.categorias (id) ON DELETE RESTRICT NOT VALID;
    END IF;
END $$;

ALTER TABLE public.compra VALIDATE CONSTRAINT compra_categoria_id_fkey;
ALTER TABLE public.conta VALIDATE CONSTRAINT conta_categoria_id_fkey;

COMMIT;

-- db/migration/V15__carro_mes_e_valor.sql
BEGIN;
SET LOCAL lock_timeout = '5s';

ALTER TABLE monitoramento.carro ADD COLUMN IF NOT EXISTS mes date
    GENERATED ALWAYS AS (
        CASE WHEN mes_referencia ~ '^(0[1-9]|1[0-2])/[0-9]{4}$'
             THEN make_date(substr(mes_referencia, 4, 4)::int, substr(mes_referencia, 1, 2)::int, 1)
        END) STORED;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'monitoramento' AND table_name = 'carro' AND column_name = 'valor'
                 AND (numeric_precision IS DISTINCT FROM 12 OR numeric_scale IS DISTINCT FROM 2)) THEN
        IF EXISTS (SELECT 1 FROM monitoramento.carro WHERE abs(valor) >= 10000000000) THEN
            RAISE EXCEPTION 'Há valor de carro que não cabe em numeric(12,2)';
        END IF;
        ALTER TABLE monitoramento.carro ALTER COLUMN valor TYPE numeric(12,2);
    END IF;
END $$;

COMMIT;

ANALYZE monitoramento.carro;

-- db/migration/V16__usuario_da_api.sql
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

-- db/migration/V17__nomes_das_colunas.sql
BEGIN;
SET LOCAL lock_timeout = '5s';

DO $$
DECLARE
    troca record;
BEGIN
    FOR troca IN
        SELECT * FROM (VALUES
            ('public', 'compra', 'nm_produto', 'produto'),
            ('public', 'compra', 'vl_produto', 'valor'),
            ('public', 'compra', 'dt_compra', 'data_compra'),
            ('public', 'compra', 'nr_parcelas', 'numero_parcelas'),
            ('public', 'compra', 'dt_cadastrato', 'criado_em'),
            ('public', 'compra_parcela', 'nr_parcela', 'numero'),
            ('public', 'compra_parcela', 'vl_parcela', 'valor'),
            ('public', 'compra_parcela', 'dt_parcela', 'data'),
            ('public', 'categorias', 'created_at', 'criado_em'),
            ('public', 'categorias', 'updated_at', 'atualizado_em'),
            ('public', 'conta', 'dt_cadastro', 'criado_em'),
            ('crescimento', 'foto', 'criada_em', 'criado_em'),
            ('passagens', 'alerta', 'em', 'criado_em')
        ) AS t(esquema, tabela, antigo, novo)
    LOOP
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = troca.esquema AND table_name = troca.tabela AND column_name = troca.antigo) THEN
            EXECUTE format('ALTER TABLE %I.%I RENAME COLUMN %I TO %I', troca.esquema, troca.tabela, troca.antigo, troca.novo);
        END IF;
    END LOOP;
END $$;

ALTER INDEX IF EXISTS public.idx_compra_parcela_dt_parcela RENAME TO ix_compra_parcela_data;

COMMIT;
