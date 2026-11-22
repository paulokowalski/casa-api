#!/usr/bin/env python3
"""Teste de velocidade da internet para a aba Internet (casa-api).

Roda a cada 4 horas (casa-monitor-velocidade.timer) e grava /var/lib/casa-monitor/velocidade.json com o último
teste e os 60 mais recentes. Usa os servidores do speed.cloudflare.com (os mesmos do site): latência sem carga
(20 requisições vazias), download com 6 conexões e upload com 4, e a latência durante o download (bufferbloat).
Gasta ~250 MB por teste. Rodar na hora: systemctl start casa-monitor-velocidade
"""
import http.client
import json
import os
import ssl
import statistics
import threading
import time
from datetime import datetime, timezone

DIR = os.environ.get("CASA_MONITOR_DIR", "/var/lib/casa-monitor")
SAIDA = os.path.join(DIR, "velocidade.json")
HOST = "speed.cloudflare.com"
DOWN_CONEXOES, DOWN_BYTES = 6, 25_000_000
UP_CONEXOES, UP_BYTES = 4, 25_000_000
TEMPO_MAX = 30
# depois do prazo, quanto esperar as conexões pararem; se alguma não parar, o teste é marcado incompleto
FOLGA_PARAR = 5
# latência sem carga: prazo da fase inteira e de cada requisição (com a rede ruim, o teste ainda termina e grava o erro)
PRAZO_LATENCIA, TIMEOUT_LATENCIA = 15, 5
# latência durante o download: só nos primeiros segundos da medição
PRAZO_LATENCIA_CARGA = 10
GUARDAR = 60
CAB = {"User-Agent": "casa-monitor/1", "Referer": "https://speed.cloudflare.com/"}


def conexao(timeout=TEMPO_MAX):
    return http.client.HTTPSConnection(HOST, timeout=timeout, context=ssl.create_default_context())


def latencias(n=20, prazo_s=PRAZO_LATENCIA):
    """Até n requisições vazias, parando no prazo da fase. Sem nenhuma resposta, levanta o erro."""
    fim = time.perf_counter() + prazo_s
    c = conexao(TIMEOUT_LATENCIA)
    tempos = []
    try:
        for _ in range(n):
            if time.perf_counter() >= fim:
                break
            ini = time.perf_counter()
            c.request("GET", "/__down?bytes=0", headers=CAB)
            r = c.getresponse()
            r.read()
            tempos.append((time.perf_counter() - ini) * 1000)
    except Exception:
        if not tempos:
            raise
    finally:
        c.close()
    if not tempos:
        raise TimeoutError(f"nenhuma resposta de latência em {prazo_s} s")
    return tempos


class Interrompido(Exception):
    """O prazo da medição acabou: a conexão para no meio (não é erro)."""


def baixar(total, erros, parar):
    c = None
    try:
        c = conexao()
        c.request("GET", f"/__down?bytes={DOWN_BYTES}", headers=CAB)
        r = c.getresponse()
        while not parar.is_set():
            b = r.read(256 * 1024)
            if not b:
                break
            total.append(len(b))
    except Exception as e:  # noqa: BLE001
        if not parar.is_set():
            erros.append(str(e))
    finally:
        if c is not None:
            c.close()


def enviar(total, erros, parar):
    bloco = b"0" * (256 * 1024)
    blocos = -(-UP_BYTES // len(bloco))

    def corpo():
        for _ in range(blocos):
            if parar.is_set():
                raise Interrompido()
            yield bloco
            total.append(len(bloco))
    c = None
    try:
        c = conexao()
        c.request("POST", "/__up", body=corpo(), headers={**CAB, "Content-Type": "application/octet-stream",
                                                          "Content-Length": str(blocos * len(bloco))})
        c.getresponse().read()
    except Interrompido:
        pass
    except Exception as e:  # noqa: BLE001
        if not parar.is_set():
            erros.append(str(e))
    finally:
        if c is not None:
            c.close()


def medir(funcao, conexoes, sob_carga=None):
    """Mede por no máximo TEMPO_MAX segundos. No prazo, manda as conexões pararem e conta só o que chegou até ali;
    nenhuma conexão segue baixando/enviando durante a medição seguinte. Devolve também se alguma não parou."""
    total, erros = [], []
    parar = threading.Event()
    threads = [threading.Thread(target=funcao, args=(total, erros, parar), daemon=True) for _ in range(conexoes)]
    ini = time.perf_counter()
    prazo = ini + TEMPO_MAX
    for t in threads:
        t.start()
    if sob_carga is not None:
        time.sleep(0.5)
        try:
            sob_carga.extend(latencias(8, PRAZO_LATENCIA_CARGA))
        except Exception:  # noqa: BLE001
            pass
    for t in threads:
        t.join(max(0.0, prazo - time.perf_counter()))
    parar.set()
    seg = time.perf_counter() - ini
    bytes_no_prazo = sum(total)
    for t in threads:
        t.join(FOLGA_PARAR)
    incompleto = any(t.is_alive() for t in threads)
    return round(bytes_no_prazo * 8 / seg / 1_000_000, 1), bytes_no_prazo, round(seg, 1), erros, incompleto


def main():
    teste = {"testadoEm": datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds"),
             "servidor": HOST, "ok": False, "erro": None}
    try:
        lat = latencias()
        teste["latenciaMs"] = round(statistics.median(lat), 1)
        teste["jitterMs"] = round(statistics.mean(abs(a - b) for a, b in zip(lat, lat[1:])), 1) if len(lat) > 1 else None
        carga = []
        teste["downloadMbps"], bd, sd, ed, incompleto_d = medir(baixar, DOWN_CONEXOES, carga)
        teste["latenciaCargaMs"] = round(statistics.median(carga), 1) if carga else None
        if incompleto_d:
            raise RuntimeError(f"teste incompleto: conexões de download não pararam em {TEMPO_MAX + FOLGA_PARAR} s")
        teste["uploadMbps"], bu, su, eu, incompleto_u = medir(enviar, UP_CONEXOES)
        if incompleto_u:
            raise RuntimeError(f"teste incompleto: conexões de upload não pararam em {TEMPO_MAX + FOLGA_PARAR} s")
        teste["bytes"] = bd + bu
        teste["duracaoS"] = round(sd + su, 1)
        falhas = ed + eu
        teste["ok"] = teste["downloadMbps"] > 0 and teste["uploadMbps"] > 0
        if falhas:
            teste["erro"] = f"{len(falhas)} conexão(ões) falharam: {falhas[0][:150]}"
    except Exception as e:  # noqa: BLE001
        teste["erro"] = str(e)[:200]

    try:
        with open(SAIDA) as f:
            historico = json.load(f).get("historico", [])
    except (OSError, ValueError):
        historico = []
    historico = ([teste] + historico)[:GUARDAR]
    tmp = SAIDA + ".tmp"
    with open(tmp, "w") as f:
        json.dump({"versao": 1, "ultimo": teste, "historico": historico}, f, ensure_ascii=False)
    os.chmod(tmp, 0o644)
    os.replace(tmp, SAIDA)
    print(json.dumps(teste, ensure_ascii=False))


if __name__ == "__main__":
    main()
