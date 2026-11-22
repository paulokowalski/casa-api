package com.kowalski.casaapi.crescimento.marco;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Marcos do desenvolvimento de 0 a 3 anos, por faixa de idade, conforme a Caderneta da Criança do Ministério da
 * Saúde (instrumento de vigilância do desenvolvimento, baseado no teste de Denver). Os ids são estáveis: ficam
 * gravados no banco. Ao mudar o catálogo, suba {@link #VERSAO} e nunca reaproveite um id com outro significado.
 */
public final class CatalogoMarcos {

    public static final String VERSAO = "caderneta-ms-2024.1";
    public static final String FONTE = "Caderneta da Criança (Ministério da Saúde), vigilância do desenvolvimento";

    public enum Area { MOTOR, SOCIAL, LINGUAGEM, COGNITIVO }

    /** Faixa de idade em meses [inicio, fim). */
    public record Faixa(String id, String nome, int inicioMeses, int fimMeses, List<String> brincadeiras) {
    }

    public record Marco(String id, String faixaId, Area area, String texto) {
    }

    public static final List<Faixa> FAIXAS = List.of(
            new Faixa("f0", "Até 1 mês", 0, 1, List.of("Converse olhando nos olhos, a cerca de 30 cm do rosto.", "Cante e fale com voz suave durante as trocas e o banho.", "Ofereça momentos de bruços acordado, sempre com um adulto por perto.")),
            new Faixa("f1", "1 a 2 meses", 1, 2, List.of("Sorria e espere o sorriso de volta: é uma conversa.", "Mostre objetos coloridos movendo-os devagar de um lado ao outro.", "Repita os sons que o bebê faz.")),
            new Faixa("f2", "2 a 4 meses", 2, 4, List.of("Deixe brinquedos leves ao alcance das mãos.", "Brinque de bruços com um espelho ou um brinquedo à frente.", "Responda aos gugus como se fosse um diálogo.")),
            new Faixa("f3", "4 a 6 meses", 4, 6, List.of("Chame pelo nome de lados diferentes para ele procurar o som.", "Ofereça objetos de texturas variadas e seguros para levar à boca.", "Deixe no chão, sobre um tapete, para treinar rolar.")),
            new Faixa("f4", "6 a 9 meses", 6, 9, List.of("Brinque de esconde-achou com um paninho.", "Ofereça dois objetos para passar de uma mão à outra.", "Leia livros de figuras grandes e nomeie o que aparece.")),
            new Faixa("f5", "9 a 12 meses", 9, 12, List.of("Ensine gestos como tchau e bater palmas.", "Ofereça pedacinhos de alimento para treinar a pinça.", "Deixe os móveis seguros para ele andar se apoiando.")),
            new Faixa("f6", "12 a 15 meses", 12, 15, List.of("Brinque de colocar e tirar blocos de um pote.", "Nomeie tudo o que ele aponta.", "Dê espaço livre para andar com segurança.")),
            new Faixa("f7", "15 a 18 meses", 15, 18, List.of("Deixe tentar comer sozinho com a colher.", "Monte torres de blocos juntos e deixe derrubar.", "Cante músicas com gestos.")),
            new Faixa("f8", "18 a 24 meses", 18, 24, List.of("Peça ajuda em tarefas simples, como guardar brinquedos.", "Mostre figuras em livros e pergunte “cadê o…?”.", "Brinque de chutar bola.")),
            new Faixa("f9", "2 anos a 2 anos e meio", 24, 30, List.of("Deixe tentar se vestir, com supervisão.", "Converse com frases curtas e amplie o que ele diz.", "Brinque de pular com os dois pés.")),
            new Faixa("f10", "2 anos e meio a 3 anos", 30, 36, List.of("Promova brincadeiras com outras crianças.", "Ofereça giz de cera e papel grande.", "Brinque de arremessar bola.")));

    public static final List<Marco> MARCOS = List.of(
            m("m00a", "f0", Area.MOTOR, "Fica de barriga para cima com pernas e braços dobrados"), m("m00b", "f0", Area.SOCIAL, "Observa um rosto"),
            m("m00c", "f0", Area.LINGUAGEM, "Reage ao som"), m("m00d", "f0", Area.MOTOR, "Eleva a cabeça quando está de bruços"),
            m("m01a", "f1", Area.SOCIAL, "Sorriso social quando estimulado"), m("m01b", "f1", Area.MOTOR, "Abre as mãos"),
            m("m01c", "f1", Area.LINGUAGEM, "Emite sons"), m("m01d", "f1", Area.MOTOR, "Movimenta ativamente braços e pernas"),
            m("m02a", "f2", Area.SOCIAL, "Responde ativamente ao contato social"), m("m02b", "f2", Area.MOTOR, "Segura objetos"),
            m("m02c", "f2", Area.LINGUAGEM, "Emite sons (gugu, agu)"), m("m02d", "f2", Area.MOTOR, "De bruços, levanta a cabeça apoiando-se nos antebraços"),
            m("m03a", "f3", Area.COGNITIVO, "Busca objetos ativamente"), m("m03b", "f3", Area.MOTOR, "Leva objetos à boca"),
            m("m03c", "f3", Area.LINGUAGEM, "Localiza o som"), m("m03d", "f3", Area.MOTOR, "Muda de posição ativamente (rola)"),
            m("m04a", "f4", Area.SOCIAL, "Brinca de esconde-achou"), m("m04b", "f4", Area.MOTOR, "Passa objetos de uma mão para a outra"),
            m("m04c", "f4", Area.LINGUAGEM, "Duplica sílabas (papa, mama)"), m("m04d", "f4", Area.MOTOR, "Senta sem apoio"),
            m("m05a", "f5", Area.SOCIAL, "Imita gestos"), m("m05b", "f5", Area.MOTOR, "Faz pinça com polegar e indicador"),
            m("m05c", "f5", Area.LINGUAGEM, "Produz “jargão” (fala embolada)"), m("m05d", "f5", Area.MOTOR, "Anda com apoio"),
            m("m06a", "f6", Area.SOCIAL, "Mostra o que quer"), m("m06b", "f6", Area.COGNITIVO, "Coloca blocos na caneca"),
            m("m06c", "f6", Area.LINGUAGEM, "Fala uma palavra"), m("m06d", "f6", Area.MOTOR, "Anda sem apoio"),
            m("m07a", "f7", Area.SOCIAL, "Usa colher ou garfo"), m("m07b", "f7", Area.COGNITIVO, "Constrói torre de 2 cubos"),
            m("m07c", "f7", Area.LINGUAGEM, "Fala 3 palavras"), m("m07d", "f7", Area.MOTOR, "Anda para trás"),
            m("m08a", "f8", Area.SOCIAL, "Tira a roupa"), m("m08b", "f8", Area.COGNITIVO, "Constrói torre de 3 cubos"),
            m("m08c", "f8", Area.LINGUAGEM, "Aponta 2 figuras"), m("m08d", "f8", Area.MOTOR, "Chuta a bola"),
            m("m09a", "f9", Area.SOCIAL, "Veste-se com supervisão"), m("m09b", "f9", Area.COGNITIVO, "Constrói torre de 6 cubos"),
            m("m09c", "f9", Area.LINGUAGEM, "Forma frases com 2 palavras"), m("m09d", "f9", Area.MOTOR, "Pula com ambos os pés"),
            m("m10a", "f10", Area.SOCIAL, "Brinca com outras crianças"), m("m10b", "f10", Area.COGNITIVO, "Imita uma linha vertical"),
            m("m10c", "f10", Area.LINGUAGEM, "Reconhece 2 ações"), m("m10d", "f10", Area.MOTOR, "Arremessa a bola"));

    private static final Map<String, Marco> POR_ID = MARCOS.stream().collect(Collectors.toMap(Marco::id, Function.identity()));

    private CatalogoMarcos() {
    }

    private static Marco m(String id, String faixa, Area area, String texto) {
        return new Marco(id, faixa, area, texto);
    }

    public static Optional<Marco> marco(String id) {
        return Optional.ofNullable(POR_ID.get(id));
    }

    /** Faixa da idade em meses (fracionários); depois dos 3 anos, a última. */
    public static Faixa faixaDaIdade(double meses) {
        return FAIXAS.stream().filter(f -> meses < f.fimMeses()).findFirst().orElse(FAIXAS.get(FAIXAS.size() - 1));
    }
}
