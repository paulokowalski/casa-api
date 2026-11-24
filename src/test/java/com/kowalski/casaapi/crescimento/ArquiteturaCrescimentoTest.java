package com.kowalski.casaapi.crescimento;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ArquiteturaCrescimentoTest {

    private static final Path MODULO = Path.of("src/main/java/com/kowalski/casaapi/crescimento");
    private static final Pattern FORA_DO_MODULO = Pattern.compile("com\\.kowalski\\.casaapi\\.(?!crescimento\\b)");

    @Test
    void moduloNaoDependeDoRestoDoCasa() throws IOException {
        assertThat(MODULO).isDirectory();
        List<String> violacoes;
        try (Stream<Path> arquivos = Files.walk(MODULO)) {
            violacoes = arquivos.filter(p -> p.toString().endsWith(".java")).flatMap(p -> {
                try {
                    return Files.readAllLines(p).stream().filter(l -> FORA_DO_MODULO.matcher(l).find())
                            .map(l -> MODULO.relativize(p) + ": " + l.trim());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).toList();
        }
        assertThat(violacoes).as("o módulo crescimento não pode importar outros pacotes do casa-api").isEmpty();
    }

    @Test
    void tabelasProprias() throws IOException {
        try (Stream<Path> arquivos = Files.walk(MODULO)) {
            arquivos.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    String s = Files.readString(p);
                    if (s.contains("@Entity")) assertThat(s).as(p.toString()).contains("schema = \"crescimento\"");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    void nenhumaIaRecebeDadosDasCriancas() throws IOException {
        assertThat(MODULO.resolve("ia")).as("o módulo crescimento não pode ter pacote de IA").doesNotExist();
        Pattern proibido = Pattern.compile("java\\.net\\.(http|Socket|URL\\b|URLConnection|HttpURLConnection)|RestClient|RestTemplate"
                + "|WebClient|HttpClient|Feign|OkHttp|(?i:anthropic|openai|claude|gemini|chatgpt|ollama|llm|GeradorIa)");
        try (Stream<Path> arquivos = Files.walk(MODULO)) {
            List<String> violacoes = arquivos.filter(p -> p.toString().endsWith(".java")).flatMap(p -> {
                try {
                    return Files.readAllLines(p).stream().filter(l -> proibido.matcher(l).find())
                            .map(l -> MODULO.relativize(p) + ": " + l.trim());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).toList();
            assertThat(violacoes).as("o módulo crescimento não pode mandar dados para fora nem para IA").isEmpty();
        }
    }
}
