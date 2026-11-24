# Casa API

Backend da casa: compras no cartão e parcelas, contas fixas e lançamentos mensais, renda, faturas,
pessoas/acessos e monitoramento (geração solar, histórico FIPE e saúde do servidor). Consumido pelo front
[`casa`](../casa) e por integrações (Node-RED).

- **Stack:** Java 25 · Spring Boot 4.1 (Web MVC, Data JPA, Security + OAuth2 Resource Server) · PostgreSQL · MariaDB (só o histórico do monitoramento) · springdoc-openapi (Swagger) · Lombok · Maven
- **Base da API:** `http://<host>:8080/casa-api` (o nginx do front também repassa `/casa-api/` para cá)
- **Documentação interativa:** [`/casa-api/swagger-ui.html`](http://localhost:8080/casa-api/swagger-ui.html) · OpenAPI JSON em `/casa-api/v3/api-docs`

## Sumário

- [Rodando](#rodando)
- [Configuração](#configuração)
- [Banco de dados e migrations](#banco-de-dados-e-migrations)
- [Monitoramento do servidor](#monitoramento-do-servidor)
- [Crescimento das crianças](#crescimento-das-crianças)
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
`/etc/hostname` do host e a pasta `/var/lib/casa-monitor` (JSONs gravados pelos timers do host), usados pelo
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
| `MONITOR_HOST_ARQUIVO` | não | vazio | JSON com o status do host, gravado pelo timer `casa-monitor-host` (no container, `/host/smart/host.json`); vazio desliga |
| `MONITOR_DISCOS` | não | `Sistema=/` | Discos a medir, `Nome=caminho` separados por vírgula. No servidor o caminho é o ponto de montagem no host (ex.: `Mídia=/mnt/media`), lido das montagens do `smart.json`; sem esse arquivo, é qualquer arquivo ou pasta dentro do disco, medido localmente |

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
| `V6__compras_consistencia.sql` | Compras consistentes: cartão com chave para `tipo_lancamento` (recuperado pelo nome quando faltava) e obrigatório nas compras novas; nome da pessoa único; `pessoa_id` obrigatório, preenchido pelo nome (um gatilho cobre a versão antiga da API até o deploy); parcela única por número, a partir de 1, apagada junto com a compra. **Antes** do deploy que passa a usar `pessoa_id` |
| `V7__compras_cartao_validado.sql` | Confirma o cartão obrigatório nas compras antigas. Só depois de escolher, pela tela de edição, o cartão das compras que ficaram sem cartão após a V6; antes disso falha sem mudar nada |
| `V8__compras_sem_nome_da_pessoa.sql` | Apaga `compra.nm_pessoa_compra`, o gatilho que a preenchia e o índice dela: a compra guarda só `pessoa_id` e o nome vem do cadastro da pessoa. **Depois** do deploy que passa a usar `pessoa_id` |
| `V9__regras_de_consistencia.sql` | Regras no banco: competência no dia 1 (renda, fatura, conta e lançamento), lançamento pago se e só se tiver data de pagamento, número da parcela a partir de 1, cor da categoria em `#RRGGBB`, `ativo` da categoria obrigatório e `id` do carro único. Só acrescenta regras que os dados e a API já cumprem: roda a qualquer momento, sem deploy |
| `V10__indices.sql` | Índices sem travar a escrita (`CONCURRENTLY`, por isso fora de transação): `geracao(data)`, que é lida por período, e as chaves `compra.cartao_id` e `conta.categoria_id`; apaga os repetidos `idx_categorias_nome` (coberto pelo único do nome), `idx_categorias_ativo` e `pessoa_email_key` (coberto por `uq_pessoa_email`). Roda a qualquer momento, sem deploy; se avisar índice inválido, apague-o com `DROP INDEX CONCURRENTLY` e rode de novo |
| `V11__arquivo.sql` | Move para o schema `arquivo`, sem apagar nada, as tabelas que nenhum programa usa (`transacao`, `lista_compra`, `rastreamento`, `rastreamento_detalhe`, `geracao_mensal`, `financa.banco`, `financa.cartao`, `investimento.tb_carteira`, `investimento.tb_investimento_renda_fixa`) e apaga os schemas `financa` e `investimento` se ficarem vazios. Roda a qualquer momento, sem deploy. Para voltar uma tabela: `ALTER TABLE arquivo.<tabela> SET SCHEMA <schema de antes>;` (e `CREATE SCHEMA` antes, se for `financa` ou `investimento`) |
| `db/crescimento/V1__estrutura.sql` | Schema `crescimento` do módulo de crescimento das crianças (pasta própria, ver abaixo) |
| `db/crescimento/V2__diario_e_album.sql` | Diário e álbum do módulo crescimento (`registro`, fotos ligadas a registros) |
| `db/crescimento/V3__marcos_e_vacinas.sql` | Marcos atingidos e vacinas aplicadas do módulo crescimento |
| `db/crescimento/V6__exames.sql` | Exames da criança (PDF ou imagem) |
| `db/crescimento/V7__remove_ia.sql` | Apaga as tabelas `resumo_mes` e `pergunta_ia` (V4 e V5, removidas) |
| `db/crescimento/V8__indices.sql` | Índices das chaves do crescimento sem travar a escrita: `crianca.foto_id`, `foto.familia_id` e `registro.medicao_id`. Roda a qualquer momento, sem deploy |
| `db/passagens/V1__estrutura.sql` | Schema `passagens`: estado do robô, preços buscados e alertas (sem ele, `/v1/passagens` responde 503) |
| `db/passagens/V2__rotas_pelo_casa.sql` | Tabela `passagens.rota`: as rotas passam a ser cadastradas pela aba Passagens (o robô só lê) |

Para aplicar um script no servidor:

```bash
docker exec -i postgresql18-postgresql18-1 psql -U postgres -d casa -v ON_ERROR_STOP=1 \
  < src/main/resources/db/migration/V5__remove_funcao_remove_zeros_esquerda.sql
```

Os scripts usam `IF EXISTS` / `IF NOT EXISTS` e podem ser reaplicados. Aplique cada um **depois** do
deploy da versão que deixou de depender do que ele remove.

### Diagnóstico do banco

`banco/diagnostico.sql` roda numa transação só de leitura e desfeita no fim (`ROLLBACK`). Mostra só quantidades e
estatísticas, nunca linhas com dados: inconsistências (cartão, pessoa e parcelas das compras, competências fora do
dia 1, pagamentos sem data, família das fotos e exames), linhas das tabelas sem uso, tamanho e forma de leitura das
tabelas, uso dos índices e a configuração do PostgreSQL. É a base da refatoração do banco. O e2e roda o script contra
a estrutura para garantir que ele continua funcionando.

```bash
docker exec -i postgresql18-postgresql18-1 psql -U postgres -d casa < banco/diagnostico.sql
```

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
| Espaço em disco | montagens do host no `smart.json` (pontos de `MONITOR_DISCOS`); um disco desmontado não aparece |
| Containers Docker | `GET /containers/json` e `GET /containers/{id}/stats?one-shot=true`, pelo proxy só-leitura |
| Temperatura dos discos | SMART, lido no host pelo timer `casa-monitor-smart` (veja abaixo) |
| Pressão (PSI) | `/proc/pressure/{cpu,memory,io}`, média de 10 s |
| GPU | `/sys/class/devfreq/*gpu*/load` |
| Limite da CPU (calor) | `scaling_max_freq` ÷ `cpuinfo_max_freq` de cada policy do cpufreq |
| Latência e ocupação dos discos | `/proc/diskstats` (tempo por operação e tempo com I/O em andamento) |
| Erros de rede | `rx/tx_errors` e `rx/tx_dropped` das interfaces físicas |
| Status do host | backup, serviços, montagens, alertas, kernel, atualizações, internet, SSH, NPU e Docker, pelo timer `casa-monitor-host` (veja abaixo) |

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
  `major:minor` de cada disco e partição, mais as montagens do host (ponto, `major:minor` e espaço, do
  `findmnt`). A API lê esse arquivo (montado só-leitura), acha cada ponto de `MONITOR_DISCOS` nas montagens e
  liga ao disco pelo `major:minor`. Tudo vem do host no mesmo instante, então não depende de `sda`/`sdb` (que
  mudam quando a dock USB cai e volta) nem de bind mounts dos discos no container, que ficariam presos ao
  sistema de arquivos de quando o container subiu. Discos em repouso não são acordados (`smartctl -n standby`); leituras com mais de 5 minutos
  são ignoradas. O espaço e a temperatura vão para `amostra_disco` a cada 5 minutos.
  Para instalar ou atualizar no host:
  ```bash
  sudo install -m 755 config/smart/casa-monitor-smart.sh /usr/local/sbin/casa-monitor-smart
  sudo install -m 644 config/smart/casa-monitor-smart.{service,timer} /etc/systemd/system/
  sudo systemctl daemon-reload && sudo systemctl enable --now casa-monitor-smart.timer
  ```
- **Status do host:** o que só o host enxerga (backup restic, timers e serviços do systemd, montagens, alertas do
  `monitorar-servidor`, erros do kernel, apt/Diun, ping/DNS/Tailscale, logins SSH, NPU, reinícios e espaço do Docker)
  é lido pelo script `config/host/casa-monitor-host.sh`, a cada minuto (segundo 50), e gravado em
  `/var/lib/casa-monitor/host.json` (mesma pasta do SMART, já montada). Só lê; partes caras (apt, Diun, `docker system df`)
  ficam em cache de 1 h. A API repassa o JSON em `GET /v1/servidor/host` e a cada minuto grava `amostra_host`
  (internet, DNS, Tailscale, NPU, alertas, serviços, backup...) e os eventos novos em `evento_host`
  (`GET /v1/servidor/eventos`): logins SSH, backups, alertas novos/resolvidos, serviços que falharam, discos
  desmontados, quedas de internet (2 minutos seguidos), containers que reiniciaram, linhas de erro do kernel e boots.
  Eventos têm chave única: reler o arquivo ou reiniciar a API não duplica. Para instalar ou atualizar no host:
  ```bash
  sudo install -m 755 config/host/casa-monitor-host.sh /usr/local/sbin/casa-monitor-host
  sudo install -m 644 config/host/casa-monitor-host.{service,timer} /etc/systemd/system/
  sudo systemctl daemon-reload && sudo systemctl enable --now casa-monitor-host.timer
  ```
- **Internet (aba Internet):** dois timers no host gravam na mesma pasta `/var/lib/casa-monitor`.
  `casa-monitor-internet` roda a cada minuto (segundo 20) e grava `internet.json`: ping (média, jitter e perda) ao
  roteador, ao provedor, à Cloudflare e ao Google, tempo de DNS (Pi-hole e públicos), IP público e Pi-hole.
  `casa-monitor-velocidade` roda a cada 4 horas e grava `velocidade.json` com o teste de velocidade da Cloudflare
  (~250 MB por teste; cada medição tem prazo de 30 s) e os 60 últimos testes. A API lê os dois a cada minuto
  (segundo 40), grava `amostra_internet` e `teste_velocidade` (só quando chega um teste novo) e gera eventos: IP
  público que mudou, perda de pacotes por 3 minutos seguidos (e o fim dela, com duração e pico), teste que falhou e
  download abaixo da metade da mediana dos últimos testes. Para instalar ou atualizar no host:
  ```bash
  sudo install -m 755 config/internet/casa-monitor-internet.py /usr/local/sbin/casa-monitor-internet
  sudo install -m 755 config/internet/casa-monitor-velocidade.py /usr/local/sbin/casa-monitor-velocidade
  sudo install -m 644 config/internet/casa-monitor-{internet,velocidade}.{service,timer} /etc/systemd/system/
  sudo systemctl daemon-reload
  sudo systemctl enable --now casa-monitor-internet.timer casa-monitor-velocidade.timer
  sudo systemctl start casa-monitor-velocidade   # primeiro teste na hora, sem esperar 4 h
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

## Crescimento das crianças

Módulo `com.kowalski.casaapi.crescimento`: famílias, crianças, medidas com os percentis da OMS e fotos.
Foi feito para **poder virar um produto separado** (com app iOS/Android) sem reescrever:

- **Código isolado:** nada do módulo importa outros pacotes do casa-api (garantido pelo `ArquiteturaCrescimentoTest`).
  A ligação com o login é pelas interfaces `crescimento.identidade.UsuarioAtual` e `DiretorioUsuarios`, implementadas
  em `config/crescimento/IdentidadeCrescimento` (pessoa logada do casa). No produto, só essa classe muda.
- **Banco próprio:** schema `crescimento` (script `src/main/resources/db/crescimento/V1__estrutura.sql`, aplicado à mão
  como os demais). Nenhuma chave estrangeira para tabelas do casa: o usuário é guardado só pelo id.
- **Família como unidade:** cada família tem membros `RESPONSAVEL` (alteram tudo) ou `LEITOR` (avós, só veem). Quem
  não é membro recebe 404, para não revelar que a criança existe. O ADMIN do casa **não** vê famílias de que não faz parte.
- **Curvas da OMS:** tabelas LMS oficiais por dia de vida, de 0 a 5 anos (`resources/crescimento/oms`, do pacote
  [anthro](https://github.com/WorldHealthOrganization/anthro) da OMS), com escore-z, percentil e o ajuste da OMS além de
  ±3 DP no peso. Prematuros (< 37 semanas) usam a idade corrigida até os 2 anos.
- **Alertas** (`/resumo`): medida fora de P3–P97, duas linhas de percentil cruzadas, peso que caiu, ganho de peso
  abaixo do esperado e pesagem atrasada.
- **Fotos em disco** atrás de `FotoStorage` (`DiscoFotoStorage` hoje; S3/MinIO no produto):
  - pasta `CRESCIMENTO_FOTOS_DIRETORIO` (no servidor, `/mnt/media/crescimento` montada em `/dados/crescimento`), com o
    arquivo `.armazenamento-crescimento`. Sem ele a API responde 503 em vez de gravar no lugar errado (disco caído);
  - cada foto vira 3 JPEGs (`original`, `media` 1600 px, `mini` 480 px) em `{familia}/{crianca}/{ano}/{uuid}-{variante}.jpg`,
    endireitados pela orientação EXIF e **sem metadados** (a localização GPS é descartada). A data em que foi tirada é
    guardada no banco. A mesma foto enviada de novo não é duplicada (SHA-256);
  - as imagens são servidas por **links assinados** (HMAC, `CRESCIMENTO_LINK_SEGREDO`, padrão = `JWT_SECRET`) que valem
    de 6 a 12 h e funcionam em `<img>` sem login; renove com `GET /v1/crescimento/fotos/{id}`;
  - limite de 25 MB por foto (Spring) e 30 MB no nginx do front; fotos acima de 16 MP são reduzidas na leitura
    (`CRESCIMENTO_FOTO_MAX_MEGAPIXELS`), uma por vez, para caber nos 512 MB de heap;
  - backup: `/mnt/media/crescimento` entra no restic diário (`/usr/local/sbin/backup-servidor.sh`), com a cópia no hd320;
    se `/mnt/media` cair e voltar, o `remontar-discos.sh` reinicia o casa-api para o bind enxergar o disco de novo.

| Endpoint (`/v1/crescimento/...`) | |
|---|---|
| `GET/POST familias`, `PUT familias/{id}/membros`, `DELETE familias/{id}/membros/{usuarioId}` | Famílias e membros |
| `GET/POST familias/{id}/criancas`, `GET/PUT/DELETE criancas/{id}` | Crianças (o PUT também define a foto de perfil) |
| `GET/POST criancas/{id}/medicoes`, `PUT/DELETE criancas/{id}/medicoes/{medicaoId}` | Medidas com escore-z e percentil |
| `GET criancas/{id}/resumo` | Últimas medidas, ganho de peso e alertas |
| `GET criancas/{id}/curvas/{PESO\|COMPRIMENTO\|PERIMETRO}` | Linhas P3–P97 da OMS e os pontos da criança |
| `POST criancas/{id}/fotos` (multipart, campo `arquivo`), `GET criancas/{id}/fotos`, `GET/DELETE fotos/{id}` | Fotos |
| `GET fotos/{id}/arquivo?v=MINI\|MEDIA\|ORIGINAL&exp=&sig=` | A imagem (sem login; link assinado) |
| `GET criancas/{id}/linha-do-tempo?tipo=&soComFotos=&pagina=&tamanho=` | Registros e medições avulsas, do mais recente ao mais antigo, com fotos, mês de vida e ganho de peso |
| `POST criancas/{id}/registros`, `GET/PUT/DELETE criancas/{id}/registros/{registroId}` | Diário: `PESAGEM`, `MOMENTO` ou `CONSULTA` (profissional, retorno), com fotos e medidas |
| `GET criancas/{id}/album` | Mêsversários (situação, capa, peso, aviso de mêsversário), primeiras vezes e momentos especiais |
| `PUT/DELETE criancas/{id}/album/{chave}`, `POST criancas/{id}/album/especiais` | Guardar fotos num lugar do álbum (`nascimento`, `mes-3`, `ano-1`, `primeira-sorriso`...) |
| `GET criancas/{id}/marcos`, `PUT/DELETE criancas/{id}/marcos/{marcoId}` | Marcos do desenvolvimento por fase (Caderneta da Criança), com idade corrigida e brincadeiras |
| `GET criancas/{id}/vacinas`, `PUT/DELETE criancas/{id}/vacinas/{vacinaId}`, `POST criancas/{id}/vacinas/extras` | Carteira de vacinação (calendário do SUS) e vacinas extras |
| `GET criancas/{id}/agenda` | Vacinas atrasadas e dos próximos 60 dias, retorno marcado e próximo mêsversário |
| `GET/POST criancas/{id}/exames` (multipart: `arquivo`, `titulo`, `data`, `observacao`), `PUT/DELETE criancas/{id}/exames/{exameId}` | Exames (PDF ou imagem) |
| `GET criancas/{id}/exames/{exameId}/arquivo` | O arquivo do exame (exige login e ser da família) |

**Diário e álbum:** um lugar do álbum é um registro do diário com `album_chave` (no máximo um por lugar e criança),
então tudo o que vai para o álbum aparece também na linha do tempo. As fotos são enviadas antes (`POST .../fotos`) e
ligadas ao registro por `fotoIds`, na ordem; foto tirada de um registro é apagada (a de perfil só fica solta). Medidas
informadas num registro viram uma medição com percentil. Os mêsversários seguem `LocalDate.plusMonths` (quem nasceu em
31/01 faz 1 mês em 28/02), também usado no mês de vida da linha do tempo. Script: `db/crescimento/V2__diario_e_album.sql`.

**Marcos e vacinas:** os catálogos ficam no código (`CatalogoMarcos`, `CatalogoVacinas`, com versão); o banco guarda
só o que foi registrado, pelo id do catálogo, que nunca deve ser reaproveitado com outro sentido. Marcos usam a idade
corrigida dos prematuros; vacinas, a idade real. Script: `db/crescimento/V3__marcos_e_vacinas.sql`.

**Exames:** PDF, JPG ou PNG até 25 MB, com nome, data e observação. O tipo é conferido pelo conteúdo (não pelo nome);
imagens são regravadas em JPEG, endireitadas e **sem metadados** (GPS); PDFs ficam como vieram. Os arquivos ficam no
mesmo armazenamento das fotos, em `exames/{familia}/{crianca}/{uuid}.pdf|.jpg`, e **só saem com login** de quem é da
família (`GET .../exames/{id}/arquivo`, sem link público, `Cache-Control: no-store`); o front mostra por um endereço
`blob:` local. O mesmo arquivo enviado de novo não é duplicado (SHA-256). Excluir a criança apaga os exames.
Script: `db/crescimento/V6__exames.sql`.

**Sem IA:** nenhuma IA tem acesso a dados pessoais, em nenhuma parte do sistema. A integração com IA do módulo foi
**removida em 23/11/2026** (as tabelas dela são apagadas por `db/crescimento/V7__remove_ia.sql`, aplicado pelo
usuário). O `ArquiteturaCrescimentoTest` falha se o módulo voltar a ter pacote de IA, cliente HTTP ou menção a serviços
de IA, e o `SemIaTest` falha se qualquer código da API mencionar um serviço de IA.

**Para levar a outro projeto:** copie o pacote `crescimento`, `resources/crescimento`, `db/crescimento` e os testes;
implemente `UsuarioAtual`/`DiretorioUsuarios` com o login do produto; exporte o schema (`pg_dump -n crescimento`) e a
pasta das fotos.

## Autenticação e permissões

- **Login:** `POST /v1/auth/login` com `{ "email", "senha" }` devolve um JWT. Envie em todas as outras
  chamadas como `Authorization: Bearer <token>`. Cinco senhas erradas seguidas bloqueiam o e-mail por
  15 minutos (resposta 429).
- **Papéis:**
  - `ADMIN` vê e altera dados de qualquer pessoa, é o único que cria, altera e exclui pessoas e o único
    que acessa o monitoramento do servidor (`/v1/servidor/**`).
  - `USUARIO` só acessa os próprios dados; pedir os de outra pessoa (por `pessoa` ou `pessoaId`) responde 403.
- **Integrações:** o cabeçalho `X-API-KEY` (variável `API_KEY`) dá o papel `INTEGRACAO`, aceito apenas
  em `POST /v1/geracao-solar` (usado pelo Node-RED para gravar as leituras do inversor) e em
  `POST /v1/servidor/ventilacao` (usado pelo Home Assistant para gravar a ventilação do servidor).
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
| | `GET /v1/servidor/ventilacao` | Ventoinhas agora: tomada, temperaturas e limites que o HA usa, desde quando e tempo ligadas hoje |
| | `GET /v1/servidor/ventilacao/dia?data` · `GET /v1/servidor/ventilacao/dias?de&ate` | Um ponto por minuto e períodos ligadas · tempo ligadas por dia |
| | `POST /v1/servidor/ventilacao` | Grava a leitura do minuto (Home Assistant, `X-API-KEY`; tabela `amostra_ventilacao`) |
| | `GET /v1/servidor/internet` | Internet agora (internet.json) e o último teste de velocidade |
| | `GET /v1/servidor/internet/dia?data` · `GET /v1/servidor/internet/dias?de&ate` | Série do dia e resumo · resumo por dia (até 366 dias) |
| | `GET /v1/servidor/internet/velocidade?de&ate` | Testes de velocidade do período |
| Passagens (só `ADMIN`) | `GET /v1/passagens?dias` | Rotas, séries de preço e alertas (padrão: 30 dias; 0 = tudo) |
| | `POST /v1/passagens/rotas` · `PUT /v1/passagens/rotas/{id}` | Cadastra · edita uma rota (o robô confere a cada 30 s) |
| | `DELETE /v1/passagens/rotas/{id}?historico` | Remove a rota; `historico=true` apaga também preços e alertas |

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
falsos em pasta temporária, e o dos containers com respostas no formato da API do Docker. Esses testes não acessam
o banco.

**Migrations** (`MigrationsTest`, só lê arquivos): todo script de `src/main/resources/db` precisa poder ser reaplicado
(`IF [NOT] EXISTS` ou um bloco `DO $$`), estar na tabela de migrations deste README, e o `estrutura.sql` dos testes e2e
precisa ter o resultado de todos eles (tabelas e colunas criadas, e as apagadas fora). Ao criar uma migration, acrescente
o DDL ao `estrutura.sql` (ou gere de novo com `pg_dump --schema-only` depois de aplicar).

**Testes de ponta a ponta** (`CasaApiE2ETest`): `bash e2e/rodar.sh` sobe numa rede Docker isolada um PostgreSQL 18
com a estrutura do banco (`src/test/resources/e2e/estrutura.sql`, gerada com `pg_dump --schema-only`, mais os dados
mínimos de `dados.sql`) e um MariaDB vazio, sobe a API inteira e a testa por HTTP, apagando tudo no fim. Precisa de
Docker; não toca nos bancos de produção. Ao criar tabelas novas, gere o `estrutura.sql` de novo. O hook pre-commit
roda esse script antes de cada commit, e o casa usa a mesma estrutura nos testes E2E dele (`CASA_API_DIR`).

## Notas de desempenho

- As associações `Compra ↔ CompraParcela` são `LAZY`. As consultas de parcelas trazem compra e
  categoria com `JOIN FETCH`, e `default_batch_fetch_size: 100` cobre os demais casos — evite voltar
  para `EAGER`, que gerava uma consulta por compra.
- Filtros por mês usam intervalo de datas (`dataParcela >= início AND < fim`), que aproveita o índice
  de `dt_parcela`; não use `date_part`/conversão para texto no `WHERE`.
- Para várias competências, prefira `GET /v1/compras/parcelas?de&ate` a uma chamada por mês.
- Consultas acima de 1 s são registradas no log (`LOG_QUERIES_SLOWER_THAN_MS`).

## Histórico de versões

Cada commit gera uma versão (major.minor.patch). A mais recente fica no topo.

### 3.0.4 — 24/11/2026

As tabelas antigas que o sistema não usa mais vão para um schema separado, o `arquivo`. Os dados continuam lá, mas a estrutura em uso fica mais limpa.

- Nova migração V11__arquivo.sql: cria o schema `arquivo` e move para ele as tabelas sem uso public.transacao, public.lista_compra, public.rastreamento, public.rastreamento_detalhe, public.geracao_mensal, financa.cartao, financa.banco, investimento.tb_carteira e investimento.tb_investimento_renda_fixa. Os dados são mantidos.
- Removidos os schemas `financa` e `investimento` quando ficam vazios depois da mudança (sem tabelas, funções nem tipos).
- Migração a aplicar no banco do servidor: roda em uma transação com lock_timeout de 5s e pode ser executada mais de uma vez (IF EXISTS).
- Diagnóstico (banco/diagnostico.sql): retiradas as verificações 3 e 4, que comparavam tipo_lancamento com financa.cartao. A contagem de linhas agora lê as tabelas no schema `arquivo`.
- Estrutura dos testes e2e (estrutura.sql) atualizada com a migração V11.

### 3.0.3 — 24/11/2026

Esta versão deixa mais rápidas as consultas de energia, compras, contas e Crescimento com índices novos no banco, e remove do banco índices e restrições que estavam duplicados. A API não muda.

- Nova migração V10__indices.sql: cria os índices ix_geracao_data (geracao.data), ix_compra_cartao_id (compra.cartao_id) e ix_conta_categoria_id (conta.categoria_id), e depois roda ANALYZE nessas tabelas.
- Na V10, removidos os índices idx_categorias_nome e idx_categorias_ativo e a restrição pessoa_email_key. O e-mail da pessoa continua único pelo índice uq_pessoa_email (lower(email)), que já existia.
- Nova migração crescimento/V8__indices.sql: cria os índices ix_crianca_foto_id, ix_foto_familia_id e ix_registro_medicao_id no esquema crescimento.
- Os índices são criados com CREATE INDEX CONCURRENTLY e lock_timeout de 5s, sem travar as tabelas. No fim, cada script falha com uma mensagem se algum índice ficou inválido.
- No servidor: as duas migrações precisam ser aplicadas à mão e fora de uma transação (sem BEGIN e sem psql --single-transaction), porque CONCURRENTLY não roda dentro de transação.
- Atualizada a estrutura.sql dos testes e2e com o resultado das duas migrações.

### 3.0.2 — 24/11/2026

O banco passa a garantir regras de consistência em rendas, faturas pagas, contas, lançamentos, categorias e carros, e assim recusa dados fora do padrão.

- Nova migração V9__regras_de_consistencia.sql: competência sempre no dia 1 em renda_mensal, fatura_paga, conta (competencia_inicio e competencia_fim, quando houver) e conta_lancamento.
- Lançamento de conta agora fica coerente: com paga verdadeiro precisa ter data_pagamento, sem pagamento não pode ter data (ck_conta_lancamento_pagamento). numero_parcela, quando houver, precisa ser >= 1.
- Categorias: cor só no formato #RRGGBB (ck_categorias_cor) e ativo obrigatório (categorias_ativo_not_null).
- Novo índice único uq_carro_id em monitoramento.carro (id).
- A migração cria as restrições como NOT VALID e depois valida todas. Se ainda houver registro fora das regras, a validação falha e a transação inteira é desfeita.
- No servidor: a V9 é aplicada à mão, depois da V8. Antes, vale o usuário rodar o banco/diagnostico.sql para ver se há dados que quebram as regras. O NOT NULL com nome usado em categorias.ativo depende da versão do PostgreSQL, então também vale o usuário conferir isso.
- Removidas do banco/diagnostico.sql as checagens 18 a 26 e 29, que agora o próprio banco garante.
- Atualizada a estrutura do banco de testes e2e com a V9. Novo teste e2e cobre pagar e desfazer o pagamento de um lançamento (PATCH /v1/contas/lancamentos/{id}) e confirma que dataPagamento é limpa.

### 3.0.1 — 24/11/2026

Corrige a migração V6 das compras para que ela não falhe em bancos que ainda têm compras antigas sem cartão.

- Corrigida a migração V6__compras_consistencia.sql: a restrição compra_cartao_id_not_null (NOT NULL NOT VALID em compra.cartao_id) agora é criada no fim do script, depois de todos os UPDATE na tabela compra. Antes, os UPDATE seguintes podiam falhar nas linhas antigas que ainda tinham cartao_id nulo.
- Atualizada a estrutura do banco de testes e2e (estrutura.sql) com a mesma ordem da V6.
- Novo teste em MigrationsTest: falha quando um script tem UPDATE em uma tabela depois de um NOT NULL NOT VALID nessa mesma tabela.
- No servidor: a V6 precisa ser aplicada com a versão corrigida se ainda não foi. Se a versão anterior já foi aplicada, vale o usuário conferir se o controle de migrações reclama da alteração do arquivo.

### 3.0.0 — 23/11/2026

Agora cada compra fica ligada a uma pessoa cadastrada, não mais só ao nome digitado. O banco passa a garantir a consistência de compras, parcelas e cartões. Esta versão traz migrações destrutivas que precisam ser aplicadas à mão no servidor.

- Compras ligadas à pessoa pela nova coluna pessoa_id (chave estrangeira para pessoa), no lugar do nome digitado. O campo nomePessoaCompra continua igual na API.
- POST e PUT /v1/compras agora recusam, com erro 400, compra para nome sem pessoa cadastrada ("Não existe pessoa cadastrada com o nome …"). Quando a compra é dividida entre várias pessoas, nenhuma parte é gravada se uma delas não existir.
- Nome de pessoa agora é único, sem diferenciar maiúsculas (índice uq_pessoa_nome). O cadastro e a edição de pessoas recusam nome repetido. Ao renomear uma pessoa, as compras dela acompanham o novo nome.
- Excluir uma pessoa que ainda tem compras agora devolve 409 com mensagem genérica, sem detalhes do banco. Esse tratamento vale para qualquer violação de integridade.
- Migração V6__compras_consistencia.sql: cria e preenche pessoa_id e cartao_id; cria chaves estrangeiras, parcela única por número, exclusão de parcelas em cascata e checagens de número de parcelas >= 1. Falha se houver pessoas com o mesmo nome.
- Migração V7__compras_cartao_validado.sql: valida que toda compra tem cartão (cartao_id).
- Migração V8__compras_sem_nome_da_pessoa.sql (destrutiva): remove a coluna nm_pessoa_compra, o índice idx_compra_pessoa e o gatilho temporário. As migrações são aplicadas à mão, nesta ordem, e a V6 precisa rodar antes da implantação desta versão.
- Removidas do banco/diagnostico.sql as checagens do nome da pessoa nas compras. Testes ajustados ao fuso da casa e novos testes de unidade e e2e para compras, pessoas e erro 409.

### 2.0.19 — 23/11/2026

Adiciona um script de diagnóstico do banco PostgreSQL, somente leitura, que mostra quantidades de inconsistências e estatísticas de tabelas e índices para orientar a refatoração do banco.

- Novo script banco/diagnostico.sql: roda dentro de uma transação somente leitura (READ ONLY) e termina com ROLLBACK, então não altera nada no banco.
- 36 verificações de inconsistência que mostram só a quantidade de registros afetados, sem conteúdo: compras × tipo_lancamento/financa.cartao, nomes de pessoa, parcelas, competências fora do dia 1, contas e lançamentos, categorias, monitoramento.carro, geração solar e família de fotos/exames do Crescimento.
- Contagem de linhas das tabelas que podem ser arquivadas (public.transacao, lista_compra, rastreamento, geracao_mensal, financa.banco, financa.cartao, tabelas de investimento).
- Relatórios de tamanho e forma de leitura das tabelas, uso dos índices, período coberto pelas estatísticas, versão do PostgreSQL e configurações principais (shared_buffers, work_mem, autovacuum etc.).
- Teste E2E (e2e/rodar.sh) agora executa o diagnóstico contra a estrutura do banco de teste para garantir que o script continua válido.

### 2.0.18 — 23/11/2026

Refatoração interna da autenticação: o login passa a ficar em um pacote próprio, sem mudar endpoints nem o comportamento para quem usa o sistema.

- Novo pacote auth com AuthController, AuthService, AdminInicial, LoginInput, LoginResponse e UsuarioResponse, que saem de api/v1 e business/auth
- UsuarioLogado e UsuarioAutenticado movidos para o pacote pessoa; imports atualizados em Compra, Conta, Despesa, Fatura, Filtro e Renda
- Acesso a pessoas centralizado no PessoaService com os métodos porEmail, porId, porIds e definirComoAdmin, usados pelo login, pelo conversor de JWT, pelo admin inicial e pela identidade do Crescimento
- Lógica do GET /v1/auth/me (dados do usuário logado) levada do controller para AuthService.usuarioAtual, mantendo a mesma resposta
- Removidos imports que não eram usados em PessoaInput e PessoaRepository
- Testes ajustados à nova organização dos pacotes (AuthServiceTest, UsuarioLogadoTest, testes de contrato, segurança e serviços)

### 2.0.17 — 23/11/2026

Reorganiza o código da aba Passagens num pacote próprio, sem mudar o comportamento da API, e adiciona testes do cadastro de rotas.

- Pacote único com.kowalski.casaapi.passagens: o controller, os serviços, a entrada (RotaPassagemInput) e a resposta (PassagensResponse) das Passagens saíram de api.v1 e business e foram para esse pacote.
- Acesso ao banco separado nas classes PassagensRepository (robô, rotas, séries, pontos e alertas) e RotaPassagemRepository (criar, editar, renomear o histórico e remover rotas do schema passagens).
- Validação da rota extraída para a classe ConfiguracaoDaRota, sem mudança nas regras (aeroportos IATA, datas, janela e passageiros).
- PassagensService e RotasPassagemService passam a só aplicar as regras: aviso de robô desatualizado, nome de rota repetido, histórico que acompanha a rota ao renomear e remoção com ou sem histórico.
- Os endpoints GET /v1/passagens e /v1/passagens/rotas (POST, PUT e DELETE) continuam com o mesmo contrato. Nenhuma migração de banco nem configuração nova.
- Novos testes unitários do RotasPassagemService com o repositório simulado e teste e2e que cria, renomeia (levando junto os alertas) e remove uma rota, com um alerta de exemplo em e2e/dados.sql.

### 2.0.16 — 23/11/2026

Refatoração interna do monitoramento da internet, que passa a ficar no próprio pacote; para quem usa o sistema, nada muda.

- Monitoramento da internet (controller, service, repositório, leitor, amostra e respostas) reunido no pacote com.kowalski.casaapi.internet, seguindo a organização por funcionalidade
- Endpoints /v1 da internet sem mudança de contrato: mesmas rotas, parâmetros e respostas
- Data e hora padrão da internet (dia atual, coleta por minuto, limpeza de amostras antigas e aviso de dado desatualizado) passam a vir do relógio único da aplicação (Clock)
- Eventos da internet agora gravados pelo novo método ServidorService.registrarEventos, sem acessar direto o repositório do servidor
- Agregados de velocidade no InternetRepository trocados de Object[] por um record tipado (Velocidade)
- Testes da internet movidos para o novo pacote e ajustados aos novos construtores

### 2.0.15 — 23/11/2026

Reorganiza o código do monitoramento do servidor em um pacote próprio. Os endpoints, as respostas e as tabelas continuam iguais.

- Funcionalidade Servidor reunida no pacote com.kowalski.casaapi.servidor (controller, serviço, repositório, leitores e respostas), que antes ficavam espalhados por api/v1 e business/monitor
- MonitorService renomeado para ServidorService e MonitorRepository para ServidorRepository; os endpoints /v1/servidor (agora, host, eventos, dia, dias) seguem iguais
- Data e hora do monitoramento do servidor agora vêm do relógio central da aplicação (Clock) em vez de LocalDateTime.now()/Instant.now(): coleta, limpeza das amostras antigas, leitura ao vivo, datas padrão dos filtros e validade do status do host
- Consulta do último resumo do host no ServidorRepository passou a usar um record no lugar de Object[], sem mudar o resultado
- Monitoramento da internet ajustado para usar as classes do novo pacote servidor; funções auxiliares de leitura do JSON do LeitorHost agora são públicas
- Testes do monitoramento movidos para o novo pacote e testes de contrato e segurança atualizados para o ServidorService

### 2.0.14 — 23/11/2026

Reorganiza o código da ventilação do servidor em um pacote próprio e passa a usar o relógio central da aplicação para datas e horários, sem mudar os endpoints.

- Funcionalidade ventilação reunida no pacote com.kowalski.casaapi.ventilacao (VentilacaoController, VentilacaoService, VentilacaoRepository e VentilacaoResponse), saindo de api.v1 e business.monitor
- VentilacaoController e VentilacaoService passam a usar o Clock injetado (RelogioConfig, fuso da casa) em vez de LocalDate.now()/LocalDateTime.now() para o dia padrão, os registros, o aviso de leitura desatualizada e a limpeza de leituras antigas
- Consulta da última leitura em VentilacaoRepository passa a devolver Optional com o record Ultima em vez de Object[] nulo
- Testes VentilacaoTest, ContratoEndpointsTest e SecurityConfigTest ajustados ao novo pacote

### 2.0.13 — 23/11/2026

Reorganizadas as funcionalidades de despesas e filtros em pacotes próprios, sem mudar os endpoints, e incluídos testes de ponta a ponta para elas.

- Refatorada a funcionalidade de despesas no pacote despesa (DespesaController, DespesaService, DespesaRepository), com consulta JPQL tipada dos totais do mês no lugar do antigo DAO; o endpoint GET /v1/despesas/{ano} não muda.
- Refatorada a funcionalidade de filtros no pacote filtro (FiltroController, FiltroService, FiltroRepository), com as consultas de categorias, anos, meses, pessoas, cartões e tipos de lançamento reunidas em um só repositório; os endpoints /v1/filtros/* não mudam.
- Removidas as camadas antigas business/dao, business/service e api/v1 (interfaces e implementações de Despesa e Filtro).
- MensagemResponse movida para o pacote comum e convertida em record, mantendo o mesmo formato JSON.
- Serviços de despesas e filtros passaram a rodar em transação somente leitura.
- Novos testes e2e para GET /v1/despesas/{ano} e para todos os filtros das telas de compras, com dados de teste de compras, parcelas, categoria e tipo de lançamento em dados.sql.
- Teste unitário DespesaServiceImplTest renomeado para DespesaServiceTest e ajustado ao novo repositório.

### 2.0.12 — 23/11/2026

Esta versão reorganiza o módulo de compras em um pacote próprio, com as regras na entidade, e corrige as datas das parcelas de compras feitas no fim do mês.

- Compras reorganizadas no pacote próprio `compra` (controller, entidades, repositórios, DTOs e serviço juntos), seguindo a refatoração por funcionalidade. Os endpoints /v1 de compras continuam iguais.
- Regras levadas para as entidades: `Compra.nova`/`alterar` geram as parcelas, `CompraParcela.valorQueFalta` calcula o saldo restante e `CompraInput` passa a validar e dividir o valor entre as pessoas.
- Corrigida a data das parcelas de compras feitas no fim do mês: agora cada parcela é calculada a partir da data da compra (ex.: 31/01 gera 28/02, 31/03 e 30/04), e não mais encadeada mês a mês, o que fazia o dia cair para 28.
- Removidas as interfaces e implementações `CompraService`/`CompraServiceImpl` e `TipoLancamentoService`/`TipoLancamentoServiceImpl`, além da classe `util.Util`. O serviço de compras passa a consultar direto o `TipoLancamentoRepository`.
- Testes ajustados ao novo pacote (`CompraServiceTest`, `CompraResponseTest`) e novo `CompraTest`, que cobre a geração das parcelas, a alteração da compra e o valor que falta pagar.

### 2.0.11 — 23/11/2026

Reorganiza internamente o módulo de Contas, sem mudar os endpoints nem o comportamento para quem usa o sistema.

- Código de Contas reunido no pacote próprio `conta` (controller, entrada, resposta, entidades, repositórios e serviço), como já foi feito com pessoa, categoria, carro, geração, fatura e renda
- Regras de cadastro e edição da conta (descrição, tipo, valor, dia do vencimento, juros, parcelas e período) levadas para a entidade `Conta`, com o novo registro `DadosDaConta`
- Cálculo de parcela atual, parcelas restantes e saldo restante separado no novo registro `SituacaoDasParcelas`
- Baixa manual, correção da data de pagamento, registro do valor real e baixa automática no débito automático levados para `ContaLancamento`
- Interface `ContaService` e implementação `ContaServiceImpl` unidas em uma só classe `ContaService`
- Novo método `PessoaService.referencia` para buscar a dona da conta, devolvendo 404 com o id quando não existe
- Novos testes unitários da entidade `Conta` e do `PessoaService.referencia`; teste do serviço renomeado para `ContaServiceTest`

### 2.0.10 — 23/11/2026

Reorganização interna das funcionalidades de fatura e renda, sem mudança de comportamento para quem usa o sistema.

- Fatura reorganizada no pacote próprio `fatura` (controller, entrada, resposta, entidade FaturaPaga, repositório e serviço juntos); os endpoints não mudaram.
- Renda reorganizada no pacote próprio `renda` (controller, entrada, resposta, entidade RendaMensal, repositório e serviço juntos); os endpoints não mudaram.
- Removidas as interfaces FaturaService/RendaService e as implementações *ServiceImpl; agora cada funcionalidade tem um único serviço concreto.
- Novo método `garantirQueExiste` no PessoaService, usado por fatura e renda para retornar 404 quando a pessoa não existe.
- Montagem das respostas passada para FaturaResponse (`paga`/`naoPaga`) e RendaResponse (`de`/`semValor`), e regra de remoção da renda para RendaInput (`removeOValorDoMes`).
- Testes separados em FaturaServiceTest e RendaServiceTest (no lugar de FaturaERendaServiceImplTest), com novo teste para `garantirQueExiste` no PessoaServiceTest.

### 2.0.9 — 23/11/2026

A geração solar foi reorganizada em um módulo próprio, com consultas mais simples. Os endpoints e o formato das respostas continuam os mesmos.

- Refatorada a funcionalidade de geração solar: controller, entidades, repositórios, serviço e respostas agora ficam juntos no pacote geracao, e as interfaces de serviço separadas foram removidas
- Consultas de geração diária, mensal e anual trocadas de SQL nativo com EXTRACT/DATE por consultas JPA por intervalo de datas; tabelas geracao e geracao_diaria sem mudança e sem migração necessária
- Total anual em GET /v1/geracao/{ano} agora volta em ordem de mês (antes a ordem não era garantida)
- Registro de leitura (POST /v1/geracao) atualiza ou cria o total do dia dentro da mesma transação; consultas marcadas como somente leitura
- GeradoDTO convertido em record, com os mesmos campos gerado e potencia
- Testes de serviço, contrato, segurança e e2e ajustados ao novo pacote (GeracaoServiceTest substitui GeracaoServiceImplTest)

### 2.0.8 — 23/11/2026

A funcionalidade de carros (histórico FIPE) foi reorganizada no novo pacote próprio. Para quem usa a API, nada muda: o endpoint e o formato da resposta continuam iguais.

- Reorganizada a funcionalidade de carros no pacote próprio com.kowalski.casaapi.carro (entidade, repositório, serviço e controller), no lugar das camadas business/api.
- Endpoint GET /v1/carros/{codigoFipe} agora responde com o DTO CarroResponse, montado direto na consulta, sem expor a entidade JPA. Os campos do JSON continuam os mesmos.
- Removidas a interface CarroService e sua implementação separada. O serviço passou a ser uma classe única com transação somente leitura.
- Testes de contrato ampliados para conferir todos os campos da resposta de /v1/carros/{codigoFipe}.
- Novo teste e2e do histórico FIPE em ordem de mês de referência, com dados de exemplo na tabela monitoramento.carro do banco de teste.

### 2.0.7 — 23/11/2026

Refatoração interna que reúne o código de categorias em um pacote próprio, sem mudar o comportamento da API.

- Entidade Categorias e CategoriaRepository movidas para o novo pacote com.kowalski.casaapi.categoria
- Interface CategoriasService e CategoriasServiceImpl substituídas por uma única classe CategoriaService com o método buscar, com transação somente leitura
- Compras e contas passaram a usar o novo CategoriaService; continua a mesma resposta de "não encontrado" para categoria inexistente
- Novos testes unitários em CategoriaServiceTest e testes de compras e contas ajustados ao novo serviço

### 2.0.6 — 23/11/2026

Reorganiza o cadastro de pessoas num módulo próprio, sem mudar o comportamento nem o contrato da API.

- Pessoas reunidas no pacote `pessoa` (entidade, papel, repositório, serviço, controller e input), que antes ficavam espalhados pelas camadas `api`/`business`.
- Endpoints `/v1/pessoas` agora devolvem o novo DTO `PessoaResponse` em vez da entidade, com os mesmos campos (id, nome, email, papel, ativo, temAcesso) e ainda sem a senha.
- Regras da pessoa levadas para a entidade: `definirSenha` (mínimo de 8 caracteres, guarda só o hash), `alterarDados` (nome e e-mail obrigatórios, sem espaços nas pontas) e `tornarAdmin`.
- `PessoaService` passou de interface + implementação a uma classe única, que recebe o id e devolve 404 quando a pessoa não existe; o método `hash` saiu do `AuthService`.
- Admin inicial (`AUTH_ADMIN_EMAIL`) configurado pelas novas regras da entidade, sem mudança de comportamento.
- Testes novos `PessoaTest` e `PessoaServiceTest`, e teste e2e que confirma que trocar o e-mail para o de outra pessoa dá 400 e não altera nada.

### 2.0.5 — 23/11/2026

Refatoração interna: datas calculadas sempre no fuso de São Paulo, erros de validação com a mensagem do campo e classes comuns reunidas em um só pacote, sem mudar nenhuma rota da API.

- Novo relógio único da aplicação (RelogioConfig, fuso America/Sao_Paulo), que substitui o relógio próprio do Crescimento. Contas, compras, faturas, geração de energia e passagens agora usam esse relógio para "hoje" e para o mês atual.
- Corrigido o mês atual e a data de "hoje" (pagamento de fatura e de lançamento, débito automático, data de cadastro), que podiam sair errados perto da meia-noite quando o servidor roda em UTC.
- A data de cadastro da conta passou a ser definida pelo serviço ao criar a conta. O @PrePersist da entidade Conta foi removido.
- Erros de validação de corpo e de parâmetros (@Valid, @Min, @NotBlank…) agora respondem 400 com a mensagem do campo. Foi adicionada a dependência spring-boot-starter-validation.
- ApiExceptionHandler, ErroResponse, Competencias e as exceções RecursoNaoEncontradoException e MonitorIndisponivelException foram movidos para o pacote comum.
- Configurações da chave de integração (ApiKeyProperties, prefixo api) e de CORS (CorsProperties) convertidas em records com injeção por construtor. As propriedades api.key e app.cors.allowed-origins continuam as mesmas.
- @Transactional do Jakarta trocado pelo do Spring em CompraServiceImpl e GeracaoServiceImpl.
- Testes atualizados para usar relógio fixo. Novos testes para as mensagens de validação, para o mês atual no fuso da casa e para a data de cadastro da conta.

### 2.0.4 — 23/11/2026

A API passa a rodar no Java 25 e atende cada requisição em uma thread virtual, o que melhora o uso de recursos enquanto espera banco, disco e host.

- Java atualizado de 17 para 25 (java.version no pom.xml)
- Imagem Docker atualizada para maven:3.9-eclipse-temurin-25 no build e eclipse-temurin:25-jre-noble na execução; o deploy precisa reconstruir a imagem
- Threads virtuais ativadas (spring.threads.virtual.enabled: true no application.yaml), uma por requisição
- Lombok declarado em annotationProcessorPaths do maven-compiler-plugin, já que a partir do JDK 23 o processador de anotações não roda sozinho
- Build local e testes passam a exigir JDK 25

### 2.0.3 — 23/11/2026

Mais testes de contrato para os endpoints de contas, pessoas, servidor, internet, ventilação e passagens; o comportamento da API não muda.

- Testes de contrato para PUT /v1/contas/{id}, com e sem o parâmetro aPartirDe, e para PATCH /v1/contas/lancamentos/{id}
- Teste que garante que GET /v1/pessoas devolve os campos da pessoa sem expor o hash da senha
- Testes para GET /v1/servidor/host e GET /v1/servidor/eventos, incluindo o período padrão de 7 dias e o filtro de/ate
- Testes para os endpoints /v1/servidor/internet (agora, dia, dias e velocidade), incluindo os padrões de data e o erro 400 com data inválida
- Teste do período padrão de 30 dias em GET /v1/servidor/ventilacao/dias
- Testes para GET /v1/passagens (padrão de 30 dias) e para criar, editar e remover rotas em /v1/passagens/rotas, com e sem histórico

### 2.0.2 — 23/11/2026

Esta versão deixa o build da imagem Docker da API mais rápido e mais enxuto, sem mudar o comportamento do sistema.

- Novo arquivo .dockerignore, que deixa de fora do contexto de build: target, .git, .idea, .vscode, logs, .env/.env.* e e2e.
- Cache do Maven entre builds no Dockerfile (RUN --mount=type=cache em /root/.m2), para não baixar as dependências de novo a cada deploy. Para funcionar, o Docker precisa usar o BuildKit.
- Maven rodando em modo batch e sem log de download (-B -ntp) no build da imagem.
- Removido o label fixo version="1.0.0" da imagem.
- Comentário no Dockerfile indicando que a imagem é a mesma dos testes do commit e do e2e (maven:3.9-eclipse-temurin-17).

### 2.0.1 — 23/11/2026

Adiciona um teste automático que confere as migrations de banco antes do commit. Nada muda no comportamento da API.

- Novo teste MigrationsTest: confere se cada script em src/main/resources/db pode ser reaplicado sem erro, ou seja, se usa IF [NOT] EXISTS ou um bloco DO $$
- Verificação de que a tabela de migrations do README lista todos os scripts de db/migration e das outras pastas de db
- Verificação de que o banco dos testes e2e (src/test/resources/e2e/estrutura.sql) tem o resultado de todas as migrations: as tabelas e colunas criadas existem e as apagadas não existem
- Leitura do estrutura.sql nos dois formatos: o do pg_dump e o DDL CREATE TABLE IF NOT EXISTS acrescentado à mão. Inclui um teste próprio para esse recorte
- O teste só lê arquivos do repositório e não acessa nenhum banco. Não há migration nem configuração para aplicar no servidor

### 2.0.0 — 23/11/2026

O Crescimento ganha os exames da criança (PDF ou imagem) e deixa de ter o resumo do mês com IA: nenhum dado das crianças vai mais para serviços de IA.

- Novos endpoints de exames em /v1/crescimento/criancas/{criancaId}/exames: POST (multipart com arquivo, titulo, data e observacao), GET para listar, PUT /{id}, DELETE /{id} e GET /{id}/arquivo (com login, sem cache e com cabeçalhos noindex/nosniff).
- Exames aceitam PDF, JPG ou PNG até 25 MB. O tipo é reconhecido pelo conteúdo do arquivo, as imagens são regravadas em JPEG sem metadados (o GPS é removido) e o mesmo arquivo enviado de novo não é duplicado (jaExistia=true).
- Removidos os endpoints de resumo do mês e de perguntas à IA (/v1/crescimento/criancas/{criancaId}/resumos…), junto com a integração com o container casa-ia e o filtro que bloqueava a rede da IA.
- Removidas as configurações crescimento.ia.* (variáveis CRESCIMENTO_IA_URL, CRESCIMENTO_IA_SEGREDO, CRESCIMENTO_IA_TEMPO e CRESCIMENTO_IA_REDE_BLOQUEADA), que podem sair do ambiente do servidor.
- Migração V6__exames.sql: cria a tabela crescimento.exame. Precisa ser aplicada no servidor, e quem roda é o usuário.
- Migração V7__remove_ia.sql, destrutiva: apaga as tabelas crescimento.resumo_mes e crescimento.pergunta_ia, e com elas os resumos e as respostas já gerados. Também precisa ser aplicada no servidor pelo usuário.
- Excluir uma criança agora apaga também os exames dela e os arquivos. O armazenamento passa a aceitar arquivos .pdf, e a mensagem de limite de tamanho fala em arquivo, não mais só em foto.
- Novos testes dos exames e o teste SemIaTest, que falha se o código ou o pom.xml citarem serviços de IA. Os testes da IA foram removidos e a estrutura e2e foi atualizada.

### 1.2.0 — 22/11/2026

A Saúde do Servidor ganha o acompanhamento da internet, da ventilação e dos acessos pela VPN, e a API passa a ter o módulo Passagens, que cadastra as rotas do robô e mostra o histórico de preços.

- Novos endpoints GET /v1/servidor/internet, /dia, /dias e /velocidade: ping, jitter e perda até o roteador, o provedor, a Cloudflare e o Google; tempo de DNS; IP público; dados do Pi-hole; interface de rede; e testes de velocidade. As tabelas amostra_internet e teste_velocidade são criadas pela própria API. Há eventos para troca de IP, perda de pacotes seguida e velocidade baixa.
- Novos scripts casa-monitor-internet (a cada minuto) e casa-monitor-velocidade (a cada 4 h, cerca de 250 MB por teste), com seus .service e .timer em config/internet. Eles precisam ser instalados e ativados no host e gravam internet.json e velocidade.json na pasta do MONITOR_HOST_ARQUIVO.
- Novos endpoints POST/GET /v1/servidor/ventilacao, /dia e /dias. O POST recebe pelo X-API-KEY a leitura que o Home Assistant manda a cada minuto (tomada das ventoinhas e temperaturas) e grava na tabela amostra_ventilacao.
- Novos eventos de acesso pela VPN (Tailscale) em /v1/servidor/eventos: quando um aparelho começa ou para de acessar o servidor (com o tempo e o volume trocado) e quando um aparelho novo entra na tailnet. O casa-monitor-host agora coleta os aparelhos da tailnet.
- Novo módulo Passagens (só ADMIN): GET /v1/passagens traz as rotas, o histórico de preços por trecho e os alertas; POST /v1/passagens/rotas, PUT e DELETE /v1/passagens/rotas/{id} cadastram, editam, pausam e removem as rotas do robô de passagens.
- Migrações do schema passagens (V1__estrutura.sql e V2__rotas_pelo_casa.sql) precisam ser aplicadas à mão no Postgres. A V1 cria o usuário robo_passagens, cuja senha precisa ser definida no servidor.
- Espaço dos discos agora medido pelas montagens do host, que o casa-monitor-smart passa a gravar. Em MONITOR_DISCOS o caminho passa a ser o ponto de montagem no host. O eMMC do sistema agora aparece com desgaste, total gravado e erros do kernel, e o backup passa a usar o HD 320.
- Novos testes de ponta a ponta (e2e/rodar.sh e CasaApiE2ETest), que rodam contra PostgreSQL e MariaDB descartáveis em Docker, e novos testes unitários de internet, ventilação, VPN e passagens.

### 1.1.0 — 22/11/2026

Novo módulo Crescimento para acompanhar as crianças da família: medidas com curvas da OMS, diário, álbum, fotos, marcos do desenvolvimento, vacinas e agenda.

- Famílias e crianças em /v1/crescimento/familias e /v1/crescimento/criancas/{id}, com membros nos papéis RESPONSAVEL ou LEITOR; o usuário do módulo é a pessoa logada no casa
- Medidas em /medicoes, com avaliação pelas tabelas da OMS em /resumo e curvas em /curvas/{indicador}
- Diário com pesagens, momentos e consultas (/registros e /linha-do-tempo) e álbum com mêsversários, primeiras vezes e momentos especiais (/album)
- Fotos enviadas por multipart (até 25 MB), guardadas em disco nas versões original, média e mini; o arquivo da foto é servido por link assinado com HMAC e prazo de validade, sem precisar de login
- Marcos do desenvolvimento (/marcos), cartão de vacinas com doses extras (/vacinas) e agenda com vacinas atrasadas ou dos próximos 60 dias, retorno marcado e próximo mêsversário (/agenda)
- Novas variáveis de ambiente, todas com valor padrão: CRESCIMENTO_FOTOS_DIRETORIO, CRESCIMENTO_LINK_SEGREDO, CRESCIMENTO_LINK_VALIDADE e CRESCIMENTO_FOTO_MAX_MEGAPIXELS. A pasta das fotos precisa ter o arquivo .armazenamento-crescimento
- Migrações a aplicar à mão no servidor, em ordem: src/main/resources/db/crescimento/V1__estrutura.sql até V3__marcos_e_vacinas.sql (criam o schema crescimento)

### 1.0.1 — 22/11/2026

Inclui as tabelas de referência de crescimento infantil da OMS (2006), que servirão de base para acompanhar peso, comprimento/estatura e perímetro cefálico de crianças de 0 a 5 anos.

- Nova tabela de referência crescimento/oms/peso.tsv com os parâmetros LMS de peso por idade da OMS, por sexo e dia de vida (0 a 1826 dias)
- Nova tabela de referência crescimento/oms/comprimento.tsv com os parâmetros LMS de comprimento/estatura por idade: comprimento deitado até 730 dias e estatura em pé a partir de 731 dias, como orienta a Caderneta da Criança
- Nova tabela de referência crescimento/oms/perimetro.tsv com os parâmetros LMS de perímetro cefálico por idade, por sexo e dia de vida
- Dados obtidos do repositório oficial WorldHealthOrganization/anthro, com a fonte e as colunas descritas no cabeçalho de cada arquivo
