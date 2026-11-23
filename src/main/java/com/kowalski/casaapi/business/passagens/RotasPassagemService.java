package com.kowalski.casaapi.business.passagens;

import com.kowalski.casaapi.api.v1.input.RotaPassagemInput;
import com.kowalski.casaapi.api.v1.input.RotaPassagemInput.Data;
import com.kowalski.casaapi.api.v1.input.RotaPassagemInput.Janela;
import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Cadastro das rotas do robô de passagens (tabela passagens.rota). O robô confere a tabela a cada 30 s e
 * busca na hora as combinações de trecho e datas que ainda não têm preço.
 */
@Service
@RequiredArgsConstructor
public class RotasPassagemService {

    private static final Pattern IATA = Pattern.compile("[A-Z]{3}");
    static final int MAX_AEROPORTOS = 5;
    static final int MAX_DATAS = 20;
    static final int MAX_DIAS_JANELA = 366;

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock relogio;

    @Transactional
    public Map<String, Object> criar(RotaPassagemInput in) {
        Map<String, Object> cfg = validar(in, LocalDate.now(relogio));
        String nome = (String) cfg.get("nome");
        if (existe(nome, null)) throw new IllegalArgumentException("Já existe uma rota chamada \"" + nome + "\"");
        Long id = jdbc.queryForObject("""
                INSERT INTO passagens.rota (nome, ordem, ativa, config, atualizado_em)
                VALUES (:nome, (SELECT COALESCE(MAX(ordem), -1) + 1 FROM passagens.rota), :ativa, CAST(:config AS jsonb), now())
                RETURNING id""", parametros(cfg), Long.class);
        return buscar(id);
    }

    /** Edita a rota. Se o nome mudar, o histórico de preços e os alertas vão junto. */
    @Transactional
    public Map<String, Object> atualizar(long id, RotaPassagemInput in) {
        String antigo = nome(id);
        Map<String, Object> cfg = validar(in, LocalDate.now(relogio));
        String nome = (String) cfg.get("nome");
        if (existe(nome, id)) throw new IllegalArgumentException("Já existe uma rota chamada \"" + nome + "\"");
        MapSqlParameterSource p = parametros(cfg).addValue("id", id).addValue("antigo", antigo);
        jdbc.update("""
                UPDATE passagens.rota SET nome = :nome, ativa = :ativa, config = CAST(:config AS jsonb), atualizado_em = now()
                WHERE id = :id""", p);
        if (!antigo.equals(nome)) {
            jdbc.update("UPDATE passagens.preco SET rota = :nome WHERE rota = :antigo", p);
            jdbc.update("UPDATE passagens.alerta SET rota = :nome, chave = :nome || substr(chave, length(:antigo) + 1) "
                    + "WHERE rota = :antigo", p);
        }
        return buscar(id);
    }

    /** Remove a rota (o robô para de buscar). Com {@code historico}, apaga também os preços e alertas dela. */
    @Transactional
    public void remover(long id, boolean historico) {
        String nome = nome(id);
        MapSqlParameterSource p = new MapSqlParameterSource("id", id).addValue("nome", nome);
        jdbc.update("DELETE FROM passagens.rota WHERE id = :id", p);
        if (historico) {
            jdbc.update("DELETE FROM passagens.preco WHERE rota = :nome", p);
            jdbc.update("DELETE FROM passagens.alerta WHERE rota = :nome", p);
        }
    }

    // ---- validação ----

    /** Valida e devolve a configuração no formato gravado (o mesmo que a tela lê). */
    static Map<String, Object> validar(RotaPassagemInput in, LocalDate hoje) {
        if (in == null) throw new IllegalArgumentException("Envie os dados da rota");
        String nome = in.nome() == null ? "" : in.nome().trim().replaceAll("\\s+", " ");
        if (nome.isEmpty() || nome.length() > 80) throw new IllegalArgumentException("Informe um nome de até 80 letras");
        List<String> origem = aeroportos(in.origem(), "origem");
        List<String> destino = aeroportos(in.destino(), "destino");
        if (origem.stream().anyMatch(destino::contains)) {
            throw new IllegalArgumentException("Origem e destino não podem ter o mesmo aeroporto");
        }

        boolean temDatas = in.datas() != null && !in.datas().isEmpty();
        if (temDatas == (in.janela() != null)) {
            throw new IllegalArgumentException("Informe datas fixas ou um período flexível (um dos dois)");
        }
        List<Map<String, Object>> datas = new ArrayList<>();
        Map<String, Object> janela = null;
        boolean somenteIda;
        if (temDatas) {
            if (in.datas().size() > MAX_DATAS) throw new IllegalArgumentException("No máximo " + MAX_DATAS + " datas");
            for (Data d : in.datas()) {
                if (d == null || d.ida() == null) throw new IllegalArgumentException("Informe a data de ida");
                if (!d.ida().isAfter(hoje)) throw new IllegalArgumentException("A ida precisa ser depois de hoje");
                if (d.volta() != null && d.volta().isBefore(d.ida())) {
                    throw new IllegalArgumentException("A volta não pode ser antes da ida");
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ida", d.ida().toString());
                m.put("volta", d.volta() == null ? null : d.volta().toString());
                datas.add(m);
            }
            somenteIda = datas.stream().allMatch(m -> m.get("volta") == null);
        } else {
            janela = janela(in.janela(), hoje);
            somenteIda = !janela.containsKey("duracao");
        }

        Integer precoAlvo = faixa(in.precoAlvo(), 1, 1_000_000, "Preço-alvo");
        Integer maxEscalas = faixa(in.maxEscalas(), 0, 3, "Máximo de escalas");
        Integer quedaPct = in.quedaPct() == null ? 25 : faixa(in.quedaPct(), 1, 90, "Queda para alerta (%)");
        Integer adultos = in.adultos() == null ? 1 : faixa(in.adultos(), 1, 9, "Adultos");
        int criancas = in.criancas() == null ? 0 : faixa(in.criancas(), 0, 8, "Crianças");
        int bebesAssento = in.bebesAssento() == null ? 0 : faixa(in.bebesAssento(), 0, 8, "Bebês com assento");
        int bebesColo = in.bebesColo() == null ? 0 : faixa(in.bebesColo(), 0, 8, "Bebês no colo");
        if (adultos + criancas + bebesAssento + bebesColo > 9) {
            throw new IllegalArgumentException("No máximo 9 passageiros no total");
        }
        if (bebesColo > adultos) throw new IllegalArgumentException("Cada bebê no colo precisa de um adulto");

        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("nome", nome);
        cfg.put("ativa", in.ativa() == null || in.ativa());
        cfg.put("origem", origem);
        cfg.put("destino", destino);
        cfg.put("datas", datas);
        cfg.put("janela", janela);
        cfg.put("somenteIda", somenteIda);
        cfg.put("precoAlvo", precoAlvo);
        cfg.put("maxEscalas", maxEscalas);
        cfg.put("quedaPct", quedaPct);
        cfg.put("adultos", adultos);
        cfg.put("criancas", criancas);
        cfg.put("bebesAssento", bebesAssento);
        cfg.put("bebesColo", bebesColo);
        return cfg;
    }

    private static Map<String, Object> janela(Janela j, LocalDate hoje) {
        if (j.inicio() == null || j.fim() == null) throw new IllegalArgumentException("Informe o início e o fim do período");
        if (!j.fim().isAfter(hoje)) throw new IllegalArgumentException("O período precisa terminar depois de hoje");
        if (j.fim().isBefore(j.inicio())) throw new IllegalArgumentException("O fim do período não pode ser antes do início");
        if (j.inicio().plusDays(MAX_DIAS_JANELA).isBefore(j.fim())) {
            throw new IllegalArgumentException("O período pode ter no máximo " + MAX_DIAS_JANELA + " dias");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("inicio", j.inicio().toString());
        m.put("fim", j.fim().toString());
        m.put("passoDias", j.passoDias() == null ? 1 : faixa(j.passoDias(), 1, 30, "Intervalo entre idas"));
        if (j.duracao() != null && !j.duracao().isEmpty()) {
            if (j.duracao().size() != 2 || j.duracao().stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalArgumentException("Duração da viagem: informe o mínimo e o máximo de dias");
            }
            int min = faixa(j.duracao().get(0), 1, 60, "Duração mínima");
            int max = faixa(j.duracao().get(1), 1, 60, "Duração máxima");
            if (max < min) throw new IllegalArgumentException("A duração máxima não pode ser menor que a mínima");
            m.put("duracao", List.of(min, max));
        }
        if (j.diasSemana() != null && !j.diasSemana().isEmpty()) {
            LinkedHashSet<Integer> dias = new LinkedHashSet<>();
            for (Integer d : j.diasSemana()) {
                if (d == null) throw new IllegalArgumentException("Dia da semana inválido (use de 0 = segunda a 6 = domingo)");
                dias.add(faixa(d, 0, 6, "Dia da semana"));
            }
            m.put("diasSemana", dias.stream().sorted().toList());
        }
        return m;
    }

    private static List<String> aeroportos(List<String> codigos, String campo) {
        if (codigos == null) codigos = List.of();
        LinkedHashSet<String> r = new LinkedHashSet<>();
        for (String c : codigos) {
            String v = c == null ? "" : c.trim().toUpperCase();
            if (v.isEmpty()) continue;
            if (!IATA.matcher(v).matches()) {
                throw new IllegalArgumentException("Aeroporto inválido em " + campo + ": \"" + c + "\" (use o código de 3 letras, ex.: GRU)");
            }
            r.add(v);
        }
        if (r.isEmpty()) throw new IllegalArgumentException("Informe pelo menos um aeroporto de " + campo);
        if (r.size() > MAX_AEROPORTOS) throw new IllegalArgumentException("No máximo " + MAX_AEROPORTOS + " aeroportos de " + campo);
        return List.copyOf(r);
    }

    private static Integer faixa(Integer v, int min, int max, String campo) {
        if (v == null) return null;
        if (v < min || v > max) throw new IllegalArgumentException(campo + " deve ficar entre " + min + " e " + max);
        return v;
    }

    // ---- banco ----

    private MapSqlParameterSource parametros(Map<String, Object> cfg) {
        return new MapSqlParameterSource("nome", cfg.get("nome"))
                .addValue("ativa", cfg.get("ativa"))
                .addValue("config", JsonMapper.shared().writeValueAsString(cfg));
    }

    private boolean existe(String nome, Long ignorarId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM passagens.rota WHERE lower(nome) = lower(:nome) AND (:id IS NULL OR id <> :id)",
                new MapSqlParameterSource("nome", nome).addValue("id", ignorarId, java.sql.Types.BIGINT), Integer.class);
        return n != null && n > 0;
    }

    private String nome(long id) {
        List<String> r = jdbc.queryForList("SELECT nome FROM passagens.rota WHERE id = :id",
                new MapSqlParameterSource("id", id), String.class);
        if (r.isEmpty()) throw new RecursoNaoEncontradoException("Rota " + id + " não encontrada");
        return r.get(0);
    }

    private Map<String, Object> buscar(Long id) {
        return jdbc.query("SELECT (config || jsonb_build_object('id', id))::text FROM passagens.rota WHERE id = :id",
                new MapSqlParameterSource("id", id), (rs, i) -> PassagensService.json(rs.getString(1))).get(0);
    }
}
