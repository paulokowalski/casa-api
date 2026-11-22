#!/bin/bash
# Status do host para a tela "Saúde do servidor" (casa / casa-api).
# Grava /var/lib/casa-monitor/host.json (montado só-leitura na API em /host/smart/host.json).
# Rodado a cada minuto por casa-monitor-host.timer. Só lê; nunca altera nada.
# Partes mais caras (apt, Diun) ficam em cache por 1 h em /var/lib/casa-monitor-host.
set -u
export LC_ALL=C
SAIDA=/var/lib/casa-monitor/host.json
CACHE=/var/lib/casa-monitor-host
mkdir -p "$(dirname "$SAIDA")" "$CACHE"
agora=$(date +%s)

# Converte "texto livre" em string JSON
js() { jq -Rn --arg v "$1" '$v'; }
# Data do systemd ("Sun 2026-11-15 03:35:22 -03") -> ISO 8601, ou null
iso() { local d; [ -n "${1:-}" ] && [ "$1" != "n/a" ] && d=$(date -d "$1" -Iseconds 2>/dev/null) && js "$d" || echo null; }
epoch_iso() { local d; [ -n "${1:-}" ] && d=$(date -d "@$1" -Iseconds 2>/dev/null) && js "$d" || echo null; }
idade() { [ -n "${1:-}" ] && echo $((agora - $1)) || echo null; }
# systemctl show em chave=valor -> variaveis com prefixo (a ordem de --value nao e garantida)
mostrar() {  # prefixo unidade campos...
  local pre=$1 u=$2; shift 2
  local IFS=,; local props="$*"; unset IFS
  while IFS='=' read -r k v; do printf -v "${pre}_$k" '%s' "$v"; done < <(systemctl show "$u" -p "$props")
}
# Executa e cacheia por N segundos
cacheado() {
  local arq=$CACHE/$1 ttl=$2; shift 2
  if [ ! -s "$arq" ] || [ $((agora - $(stat -c %Y "$arq"))) -gt "$ttl" ]; then
    "$@" > "$arq.tmp" 2>/dev/null && mv -f "$arq.tmp" "$arq"
  fi
  cat "$arq" 2>/dev/null
}

# ---------- Backup (sem chamar restic: só a data dos arquivos de snapshot) ----------
repo() {  # nome caminho-do-repo ponto-de-montagem
  local nome=$1 dir=$2 mnt=$3 montado=false ult qtd
  findmnt -n "$mnt" >/dev/null && montado=true
  ult=$(find "$dir/snapshots" -type f -printf '%T@\n' 2>/dev/null | sort -n | tail -1); ult=${ult%.*}
  qtd=$(find "$dir/snapshots" -type f 2>/dev/null | wc -l)
  jq -n --arg nome "$nome" --argjson montado $montado --argjson ult "$(epoch_iso "$ult")" \
        --argjson qtd "$qtd" --argjson idade "$(idade "$ult")" \
        '{nome:$nome, montado:$montado, ultimoSnapshot:$ult, idadeSegundos:$idade, snapshots:$qtd}'
}
od=$(stat -c %Y /var/lib/backup-servidor/onedrive-ok 2>/dev/null)
mostrar B backup-servidor.service Result ExecMainStatus ExecMainStartTimestamp ExecMainExitTimestamp ActiveState
b_res=$B_Result b_status=$B_ExecMainStatus b_ini=$B_ExecMainStartTimestamp b_fim=$B_ExecMainExitTimestamp b_ativo=$B_ActiveState
backup=$(jq -n \
  --argjson repos "$(jq -s . <(repo "HD Mídia" /mnt/media/_backup-servidor/restic /mnt/media) \
                            <(repo "HD 320" /mnt/hd320/_backup-servidor/restic /mnt/hd320))" \
  --argjson od "$(epoch_iso "$od")" --argjson odIdade "$(idade "$od")" \
  --arg res "$b_res" --argjson falhas "${b_status:-null}" --arg ativo "$b_ativo" \
  --argjson ini "$(iso "$b_ini")" --argjson fim "$(iso "$b_fim")" \
  --argjson prox "$(iso "$(systemctl show backup-servidor.timer -p NextElapseUSecRealtime --value)")" \
  '{repositorios: ($repos + [{nome:"OneDrive", montado:null, ultimoSnapshot:$od, idadeSegundos:$odIdade, snapshots:null}]),
    ultimaExecucao: {inicio:$ini, fim:$fim, rodandoAgora:($ativo=="activating"), sucesso:($res=="success"), falhas:$falhas},
    proximaExecucao:$prox}')

# ---------- Timers e serviços ----------
TIMERS=$(systemctl list-timers --all --output=json 2>/dev/null || echo '[]')
unidade() {  # unidade [timer]
  local u=$1 t=${2:-}
  local U_ActiveState= U_SubState= U_Result= U_ExecMainExitTimestamp= U_NRestarts= U_LoadState= ativo_t=
  mostrar U "$u" ActiveState SubState Result ExecMainExitTimestamp NRestarts LoadState
  local ativo=$U_ActiveState sub=$U_SubState res=$U_Result fim=$U_ExecMainExitTimestamp nr=${U_NRestarts:-0}
  local ult=null prox=null
  if [ -n "$t" ]; then
    # list-timers resolve tanto timers de calendário quanto de intervalo (us desde a época)
    ult=$(echo "$TIMERS" | jq --arg t "$t" '[.[]|select(.unit==$t and (.last//0)>0)|.last/1000000|floor|todateiso8601][0]')
    prox=$(echo "$TIMERS" | jq --arg t "$t" '[.[]|select(.unit==$t and (.next//0)>0)|.next/1000000|floor|todateiso8601][0]')
    ativo_t=$(systemctl is-active "$t")
  fi
  jq -n --arg u "$u" --arg a "$ativo" --arg s "$sub" --arg r "$res" --argjson fim "$(iso "$fim")" \
        --argjson ult "$ult" --argjson prox "$prox" --arg at "${ativo_t:-}" --argjson nr "${nr:-0}" --arg load "$U_LoadState" \
        '{unidade:$u, instalado:($load=="loaded"), estado:$a, subestado:$s, resultado:$r,
          ok:($load=="loaded" and $r=="success" and $a!="failed"),
          timerAtivo:(if $at=="" then null else $at=="active" end),
          ultimaExecucao:(if $ult==null then $fim else $ult end), proximaExecucao:$prox, reinicios:$nr}'
}
servicos=$(jq -s . \
  <(unidade backup-servidor.service backup-servidor.timer) \
  <(unidade monitorar-servidor.service monitorar-servidor.timer) \
  <(unidade remontar-discos.service remontar-discos.timer) \
  <(unidade avisar-atualizacoes.service avisar-atualizacoes.timer) \
  <(unidade casa-monitor-smart.service casa-monitor-smart.timer) \
  <(unidade casa-monitor-internet.service casa-monitor-internet.timer) \
  <(unidade casa-monitor-velocidade.service casa-monitor-velocidade.timer) \
  <(unidade docker.service) <(unidade smartmontools.service) <(unidade pihole-FTL.service) \
  <(unidade smbd.service) <(unidade tailscaled.service) <(unidade ssh.service))
falhos=$(systemctl list-units --state=failed --no-legend --plain 2>/dev/null | awk '{print $1}' | jq -R . | jq -s .)

# ---------- Montagens (discos ext4 /mnt/* do fstab) ----------
montagens=$(awk '$1!~/^#/ && $2~/^\/mnt\// && $3=="ext4"{print $2}' /etc/fstab | while read -r m; do
  if findmnt -n "$m" >/dev/null; then
    src=$(findmnt -n -o SOURCE "$m"); opt=$(findmnt -n -o OPTIONS "$m")
    ro=false; [[ ",$opt," == *,ro,* ]] && ro=true
    jq -n --arg m "$m" --arg s "$src" --argjson ro $ro '{ponto:$m, montado:true, dispositivo:$s, somenteLeitura:$ro}'
  else
    jq -n --arg m "$m" '{ponto:$m, montado:false, dispositivo:null, somenteLeitura:null}'
  fi; done | jq -s .)
ultRemontagem=$(find /var/lib/remontar-discos -type f -printf '%T@\n' 2>/dev/null | sort -n | tail -1)

# ---------- Alertas ativos do monitorar-servidor ----------
lista() { sed '/^$/d' "$1" 2>/dev/null | jq -R . | jq -s .; }
alertas=$(jq -n --argjson a "$(lista /var/lib/monitorar-servidor/alertado)" \
                --argjson p "$(lista /var/lib/monitorar-servidor/pendente)" \
                --argjson em "$(epoch_iso "$(stat -c %Y /var/lib/monitorar-servidor/pendente 2>/dev/null)")" \
                '{alertados:$a, pendentes:($p - $a), verificadoEm:$em}')

# ---------- Kernel: erros de disco/USB/memória nas últimas 24 h ----------
kern=$(journalctl -k -q --no-pager -o short-iso --since -24h 2>/dev/null | \
  grep -iE "I/O error|USB disconnect|uas_eh|reset (high|super)[- ]?speed|EXT4-fs error|blk_update_request|Out of memory|oom-kill")
kernel=$(jq -n --argjson n "$(printf '%s' "$kern" | grep -c . )" \
  --argjson ult "$(printf '%s\n' "$kern" | tail -5 | cut -c1-200 | sed '/^$/d' | jq -R . | jq -s .)" \
  '{eventos24h:$n, ultimos:$ult}')

# ---------- Atualizações (cache 1 h) ----------
apt_pend() { apt list --upgradable 2>/dev/null | tail -n +2 | awk -F'[/ ]' '{print $1"|"$3"|"($2~/security/)}'; }
pend=$(cacheado apt 3600 apt_pend)
diun_novas() { docker logs --since 168h diun 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -i "New image found" | grep -o 'image=[^ ]*' | cut -d= -f2 | sort -u; }
diun=$(cacheado diun 3600 diun_novas)
reboot=false; [ -f /var/run/reboot-required ] && reboot=true
atualizacoes=$(jq -n --argjson reboot $reboot \
  --argjson pk "$(sort -u /var/run/reboot-required.pkgs 2>/dev/null | jq -R . | jq -s .)" \
  --argjson apt "$(printf '%s\n' "$pend" | sed '/^$/d' | jq -R 'split("|") | {pacote:.[0], versao:.[1], seguranca:(.[2]=="1")}' | jq -s .)" \
  --argjson img "$(printf '%s\n' "$diun" | sed '/^$/d' | jq -R . | jq -s .)" \
  '{reinicioNecessario:$reboot, reinicioPor:$pk, pacotesPendentes:($apt|length),
    pacotesSeguranca:([$apt[]|select(.seguranca)]|length), pacotes:$apt, imagensNovas7d:$img}')

# ---------- Conectividade ----------
ms() { ping -c 3 -i 0.3 -W 2 -q "$1" 2>/dev/null | awk -F'/' '/^rtt|^round-trip/{print $5}'; }
lat=$(ms 1.1.1.1); gw=$(ip route show default | awk '{print $3; exit}'); latgw=$(ms "$gw")
dns_ini=$(date +%s%N); dns_ok=false
[ -n "$(dig +short +time=2 +tries=1 @127.0.0.1 google.com 2>/dev/null)" ] && dns_ok=true
dns_ms=$(( ($(date +%s%N) - dns_ini) / 1000000 ))
ts=$(tailscale status --json 2>/dev/null)
rede=$(jq -n --argjson lat "${lat:-null}" --argjson latgw "${latgw:-null}" --arg gw "$gw" \
  --argjson dok $dns_ok --argjson dms $dns_ms \
  --argjson ts "$( [ -n "$ts" ] && echo "$ts" | jq '
       # datas "zero" do Go (0001-01-01) viram null
       def data: if . == null or startswith("0001-") then null else . end;
       . as $r
       | {estado:.BackendState, online:.Self.Online, ip:.Self.TailscaleIPs[0],
          usuario:(.User[(.Self.UserID|tostring)].LoginName // null),
          pares:([.Peer[]?]|length), paresOnline:([.Peer[]?|select(.Online)]|length),
          # cada aparelho da tailnet: bytes e último handshake WireGuard COM ESTE servidor (só mudam quando o
          # aparelho troca tráfego com ele; "online" é só estar conectado à tailnet)
          aparelhos:[.Peer[]? | {id:.ID, nome:(((.DNSName // "") | split(".")[0] | if . == "" then null else . end) // .HostName),
                                so:.OS, ip:.TailscaleIPs[0], dono:($r.User[(.UserID|tostring)].LoginName // null),
                                online:.Online, ultimoVisto:(.LastSeen|data), ultimoHandshake:(.LastHandshake|data),
                                recebidos:.RxBytes, enviados:.TxBytes, criado:(.Created|data)}]}' || echo null)" \
  '{internet:{online:($lat!=null), latenciaMs:$lat}, gateway:{ip:$gw, latenciaMs:$latgw},
    dns:{ok:$dok, tempoMs:$dms}, tailscale:$ts}')

# ---------- Logins SSH (7 dias) ----------
ssh=$(journalctl -u ssh --since -7d --no-pager -q -o short-iso 2>/dev/null | grep "Accepted " | tail -10 | \
  awk '{for(i=1;i<=NF;i++){if($i=="for")u=$(i+1); if($i=="from")ip=$(i+1); if($i=="Accepted")m=$(i+1)}; print $1"|"u"|"ip"|"m}' | \
  jq -R 'split("|") | {quando:.[0], usuario:.[1], origem:.[2], metodo:.[3]}' | jq -s 'reverse')


# ---------- NPU (debugfs; a API não enxerga) ----------
npu=$(sed -n 's/.*NPU load://p' /sys/kernel/debug/rknpu/load 2>/dev/null | grep -o '[0-9]\+%' | tr -d '%' | jq -R 'tonumber' | jq -s .)
[ -z "$npu" ] && npu='[]'

# ---------- Docker: reinícios por container e espaço (df em cache 1 h) ----------
reinicios=$(docker ps -aq 2>/dev/null | xargs -r docker inspect -f '{{.Name}}|{{.RestartCount}}|{{.State.StartedAt}}|{{.State.OOMKilled}}' 2>/dev/null | \
  sed 's|^/||' | jq -R 'split("|") | {nome:.[0], reinicios:(.[1]|tonumber), iniciadoEm:.[2], oomKilled:(.[3]=="true")}' | jq -s 'sort_by(.nome)')
docker_df() { docker system df --format '{{json .}}'; }
tam() { numfmt --from=auto --suffix=B "${1%B}" 2>/dev/null | tr -d B || echo 0; }
ddf=$(cacheado docker-df 3600 docker_df | jq -s '[.[] | {tipo:.Type, total:(.TotalCount|tonumber), ativos:(.Active|tonumber), tamanho:.Size, recuperavel:(.Reclaimable|split(" ")[0])}]' 2>/dev/null)
ddf=$(echo "${ddf:-[]}" | jq -c '.[]' | while read -r l; do
  t=$(echo "$l" | jq -r .tamanho); r=$(echo "$l" | jq -r .recuperavel)
  echo "$l" | jq --argjson t "$(tam "$t")" --argjson r "$(tam "$r")" '.tamanho=$t | .recuperavel=$r'
done | jq -s .)

# ---------- Inodes dos discos ----------
inodes=$(df -i -x tmpfs -x devtmpfs -x overlay -x squashfs 2>/dev/null | awk 'NR>1 && ($6=="/" || $6 ~ /^\/mnt\//) {gsub("%","",$5); print $6"|"$2"|"$3"|"$5}' | \
  jq -R 'split("|") | {ponto:.[0], total:(.[1]|tonumber), usados:(.[2]|tonumber), pct:((.[3]|tonumber?) // 0)}' | jq -s .)

# ---------- Monta e grava (atômico) ----------
tmp=$(mktemp "$SAIDA.XXXXXX")
if jq -n --argjson lido "$(epoch_iso "$agora")" --argjson backup "$backup" --argjson servicos "$servicos" \
      --argjson falhos "$falhos" --argjson montagens "$montagens" \
      --argjson ultRem "$(epoch_iso "${ultRemontagem%.*}")" --argjson alertas "$alertas" \
      --argjson kernel "$kernel" --argjson atualizacoes "$atualizacoes" --argjson rede "$rede" --argjson ssh "$ssh" \
      --argjson npu "$npu" --argjson reinicios "${reinicios:-[]}" --argjson ddf "$ddf" --argjson inodes "$inodes" \
      '{versao:1, lidoEm:$lido, backup:$backup, servicos:$servicos, unidadesFalhas:$falhos,
        montagens:{discos:$montagens, ultimaRemontagemAutomatica:$ultRem}, alertas:$alertas,
        kernel:$kernel, atualizacoes:$atualizacoes, rede:$rede, loginsSsh:$ssh,
        npu:{nucleosPct:$npu}, docker:{containers:$reinicios, espaco:$ddf}, inodes:$inodes}' > "$tmp"; then
  chmod 644 "$tmp"; mv -f "$tmp" "$SAIDA"
else
  rm -f "$tmp"; logger -t casa-monitor-host "falha ao montar o JSON"; exit 1
fi
