package com.kowalski.casaapi.pessoa;

import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import com.kowalski.casaapi.support.Login;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PessoaServiceTest {

    private final PessoaRepository repository = mock(PessoaRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final PessoaService service = new PessoaService(repository, encoder, new UsuarioLogado());

    @BeforeEach
    void setUp() {
        Login.comoAdmin();
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(encoder.encode("segredo123")).thenReturn("HASH");
    }

    @AfterEach
    void tearDown() {
        Login.sair();
    }

    private Pessoa cadastrada(long id, String nome) {
        Pessoa pessoa = Login.pessoa(id, nome);
        when(repository.findById(id)).thenReturn(Optional.of(pessoa));
        return pessoa;
    }

    @Test
    void listaEmOrdemAlfabeticaSemDiferenciarMaiusculas() {
        when(repository.findAll()).thenReturn(List.of(
                Login.pessoa(1, "bruno"), Login.pessoa(2, "Ana"), Login.pessoa(3, "CARLA")));

        assertThat(service.listar()).extracting(PessoaResponse::nome).containsExactly("Ana", "bruno", "CARLA");
    }

    @Test
    void criarComSenhaDaAcessoEPadraoUsuarioAtivo() {
        PessoaResponse p = service.criar(new PessoaInput(" Ana ", " ana@x.com ", "segredo123", null, null));

        assertThat(p.nome()).isEqualTo("Ana");
        assertThat(p.email()).isEqualTo("ana@x.com");
        assertThat(p.papel()).isEqualTo(Papel.USUARIO);
        assertThat(p.ativo()).isTrue();
        assertThat(p.temAcesso()).isTrue();
        verify(repository).save(argThat(salva -> "HASH".equals(salva.getSenhaHash())));
    }

    @Test
    void senhaEmBrancoMantemAAtual() {
        Pessoa ana = cadastrada(Login.ANA_ID, "ANA");
        ana.setSenhaHash("ANTIGA");

        service.atualizar(Login.ANA_ID, new PessoaInput("Ana", "ana@x.com", "", Papel.USUARIO, true));

        assertThat(ana.getSenhaHash()).isEqualTo("ANTIGA");
        verify(encoder, never()).encode(any());
    }

    @Test
    void nomeSemPessoaCadastradaERecusado() {
        when(repository.findByNomeIgnoreCase("ZECA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorNome(" ZECA "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Não existe pessoa cadastrada com o nome ZECA");
    }

    @Test
    void nomeDePessoaCadastradaPassaSemDiferenciarMaiusculas() {
        Pessoa ana = Login.pessoa(Login.ANA_ID, "ANA");
        when(repository.findByNomeIgnoreCase("ana")).thenReturn(Optional.of(ana));

        assertThat(service.buscarPorNome(" ana ")).isSameAs(ana);
    }

    @Test
    void nomeRepetidoNaoPode() {
        when(repository.findByNomeIgnoreCase("Ana")).thenReturn(Optional.of(Login.pessoa(9, "ANA")));

        assertThatThrownBy(() -> service.criar(new PessoaInput(" Ana ", "nova@x.com", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Já existe uma pessoa com este nome.");
        verify(repository, never()).save(any());
    }

    @Test
    void manterOProprioNomeNaEdicaoPode() {
        Pessoa ana = cadastrada(Login.ANA_ID, "ANA");
        when(repository.findByNomeIgnoreCase("ANA")).thenReturn(Optional.of(ana));

        service.atualizar(Login.ANA_ID, new PessoaInput("ANA", "ana@x.com", null, null, null));

        verify(repository).save(ana);
    }

    @Test
    void emailRepetidoNaoPode() {
        when(repository.findByEmailIgnoreCase("ana@x.com")).thenReturn(Optional.of(Login.pessoa(9, "OUTRA")));

        assertThatThrownBy(() -> service.criar(new PessoaInput("Ana", "ana@x.com", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Já existe uma pessoa com este e-mail.");
    }

    @Test
    void trocarParaOEmailDeOutraPessoaNaoAlteraNada() {
        Pessoa ana = cadastrada(Login.ANA_ID, "ANA");
        String emailDaAna = ana.getEmail();
        when(repository.findByEmailIgnoreCase("bruno@x.com")).thenReturn(Optional.of(Login.pessoa(Login.BRUNO_ID, "BRUNO")));

        assertThatThrownBy(() -> service.atualizar(Login.ANA_ID, new PessoaInput("Ana Nova", " bruno@x.com ", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Já existe uma pessoa com este e-mail.");

        assertThat(ana.getEmail()).isEqualTo(emailDaAna);
        assertThat(ana.getNome()).isEqualTo("ANA");
        verify(repository, never()).save(any());
    }

    @Test
    void manterOProprioEmailNaEdicaoPode() {
        Pessoa ana = cadastrada(Login.ANA_ID, "ANA");
        when(repository.findByEmailIgnoreCase("ana@x.com")).thenReturn(Optional.of(ana));

        service.atualizar(Login.ANA_ID, new PessoaInput("Ana", "ana@x.com", null, null, null));

        verify(repository).save(ana);
    }

    @Test
    void adminNaoTiraOProprioAcessoNemSeExclui() {
        cadastrada(Login.ADMIN_ID, "ADMIN");

        assertThatThrownBy(() -> service.atualizar(Login.ADMIN_ID, new PessoaInput("Admin", "a@x.com", null, Papel.USUARIO, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.atualizar(Login.ADMIN_ID, new PessoaInput("Admin", "a@x.com", null, null, false)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.excluir(Login.ADMIN_ID)).isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void pessoaInexistenteDevolveNaoEncontrada() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, new PessoaInput("Ana", "a@x.com", null, null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.excluir(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void garantirQueExisteDa404ComOId() {
        when(repository.existsById(1L)).thenReturn(true);

        service.garantirQueExiste(1L);
        assertThatThrownBy(() -> service.garantirQueExiste(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class).hasMessage("Pessoa 99 não encontrada");
    }

    @Test
    void referenciaDevolveAPessoaOuDa404ComOId() {
        Pessoa ana = cadastrada(Login.ANA_ID, "ANA");
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.referencia(Login.ANA_ID)).isSameAs(ana);
        assertThatThrownBy(() -> service.referencia(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class).hasMessage("Pessoa 99 não encontrada");
    }

    @Test
    void porEmailIgnoraEspacos() {
        Pessoa ana = Login.pessoa(Login.ANA_ID, "ANA");
        when(repository.findByEmailIgnoreCase("ana@x.com")).thenReturn(Optional.of(ana));

        assertThat(service.porEmail("  ana@x.com ")).containsSame(ana);
    }

    @Test
    void definirComoAdminGravaASenhaEOPapel() {
        Pessoa ana = Login.pessoa(Login.ANA_ID, "ANA");

        service.definirComoAdmin(ana, "segredo123");

        assertThat(ana.getSenhaHash()).isEqualTo("HASH");
        assertThat(ana.ehAdmin()).isTrue();
        verify(repository).save(ana);
    }

    @Test
    void excluiOutraPessoa() {
        Pessoa ana = cadastrada(Login.ANA_ID, "ANA");

        service.excluir(Login.ANA_ID);

        verify(repository).delete(ana);
    }
}
