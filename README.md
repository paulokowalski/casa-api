# Casa API

Backend da casa: compras no cartão e parcelas, contas fixas e lançamentos mensais, renda, faturas,
pessoas/acessos e monitoramento (geração solar, histórico FIPE e saúde do servidor). Consumido pelo front
[`casa`](../casa) e por integrações (Node-RED).

- **Stack:** Java 17 · Spring Boot 4.1 (Web MVC, Data JPA, Security + OAuth2 Resource Server) · PostgreSQL · MariaDB (só o histórico do monitoramento) · springdoc-openapi (Swagger) · Lombok · Maven
- **Base da API:** `http://<host>:8080/casa-api` (o nginx do front também repassa `/casa-api/` para cá)
- **Documentação interativa:** [`/casa-api/swagger-ui.html`](http://localhost:8080/casa-api/swagger-ui.html) · OpenAPI JSON em `/casa-api/v3/api-docs`

## Sumário

- [Rodando](#rodando)
- [Configuração](#configuração)
- [Banco de dados e migrations](#banco-de-dados-e-migrations)
- [Monitoramento do servidor](#monitoramento-do-servidor)
- [Autenticação e permissões](#autenticação-e-permissões)
- [Swagger / OpenAPI](#swagger--openapi)
- [Endpoints](#endpoints)
- [Erros](#erros)
- [Estrutura do código](#estrutura-do-código)
- [Testes](#testes)
- [Notas de desempenho](#notas-de-desempenho)

## Rodando

### Docker (servidor)

```bash
docker compose up -d --build casa-api
```

O `docker-compose.yml` (fora do git) lê as variáveis do `.env` e conecta nas redes `casa-network` (a mesma
do container do front) e `mariadb_default` (do container `mariadb`). Também monta, só leitura, o `/sys` e o
`/etc/hostname` do host e o `lost+found` de cada disco externo, usados pelo
[monitoramento do servidor](#monitoramento-do-servidor). O mesmo arquivo sobe o `casa-docker-proxy`, que dá à
API acesso só-leitura à lista e às estatísticas dos containers. A aplicação leva cerca de 25 s para subir; requisições nesse intervalo
recebem 502 do nginx.

### Desenvolvimento local

1. Copie `config/dev-secrets.example.yaml` para `config/dev-secrets.yaml` (fora do git) e preencha
   banco, segredo JWT e e-mail/senha do admin inicial. O arquivo é lido automaticamente quando existe.
2. Com o diretório de trabalho na raiz do projeto:
   ```bash
   mvn spring-boot:run
   ```
3. No front (`casa`), aponte `VITE_API_BASE_URL=http://localhost:8080/casa-api/` no `.env.local`.

## Configuração

Todas as opções vêm de variáveis de ambiente (ou do `config/dev-secrets.yaml` em desenvolvimento).

| Variável | Obrigatória | Padrão | Uso |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | sim | — | JDBC do PostgreSQL, ex.: `jdbc:postgresql://host:5432/casa` |
| `SPRING_DATASOURCE_USERNAME` | sim | — | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | sim | — | Senha do banco |
| `JWT_SECRET` | sim | — | Segredo HS256 dos tokens; **mínimo 32 caracteres** (a API não sobe sem) |
| `JWT_EXPIRACAO_HORAS` | não | `8` | Validade do token |
| `AUTH_ADMIN_EMAIL` / `AUTH_ADMIN_SENHA` | não | — | Na subida, se a pessoa com esse e-mail ainda não tem senha, recebe essa senha e o papel `ADMIN` |
| `API_KEY` | não | vazio | Chave do cabeçalho `X-API-KEY` para integrações; vazio desliga |
| `CORS_URL` | não | `http://localhost:3000` | Origens permitidas, separadas por vírgula |
| `SWAGGER_ENABLED` | não | `true` | `false` desliga o Swagger UI e o `/v3/api-docs` |
| `MONITOR_DATASOURCE_URL` | não | `jdbc:mariadb://mariadb:3306/casa_monitor` | JDBC do MariaDB com o histórico do monitoramento |
| `MONITOR_DATASOURCE_USERNAME` / `MONITOR_DATASOURCE_PASSWORD` | sim, para o histórico | `casa_monitor` / — | Usuário e senha desse banco |
| `MONITOR_HABILITADO` | não | `true` | `false` desliga a coleta (os endpoints ao vivo continuam funcionando) |
| `MONITOR_COLETA_CRON` | não | `0 * * * * *` | Quando coletar (cron do Spring, com segundos): a cada minuto |
| `MONITOR_RETENCAO_DIAS` | não | `365` | Amostras mais antigas são apagadas todo dia às 03:15 |
| `MONITOR_SYS_PATH` | não | `/sys` | `/sys` do host (no container, `/host/sys`; sem ele a rede mostra só a do container) |
| `MONITOR_PROC_PATH` | não | `/proc` | `/proc` a ler (o do container já mostra CPU, memória e discos do host) |
| `MONITOR_HOSTNAME_ARQUIVO` | não | `/etc/hostname` | Arquivo com o nome do host (no container, `/host/hostname`) |
| `MONITOR_DOCKER_URL` | não | vazio | API do Docker para os containers (no servidor, `http://casa-docker-proxy:2375`); vazio desliga essa parte |
| `MONITOR_RETENCAO_CONTAINERS_DIAS` | não | `90` | Dias de histórico dos containers (uma linha por container por minuto) |
| `MONITOR_SMART_ARQUIVO` | não | vazio | JSON com a temperatura dos discos, gravado pelo timer `casa-monitor-smart` (no container, `/host/smart/smart.json`); vazio desliga |
| `MONITOR_DISCOS` | não | `Sistema=/` | Discos a medir, `Nome=caminho` separados por vírgula; o caminho é qualquer arquivo ou pasta dentro do disco |

## Banco de dados e migrations

PostgreSQL, com `ddl-auto: none` — o Hibernate não cria nem altera tabelas. Os scripts ficam em
`src/main/resources/db/migration` e **são aplicados à mão** (não há Flyway/Liquibase), em ordem:

| Script | O que faz |
|---|---|
| `V1__contas_e_renda.sql` | Tabelas de contas, lançamentos e renda mensal |
| `V2__pagamentos.sql` | Baixa manual de lançamentos e faturas pagas |
| `V3__login.sql` | Senha, papel e ativo em `pessoa` (login por pessoa) |
| `V4__indices_compras.sql` | Índices de `compra_parcela` (compra e data) e de `compra` (pessoa) |
| `V5__remove_funcao_remove_zeros_esquerda.sql` | Remove a função SQL que os filtros antigos usavam |

Para aplicar um script no servidor:

```bash
docker exec -i postgresql18-postgresql18-1 psql -U postgres -d casa -v ON_ERROR_STOP=1 \
  < src/main/resources/db/migration/V5__remove_funcao_remove_zeros_esquerda.sql
```

Os scripts usam `IF EXISTS` / `IF NOT EXISTS` e podem ser reaplicados. Aplique cada um **depois** do
deploy da versão que deixou de depender do que ele remove.

## Monitoramento do servidor

A cada minuto a API lê as métricas direto do kernel e grava uma amostra no **MariaDB** (banco
`casa_monitor`), sem depender de outro serviço:

| Métrica | Fonte |
|---|---|
| CPU total, por núcleo e iowait | `/proc/stat` (diferença entre duas leituras) e `/sys/devices/system/cpu/*/cpufreq` |
| Memória e swap | `/proc/meminfo` (usada = total − disponível, sem cache) |
| Carga e processos | `/proc/loadavg` · tempo ligado em `/proc/uptime` |
| Temperaturas | `/sys/class/thermal/thermal_zone*` |
| Rede | `/sys/class/net/*/statistics`, só interfaces físicas (sem `lo`, bridges, `veth` e VPN) |
| Leitura/escrita de disco | `/proc/diskstats`, só discos inteiros (sem partições, `zram` e `loop`) |
| Espaço em disco | caminhos de `MONITOR_DISCOS`; um disco desmontado (cuja pasta cai no disco do sistema) é ignorado |
| Containers Docker | `GET /containers/json` e `GET /containers/{id}/stats?one-shot=true`, pelo proxy só-leitura |
| Temperatura dos discos | SMART, lido no host pelo timer `casa-monitor-smart` (veja abaixo) |

- **Tabelas** (criadas pela própria API na subida, com `CREATE TABLE IF NOT EXISTS`):
  `amostra_servidor` (uma linha por minuto, com as médias do minuto), `amostra_disco` (espaço e
  temperatura de cada disco, a cada 5 minutos) e `amostra_container` (uma linha por container por minuto, inclusive os
  parados, para contar o tempo fora do ar). A retenção padrão é de 365 dias para o servidor e 90 para os
  containers (~20 mil linhas por dia com 14 containers).
- **Containers:** CPU em % do servidor inteiro (100% = todos os núcleos ocupados), memória sem o cache
  inativo (o mesmo cálculo do `docker stats`), rede (nula para quem usa a rede do host), leitura/escrita
  de disco, processos, estado e healthcheck. Contadores que zeram (container reiniciado) não geram taxa.
- **Temperatura dos discos:** ler SMART exige root e acesso direto aos discos, que a API não tem. Quem lê é
  o host: o script `config/smart/casa-monitor-smart.sh`, rodado a cada minuto (segundo 40) pelo timer
  `casa-monitor-smart.timer`, grava `/var/lib/casa-monitor/smart.json` com modelo, tipo, temperatura e os
  `major:minor` de cada disco e partição. A API lê esse arquivo (montado só-leitura) e liga cada caminho de
  `MONITOR_DISCOS` ao disco pelo `st_dev` do caminho, então não depende de `sda`/`sdb`, que mudam entre
  reinícios. Discos em repouso não são acordados (`smartctl -n standby`); leituras com mais de 5 minutos
  são ignoradas. O espaço e a temperatura vão para `amostra_disco` a cada 5 minutos.
  Para instalar ou atualizar no host:
  ```bash
  sudo install -m 755 config/smart/casa-monitor-smart.sh /usr/local/sbin/casa-monitor-smart
  sudo install -m 644 config/smart/casa-monitor-smart.{service,timer} /etc/systemd/system/
  sudo systemctl daemon-reload && sudo systemctl enable --now casa-monitor-smart.timer
  ```
- **Acesso ao Docker:** a API **não** monta o `docker.sock`, que daria controle total do servidor. Ela fala
  com o `casa-docker-proxy` ([`tecnativa/docker-socket-proxy`](https://github.com/Tecnativa/docker-socket-proxy)),
  numa rede interna sem saída para a internet e sem porta publicada, com as regras de
  `config/docker-proxy/haproxy.cfg.template`: só `GET /containers/json` e `GET /containers/{id}/stats`
  passam. Inspect (que mostra variáveis de ambiente), logs, archive, export e qualquer escrita respondem 403.
- **Falhas:** se o MariaDB ou a leitura do sistema falhar, a coleta só registra um aviso no log e tenta
  de novo no minuto seguinte. A API sobe mesmo com o MariaDB fora do ar; o histórico responde 503.
- **Banco e usuário** (já criados no servidor; para recriar, pelo socket do container `mariadb`):
  ```bash
  docker exec -i mariadb mariadb -uroot <<'SQL'
  CREATE DATABASE IF NOT EXISTS casa_monitor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE USER IF NOT EXISTS 'casa_monitor'@'%' IDENTIFIED BY '<senha>';
  GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP ON casa_monitor.* TO 'casa_monitor'@'%';
  SQL
  ```
  A senha vai em `MONITOR_DATASOURCE_PASSWORD` no `.env`.

## Autenticação e permissões

- **Login:** `POST /v1/auth/login` com `{ "email", "senha" }` devolve um JWT. Envie em todas as outras
  chamadas como `Authorization: Bearer <token>`. Cinco senhas erradas seguidas bloqueiam o e-mail por
  15 minutos (resposta 429).
- **Papéis:**
  - `ADMIN` vê e altera dados de qualquer pessoa, é o único que cria, altera e exclui pessoas e o único
    que acessa o monitoramento do servidor (`/v1/servidor/**`).
  - `USUARIO` só acessa os próprios dados; pedir os de outra pessoa (por `pessoa` ou `pessoaId`) responde 403.
- **Integrações:** o cabeçalho `X-API-KEY` (variável `API_KEY`) dá o papel `INTEGRACAO`, aceito apenas
  em `POST /v1/geracao-solar` (usado pelo Node-RED para gravar as leituras do inversor).
- **Rotas públicas:** login, `/actuator/health` e a documentação Swagger.

## Swagger / OpenAPI

- UI: `http://<host>:8080/casa-api/swagger-ui.html` (ou `http://<host>:3000/casa-api/swagger-ui.html` pelo front)
- JSON: `/casa-api/v3/api-docs`

Para testar endpoints protegidos: execute `POST /v1/auth/login`, copie o `token`, clique em
**Authorize** e cole no esquema `jwt`. A autorização fica salva no navegador entre recarregamentos.

Toda operação, parâmetro e campo de request/response tem descrição e exemplo, escritos com as
anotações `@Tag`, `@Operation`, `@Parameter`, `@ApiResponse` e `@Schema` nos controllers e records
(`api/v1`). As respostas de erro padrão (400, 401, 403, 500) são acrescentadas a todas as operações por
`config/OpenApiConfig`, sempre com o corpo `ErroResponse`. **Ao criar um endpoint, documente-o do
mesmo jeito.**

## Endpoints

Resumo; detalhes, parâmetros e exemplos no Swagger. Mês/competência é sempre `aaaa-mm`.

| Grupo | Método e caminho | Descrição |
|---|---|---|
| Autenticação | `POST /v1/auth/login` | Login (público); devolve token e usuário |
| | `GET /v1/auth/me` | Usuário logado |
| Compras | `GET /v1/compras?pessoa&cartao` | Compras sem parcelas, filtradas no banco |
| | `GET /v1/compras/parcelas?de&ate&pessoa&cartao` | Parcelas de um período (até 36 meses) numa chamada |
| | `GET /v1/compras/parcelas/{ano}/{mes}?pessoa&cartao&ultimaParcela` | Parcelas de um mês |
| | `POST /v1/compras` | Cadastra compra e gera as parcelas (divide entre pessoas separadas por vírgula) |
| | `PUT /v1/compras/{id}` · `DELETE /v1/compras/{id}` | Edita (recria parcelas) · exclui |
| Contas | `GET /v1/contas?pessoaId` | Contas da pessoa, com parcela atual e saldo |
| | `POST /v1/contas` · `PUT /v1/contas/{id}?aPartirDe` | Cadastra · edita a partir de um mês |
| | `DELETE /v1/contas/{id}?aPartirDe` | Exclui ou encerra a partir de um mês |
| | `GET /v1/contas/lancamentos?pessoaId&de&ate` | Lançamentos mês a mês (gerados sob demanda) |
| | `PATCH /v1/contas/lancamentos/{id}` | Marca pago e/ou informa valor real |
| Despesas | `GET /v1/despesas/{ano}?pessoa` | Totais do cartão por mês |
| Faturas | `GET` · `PUT /v1/faturas/{ano}/{mes}?pessoaId` | Situação de pagamento da fatura |
| Renda | `GET` · `PUT /v1/rendas/{ano}/{mes}?pessoaId` | Renda do mês (herda do último mês informado) |
| Filtros | `GET /v1/filtros/{anos,meses,pessoas,cartoes,categorias,tipos-lancamento}` | Opções das telas |
| Pessoas | `GET /v1/pessoas` | Lista (sem senha) |
| | `POST /v1/pessoas` · `PUT` · `DELETE /v1/pessoas/{id}` | Só `ADMIN` |
| Geração solar | `GET /v1/geracao-solar?data` | Leituras de um dia |
| | `GET /v1/geracao-solar/{ano}/{mes}` · `GET /v1/geracao-solar/{ano}` | Geração por dia · por mês |
| | `POST /v1/geracao-solar` | Grava leitura (integração, `X-API-KEY`) |
| Carros | `GET /v1/carros/{codigoFipe}` | Histórico FIPE |
| Servidor (só `ADMIN`) | `GET /v1/servidor/agora` | Leitura ao vivo: CPU por núcleo, carga, memória, temperaturas, discos, rede e containers |
| | `GET /v1/servidor/dia?data` | Mín/média/máx do dia, série a cada 5 min, espaço e temperatura dos discos e resumo/série de cada container (padrão: hoje) |
| | `GET /v1/servidor/dias?de&ate` | Estatísticas por dia (padrão: últimos 30 dias; até 366 por consulta) |

## Erros

Todo erro responde JSON no formato `{ "mensagem": "texto para o usuário" }`
(`api/v1/handler/ApiExceptionHandler` e `config/security/ErroSegurancaJson`):

| Status | Quando |
|---|---|
| 400 | Validação (`IllegalArgumentException`/`Assert`), JSON inválido, parâmetro faltando ou de tipo errado |
| 401 | Sem token, token inválido/expirado, credenciais erradas |
| 403 | Sem permissão (dados de outra pessoa, ação só de admin) |
| 404 | Recurso não encontrado (`RecursoNaoEncontradoException`) ou rota inexistente |
| 405 / 415 | Método não suportado / corpo que não é JSON |
| 429 | Login bloqueado por excesso de tentativas |
| 503 | Monitoramento sem acesso ao MariaDB ou ao sistema (`MonitorIndisponivelException`) |
| 500 | Erro inesperado (detalhe só no log, nunca na resposta) |

## Estrutura do código

```
src/main/java/com/kowalski/casaapi/
├── api/v1/
│   ├── controller/   # Endpoints REST (documentados com OpenAPI)
│   ├── input/        # Corpos de requisição (records)
│   ├── response/     # Corpos de resposta (records)
│   ├── dto/          # DTOs de integração (leitura do inversor)
│   └── handler/      # ApiExceptionHandler: exceção → status + {"mensagem"}
├── business/
│   ├── auth/         # Login, JWT, usuário logado e admin inicial
│   ├── service/      # Regras de negócio (interfaces + impl/)
│   ├── repository/   # Spring Data JPA
│   ├── dao/          # Consultas nativas (filtros e totais de despesas)
│   ├── model/        # Entidades JPA
│   ├── enumerator/   # Papel, TipoConta, Meses
│   ├── monitor/      # Saúde do servidor: /proc, /sys e containers (API do Docker), coleta agendada e histórico no MariaDB
│   └── exception/    # RecursoNaoEncontradoException, MonitorIndisponivelException
├── config/           # Segurança, CORS, OpenAPI, filtro da API key, DataSource do monitoramento
└── util/             # Competencias (datas aaaa-mm), Util
src/main/resources/
├── application.yaml
└── db/migration/     # Scripts SQL (aplicados à mão)
```

## Testes

```bash
mvn test
```

São testes unitários (services com mocks) e de fatia web (`@WebMvcTest`): contrato dos endpoints,
regras de segurança e formato dos erros. O leitor do monitoramento é testado com um `/proc` e um `/sys`
falsos em pasta temporária, e o dos containers com respostas no formato da API do Docker. **Nenhum teste acessa o banco**, então as consultas JPQL e
nativas só são exercitadas com a aplicação rodando.

## Notas de desempenho

- As associações `Compra ↔ CompraParcela` são `LAZY`. As consultas de parcelas trazem compra e
  categoria com `JOIN FETCH`, e `default_batch_fetch_size: 100` cobre os demais casos — evite voltar
  para `EAGER`, que gerava uma consulta por compra.
- Filtros por mês usam intervalo de datas (`dataParcela >= início AND < fim`), que aproveita o índice
  de `dt_parcela`; não use `date_part`/conversão para texto no `WHERE`.
- Para várias competências, prefira `GET /v1/compras/parcelas?de&ate` a uma chamada por mês.
- Consultas acima de 1 s são registradas no log (`LOG_QUERIES_SLOWER_THAN_MS`).
