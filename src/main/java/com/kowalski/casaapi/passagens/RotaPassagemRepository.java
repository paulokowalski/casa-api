package com.kowalski.casaapi.passagens;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Types;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RotaPassagemRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public Long inserir(Map<String, Object> cfg) {
        return jdbc.queryForObject("""
                INSERT INTO passagens.rota (nome, ordem, ativa, config, atualizado_em)
                VALUES (:nome, (SELECT COALESCE(MAX(ordem), -1) + 1 FROM passagens.rota), :ativa, CAST(:config AS jsonb), now())
                RETURNING id""", parametros(cfg), Long.class);
    }

    public void atualizar(long id, Map<String, Object> cfg) {
        jdbc.update("""
                UPDATE passagens.rota SET nome = :nome, ativa = :ativa, config = CAST(:config AS jsonb), atualizado_em = now()
                WHERE id = :id""", parametros(cfg).addValue("id", id));
    }

    public void renomearHistorico(String antigo, String nome) {
        MapSqlParameterSource p = new MapSqlParameterSource("nome", nome).addValue("antigo", antigo);
        jdbc.update("UPDATE passagens.preco SET rota = :nome WHERE rota = :antigo", p);
        jdbc.update("UPDATE passagens.alerta SET rota = :nome, chave = :nome || substr(chave, length(:antigo) + 1) "
                + "WHERE rota = :antigo", p);
    }

    public void remover(long id) {
        jdbc.update("DELETE FROM passagens.rota WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public void apagarHistorico(String nome) {
        MapSqlParameterSource p = new MapSqlParameterSource("nome", nome);
        jdbc.update("DELETE FROM passagens.preco WHERE rota = :nome", p);
        jdbc.update("DELETE FROM passagens.alerta WHERE rota = :nome", p);
    }

    public boolean existe(String nome, Long ignorarId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM passagens.rota WHERE lower(nome) = lower(:nome) AND (:id IS NULL OR id <> :id)",
                new MapSqlParameterSource("nome", nome).addValue("id", ignorarId, Types.BIGINT), Integer.class);
        return n != null && n > 0;
    }

    public Optional<String> nome(long id) {
        return jdbc.queryForList("SELECT nome FROM passagens.rota WHERE id = :id",
                new MapSqlParameterSource("id", id), String.class).stream().findFirst();
    }

    public Map<String, Object> buscar(Long id) {
        return jdbc.query("SELECT (config || jsonb_build_object('id', id))::text FROM passagens.rota WHERE id = :id",
                new MapSqlParameterSource("id", id), (rs, i) -> PassagensRepository.json(rs.getString(1))).get(0);
    }

    private static MapSqlParameterSource parametros(Map<String, Object> cfg) {
        return new MapSqlParameterSource("nome", cfg.get("nome"))
                .addValue("ativa", cfg.get("ativa"))
                .addValue("config", JsonMapper.shared().writeValueAsString(cfg));
    }
}
