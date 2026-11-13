# Casa API

API para gerenciamento de despesas domésticas desenvolvida com Spring Boot.

## 🚀 Tecnologias

- Java 17
- Spring Boot
- PostgreSQL
- Docker
- Maven

## 📋 Pré-requisitos

- Java 17+
- Docker e Docker Compose
- Maven
- PostgreSQL

## 🔧 Configuração do Ambiente

### Variáveis de Ambiente

```properties
# Database
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/casa
```

### Configurações do Banco de Dados

O projeto utiliza PostgreSQL com as seguintes configurações otimizadas:
- HikariCP como pool de conexões
- Configurações de batch para melhor performance
- Timezone configurado para America/Sao_Paulo

## 💻 Ambiente de desenvolvimento

1. Copie `config/dev-secrets.example.yaml` para `config/dev-secrets.yaml` (fora do git) e preencha o segredo JWT e o e-mail/senha inicial do admin.
2. Suba a API normalmente (pelo IntelliJ ou `mvn spring-boot:run`), com o diretório de trabalho na raiz do `casa-api`. O arquivo `config/dev-secrets.yaml` é lido automaticamente quando existe; em servidor/Docker ele não existe e valem as variáveis de ambiente.
3. No frontend (`casa`), com Node 26 (`nvm use 26`): `npm run dev` (porta 3000, `VITE_API_BASE_URL=http://localhost:8080/casa-api/` no `.env.local`).

## 🏃‍♂️ Executando o Projeto

1. Clone o repositório:
```bash
git clone https://github.com/seu-usuario/casa-api.git
cd casa-api
```

2. Compile o projeto:
```bash
mvn clean package
```

3. Execute com Docker:
```bash
docker-compose up -d
```

Ou execute localmente:
```bash
java -jar target/casa-api.jar
```

## 📦 Estrutura do Projeto

```
src/
├── main/
│   ├── java/
│   │   └── com/kowalski/casaapi/
│   │       ├── config/         # Configurações (segurança, CORS)
│   │       ├── domain/         # Entidades e regras de negócio
│   │       └── repository/     # Repositórios JPA
│   └── resources/
│       └── application.yaml    # Configurações da aplicação
└── test/
    └── java/                   # Testes unitários e de integração
```

## 🔍 Monitoramento

### Logs
- Configuração otimizada do Logback
- Níveis de log configurados para melhor performance
- Monitoramento de queries lentas (>1000ms)

### Métricas
- Hibernate statistics desabilitadas em produção
- Monitoramento do pool de conexões via HikariCP

## 📄 API Endpoints

A API está disponível em `http://localhost:8080/casa-api`

## 🛠️ Construído Com

* [Spring Boot](https://spring.io/projects/spring-boot)
* [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
* [PostgreSQL](https://www.postgresql.org/)
* [HikariCP](https://github.com/brettwooldridge/HikariCP)
* [Maven](https://maven.apache.org/)