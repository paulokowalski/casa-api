package com.kowalski.casaapi.crescimento.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração da IA ({@code crescimento.ia.*}). Sem {@code url}, o resumo do mês fica desligado.
 * ({@code crescimento.ia.rede-bloqueada} é lida por {@link BloqueioRedeIaFilter}.)
 *
 * @param url     endereço do serviço que gera os textos (no casa: o container casa-ia, que usa a assinatura do Claude)
 * @param segredo segredo compartilhado com esse serviço (cabeçalho X-Ia-Segredo)
 * @param tempo   quanto esperar por uma resposta
 */
@ConfigurationProperties(prefix = "crescimento.ia")
public record IaProperties(String url, String segredo, Duration tempo) {

    public IaProperties {
        if (tempo == null || tempo.isNegative() || tempo.isZero()) tempo = Duration.ofSeconds(58);
    }

    public boolean habilitada() {
        return url != null && !url.isBlank() && segredo != null && !segredo.isBlank();
    }
}
