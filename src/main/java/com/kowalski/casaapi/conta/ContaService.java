package com.kowalski.casaapi.conta;

import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.categoria.CategoriaService;
import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import com.kowalski.casaapi.pessoa.PessoaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.kowalski.casaapi.comum.Competencias.atual;
import static com.kowalski.casaapi.comum.Competencias.mesesEntre;

@Service
@RequiredArgsConstructor
public class ContaService {

    private static final int MAX_MESES_CONSULTA = 36;

    private final ContaRepository contaRepository;
    private final ContaLancamentoRepository lancamentoRepository;
    private final PessoaService pessoaService;
    private final CategoriaService categoriaService;
    private final UsuarioLogado usuarioLogado;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public List<ContaResponse> listar(Long pessoaId) {
        Assert.notNull(pessoaId, "A pessoa é obrigatória");
        LocalDate mesAtual = atual(relogio);
        return contaRepository.findByPessoaIdOrderByDescricaoAsc(pessoaId).stream()
                .map(c -> ContaResponse.de(c, mesAtual))
                .toList();
    }

    @Transactional
    public ContaResponse criar(ContaInput input) {
        validarDono(input);
        Conta conta = Conta.nova(input.paraDados(), LocalDateTime.now(relogio));
        vincular(conta, input);
        return ContaResponse.de(contaRepository.save(conta), atual(relogio));
    }

    @Transactional
    public ContaResponse editar(UUID id, ContaInput input, LocalDate aPartirDe) {
        Conta conta = buscarConta(id);
        validarDono(input);
        conta.alterar(input.paraDados());
        vincular(conta, input);
        contaRepository.save(conta);

        LocalDate desde = aPartirDe != null ? aPartirDe : atual(relogio);
        lancamentoRepository.removerNaoPagosAPartirDe(id, desde);
        lancamentoRepository.removerNaoPagosForaDoPeriodo(id, conta.getCompetenciaInicio(), conta.fimOuSemFim());
        return ContaResponse.de(conta, atual(relogio));
    }

    @Transactional
    public void excluir(UUID id, LocalDate aPartirDe) {
        Conta conta = buscarConta(id);
        if (conta.apagadaPorInteiroAoExcluirDesde(aPartirDe)) {
            contaRepository.delete(conta);
            return;
        }
        conta.encerrarAntesDe(aPartirDe);
        contaRepository.save(conta);
        lancamentoRepository.removerNaoPagosAPartirDe(id, aPartirDe);
    }

    @Transactional
    public List<LancamentoResponse> lancamentos(Long pessoaId, LocalDate de, LocalDate ate) {
        Assert.notNull(pessoaId, "A pessoa é obrigatória");
        Assert.isTrue(!ate.isBefore(de), "O fim do período deve ser depois do início");
        Assert.isTrue(mesesEntre(de, ate) < MAX_MESES_CONSULTA, "Consulte no máximo " + MAX_MESES_CONSULTA + " meses por vez");

        List<Conta> contas = contaRepository.ativasNoPeriodo(pessoaId, de, ate);
        if (contas.isEmpty()) return List.of();

        List<ContaLancamento> lancamentos = new ArrayList<>(
                lancamentoRepository.doPeriodo(contas.stream().map(Conta::getId).toList(), de, ate));
        lancamentos.addAll(lancamentoRepository.saveAll(lancamentosQueFaltam(contas, lancamentos, de, ate)));

        LocalDate hoje = LocalDate.now(relogio);
        lancamentos.stream().filter(l -> l.baixaAutomaticaPendenteEm(hoje)).forEach(ContaLancamento::baixarNoVencimento);

        return lancamentos.stream()
                .sorted(Comparator.comparing(ContaLancamento::getCompetencia)
                        .thenComparing(ContaLancamento::getVencimento)
                        .thenComparing(l -> l.getConta().getDescricao(), String.CASE_INSENSITIVE_ORDER))
                .map(LancamentoResponse::de)
                .toList();
    }

    @Transactional
    public LancamentoResponse atualizarLancamento(UUID id, LancamentoInput input) {
        Assert.notNull(input, "Informe o que alterar");
        ContaLancamento lancamento = lancamentoRepository.findById(id)
                .filter(l -> usuarioLogado.podeAcessar(l.getConta().getPessoa().getId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento " + id + " não encontrado"));

        if (input.valor() != null) {
            lancamento.registrarValorReal(input.valor());
            if (lancamento.getConta().isValorVariavel()) {
                lancamentoRepository.atualizarEstimativasDepoisDe(
                        lancamento.getConta().getId(), lancamento.getCompetencia(), input.valor());
            }
        }
        if (input.paga() != null) {
            lancamento.marcarPaga(input.paga(), input.dataPagamento(), LocalDate.now(relogio));
        } else if (input.dataPagamento() != null) {
            lancamento.corrigirDataDePagamento(input.dataPagamento());
        }
        return LancamentoResponse.de(lancamentoRepository.save(lancamento));
    }

    private List<ContaLancamento> lancamentosQueFaltam(List<Conta> contas, List<ContaLancamento> existentes,
                                                       LocalDate de, LocalDate ate) {
        record ContaNoMes(UUID conta, LocalDate competencia) {
        }
        Set<ContaNoMes> jaGerados = new HashSet<>();
        existentes.forEach(l -> jaGerados.add(new ContaNoMes(l.getConta().getId(), l.getCompetencia())));

        List<ContaLancamento> novos = new ArrayList<>();
        for (Conta conta : contas) {
            LocalDate inicio = conta.getCompetenciaInicio().isAfter(de) ? conta.getCompetenciaInicio() : de;
            LocalDate fim = conta.fimOuSemFim().isBefore(ate) ? conta.fimOuSemFim() : ate;
            for (LocalDate mes = inicio; !mes.isAfter(fim); mes = mes.plusMonths(1)) {
                if (!jaGerados.contains(new ContaNoMes(conta.getId(), mes))) {
                    novos.add(conta.lancamentoDe(mes, ultimoValorReal(conta, mes)));
                }
            }
        }
        return novos;
    }

    private BigDecimal ultimoValorReal(Conta conta, LocalDate competencia) {
        if (!conta.isValorVariavel()) return null;
        return lancamentoRepository
                .findFirstByContaIdAndCompetenciaBeforeAndValorEstimadoFalseOrderByCompetenciaDesc(conta.getId(), competencia)
                .map(ContaLancamento::getValor)
                .orElse(null);
    }

    private Conta buscarConta(UUID id) {
        return contaRepository.findById(id)
                .filter(c -> usuarioLogado.podeAcessar(c.getPessoa().getId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta " + id + " não encontrada"));
    }

    private void validarDono(ContaInput input) {
        Assert.notNull(input, "Os dados da conta são obrigatórios");
        Assert.notNull(input.pessoaId(), "A pessoa é obrigatória");
        usuarioLogado.pessoaId(input.pessoaId());
    }

    private void vincular(Conta conta, ContaInput input) {
        conta.vincular(pessoaService.referencia(input.pessoaId()),
                input.categoriaId() == null ? null : categoriaService.buscar(input.categoriaId()));
    }
}
