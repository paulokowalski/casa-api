#!/usr/bin/env bash
# Testes de ponta a ponta da casa-api (CasaApiE2ETest).
#
# Sobe numa rede Docker isolada um PostgreSQL 18 com a estrutura do banco (src/test/resources/e2e/estrutura.sql,
# sem dados, + dados.sql) e um MariaDB vazio para o monitoramento, roda os testes E2E no Maven (a API sobe inteira
# numa porta e é testada por HTTP) e apaga tudo no fim — passe ou falhe. Não toca nos bancos de produção.
#
# Uso, na raiz do projeto:  bash e2e/rodar.sh
# O hook pre-commit (testar-commit) roda este script sozinho antes de cada commit.
set -euo pipefail

RAIZ=$(cd "$(dirname "$0")/.." && pwd)
ID=$(date +%s)-$$
REDE=casa-api-e2e-$ID
PG=casa-api-e2e-pg-$ID
MDB=casa-api-e2e-mdb-$ID
SENHA=e2e-$RANDOM$RANDOM
JAVA=$(grep -oPm1 '<java.version>\K[0-9]+' "$RAIZ/pom.xml" || echo 17)

limpar() {
  docker rm -f "$PG" "$MDB" >/dev/null 2>&1 || true
  docker network rm "$REDE" >/dev/null 2>&1 || true
}
trap limpar EXIT

esperar() { # esperar "descrição" comando... (até 90 s)
  local d=$1; shift
  for _ in $(seq 1 90); do "$@" >/dev/null 2>&1 && return 0; sleep 1; done
  echo "E2E: $d não ficou pronto em 90 s" >&2
  return 1
}

echo "E2E: subindo PostgreSQL e MariaDB de teste..."
docker network create "$REDE" >/dev/null
docker run -d --name "$PG" --network "$REDE" -e POSTGRES_PASSWORD="$SENHA" -e POSTGRES_DB=casa \
  --tmpfs /var/lib/postgresql postgres:18 >/dev/null
docker run -d --name "$MDB" --network "$REDE" -e MARIADB_ROOT_PASSWORD="$SENHA" -e MARIADB_DATABASE=casa_monitor \
  -e MARIADB_USER=casa_monitor -e MARIADB_PASSWORD="$SENHA" --tmpfs /var/lib/mysql mariadb:11.8 >/dev/null

esperar "PostgreSQL" docker exec "$PG" pg_isready -U postgres -d casa -h 127.0.0.1
# o pg_isready responde ainda durante a inicialização; confere com uma consulta de verdade
esperar "PostgreSQL" docker exec "$PG" psql -U postgres -d casa -h 127.0.0.1 -tAc 'select 1'
for f in estrutura.sql dados.sql; do
  docker exec -i "$PG" psql -q -U postgres -d casa -h 127.0.0.1 -v ON_ERROR_STOP=1 \
    < "$RAIZ/src/test/resources/e2e/$f" >/dev/null
done
esperar "MariaDB" docker exec "$MDB" mariadb -ucasa_monitor -p"$SENHA" casa_monitor -e 'select 1'

echo "E2E: rodando os testes (a API sobe inteira, ligada aos bancos de teste)..."
docker run --rm --network "$REDE" -v casa-m2:/root/.m2 -v "$RAIZ:/app" -w /app \
  -e E2E=1 \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://$PG:5432/casa" -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD="$SENHA" \
  -e MONITOR_DATASOURCE_URL="jdbc:mariadb://$MDB:3306/casa_monitor" -e MONITOR_DATASOURCE_USERNAME=casa_monitor \
  -e MONITOR_DATASOURCE_PASSWORD="$SENHA" \
  -e JWT_SECRET="e2e-segredo-do-jwt-com-mais-de-32-caracteres-$SENHA" \
  -e AUTH_ADMIN_EMAIL=admin@e2e.local -e AUTH_ADMIN_SENHA="admin-$SENHA" \
  -e API_KEY="chave-e2e-$SENHA" \
  -e CRESCIMENTO_FOTOS_DIRETORIO=/tmp/fotos-e2e \
  "maven:3.9-eclipse-temurin-$JAVA" sh -c \
  'mkdir -p /tmp/fotos-e2e && touch /tmp/fotos-e2e/.armazenamento-crescimento &&
   mvn -B -ntp test -Dtest=CasaApiE2ETest -Dsurefire.failIfNoSpecifiedTests=false'
echo "E2E: ok"
