package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.input.PessoaInput;
import com.kowalski.casaapi.business.auth.AuthService;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.business.enumerator.Papel;
import com.kowalski.casaapi.business.model.Pessoa;
import com.kowalski.casaapi.business.repository.PessoaRepository;
import com.kowalski.casaapi.support.Login;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PessoaServiceImplTest {

    private final PessoaRepository repository = mock(PessoaRepository.class);
    private final AuthService authService = mock(AuthService.class);
    private final PessoaServiceImpl service = new PessoaServiceImpl(repository, authService, new UsuarioLogado());

    @BeforeEach
    void setUp() {
        Login.comoAdmin();
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(authService.hash("segredo123")).thenReturn("HASH");
    }

    @AfterEach
    void tearDown() {
        Login.sair();
    }

    @Test
    void listaEmOrdemAlfabeticaSemDiferenciarMaiusculas() {
        when(repository.findAll()).thenReturn(List.of(
                Login.pessoa(1, "bruno"), Login.pessoa(2, "Ana"), Login.pessoa(3, "CARLA")));

        assertThat(service.listar()).extracting(Pessoa::getNome).containsExactly("Ana", "bruno", "CARLA");
    }

    @Test
    void criarComSenhaDaAcessoEPadraoUsuarioAtivo() {
        Pessoa p = service.criar(new PessoaInput(" Ana ", " ana@x.com ", "segredo123", null, null));

        assertThat(p.getNome()).isEqualTo("Ana");
        assertThat(p.getEmail()).isEqualTo("ana@x.com");
        assertThat(p.getSenhaHash()).isEqualTo("HASH");
        assertThat(p.getPapel()).isEqualTo(Papel.USUARIO);
        assertThat(p.isAtivo()).isTrue();
        assertThat(p.isTemAcesso()).isTrue();
    }

    @Test
    void senhaEmBrancoMantemAAtual() {
        Pessoa p = Login.pessoa(Login.ANA_ID, "ANA");
        p.setSenhaHash("ANTIGA");

        service.atualizar(p, new PessoaInput("Ana", "ana@x.com", "", Papel.USUARIO, true));

        assertThat(p.getSenhaHash()).isEqualTo("ANTIGA");
        verify(authService, never()).hash(any());
    }

    @Test
    void emailRepetidoNaoPode() {
        when(repository.findByEmailIgnoreCase("ana@x.com")).thenReturn(Optional.of(Login.pessoa(9, "OUTRA")));

        assertThatThrownBy(() -> service.criar(new PessoaInput("Ana", "ana@x.com", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Já existe uma pessoa com este e-mail.");
    }

    @Test
    void manterOProprioEmailNaEdicaoPode() {
        Pessoa ana = Login.pessoa(Login.ANA_ID, "ANA");
        when(repository.findByEmailIgnoreCase("ana@x.com")).thenReturn(Optional.of(ana));

        service.atualizar(ana, new PessoaInput("Ana", "ana@x.com", null, null, null));

        verify(repository).save(ana);
    }

    @Test
    void adminNaoTiraOProprioAcessoNemSeExclui() {
        Pessoa eu = Login.pessoa(Login.ADMIN_ID, "ADMIN");

        assertThatThrownBy(() -> service.atualizar(eu, new PessoaInput("Admin", "a@x.com", null, Papel.USUARIO, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.atualizar(eu, new PessoaInput("Admin", "a@x.com", null, null, false)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deletar(eu)).isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void nomeEEmailSaoObrigatorios() {
        assertThatThrownBy(() -> service.criar(new PessoaInput(" ", "a@x.com", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.criar(new PessoaInput("Ana", null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
