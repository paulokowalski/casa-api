package com.kowalski.casaapi.crescimento.ia;

import com.kowalski.casaapi.crescimento.comum.IaIndisponivelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Chama o container casa-ia, que só sabe fazer o resumo do mês e responder perguntas sobre ele. */
@Slf4j
@Component
public class CasaIaGerador implements GeradorIa {

    private static final ParameterizedTypeReference<Map<String, Object>> OBJETO = new ParameterizedTypeReference<>() { };

    private final RestClient http;
    private final String segredo;

    public CasaIaGerador(IaProperties props) {
        if (!props.habilitada()) {
            this.http = null;
            this.segredo = null;
            return;
        }
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(props.tempo());
        this.http = RestClient.builder().baseUrl(props.url()).requestFactory(fabrica).build();
        this.segredo = props.segredo();
    }

    @Override
    public boolean disponivel() {
        return http != null;
    }

    @Override
    public Resultado resumirMes(String dados) {
        Map<String, Object> r = chamar("/v1/resumo-mes", Map.of("dados", dados));
        if (!(r.get("resumo") instanceof String resumo) || !(r.get("perguntas") instanceof List<?> perguntas))
            throw new IaIndisponivelException("A IA respondeu num formato inesperado. Tente de novo.", null);
        log.info("Resumo do mês gerado (modelo {})", r.get("modelo"));
        return new Resultado(resumo, perguntas.stream().filter(String.class::isInstance).map(String.class::cast).toList(),
                r.get("modelo") instanceof String m ? m : null);
    }

    @Override
    public Resposta responder(String dados, String pergunta) {
        Map<String, Object> r = chamar("/v1/pergunta", Map.of("dados", dados, "pergunta", pergunta));
        if (!(r.get("foraDoAssunto") instanceof Boolean fora) || !(r.get("resposta") instanceof String resposta))
            throw new IaIndisponivelException("A IA respondeu num formato inesperado. Tente de novo.", null);
        return new Resposta(fora, resposta, r.get("modelo") instanceof String m ? m : null);
    }

    private Map<String, Object> chamar(String rota, Map<String, Object> corpo) {
        if (http == null) throw new IaIndisponivelException("A IA não está configurada neste servidor.", null);
        Map<String, Object> r;
        try {
            r = http.post().uri(rota).contentType(MediaType.APPLICATION_JSON).header("X-Ia-Segredo", segredo)
                    .body(corpo).retrieve().body(OBJETO);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new IaIndisponivelException("A IA está ocupada ou muitas perguntas foram feitas agora. Tente de novo em alguns minutos.", e);
        } catch (RestClientException e) {
            throw new IaIndisponivelException("A IA não respondeu agora. Tente de novo em alguns minutos.", e);
        }
        if (r == null) throw new IaIndisponivelException("A IA respondeu num formato inesperado. Tente de novo.", null);
        return r;
    }
}
