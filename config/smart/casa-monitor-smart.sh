#!/bin/bash
set -u

DESTINO=${1:-/var/lib/casa-monitor/smart.json}
mkdir -p "$(dirname "$DESTINO")"
tmp=$(mktemp "${DESTINO}.XXXXXX")
trap 'rm -f "$tmp"' EXIT

anterior='{"discos":[]}'
[ -s "$DESTINO" ] && jq -e . "$DESTINO" >/dev/null 2>&1 && anterior=$(cat "$DESTINO")
agora=$(date --iso-8601=seconds)

discos=()
while read -r nome rota tran; do
    smart=$(timeout 60 smartctl -j -i -H -A -l scttemp -n standby "/dev/$nome" 2>/dev/null)
    [ -n "$smart" ] && echo "$smart" | jq -e . >/dev/null 2>&1 || smart=null
    dispositivos=$(lsblk -nr -o MAJ:MIN "/dev/$nome" | jq -R -s -c 'split("\n") | map(select(length > 0))')
    discos+=("$(jq -n -c --arg nome "$nome" --arg rota "$rota" --arg tran "$tran" --arg agora "$agora" \
        --argjson dispositivos "$dispositivos" --argjson smart "$smart" --argjson anterior "$anterior" '
        ($smart.temperature.current // null) as $temp
        | (first($anterior.discos[]? | select(.dispositivo == $nome)) // null) as $ant
        | ($smart.nvme_smart_health_information_log // null) as $nvme
        | {
            dispositivo: $nome,
            modelo: ($smart.model_name // $smart.model_family // $ant.modelo // null),
            serial: ($smart.serial_number // $ant.serial // null),
            tipo: (if $tran == "nvme" then "nvme" elif $rota == "1" then "hdd" else "ssd" end),
            temperatura: $temp,
            estado: (if $temp != null then "ativo"
                     elif ((($smart.smartctl.messages // []) | map(.string) | join(" ")) | test("STANDBY|SLEEP"; "i"))
                     then "repouso" else "sem_dados" end),
            dispositivos: $dispositivos,
            saude: (if ($smart.smart_status // null) == null then ($ant.saude // null) else {
                lidoEm: $agora,
                aprovado: $smart.smart_status.passed,
                horasLigado: ($smart.power_on_time.hours // null),
                vezesLigado: ($smart.power_cycle_count // null),
                temperaturaMinVida: ($smart.temperature.lifetime_min // null),
                temperaturaMaxVida: ($smart.temperature.lifetime_max // null),
                temperaturaLimite: ($smart.temperature.op_limit_max // null),
                temperaturaLimiteAbsoluto: ($smart.temperature.limit_max // null),
                vezesAcimaLimite: ($smart.ata_sct_status.temperature.over_limit_count // null),
                atributos: ([$smart.ata_smart_attributes.table[]?
                              | select(.id | IN(5, 10, 187, 193, 196, 197, 198, 199))
                              | {key: (.id | tostring), value: .raw.value}] | from_entries),
                falhas: [$smart.ata_smart_attributes.table[]?
                         | select((.when_failed // "") != "")
                         | {atributo: .name, quando: .when_failed}],
                nvme: (if $nvme == null then null else {
                    avisoCritico: $nvme.critical_warning,
                    reservaDisponivel: $nvme.available_spare,
                    reservaMinima: $nvme.available_spare_threshold,
                    desgaste: $nvme.percentage_used,
                    errosMidia: $nvme.media_errors,
                    errosRegistrados: $nvme.num_err_log_entries,
                    desligamentosInesperados: $nvme.unsafe_shutdowns,
                    minutosAcimaAviso: $nvme.warning_temp_time,
                    minutosAcimaCritico: $nvme.critical_comp_time,
                    bytesEscritos: (($nvme.data_units_written // 0) * 512000),
                    bytesLidos: (($nvme.data_units_read // 0) * 512000)
                } end)
            } end)
          }')")
done < <(lsblk -dn -o NAME,ROTA,TRAN | awk '$3 == "usb" || $3 == "sata" || $3 == "sas" || $3 == "nvme"')

for emmc in /sys/block/mmcblk*/device; do
    [ -r "$emmc/life_time" ] || continue
    nome=$(basename "$(dirname "$emmc")")
    read -r la lb < "$emmc/life_time"
    eol=$(cat "$emmc/pre_eol_info" 2>/dev/null || echo 0x01)
    m=$(( la > lb ? la : lb )); e=$(( eol ))
    desgaste=$(( m * 10 )); [ $desgaste -gt 100 ] && desgaste=100
    aprovado=$([ $m -ge 11 ] || [ $e -ge 3 ] && echo false || echo true)
    escrito=0
    for part in /sys/fs/ext4/${nome}p*; do
        [ -r "$part/lifetime_write_kbytes" ] && escrito=$(( escrito + $(cat "$part/lifetime_write_kbytes") * 1024 ))
    done
    erros=$(journalctl -k -b -q --no-pager -o cat 2>/dev/null | grep -av 'Opts:' \
        | grep -aciE "(mmc[0-9]+|$nome)[^ ]*:? .*(error|timeout|crc|failed|recovery)" || true)
    dispositivos=$(lsblk -nr -o MAJ:MIN "/dev/$nome" | jq -R -s -c 'split("\n") | map(select(length > 0))')
    discos+=("$(jq -n -c --arg nome "$nome" --arg agora "$agora" --argjson dispositivos "$dispositivos" \
        --arg modelo "eMMC $(cat "$emmc/name" 2>/dev/null)" --arg serial "$(cat "$emmc/serial" 2>/dev/null)" \
        --argjson desgaste "$desgaste" --argjson aviso "$([ $e -ge 3 ] && echo $e || echo 0)" \
        --argjson aprovado "$aprovado" --argjson escrito "$escrito" --argjson erros "${erros:-0}" \
        '{dispositivo: $nome, modelo: $modelo, serial: $serial, tipo: "emmc", temperatura: null, estado: "ativo",
          dispositivos: $dispositivos,
          saude: {lidoEm: $agora, aprovado: $aprovado, atributos: {}, falhas: [],
                  nvme: {avisoCritico: $aviso, desgaste: $desgaste, errosMidia: $erros,
                         bytesEscritos: (if $escrito > 0 then $escrito else null end)}}}')")
done

montagens=$(timeout 10 findmnt -J -b -o TARGET,MAJ:MIN,SIZE,USED,AVAIL \
        -t ext4,ext3,ext2,xfs,btrfs,f2fs,vfat,exfat,ntfs3,ntfs 2>/dev/null \
    | jq -c '[.. | objects | select(has("target"))
              | {ponto: .target, dispositivo: .["maj:min"], total: .size, usado: .used, disponivel: .avail}]' \
        2>/dev/null)
[ -n "$montagens" ] || montagens='[]'

printf '%s\n' "${discos[@]}" | jq -s --arg lidoEm "$agora" --argjson montagens "$montagens" \
    '{lidoEm: $lidoEm, discos: ., montagens: $montagens}' > "$tmp"
chmod 644 "$tmp"
mv -f "$tmp" "$DESTINO"
trap - EXIT
