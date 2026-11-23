package com.kowalski.casaapi.config;

import com.kowalski.casaapi.comum.ErroResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Documentação OpenAPI da API. A interface fica em {@code /casa-api/swagger-ui.html}
 * e o JSON em {@code /casa-api/v3/api-docs}. Pode ser desligada com {@code SWAGGER_ENABLED=false}.
 */
@Configuration
public class OpenApiConfig {

    public static final String JWT = "jwt";
    public static final String API_KEY = "apiKey";

    private static final String ERRO = "ErroResponse";

    /** Respostas de erro que qualquer endpoint pode devolver (ver ApiExceptionHandler e ErroSegurancaJson). */
    private static final Map<String, String> ERROS_PADRAO = new LinkedHashMap<>();

    static {
        ERROS_PADRAO.put("400", "Dados ou parâmetros inválidos");
        ERROS_PADRAO.put("401", "Não autenticado: token ausente, inválido ou expirado");
        ERROS_PADRAO.put("403", "Sem permissão (dados de outra pessoa ou ação só de administrador)");
        ERROS_PADRAO.put("500", "Erro inesperado no servidor");
    }

    @Bean
    public OpenAPI casaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Casa API")
                        .version("v1")
                        .description("""
                                API da casa: compras no cartão e parcelas, contas e lançamentos mensais, \
                                renda, faturas, pessoas e monitoramento (geração solar e FIPE).

                                **Autenticação:** faça `POST /v1/auth/login`, copie o `token` da resposta e \
                                use o botão **Authorize** (esquema `jwt`). Integrações usam o cabeçalho \
                                `X-API-KEY` (esquema `apiKey`), aceito só em `POST /v1/geracao-solar` (Node-RED) \
                                e `POST /v1/servidor/ventilacao` (Home Assistant).

                                **Permissões:** `ADMIN` vê e altera dados de todas as pessoas; `USUARIO` só os \
                                próprios — pedir dados de outra pessoa responde 403.

                                **Erros:** sempre no formato `{"mensagem": "..."}`."""))
                // Relativo ao host: funciona tanto direto na API (:8080) quanto pelo nginx do front (:3000).
                .servers(List.of(new Server().url("/casa-api").description("Esta instância")))
                .components(new Components()
                        .addSecuritySchemes(JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                                .description("Token devolvido por POST /v1/auth/login (vale 8 horas por padrão)."))
                        .addSecuritySchemes(API_KEY, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-API-KEY")
                                .description("Chave de integração (variável API_KEY). Só para POST /v1/geracao-solar e POST /v1/servidor/ventilacao.")))
                .addSecurityItem(new SecurityRequirement().addList(JWT));
    }

    @Bean
    public OpenApiCustomizer errosPadrao() {
        return openApi -> {
            Schema<?> erro = ModelConverters.getInstance().read(ErroResponse.class).get(ERRO);
            openApi.getComponents().addSchemas(ERRO, erro);
            Content corpo = new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ERRO)));

            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(op -> {
                ApiResponses respostas = op.getResponses() == null ? new ApiResponses() : op.getResponses();
                boolean publico = op.getSecurity() != null && op.getSecurity().isEmpty();
                ERROS_PADRAO.forEach((codigo, descricao) -> {
                    if (publico && codigo.equals("403")) return;
                    if (!respostas.containsKey(codigo)) {
                        respostas.addApiResponse(codigo, new ApiResponse().description(descricao).content(corpo));
                    }
                });
                // Erros declarados nos controllers herdam o tipo de retorno do método; todo erro é ErroResponse.
                respostas.forEach((codigo, resposta) -> {
                    if (codigo.startsWith("4") || codigo.startsWith("5")) resposta.setContent(corpo);
                });
                op.setResponses(respostas);
            }));
        };
    }
}
