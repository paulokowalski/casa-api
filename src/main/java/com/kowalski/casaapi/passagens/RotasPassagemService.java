package com.kowalski.casaapi.passagens;

import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RotasPassagemService {

    private final RotaPassagemRepository repository;
    private final Clock relogio;

    @Transactional
    public Map<String, Object> criar(RotaPassagemInput in) {
        Map<String, Object> cfg = ConfiguracaoDaRota.validar(in, LocalDate.now(relogio));
        garantirNomeLivre((String) cfg.get("nome"), null);
        return repository.buscar(repository.inserir(cfg));
    }

    @Transactional
    public Map<String, Object> atualizar(long id, RotaPassagemInput in) {
        String antigo = nome(id);
        Map<String, Object> cfg = ConfiguracaoDaRota.validar(in, LocalDate.now(relogio));
        String nome = (String) cfg.get("nome");
        garantirNomeLivre(nome, id);
        repository.atualizar(id, cfg);
        if (!antigo.equals(nome)) repository.renomearHistorico(antigo, nome);
        return repository.buscar(id);
    }

    @Transactional
    public void remover(long id, boolean historico) {
        String nome = nome(id);
        repository.remover(id);
        if (historico) repository.apagarHistorico(nome);
    }

    private void garantirNomeLivre(String nome, Long id) {
        if (repository.existe(nome, id)) throw new IllegalArgumentException("Já existe uma rota chamada \"" + nome + "\"");
    }

    private String nome(long id) {
        return repository.nome(id).orElseThrow(() -> new RecursoNaoEncontradoException("Rota " + id + " não encontrada"));
    }
}
