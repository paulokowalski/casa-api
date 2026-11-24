#!/usr/bin/env python3
"""Internet da casa para a aba Internet da tela Saúde do servidor (casa-api).

Roda a cada minuto (casa-monitor-internet.timer) e grava /var/lib/casa-monitor/internet.json:
ping (média, jitter e perda) ao roteador, ao primeiro salto do provedor, à Cloudflare e ao Google; tempo de DNS
pelo Pi-hole e direto em servidores públicos; IPv6; IP público e provedor (consultado a cada 15 min);
estatísticas do Pi-hole; e a interface de rede (velocidade do link, erros). O teste de velocidade fica no
casa-monitor-velocidade (velocidade.json), que é pesado e roda poucas vezes por dia.
Só usa a biblioteca padrão do Python, ping, dig e ip.
"""
import ipaddress
import json
import os
import re
import subprocess
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone

DIR = os.environ.get("CASA_MONITOR_DIR", "/var/lib/casa-monitor")
SAIDA = os.path.join(DIR, "internet.json")
CACHE_PUBLICO = os.path.join(DIR, "internet-publico.json")
PUBLICO_A_CADA = 15 * 60
PINGS = 10
DOMINIO_DNS = "google.com"
PIHOLE = "http://127.0.0.1"
PIHOLE_SENHA = "/etc/pihole/cli_pw"


def agora_iso():
    return datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")


def rodar(cmd, tempo=10):
    try:
        return subprocess.run(cmd, capture_output=True, text=True, timeout=tempo).stdout
    except (OSError, subprocess.TimeoutExpired):
        return ""


def ler(caminho):
    try:
        with open(caminho) as f:
            return f.read().strip()
    except OSError:
        return None


def numero(texto):
    try:
        return int(texto)
    except (TypeError, ValueError):
        return None


def ping(alvo, v6=False):
    """10 pings em 2 s: média, mínimo, máximo, jitter (mdev) e perda."""
    if not alvo:
        return None
    saida = rodar(["ping", "-6" if v6 else "-4", "-n", "-q", "-c", str(PINGS), "-i", "0.2", "-W", "2", alvo], 8)
    perda = re.search(r"([\d.]+)% packet loss", saida)
    rtt = re.search(r"= ([\d.]+)/([\d.]+)/([\d.]+)/([\d.]+) ms", saida)
    r = {"perdaPct": float(perda.group(1)) if perda else 100.0,
         "minMs": None, "mediaMs": None, "maxMs": None, "jitterMs": None}
    if rtt:
        r.update(minMs=float(rtt.group(1)), mediaMs=float(rtt.group(2)), maxMs=float(rtt.group(3)),
                 jitterMs=float(rtt.group(4)))
    return r


def salto(ttl):
    saida = rodar(["ping", "-4", "-n", "-c", "1", "-t", str(ttl), "-W", "2", "1.1.1.1"], 5)
    m = re.search(r"From ([\d.]+).*(Time to live exceeded|Time exceeded)", saida, re.I)
    return m.group(1) if m else None


def saltos():
    """Do 2º ao 5º salto até a Cloudflare: o modem (se houver um roteador na frente dele, IP privado) e o
    primeiro roteador do provedor (IP público). Separa lentidão na casa, no modem ou no provedor."""
    with ThreadPoolExecutor(max_workers=4) as ex:
        ips = list(ex.map(salto, range(2, 6)))
    modem = provedor = None
    for ip in ips:
        if not ip:
            continue
        if ipaddress.ip_address(ip).is_private:
            modem = modem or ip
        elif not ipaddress.ip_address(ip).is_private and not ip.startswith("100.") and provedor is None:
            provedor = ip
            break
        elif provedor is None:
            provedor = ip
            break
    return modem, provedor


def dns(servidor):
    ini = time.monotonic()
    saida = rodar(["dig", "+time=2", "+tries=1", "+stats", f"@{servidor}", DOMINIO_DNS, "A"], 6)
    total = round((time.monotonic() - ini) * 1000)
    ok = "status: NOERROR" in saida and "ANSWER SECTION" in saida
    q = re.search(r"Query time: (\d+) msec", saida)
    return {"ok": ok, "tempoMs": int(q.group(1)) if (ok and q) else (None if not ok else total)}


def interface():
    rota = rodar(["ip", "-4", "-j", "route", "show", "default"])
    try:
        r = json.loads(rota)[0]
    except (ValueError, IndexError):
        return None
    nome, gw = r.get("dev"), r.get("gateway")
    ip_local = None
    try:
        addr = json.loads(rodar(["ip", "-4", "-j", "addr", "show", "dev", nome]))
        ip_local = addr[0]["addr_info"][0]["local"]
    except (ValueError, IndexError, KeyError):
        pass
    base = f"/sys/class/net/{nome}"
    est = lambda c: numero(ler(f"{base}/statistics/{c}"))
    v6 = rodar(["ip", "-6", "-j", "addr", "show", "dev", nome, "scope", "global"])
    try:
        tem_v6 = any(a.get("addr_info") for a in json.loads(v6))
    except ValueError:
        tem_v6 = False
    return {"nome": nome, "gateway": gw, "ipLocal": ip_local,
            "velocidadeMbps": numero(ler(f"{base}/speed")), "duplex": ler(f"{base}/duplex"),
            "estado": ler(f"{base}/operstate"), "ipv6Global": tem_v6,
            "recebidos": est("rx_bytes"), "enviados": est("tx_bytes"),
            "erros": (est("rx_errors") or 0) + (est("tx_errors") or 0),
            "descartados": (est("rx_dropped") or 0) + (est("tx_dropped") or 0)}


def http_json(url, tempo=8, metodo="GET", corpo=None, cabecalhos=None):
    req = urllib.request.Request(url, method=metodo, data=corpo, headers={
        "User-Agent": "casa-monitor/1", **(cabecalhos or {})})
    with urllib.request.urlopen(req, timeout=tempo) as r:
        texto = r.read().decode()
        return json.loads(texto) if texto else None


def publico(gateway, tem_v6):
    """IP público e provedor pela Cloudflare; cache de 15 min (refaz se o roteador mudar)."""
    try:
        with open(CACHE_PUBLICO) as f:
            cache = json.load(f)
    except (OSError, ValueError):
        cache = {}
    if cache.get("_gateway") == gateway and time.time() - cache.get("_quando", 0) < PUBLICO_A_CADA:
        return {k: v for k, v in cache.items() if not k.startswith("_")}
    r = {k: v for k, v in cache.items() if not k.startswith("_")}
    try:
        m = http_json("https://speed.cloudflare.com/meta",
                      cabecalhos={"Referer": "https://speed.cloudflare.com/"})
        r = {"ip": m.get("clientIp"), "provedor": m.get("asOrganization"), "asn": m.get("asn"),
             "cidade": m.get("city"), "regiao": m.get("region"), "pais": m.get("country"),
             "servidorCloudflare": (m.get("colo") or {}).get("iata"),
             "verificadoEm": agora_iso(), "erro": None}
    except Exception as e:
        r["erro"] = str(e)[:200]
    r["ipv6"] = None
    if tem_v6:
        trace = rodar(["curl", "-6", "-s", "-m", "4", "https://[2606:4700:4700::1111]/cdn-cgi/trace"], 6)
        r["ipv6"] = next((l[3:] for l in trace.splitlines() if l.startswith("ip=")), None)
    if r.get("ip"):
        try:
            tmp = CACHE_PUBLICO + ".tmp"
            with open(tmp, "w") as f:
                json.dump({**r, "_gateway": gateway, "_quando": time.time() if not r.get("erro") else 0}, f)
            os.replace(tmp, CACHE_PUBLICO)
        except OSError:
            pass
    return r


def pihole():
    senha = ler(PIHOLE_SENHA)
    if not senha:
        return None
    sid = None
    try:
        sid = http_json(f"{PIHOLE}/api/auth", 4, "POST", json.dumps({"password": senha}).encode(),
                        {"Content-Type": "application/json"})["session"]["sid"]
        h = {"sid": sid}
        s = http_json(f"{PIHOLE}/api/stats/summary", 4, cabecalhos=h)
        up = http_json(f"{PIHOLE}/api/stats/upstreams", 4, cabecalhos=h)
        bl = http_json(f"{PIHOLE}/api/dns/blocking", 4, cabecalhos=h)
        q = s.get("queries", {})
        return {"ok": True, "bloqueio": bl.get("blocking"),
                "consultas24h": q.get("total"), "bloqueadas24h": q.get("blocked"),
                "bloqueadasPct": round(q.get("percent_blocked") or 0, 1),
                "doCache24h": q.get("cached"), "encaminhadas24h": q.get("forwarded"),
                "consultasPorSegundo": q.get("frequency"),
                "clientesAtivos": (s.get("clients") or {}).get("active"),
                "dominiosNaLista": (s.get("gravity") or {}).get("domains_being_blocked"),
                "upstreams": [{"nome": u.get("name") or u.get("ip"), "ip": u.get("ip"), "porta": u.get("port"),
                               "consultas": u.get("count"),
                               "tempoMs": round(u["statistics"]["response"] * 1000, 1)
                               if (u.get("statistics") or {}).get("response") is not None else None}
                              for u in (up or {}).get("upstreams", [])
                              if u.get("ip") not in ("blocklist", "cache")]}
    except Exception as e:
        return {"ok": False, "erro": str(e)[:200]}
    finally:
        if sid:
            try:
                http_json(f"{PIHOLE}/api/auth", 3, "DELETE", cabecalhos={"sid": sid})
            except Exception:
                pass


def main():
    iface = interface() or {}
    gw = iface.get("gateway")
    modem, provedor = saltos()
    alvos = [("roteador", "Roteador", gw, False), ("modem", "Modem", modem, False),
             ("provedor", "Provedor", provedor, False),
             ("cloudflare", "Cloudflare", "1.1.1.1", False), ("google", "Google", "8.8.8.8", False)]
    if iface.get("ipv6Global"):
        alvos.append(("cloudflare6", "Cloudflare IPv6", "2606:4700:4700::1111", True))
    servidores_dns = [("pihole", "Pi-hole (local)", "127.0.0.1"), ("cloudflare", "Cloudflare", "1.1.1.1"),
                      ("google", "Google", "8.8.8.8")]
    with ThreadPoolExecutor(max_workers=10) as ex:
        f_ping = [(i, n, a, ex.submit(ping, a, v6)) for i, n, a, v6 in alvos]
        f_dns = [(i, n, s, ex.submit(dns, s)) for i, n, s in servidores_dns]
        f_pihole = ex.submit(pihole)
        f_publico = ex.submit(publico, gw, iface.get("ipv6Global"))
        pings = [{"id": i, "nome": n, "alvo": a, **(f.result() or {})} for i, n, a, f in f_ping if a]
        dnss = [{"id": i, "nome": n, "servidor": s, **f.result()} for i, n, s, f in f_dns]
        ph, pub = f_pihole.result(), f_publico.result()

    externos = [p for p in pings if p["id"] in ("cloudflare", "google")]
    online = any(p.get("mediaMs") is not None for p in externos)
    dados = {"versao": 1, "lidoEm": agora_iso(), "online": online, "interface": iface, "ping": pings,
             "dns": dnss, "publico": pub, "pihole": ph}
    tmp = SAIDA + ".tmp"
    with open(tmp, "w") as f:
        json.dump(dados, f, ensure_ascii=False)
    os.chmod(tmp, 0o644)
    os.replace(tmp, SAIDA)


if __name__ == "__main__":
    main()
