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

    // ---- apoio ----

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
