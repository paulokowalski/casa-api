package com.kowalski.casaapi;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As migrations ({@code src/main/resources/db/**}) são aplicadas à mão no servidor, então três coisas costumam ser
 * esquecidas e este teste confere antes do commit:
 * <ol>
 *   <li>o script pode ser reaplicado sem erro ({@code IF [NOT] EXISTS}, ou dentro de um bloco {@code DO $$});</li>
 *   <li>o README lista o script na tabela de migrations;</li>
 *   <li>o banco dos testes e2e ({@code src/test/resources/e2e/estrutura.sql}) tem o resultado de todas as migrations:
 *       as tabelas e colunas criadas existem e as apagadas não existem.</li>
 * </ol>
 * Só lê arquivos do repositório (nenhum banco).
 */
class MigrationsTest {

    private static final Path DB = Path.of("src/main/resources/db");
    private static final Path ESTRUTURA_E2E = Path.of("src/test/resources/e2e/estrutura.sql");
    private static final Pattern VERSAO = Pattern.compile("V(\\d+)__.*\\.sql");

    private static final Pattern SEM_IF = Pattern.compile(
            "(?i)^\\s*(CREATE\\s+(UNIQUE\\s+)?(TABLE|INDEX|SCHEMA|SEQUENCE)|DROP\\s+(TABLE|INDEX|SCHEMA|FUNCTION|VIEW|SEQUENCE)|"
                    + "(ALTER\\s+TABLE\\s+[\\w.]+\\s+)?(ADD|DROP)\\s+COLUMN)\\b");
    private static final Pattern COM_IF = Pattern.compile("(?i)\\bIF\\s+(NOT\\s+)?EXISTS\\b");
    private static final Pattern CRIA_TABELA = Pattern.compile("(?i)CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\\w.]+)");
    private static final Pattern APAGA_TABELA = Pattern.compile("(?i)DROP\\s+TABLE\\s+(?:IF\\s+EXISTS\\s+)?([\\w.]+)");
    private static final Pattern ALTERA_TABELA = Pattern.compile("(?is)ALTER\\s+TABLE\\s+(?:IF\\s+EXISTS\\s+)?(?:ONLY\\s+)?([\\w.]+)(.*?);");
    private static final Pattern ADD_COLUNA = Pattern.compile("(?i)ADD\\s+COLUMN\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)");
    private static final Pattern DROP_COLUNA = Pattern.compile("(?i)DROP\\s+COLUMN\\s+(?:IF\\s+EXISTS\\s+)?(\\w+)");
    private static final Pattern TABELA_E2E = Pattern.compile("(?s)CREATE TABLE ([\\w.]+) \\((.*?)\\n\\);");

    static List<Path> scripts;

    @BeforeAll
    static void carregar() throws IOException {
        try (Stream<Path> s = Files.walk(DB)) {
            // por pasta e pelo número da versão (V10 depois de V9)
            scripts = s.filter(p -> VERSAO.matcher(p.getFileName().toString()).matches())
                    .sorted(Comparator.comparing((Path p) -> p.getParent().toString())
                            .thenComparingInt(MigrationsTest::versao))
                    .toList();
        }
        assertThat(scripts).isNotEmpty();
    }

    static int versao(Path p) {
        Matcher m = VERSAO.matcher(p.getFileName().toString());
        return m.matches() ? Integer.parseInt(m.group(1)) : 0;
    }

    /** Tira comentários de linha ({@code -- ...}) para não confundir com comandos. */
    static String semComentarios(String sql) {
        return sql.replaceAll("(?m)--.*$", "");
    }

    static String tabela(String nome) {
        String n = nome.toLowerCase(Locale.ROOT);
        return n.contains(".") ? n : "public." + n;
    }

    @Test
    void scriptsPodemSerReaplicados() throws IOException {
        List<String> problemas = new ArrayList<>();
        for (Path p : scripts) {
            boolean dentroDeDo = false;
            int n = 0;
            for (String linha : semComentarios(Files.readString(p)).split("\n")) {
                n++;
                int marcas = linha.split("\\$\\$", -1).length - 1;
                if (!dentroDeDo && SEM_IF.matcher(linha).find() && !COM_IF.matcher(linha).find())
                    problemas.add(DB.relativize(p) + ":" + n + ": " + linha.strip());
                if (marcas % 2 == 1) dentroDeDo = !dentroDeDo;
            }
        }
        assertThat(problemas).as("use IF [NOT] EXISTS (ou um bloco DO $$ que trate o erro) para o script poder ser reaplicado")
                .isEmpty();
    }

    @Test
    void readmeListaTodosOsScripts() throws IOException {
        String readme = Files.readString(Path.of("README.md"));
        List<String> faltando = scripts.stream()
                .map(p -> DB.relativize(p).toString())
                .filter(rel -> {
                    String nome = Path.of(rel).getFileName().toString();
                    // os de db/migration aparecem só pelo nome; os das outras pastas, como db/<pasta>/<nome>
                    return rel.startsWith("migration/") ? !readme.contains(nome) : !readme.contains("db/" + rel);
                }).toList();
        assertThat(faltando).as("migrations que faltam na tabela do README (seção Banco de dados e migrations)").isEmpty();
    }

    @Test
    void bancoDosTestesE2eTemOResultadoDasMigrations() throws IOException {
        Map<String, Set<String>> esperado = new LinkedHashMap<>(); // tabela -> colunas adicionadas
        Set<String> apagadas = new LinkedHashSet<>();
        Map<String, Set<String>> colunasApagadas = new HashMap<>();
        for (Path p : scripts) {
            String sql = semComentarios(Files.readString(p));
            // na ordem em que aparecem no script
            List<int[]> posicoes = new ArrayList<>();
            Matcher m;
            for (m = CRIA_TABELA.matcher(sql); m.find(); ) posicoes.add(new int[]{m.start(), 0});
            for (m = APAGA_TABELA.matcher(sql); m.find(); ) posicoes.add(new int[]{m.start(), 1});
            for (m = ALTERA_TABELA.matcher(sql); m.find(); ) posicoes.add(new int[]{m.start(), 2});
            posicoes.sort(Comparator.comparingInt(a -> a[0]));
            for (int[] pos : posicoes) {
                if (pos[1] == 0) {
                    m = CRIA_TABELA.matcher(sql);
                    m.find(pos[0]);
                    String t = tabela(m.group(1));
                    esperado.putIfAbsent(t, new LinkedHashSet<>());
                    apagadas.remove(t);
                } else if (pos[1] == 1) {
                    m = APAGA_TABELA.matcher(sql);
                    m.find(pos[0]);
                    String t = tabela(m.group(1));
                    esperado.remove(t);
                    apagadas.add(t);
                } else {
                    m = ALTERA_TABELA.matcher(sql);
                    m.find(pos[0]);
                    String t = tabela(m.group(1));
                    for (Matcher c = ADD_COLUNA.matcher(m.group(2)); c.find(); ) {
                        esperado.computeIfAbsent(t, k -> new LinkedHashSet<>()).add(c.group(1).toLowerCase(Locale.ROOT));
                        colunasApagadas.getOrDefault(t, new HashSet<>()).remove(c.group(1).toLowerCase(Locale.ROOT));
                    }
                    for (Matcher c = DROP_COLUNA.matcher(m.group(2)); c.find(); ) {
                        String col = c.group(1).toLowerCase(Locale.ROOT);
                        Optional.ofNullable(esperado.get(t)).ifPresent(cols -> cols.remove(col));
                        colunasApagadas.computeIfAbsent(t, k -> new HashSet<>()).add(col);
                    }
                }
            }
        }

        Map<String, String> e2e = tabelasDoE2e(Files.readString(ESTRUTURA_E2E));

        List<String> problemas = new ArrayList<>();
        esperado.forEach((t, colunas) -> {
            String corpo = e2e.get(t);
            if (corpo == null) {
                problemas.add("falta a tabela " + t);
                return;
            }
            for (String c : colunas)
                if (!Pattern.compile("(?mi)^\\s*\"?" + Pattern.quote(c) + "\"?\\s").matcher(corpo).find())
                    problemas.add("falta a coluna " + t + "." + c);
        });
        apagadas.stream().filter(e2e::containsKey).forEach(t -> problemas.add("a tabela " + t + " foi apagada por uma migration"));
        colunasApagadas.forEach((t, cols) -> cols.forEach(c -> {
            String corpo = e2e.get(t);
            if (corpo != null && Pattern.compile("(?mi)^\\s*\"?" + Pattern.quote(c) + "\"?\\s").matcher(corpo).find())
                problemas.add("a coluna " + t + "." + c + " foi apagada por uma migration");
        }));
        assertThat(problemas).as("atualize " + ESTRUTURA_E2E + " (pg_dump --schema-only depois de aplicar, ou acrescente o DDL)")
                .isEmpty();
    }

    /**
     * Tabela -> texto das colunas, no formato do pg_dump ({@code CREATE TABLE x (...\n);}) e no das migrations
     * ({@code CREATE TABLE IF NOT EXISTS}, acrescentado à mão). A busca e o recorte usam o mesmo texto sem comentários;
     * senão as posições não batem.
     */
    static Map<String, String> tabelasDoE2e(String sqlOriginal) {
        String sql = semComentarios(sqlOriginal);
        Map<String, String> e2e = new HashMap<>();
        for (Matcher m = TABELA_E2E.matcher(sql); m.find(); )
            e2e.put(tabela(m.group(1)), m.group(2));
        for (Matcher m = CRIA_TABELA.matcher(sql); m.find(); )
            e2e.putIfAbsent(tabela(m.group(1)), corpo(sql, m.end()));
        return e2e;
    }

    @Test
    void tabelaAcrescentadaDepoisDeComentariosTemAsPropriasColunas() {
        String sql = """
                --
                -- Name: conta; Type: TABLE; Schema: public; Owner: -
                --
                CREATE TABLE public.conta (
                    id integer NOT NULL,
                    nome text
                );
                -- comentários do pg_dump antes do DDL acrescentado à mão
                -- (mais um, para as posições com e sem comentários ficarem bem diferentes)
                CREATE TABLE IF NOT EXISTS exemplo.exame (
                    id uuid PRIMARY KEY,
                    titulo varchar(120) NOT NULL,
                    tipo varchar(4) CHECK (tipo IN ('PDF', 'JPEG'))
                );
                """;
        Map<String, String> e2e = tabelasDoE2e(sql);
        assertThat(e2e.get("exemplo.exame")).contains("titulo").contains("tipo").doesNotContain("nome");
        assertThat(e2e.get("public.conta")).contains("nome").doesNotContain("titulo");
    }

    /** O texto entre o parêntese que abre depois de {@code inicio} e o que fecha. */
    private static String corpo(String sql, int inicio) {
        int abre = sql.indexOf('(', inicio);
        if (abre < 0) return "";
        int nivel = 0;
        for (int i = abre; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '(') nivel++;
            else if (ch == ')' && --nivel == 0) return sql.substring(abre + 1, i);
        }
        return sql.substring(abre + 1);
    }
}
