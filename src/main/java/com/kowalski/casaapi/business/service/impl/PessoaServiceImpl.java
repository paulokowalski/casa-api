package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.input.PessoaInput;
import com.kowalski.casaapi.business.auth.AuthService;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.business.enumerator.Papel;
import com.kowalski.casaapi.business.model.Pessoa;
import com.kowalski.casaapi.business.repository.PessoaRepository;
import com.kowalski.casaapi.business.service.PessoaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PessoaServiceImpl implements PessoaService {

    private final PessoaRepository pessoaRepository;
    private final AuthService authService;
    private final UsuarioLogado usuarioLogado;

    @Override
    public List<Pessoa> listar() {
        return pessoaRepository.findAll().stream()
                .sorted(Comparator.comparing(Pessoa::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    @Override
    public Optional<Pessoa> buscarPorId(Long id) {
        return pessoaRepository.findById(id);
    }

    @Override
    @Transactional
    public Pessoa criar(PessoaInput input) {
        Pessoa pessoa = new Pessoa();
        aplicar(pessoa, input);
        return pessoaRepository.save(pessoa);
    }

    @Override
    @Transactional
    public Pessoa atualizar(Pessoa pessoa, PessoaInput input) {
        boolean ehVoceMesmo = pessoa.getId().equals(usuarioLogado.atual().getPessoaId());
        if (ehVoceMesmo) {
            Assert.isTrue(input.papel() == null || input.papel() == Papel.ADMIN, "Você não pode remover o seu próprio acesso de admin.");
            Assert.isTrue(input.ativo() == null || input.ativo(), "Você não pode desativar a si mesmo.");
        }
        aplicar(pessoa, input);
        return pessoaRepository.save(pessoa);
    }

    @Override
    @Transactional
    public void deletar(Pessoa pessoa) {
        Assert.isTrue(!pessoa.getId().equals(usuarioLogado.atual().getPessoaId()), "Você não pode excluir a si mesmo.");
        pessoaRepository.delete(pessoa);
    }

    private void aplicar(Pessoa pessoa, PessoaInput in) {
        Assert.notNull(in, "Os dados da pessoa são obrigatórios");
        Assert.hasText(in.nome(), "O nome é obrigatório");
        Assert.hasText(in.email(), "O e-mail é obrigatório");
        String email = in.email().trim();
        pessoaRepository.findByEmailIgnoreCase(email)
                .filter(outra -> !outra.getId().equals(pessoa.getId()))
                .ifPresent(outra -> {
                    throw new IllegalArgumentException("Já existe uma pessoa com este e-mail.");
                });

        pessoa.setNome(in.nome().trim());
        pessoa.setEmail(email);
        if (in.papel() != null) pessoa.setPapel(in.papel());
        if (in.ativo() != null) pessoa.setAtivo(in.ativo());
        if (in.senha() != null && !in.senha().isEmpty()) pessoa.setSenhaHash(authService.hash(in.senha()));
    }
}
