package com.kowalski.casaapi.compra;

import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.categoria.CategoriaService;
import com.kowalski.casaapi.comum.Competencias;
import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompraService {

    private static final String CARTAO_TODOS = "TODOS";
    static final int MAX_MESES_PERIODO = 36;

    private final CompraRepository compraRepository;
    private final TipoLancamentoRepository tipoLancamentoRepository;
    private final CategoriaService categoriaService;
    private final CompraParcelaRepository compraParcelaRepository;
    private final UsuarioLogado usuarioLogado;
    private final Clock relogio;

    public List<CompraRegistroResponse> buscarTodos(String pessoa, String cartao) {
        String nome = StringUtils.hasText(pessoa)
                ? usuarioLogado.nomePessoa(pessoa.trim())
                : (usuarioLogado.isAdmin() ? null : usuarioLogado.atual().getNome().trim());
        String cartaoFiltro = StringUtils.hasText(cartao) && !CARTAO_TODOS.equalsIgnoreCase(cartao.trim()) ? cartao.trim() : null;

        if (nome == null && cartaoFiltro == null) return compraRepository.listarRegistros();
        if (nome == null) return compraRepository.listarRegistrosDoCartao(cartaoFiltro);
        if (cartaoFiltro == null) return compraRepository.listarRegistrosDaPessoa(nome);
        return compraRepository.listarRegistrosDaPessoaECartao(nome, cartaoFiltro);
    }

    public CompraResponse buscarPorMesENome(String ano, String mes, String pessoa, String cartao, String ultimaParcelaSelecionado) {
        Assert.hasText(ano, "O ano é obrigatório");
        Assert.hasText(mes, "O mês é obrigatório");
        Assert.hasText(pessoa, "A pessoa é obrigatória");
        Assert.hasText(cartao, "O cartão é obrigatório");

        LocalDate inicio = Competencias.de(ano, mes);
        return CompraResponse.de(buscarParcelas(inicio, inicio.plusMonths(1), pessoa, cartao), ultimaParcelaSelecionado);
    }

    public CompraResponse buscarPorPeriodo(String de, String ate, String pessoa, String cartao) {
        Assert.hasText(pessoa, "A pessoa é obrigatória");
        LocalDate inicio = Competencias.parse(de, "de");
        LocalDate fim = Competencias.parse(ate, "ate");
        Assert.isTrue(!fim.isBefore(inicio), "O mês final deve ser igual ou posterior ao inicial");
        Assert.isTrue(Competencias.mesesEntre(inicio, fim) < MAX_MESES_PERIODO,
                "O período pode ter no máximo " + MAX_MESES_PERIODO + " meses");

        return CompraResponse.de(buscarParcelas(inicio, fim.plusMonths(1), pessoa, cartao), CARTAO_TODOS);
    }

    @Transactional
    public void salvar(CompraInput input) {
        validar(input);
        input.dividirEntrePessoas().forEach(daPessoa ->
                compraRepository.save(Compra.nova(dadosDe(daPessoa), LocalDateTime.now(relogio))));
    }

    @Transactional
    public void editar(UUID id, CompraInput input) {
        validar(input);
        Compra compra = buscarDoUsuario(id);
        usuarioLogado.nomePessoa(input.nomePessoaCompra());
        DadosDaCompra dados = dadosDe(input);

        compraParcelaRepository.deleteByCompraId(compra.getId());
        compra.alterar(dados);
        compraRepository.save(compra);
    }

    @Transactional
    public void remover(UUID id) {
        Assert.notNull(id, "O ID da compra é obrigatório");
        compraRepository.delete(buscarDoUsuario(id));
    }

    private static void validar(CompraInput input) {
        Assert.notNull(input, "Os dados da compra são obrigatórios");
        input.validar();
    }

    private DadosDaCompra dadosDe(CompraInput input) {
        return input.paraDados(buscarCartao(input.tipoLancamentoId()), categoriaService.buscar(input.categoriaId()));
    }

    private TipoLancamento buscarCartao(UUID id) {
        return tipoLancamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo Lançamento com uuid: " + id + " não encontrado!"));
    }

    private Compra buscarDoUsuario(UUID id) {
        return compraRepository.findById(id)
                .filter(c -> usuarioLogado.podeAcessarNome(c.getNomePessoaCompra()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Compra não encontrada"));
    }

    private List<CompraParcela> buscarParcelas(LocalDate inicio, LocalDate fim, String pessoa, String cartao) {
        if (StringUtils.hasText(cartao) && !CARTAO_TODOS.equalsIgnoreCase(cartao)) {
            return compraParcelaRepository.buscarPorPeriodoECartao(inicio, fim, pessoa, cartao);
        }
        return compraParcelaRepository.buscarPorPeriodo(inicio, fim, pessoa);
    }
}
