#!/bin/bash
# Lê a temperatura dos discos por SMART e grava um JSON para o monitoramento do casa-api.
#
# Roda no host (precisa de root para o smartctl), pelo timer casa-monitor-smart.timer, a cada minuto.
# A API não tem acesso aos discos: só lê este arquivo, montado só-leitura no container.
#
# Para cada disco (NVMe, SATA, SAS e USB) grava modelo, tipo, temperatura e os major:minor do disco e das
# partições, que a API usa para saber qual disco está por trás de cada caminho em MONITOR_DISCOS.
# Com "-n standby", um disco em repouso não é acordado: ele sai com estado "repouso" e sem temperatura.
set -u

DESTINO=${1:-/var/lib/casa-monitor/smart.json}
mkdir -p "$(dirname "$DESTINO")"
tmp=$(mktemp "${DESTINO}.XXXXXX")
trap 'rm -f "$tmp"' EXIT

discos=()
while read -r nome rota tran; do
    smart=$(timeout 30 smartctl -j -i -A -n standby "/dev/$nome" 2>/dev/null)
    [ -n "$smart" ] && echo "$smart" | jq -e . >/dev/null 2>&1 || smart=null
    dispositivos=$(lsblk -nr -o MAJ:MIN "/dev/$nome" | jq -R -s -c 'split("\n") | map(select(length > 0))')
    discos+=("$(jq -n -c --arg nome "$nome" --arg rota "$rota" --arg tran "$tran" \
        --argjson dispositivos "$dispositivos" --argjson smart "$smart" '
        ($smart.temperature.current // null) as $temp
        | {
            dispositivo: $nome,
            modelo: ($smart.model_name // $smart.model_family // null),
            serial: ($smart.serial_number // null),
            tipo: (if $tran == "nvme" then "nvme" elif $rota == "1" then "hdd" else "ssd" end),
            temperatura: $temp,
            estado: (if $temp != null then "ativo"
                     elif ((($smart.smartctl.messages // []) | map(.string) | join(" ")) | test("STANDBY|SLEEP"; "i"))
                     then "repouso" else "sem_dados" end),
            dispositivos: $dispositivos
          }')")
done < <(lsblk -dn -o NAME,ROTA,TRAN | awk '$3 == "usb" || $3 == "sata" || $3 == "sas" || $3 == "nvme"')

printf '%s\n' "${discos[@]}" | jq -s --arg lidoEm "$(date --iso-8601=seconds)" '{lidoEm: $lidoEm, discos: .}' > "$tmp"
chmod 644 "$tmp"
mv -f "$tmp" "$DESTINO"
trap - EXIT
