# Casa API

API para gerenciamento de despesas domésticas desenvolvida com Spring Boot.

## 🚀 Tecnologias

- Java 17
- Spring Boot
- PostgreSQL
- Apache Kafka
- Docker
- Maven

## 📋 Pré-requisitos

- Java 17+
- Docker e Docker Compose
- Maven
- PostgreSQL
- Apache Kafka

## 🔧 Configuração do Ambiente

### Variáveis de Ambiente

```properties
# Database
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/casa

# Kafka
KAFKA_BOOTSTRAP_ADDRESS=localhost:9092
KAFKA_GROUP_ID=casa-group
KAFKA_RETRY_INITIAL_INTERVAL=500
KAFKA_RETRY_MAX_ATTEMPTS=15
```

### Configurações do Banco de Dados

O projeto utiliza PostgreSQL com as seguintes configurações otimizadas:
- HikariCP como pool de conexões
- Configurações de batch para melhor performance
- Timezone configurado para America/Sao_Paulo

### Configurações do Kafka

O projeto utiliza uma implementação genérica para eventos Kafka:
- Deserialização automática de eventos
- Interface base `Event` para todos os eventos
- Retry policies configuráveis
- Configurações otimizadas para producer e consumer

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
│   │       ├── config/         # Configurações (Kafka, Database)
│   │       ├── domain/         # Entidades e regras de negócio
│   │       ├── event/          # Eventos Kafka
│   │       ├── listener/       # Consumidores Kafka
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
* [Apache Kafka](https://kafka.apache.org/)
* [PostgreSQL](https://www.postgresql.org/)
* [HikariCP](https://github.com/brettwooldridge/HikariCP)
* [Maven](https://maven.apache.org/)