package com.kowalski.casaapi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regra do sistema: dados pessoais nunca vão para IA. Nenhum código da API (de nenhuma tela) pode chamar ou citar
 * serviços de IA.
 */
class SemIaTest {

    private static final Pattern IA = Pattern.compile(
            "(?i)anthropic|openai|\\bclaude\\b|gemini|chatgpt|\\bgpt-|ollama|\\bllm\\b|mistral\\.ai|cohere|huggingface|"
                    + "bedrock|vertex\\s*ai|langchain|spring\\.ai|springframework\\.ai");

    @Test
    void nenhumCodigoUsaServicoDeIa() throws IOException {
        List<String> violacoes;
        try (Stream<Path> arquivos = Files.walk(Path.of("src/main"))) {
            violacoes = arquivos.filter(Files::isRegularFile).flatMap(p -> {
                try {
                    return Files.readAllLines(p).stream().filter(l -> IA.matcher(l).find()).map(l -> p + ": " + l.trim());
                } catch (IOException e) {
                    return Stream.of(p + ": não consegui ler (" + e.getMessage() + ")");
                }
            }).toList();
        }
        assertThat(violacoes).as("dados pessoais não podem ir para serviços de IA").isEmpty();
    }

    @Test
    void dependenciasSemIa() throws IOException {
        assertThat(IA.matcher(Files.readString(Path.of("pom.xml"))).find()).as("pom.xml sem bibliotecas de IA").isFalse();
    }
}
