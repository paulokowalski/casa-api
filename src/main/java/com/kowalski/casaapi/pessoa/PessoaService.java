package com.kowalski.casaapi.pessoa;

import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioLogado usuarioLogado;

    @Transactional(readOnly = true)
    public List<PessoaResponse> listar() {
        return pessoaRepository.findAll().stream()
                .sorted(Comparator.comparing(Pessoa::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(PessoaResponse::de)
                .toList();
    }

    @Transactional
    public PessoaResponse criar(PessoaInput input) {
        Pessoa pessoa = new Pessoa();
        aplicar(pessoa, input);
        return PessoaResponse.de(pessoaRepository.save(pessoa));
    }

    @Transactional
    public PessoaResponse atualizar(Long id, PessoaInput input) {
        Pessoa pessoa = buscar(id);
        if (ehVoceMesmo(pessoa)) {
            Assert.isTrue(input == null || input.papel() == null || input.papel() == Papel.ADMIN,
                    "Você não pode remover o seu próprio acesso de admin.");
            Assert.isTrue(input == null || input.ativo() == null || input.ativo(), "Você não pode desativar a si mesmo.");
        }
        aplicar(pessoa, input);
        return PessoaResponse.de(pessoaRepository.save(pessoa));
    }

    @Transactional
    public void excluir(Long id) {
        Pessoa pessoa = buscar(id);
        Assert.isTrue(!ehVoceMesmo(pessoa), "Você não pode excluir a si mesmo.");
        pessoaRepository.delete(pessoa);
    }

    @Transactional(readOnly = true)
    public void garantirQueExiste(Long id) {
        if (!pessoaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Pessoa " + id + " não encontrada");
        }
    }

    @Transactional(readOnly = true)
    public Pessoa buscarPorNome(String nome) {
        Assert.hasText(nome, "O nome da pessoa é obrigatório");
        return pessoaRepository.findByNomeIgnoreCase(nome.trim())
                .orElseThrow(() -> new IllegalArgumentException("Não existe pessoa cadastrada com o nome " + nome.trim()));
    }

    @Transactional(readOnly = true)
    public Pessoa referencia(Long id) {
        return pessoaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa " + id + " não encontrada"));
    }

    @Transactional(readOnly = true)
    public Optional<Pessoa> porEmail(String email) {
        return pessoaRepository.findByEmailIgnoreCase(email.trim());
    }

    @Transactional(readOnly = true)
    public Optional<Pessoa> porId(Long id) {
        return pessoaRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Pessoa> porIds(Collection<Long> ids) {
        return pessoaRepository.findAllById(ids);
    }

    @Transactional
    public void definirComoAdmin(Pessoa pessoa, String senha) {
        pessoa.definirSenha(senha, passwordEncoder);
        pessoa.tornarAdmin();
        pessoaRepository.save(pessoa);
    }

    private Pessoa buscar(Long id) {
        return pessoaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada"));
    }

    private boolean ehVoceMesmo(Pessoa pessoa) {
        return pessoa.getId().equals(usuarioLogado.atual().getPessoaId());
    }

    private void aplicar(Pessoa pessoa, PessoaInput in) {
        Assert.notNull(in, "Os dados da pessoa são obrigatórios");
        Assert.hasText(in.nome(), "O nome é obrigatório");
        Assert.hasText(in.email(), "O e-mail é obrigatório");
        pessoaRepository.findByEmailIgnoreCase(in.email().trim())
                .filter(outra -> !outra.getId().equals(pessoa.getId()))
                .ifPresent(_ -> {
                    throw new IllegalArgumentException("Já existe uma pessoa com este e-mail.");
                });
        pessoaRepository.findByNomeIgnoreCase(in.nome().trim())
                .filter(outra -> !outra.getId().equals(pessoa.getId()))
                .ifPresent(_ -> {
                    throw new IllegalArgumentException("Já existe uma pessoa com este nome.");
                });
        pessoa.alterarDados(in.nome(), in.email());
        if (in.papel() != null) pessoa.setPapel(in.papel());
        if (in.ativo() != null) pessoa.setAtivo(in.ativo());
        if (in.senha() != null && !in.senha().isEmpty()) pessoa.definirSenha(in.senha(), passwordEncoder);
    }
}
