package com.kowalski.casaapi.compra;

import com.kowalski.casaapi.support.Login;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompraTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 23, 12, 0);
    private final TipoLancamento nubank = TipoLancamento.builder().id(UUID.randomUUID()).nome("NUBANK").build();

    private DadosDaCompra dados(String valor, int parcelas) {
        return new DadosDaCompra("tv", new BigDecimal(valor), LocalDate.of(2026, 1, 31), parcelas, Login.pessoa(Login.ANA_ID, "ana"), nubank, null);
    }

    @Test
    void parcelasComecamNoMesSeguinteEArredondamParaBaixo() {
        Compra compra = Compra.nova(dados("100.00", 3), AGORA);

        assertThat(compra.getNomeProduto()).isEqualTo("TV");
        assertThat(compra.nomeDaPessoa()).isEqualTo("ANA");
        assertThat(compra.nomeDoCartao()).isEqualTo("NUBANK");
        assertThat(compra.getDataCadastro()).isEqualTo(AGORA);
        assertThat(compra.getParcelas()).extracting(CompraParcela::getDataParcela).containsExactly(
                LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30));
        assertThat(compra.getParcelas()).allSatisfy(p -> assertThat(p.getValorParcela()).isEqualByComparingTo("33.33"));
    }

    @Test
    void alterarRefazAsParcelas() {
        Compra compra = Compra.nova(dados("100.00", 3), AGORA);

        compra.alterar(dados("50.00", 2));

        assertThat(compra.getParcelas()).extracting(CompraParcela::getNumeroParcela).containsExactly(1, 2);
        assertThat(compra.getParcelas()).allSatisfy(p -> assertThat(p.getValorParcela()).isEqualByComparingTo("25.00"));
    }

    @Test
    void valorQueFaltaDepoisDeCadaParcela() {
        Compra compra = Compra.nova(dados("300.00", 3), AGORA);

        assertThat(compra.getParcelas()).extracting(CompraParcela::valorQueFalta).containsExactly(200.0, 100.0, 0.0);
        assertThat(compra.getParcelas().getLast().isUltimaParcela()).isTrue();
    }
}
