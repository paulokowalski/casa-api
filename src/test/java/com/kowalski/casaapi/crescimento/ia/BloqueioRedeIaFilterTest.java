package com.kowalski.casaapi.crescimento.ia;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueioRedeIaFilterTest {

    static BloqueioRedeIaFilter filtro(String rede) {
        return new BloqueioRedeIaFilter(rede);
    }

    @Test
    void reconheceARede() {
        BloqueioRedeIaFilter f = filtro("172.28.250.0/28");
        assertThat(f.bloqueado("172.28.250.2")).isTrue();
        assertThat(f.bloqueado("172.28.250.15")).isTrue();
        assertThat(f.bloqueado("172.28.250.16")).isFalse();
        assertThat(f.bloqueado("172.25.0.3")).isFalse();
        assertThat(f.bloqueado("::1")).isFalse();
        assertThat(f.bloqueado("casa-ia")).isFalse();
        assertThat(filtro("").bloqueado("172.28.250.2")).isFalse();
    }

    @Test
    void recusaInclusiveOLinkDeFoto() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/v1/crescimento/fotos/arquivo/x");
        req.setRemoteAddr("172.28.250.3");
        req.addHeader("X-Forwarded-For", "192.168.68.10"); // cabeçalho não engana
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain cadeia = new MockFilterChain();
        filtro("172.28.250.0/28").doFilter(req, res, cadeia);
        assertThat(res.getStatus()).isEqualTo(403);
        assertThat(cadeia.getRequest()).isNull();

        MockHttpServletRequest ok = new MockHttpServletRequest("GET", "/v1/crescimento/familias");
        ok.setRemoteAddr("172.25.0.4");
        MockFilterChain cadeia2 = new MockFilterChain();
        filtro("172.28.250.0/28").doFilter(ok, new MockHttpServletResponse(), cadeia2);
        assertThat(cadeia2.getRequest()).isNotNull();
    }
}
