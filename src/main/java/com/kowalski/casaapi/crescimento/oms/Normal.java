package com.kowalski.casaapi.crescimento.oms;

/** Distribuição normal padrão. */
final class Normal {

    private Normal() {
    }

    /** Função de distribuição acumulada, via erf (Abramowitz e Stegun 7.1.26, erro < 1,5e-7). */
    static double cdf(double z) {
        double x = Math.abs(z) / Math.sqrt(2);
        double t = 1 / (1 + 0.3275911 * x);
        double erf = 1 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * Math.exp(-x * x);
        return z >= 0 ? 0.5 * (1 + erf) : 0.5 * (1 - erf);
    }
}
