package com.kowalski.casaapi.crescimento;

import com.kowalski.casaapi.crescimento.crianca.Sexo;
import com.kowalski.casaapi.crescimento.oms.Indicador;
import com.kowalski.casaapi.crescimento.oms.TabelaOms;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TabelaOmsTest {

    static final TabelaOms OMS = new TabelaOms();

    @Test
    void cobreDoNascimentoAosCincoAnos() {
        assertThat(OMS.ultimoDia()).isEqualTo(1826);
        assertThat(OMS.cobre(0)).isTrue();
        assertThat(OMS.cobre(1826)).isTrue();
        assertThat(OMS.cobre(-1)).isFalse();
        assertThat(OMS.cobre(1827)).isFalse();
        assertThat(OMS.z(Indicador.PESO, Sexo.M, 2000, 18)).isEmpty();
    }

    @Test
    void medianaTemZeroEPercentil50() {
        assertThat(OMS.z(Indicador.PESO, Sexo.M, 0, 3.3464).getAsDouble()).isCloseTo(0, within(1e-6));
        assertThat(OMS.z(Indicador.PERIMETRO, Sexo.M, 0, 34.4618).getAsDouble()).isCloseTo(0, within(1e-6));
        assertThat(TabelaOms.percentil(0)).isCloseTo(50, within(1e-6));
    }

    @Test
    void batemComAsTabelasPublicadasPelaOms() {
        // Tabelas de percentis da OMS (arredondadas a 0,1): menino ao nascer P3 2,5 kg, P97 4,3 kg;
        // menina com 12 meses: comprimento mediano 74,0 cm; menino com 24 meses: peso mediano 12,2 kg
        assertThat(OMS.valor(Indicador.PESO, Sexo.M, 0, TabelaOms.Z_P3)).isCloseTo(2.5, within(0.06));
        assertThat(OMS.valor(Indicador.PESO, Sexo.M, 0, TabelaOms.Z_P97)).isCloseTo(4.3, within(0.06));
        assertThat(OMS.valor(Indicador.COMPRIMENTO, Sexo.F, 365, 0)).isCloseTo(74.0, within(0.1));
        assertThat(OMS.valor(Indicador.PESO, Sexo.M, 730, 0)).isCloseTo(12.2, within(0.1));
    }

    @Test
    void percentisDasLinhasDeReferencia() {
        double p3 = OMS.valor(Indicador.PESO, Sexo.F, 120, TabelaOms.Z_P3);
        double z = OMS.z(Indicador.PESO, Sexo.F, 120, p3).getAsDouble();
        assertThat(TabelaOms.percentil(z)).isCloseTo(3, within(0.01));
        assertThat(TabelaOms.percentil(TabelaOms.Z_P97)).isCloseTo(97, within(0.01));
    }

    @Test
    void pesoAlemDeTresDesviosUsaOAjusteDaOms() {
        double sd3 = OMS.valor(Indicador.PESO, Sexo.M, 0, 3);
        double sd2 = OMS.valor(Indicador.PESO, Sexo.M, 0, 2);
        double z = OMS.z(Indicador.PESO, Sexo.M, 0, sd3 + (sd3 - sd2)).getAsDouble();
        assertThat(z).isCloseTo(4, within(1e-6));
        assertThat(OMS.z(Indicador.PESO, Sexo.M, 0, 0.9).getAsDouble()).isLessThan(-3).isGreaterThan(-12);
    }

    @Test
    void valorInvalidoNaoTemZ() {
        assertThat(OMS.z(Indicador.PESO, Sexo.M, 10, 0)).isEmpty();
        assertThat(OMS.z(Indicador.PESO, Sexo.M, 10, Double.NaN)).isEmpty();
    }
}
