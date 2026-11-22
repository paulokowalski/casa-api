package com.kowalski.casaapi.crescimento.ia;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Barreira para a IA nunca chegar às fotos (nem a nada da API): recusa toda requisição cuja conexão venha da rede do
 * serviço de IA ({@code crescimento.ia.rede-bloqueada}, ex.: 172.28.250.0/28). A casa-api chama a IA, nunca o
 * contrário. Usa o endereço da conexão TCP, não cabeçalhos como X-Forwarded-For, que poderiam ser forjados.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BloqueioRedeIaFilter extends OncePerRequestFilter {

    private final byte[] rede;
    private final int prefixo;

    public BloqueioRedeIaFilter(@Value("${crescimento.ia.rede-bloqueada:}") String cidr) {
        if (cidr == null || cidr.isBlank()) {
            rede = null;
            prefixo = 0;
            return;
        }
        String[] partes = cidr.strip().split("/");
        try {
            rede = InetAddress.getByName(partes[0]).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("crescimento.ia.rede-bloqueada inválida: " + cidr, e);
        }
        prefixo = partes.length > 1 ? Integer.parseInt(partes[1]) : rede.length * 8;
        if (prefixo < 0 || prefixo > rede.length * 8) throw new IllegalArgumentException("crescimento.ia.rede-bloqueada inválida: " + cidr);
    }

    boolean bloqueado(String endereco) {
        if (rede == null || endereco == null) return false;
        byte[] ip;
        try {
            // só literais: nada de consulta DNS
            if (!endereco.matches("[0-9a-fA-F:.]+")) return false;
            ip = InetAddress.getByName(endereco).getAddress();
        } catch (UnknownHostException e) {
            return false;
        }
        if (ip.length != rede.length) return false;
        int bits = prefixo;
        for (int i = 0; i < ip.length && bits > 0; i++, bits -= 8) {
            int mascara = bits >= 8 ? 0xFF : (0xFF << (8 - bits)) & 0xFF;
            if ((ip[i] & mascara) != (rede[i] & mascara)) return false;
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        if (bloqueado(req.getRemoteAddr())) {
            log.warn("Requisição recusada: veio da rede da IA ({} {} de {})", req.getMethod(), req.getRequestURI(), req.getRemoteAddr());
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(req, res);
    }
}
