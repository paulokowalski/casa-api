#!/bin/bash
# Lê a temperatura dos discos por SMART e grava um JSON para o monitoramento do casa-api.
#
# Roda no host (precisa de root para o smartctl), pelo timer casa-monitor-smart.timer, a cada minuto.
# A API não tem acesso aos discos: só lê este arquivo, montado só-leitura no container.
#
# Para cada disco (NVMe, SATA, SAS e USB) grava modelo, tipo, temperatura, os dados de saúde do SMART e os
# major:minor do disco e das partições, que a API usa para saber qual disco está por trás de cada caminho em
# MONITOR_DISCOS. A avaliação (boa / atenção / crítica) é feita pela API; aqui só vão os números.
# Com "-n standby", um disco em repouso não é acordado: ele sai com estado "repouso", sem temperatura, e
# mantém a última saúde lida (que muda devagar), com a data dessa leitura.
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
                # limites do fabricante (tabela SCT): máximo recomendado para operar e máximo absoluto
                temperaturaLimite: ($smart.temperature.op_limit_max // null),
                temperaturaLimiteAbsoluto: ($smart.temperature.limit_max // null),
                vezesAcimaLimite: ($smart.ata_sct_status.temperature.over_limit_count // null),
                # valores brutos dos atributos ATA que indicam desgaste ou defeito (pelo número do atributo)
                atributos: ([$smart.ata_smart_attributes.table[]?
                              | select(.id | IN(5, 10, 187, 193, 196, 197, 198, 199))
                              | {key: (.id | tostring), value: .raw.value}] | from_entries),
                # atributos que já ficaram abaixo do limite do fabricante: "now" (agora) ou "past" (no passado)
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

printf '%s\n' "${discos[@]}" | jq -s --arg lidoEm "$agora" '{lidoEm: $lidoEm, discos: .}' > "$tmp"
chmod 644 "$tmp"
mv -f "$tmp" "$DESTINO"
trap - EXIT
