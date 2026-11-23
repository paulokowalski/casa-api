package com.kowalski.casaapi.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de ponta a ponta: a API inteira sobe numa porta de verdade, ligada a um PostgreSQL e a um MariaDB vazios
 * (containers descartáveis criados pelo {@code e2e/rodar.sh}), e os testes falam com ela só por HTTP, como o front.
 * <p>
 * Só rodam com {@code E2E=1} (o script define isso e os bancos). Fora dele ficam desligados, porque precisam dos
 * bancos. Rodar à mão: {@code bash e2e/rodar.sh} na raiz do projeto.
 */
@EnabledIfEnvironmentVariable(named = "E2E", matches = "1")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CasaApiE2ETest {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA = new ParameterizedTypeReference<>() {
    };

    @LocalServerPort
    int porta;

    private RestClient http;

    @BeforeEach
    void preparar() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + porta + "/casa-api")
                // os testes conferem o status: 4xx/5xx não viram exceção
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
                })
                .build();
    }

    // ---- login e permissões ----

    @Test
    void adminEntraERecebeToken() {
        var r = http.post().uri("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email(), "senha", senha())).retrieve().toEntity(MAPA);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat((String) r.getBody().get("token")).isNotBlank();
        assertThat(mapa(r.getBody(), "usuario")).containsEntry("papel", "ADMIN");
    }

    @Test
    void senhaErradaNaoEntra() {
        var r = http.post().uri("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email(), "senha", "errada")).retrieve().toBodilessEntity();
        assertThat(r.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void semTokenNaoAcessaNada() {
        assertThat(http.get().uri("/v1/pessoas").retrieve().toBodilessEntity().getStatusCode().value()).isEqualTo(401);
        assertThat(http.get().uri("/v1/servidor/ventilacao").retrieve().toBodilessEntity().getStatusCode().value())
                .isEqualTo(401);
    }

    // ---- crescimento: família, criança, medição e resumo (gravando de verdade no PostgreSQL) ----

    @Test
    void crescimentoDaFamiliaAteOResumoDaPesagem() {
        String token = token();
        var familia = http.post().uri("/v1/crescimento/familias").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("nome", "Família E2E"))
                .retrieve().toEntity(MAPA);
        assertThat(familia.getStatusCode().is2xxSuccessful()).isTrue();
        String familiaId = (String) familia.getBody().get("id");

        LocalDate nascimento = LocalDate.now().minusDays(60);
        var crianca = http.post().uri("/v1/crescimento/familias/{f}/criancas", familiaId).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nome", "Bebê E2E", "nascimento", nascimento.toString(), "sexo", "F"))
                .retrieve().toEntity(MAPA);
        assertThat(crianca.getStatusCode().is2xxSuccessful()).isTrue();
        String criancaId = (String) crianca.getBody().get("id");

        var medicao = http.post().uri("/v1/crescimento/criancas/{c}/medicoes", criancaId).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("data", LocalDate.now().toString(), "pesoKg", 5.1, "comprimentoCm", 57.0))
                .retrieve().toEntity(MAPA);
        assertThat(medicao.getStatusCode().is2xxSuccessful()).isTrue();

        var medicoes = http.get().uri("/v1/crescimento/criancas/{c}/medicoes", criancaId).header("Authorization", token)
                .retrieve().toEntity(LISTA);
        assertThat(medicoes.getBody()).hasSize(1);

        var resumo = http.get().uri("/v1/crescimento/criancas/{c}/resumo", criancaId).header("Authorization", token)
                .retrieve().toEntity(MAPA);
        assertThat(resumo.getStatusCode().value()).isEqualTo(200);
        assertThat(resumo.getBody()).containsEntry("idadeDias", 60);
        Map<String, Object> peso = mapa(mapa(resumo.getBody(), "ultimas"), "PESO");
        assertThat(((Number) peso.get("valor")).doubleValue()).isEqualTo(5.1);
        assertThat(peso.get("percentil")).isNotNull();

        var familias = http.get().uri("/v1/crescimento/familias").header("Authorization", token).retrieve().toEntity(LISTA);
        assertThat(familias.getBody()).extracting(f -> f.get("id")).contains(familiaId);
    }

    @Test
    void criancaDeOutraFamiliaNaoAparece() {
        var r = http.get().uri("/v1/crescimento/criancas/{c}", "00000000-0000-0000-0000-000000000000")
                .header("Authorization", token()).retrieve().toBodilessEntity();
        assertThat(r.getStatusCode().value()).isIn(403, 404);
    }

    // ---- ventilação: o Home Assistant grava com a chave de integração e o admin lê (MariaDB de verdade) ----

    @Test
    void ventilacaoGravadaPelaIntegracaoApareceParaOAdmin() {
        var leitura = Map.of("tomada", "on", "precisa", true, "automatica", true, "soc", 48.5, "hds", 41.0, "nvme", 52.0,
                "limiteSoc", Map.of("liga", 55, "desliga", 48), "limiteHds", Map.of("liga", 55, "desliga", 45),
                "limiteNvme", Map.of("liga", 60, "desliga", 55));
        var gravou = http.post().uri("/v1/servidor/ventilacao").header("X-API-KEY", chave())
                .contentType(MediaType.APPLICATION_JSON).body(leitura).retrieve().toBodilessEntity();
        assertThat(gravou.getStatusCode().value()).isEqualTo(204);

        String token = token();
        var agora = http.get().uri("/v1/servidor/ventilacao").header("Authorization", token).retrieve().toEntity(MAPA);
        assertThat(agora.getStatusCode().value()).isEqualTo(200);
        assertThat(agora.getBody()).containsEntry("desatualizado", false);
        Map<String, Object> l = mapa(agora.getBody(), "leitura");
        assertThat(l).containsEntry("tomada", "on");
        assertThat(mapa(l, "limiteHds")).containsEntry("liga", 55.0).containsEntry("desliga", 45.0);
        assertThat(((Number) agora.getBody().get("minutosLigadaHoje")).longValue()).isGreaterThanOrEqualTo(1);

        var dia = http.get().uri("/v1/servidor/ventilacao/dia").header("Authorization", token).retrieve().toEntity(MAPA);
        assertThat(dia.getStatusCode().value()).isEqualTo(200);
        assertThat((List<?>) dia.getBody().get("pontos")).isNotEmpty();
    }

    @Test
    void chaveDeIntegracaoNaoLeAVentilacaoEEstadoInvalidoERecusado() {
        assertThat(http.get().uri("/v1/servidor/ventilacao").header("X-API-KEY", chave()).retrieve()
                .toBodilessEntity().getStatusCode().value()).isEqualTo(403);
        var invalida = http.post().uri("/v1/servidor/ventilacao").header("X-API-KEY", chave())
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("tomada", "talvez")).retrieve().toBodilessEntity();
        assertThat(invalida.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void trocarOEmailParaODeOutraPessoaDa400ENaoMudaNada() {
        String token = token();
        String sufixo = String.valueOf(System.nanoTime());
        String emailDaAna = "ana-" + sufixo + "@e2e.local";
        Number ana = (Number) criarPessoa(token, "Ana E2E", emailDaAna).get("id");
        Number bruno = (Number) criarPessoa(token, "Bruno E2E", "bruno-" + sufixo + "@e2e.local").get("id");

        var r = http.put().uri("/v1/pessoas/" + bruno).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("nome", "Bruno E2E", "email", " " + emailDaAna.toUpperCase() + " "))
                .retrieve().toEntity(MAPA);

        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(r.getBody()).containsEntry("mensagem", "Já existe uma pessoa com este e-mail.");
        var pessoas = http.get().uri("/v1/pessoas").header("Authorization", token).retrieve().body(LISTA);
        assertThat(pessoas).filteredOn(p -> ((Number) p.get("id")).longValue() == bruno.longValue())
                .singleElement().satisfies(p -> assertThat(p.get("email")).isEqualTo("bruno-" + sufixo + "@e2e.local"));

        for (Number id : List.of(ana, bruno)) {
            assertThat(http.delete().uri("/v1/pessoas/" + id).header("Authorization", token).retrieve()
                    .toBodilessEntity().getStatusCode().value()).isEqualTo(204);
        }
    }

    @Test
    void historicoFipeEmOrdemDeMesComOsValores() {
        var r = http.get().uri("/v1/carros/E2E-001").header("Authorization", token()).retrieve().toEntity(LISTA);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).extracting(c -> c.get("mesReferencia")).containsExactly("11/2025", "12/2025", "01/2026");
        assertThat(r.getBody().getFirst()).containsEntry("codigoVeiculo", "E2E-001").containsEntry("marca", "MARCA E2E")
                .containsEntry("modelo", "MODELO E2E").containsEntry("ano", "2020").containsEntry("valor", 52000.0)
                .containsKey("id");
    }

    @Test
    void leiturasDoInversorViramODiaOMesEOAno() {
        for (var leitura : List.of(Map.of("gerado", 2.5, "potencia", 900.0), Map.of("gerado", 3.0, "potencia", 1500.0))) {
            var r = http.post().uri("/v1/geracao-solar").header("X-API-KEY", chave())
                    .contentType(MediaType.APPLICATION_JSON).body(leitura).retrieve().toBodilessEntity();
            assertThat(r.getStatusCode().value()).isEqualTo(200);
        }
        String token = token();
        LocalDate hoje = LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));

        var dia = http.get().uri("/v1/geracao-solar?data=" + hoje).header("Authorization", token).retrieve().toEntity(MAPA);
        assertThat(dia.getStatusCode().value()).isEqualTo(200);
        assertThat(dia.getBody()).containsEntry("geracao", 3.0);
        assertThat((List<?>) dia.getBody().get("valores")).hasSizeGreaterThanOrEqualTo(2);

        var mes = http.get().uri("/v1/geracao-solar/" + hoje.getYear() + "/" + hoje.getMonthValue())
                .header("Authorization", token).retrieve().toEntity(MAPA);
        assertThat(mes.getStatusCode().value()).isEqualTo(200);
        assertThat((List<Map<String, Object>>) mes.getBody().get("valores")).extracting(v -> v.get("valor")).containsExactly(3.0);

        var ano = http.get().uri("/v1/geracao-solar/" + hoje.getYear()).header("Authorization", token).retrieve().toEntity(LISTA);
        assertThat(ano.getStatusCode().value()).isEqualTo(200);
        assertThat(ano.getBody()).singleElement().satisfies(m -> {
            assertThat(m).containsEntry("month", hoje.getMonth().name()).containsEntry("year", String.valueOf(hoje.getYear()));
            assertThat(m).containsEntry("value", 3.0);
        });
    }

    @Test
    void despesasDoAnoSomamAsParcelasDeCadaMes() {
        var r = http.get().uri("/v1/despesas/2031?pessoa=E2E COMPRADOR").header("Authorization", token()).retrieve().toEntity(MAPA);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("pessoa", "E2E COMPRADOR").containsEntry("ano", "2031");
        var porMes = mapa(r.getBody(), "despesasPorMes");
        var fevereiro = mapa(porMes, "02");
        assertThat(((Number) fevereiro.get("valorMes")).doubleValue()).isEqualTo(150.0);
        assertThat(((Number) fevereiro.get("comprasParceladas")).doubleValue()).isEqualTo(100.0);
        assertThat(((Number) fevereiro.get("gastosNoCartao")).doubleValue()).isEqualTo(50.0);
        assertThat(((Number) mapa(porMes, "04").get("valorMes")).doubleValue()).isEqualTo(100.0);
    }

    @Test
    void filtrosDasTelasDeCompras() {
        String token = token();

        assertThat(filtro(token, "/v1/filtros/anos")).contains(Map.of("codigo", "2031", "descricao", "2031"));
        assertThat(filtro(token, "/v1/filtros/meses?ano=2031")).containsExactly(
                Map.of("codigo", "02", "descricao", "FEVEREIRO"),
                Map.of("codigo", "03", "descricao", "MARCO"),
                Map.of("codigo", "04", "descricao", "ABRIL"));
        assertThat(filtro(token, "/v1/filtros/pessoas?ano=2031&mes=02"))
                .containsExactly(Map.of("codigo", "E2E COMPRADOR", "descricao", "E2E COMPRADOR"));
        assertThat(filtro(token, "/v1/filtros/cartoes?ano=2031&mes=02&pessoa=E2E COMPRADOR"))
                .containsExactly(Map.of("codigo", "E2E CARTAO", "descricao", "E2E CARTAO"));
        assertThat(filtro(token, "/v1/filtros/categorias"))
                .contains(Map.of("codigo", "00000000-0000-0000-0000-0000000000a1", "descricao", "E2E CATEGORIA"));
        assertThat(filtro(token, "/v1/filtros/tipos-lancamento"))
                .contains(Map.of("codigo", "00000000-0000-0000-0000-0000000000c1", "descricao", "E2E CARTAO"));
    }

    // ---- apoio ----

    private List<Map<String, Object>> filtro(String token, String url) {
        var r = http.get().uri(url).header("Authorization", token).retrieve().toEntity(LISTA);
        assertThat(r.getStatusCode().value()).as(url).isEqualTo(200);
        return r.getBody();
    }

    private Map<String, Object> criarPessoa(String token, String nome, String email) {
        var r = http.post().uri("/v1/pessoas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nome", nome, "email", email)).retrieve().toEntity(MAPA);
        assertThat(r.getStatusCode().value()).as("cadastro de " + nome).isEqualTo(201);
        return r.getBody();
    }

    private String token() {
        var r = http.post().uri("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email(), "senha", senha())).retrieve().toEntity(MAPA);
        assertThat(r.getStatusCode().value()).as("login do admin E2E").isEqualTo(200);
        return "Bearer " + r.getBody().get("token");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Map<String, Object> m, String campo) {
        assertThat(m).containsKey(campo);
        return (Map<String, Object>) m.get(campo);
    }

    private static String email() {
        return System.getenv("AUTH_ADMIN_EMAIL");
    }

    private static String senha() {
        return System.getenv("AUTH_ADMIN_SENHA");
    }

    private static String chave() {
        return System.getenv("API_KEY");
    }
}
