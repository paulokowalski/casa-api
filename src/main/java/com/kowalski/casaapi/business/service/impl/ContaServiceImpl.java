package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.input.ContaInput;
import com.kowalski.casaapi.api.v1.input.LancamentoInput;
import com.kowalski.casaapi.api.v1.response.ContaResponse;
import com.kowalski.casaapi.api.v1.response.LancamentoResponse;
import com.kowalski.casaapi.business.enumerator.TipoConta;
import com.kowalski.casaapi.business.exception.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.model.Conta;
import com.kowalski.casaapi.business.model.ContaLancamento;
import com.kowalski.casaapi.business.repository.ContaLancamentoRepository;
import com.kowalski.casaapi.business.repository.ContaRepository;
import com.kowalski.casaapi.business.repository.PessoaRepository;
import com.kowalski.casaapi.business.service.CategoriasService;
import com.kowalski.casaapi.business.service.ContaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static com.kowalski.casaapi.util.Competencias.*;

@Service
@RequiredArgsConstructor
public class ContaServiceImpl implements ContaService {

    /** Limite de meses por consulta de lançamentos */
    private static final int MAX_MESES_CONSULTA = 36;

    private final ContaRepository contaRepository;
    private final ContaLancamentoRepository lancamentoRepository;
    private final PessoaRepository pessoaRepository;
    private final CategoriasService categoriasService;

    // ------------------------------------------------------------------ contas

    @Override
    @Transactional(readOnly = true)
    public List<ContaResponse> listar(Long pessoaId) {
        Assert.notNull(pessoaId, "A pessoa é obrigatória");
        LocalDate mesAtual = atual();
        return contaRepository.findByPessoaIdOrderByDescricaoAsc(pessoaId).stream()
                .map(c -> paraResponse(c, mesAtual))
                .toList();
    }

    @Override
    @Transactional
    public ContaResponse criar(ContaInput input) {
        Conta conta = new Conta();
        aplicar(conta, input);
        return paraResponse(contaRepository.save(conta), atual());
    }

    @Override
    @Transactional
    public ContaResponse editar(UUID id, ContaInput input, LocalDate aPartirDe) {
        Conta conta = buscarConta(id);
        aplicar(conta, input);
        contaRepository.save(conta);

        // Meses não pagos são gerados de novo, com os dados atuais, na próxima consulta
        LocalDate desde = aPartirDe != null ? aPartirDe : atual();
        lancamentoRepository.removerNaoPagosAPartirDe(id, desde);
        lancamentoRepository.removerNaoPagosForaDoPeriodo(id, conta.getCompetenciaInicio(), fimOuSemFim(conta));
        return paraResponse(conta, atual());
    }

    @Override
    @Transactional
    public void excluir(UUID id, LocalDate aPartirDe) {
        Conta conta = buscarConta(id);
        if (aPartirDe == null || !aPartirDe.isAfter(conta.getCompetenciaInicio())) {
            contaRepository.delete(conta); // lançamentos saem por ON DELETE CASCADE
            return;
        }
        LocalDate novoFim = aPartirDe.minusMonths(1);
        if (conta.getCompetenciaFim() == null || conta.getCompetenciaFim().isAfter(novoFim)) {
            conta.setCompetenciaFim(novoFim);
            contaRepository.save(conta);
        }
        lancamentoRepository.removerNaoPagosAPartirDe(id, aPartirDe);
    }

    // ------------------------------------------------------------- lançamentos

    @Override
    @Transactional
    public List<LancamentoResponse> lancamentos(Long pessoaId, LocalDate de, LocalDate ate) {
        Assert.notNull(pessoaId, "A pessoa é obrigatória");
        Assert.isTrue(!ate.isBefore(de), "O fim do período deve ser depois do início");
        Assert.isTrue(mesesEntre(de, ate) < MAX_MESES_CONSULTA, "Consulte no máximo " + MAX_MESES_CONSULTA + " meses por vez");

        List<Conta> contas = contaRepository.ativasNoPeriodo(pessoaId, de, ate);
        if (contas.isEmpty()) return List.of();

        Map<UUID, Conta> porId = new HashMap<>();
        contas.forEach(c -> porId.put(c.getId(), c));
        List<ContaLancamento> lancamentos = new ArrayList<>(lancamentoRepository.doPeriodo(porId.keySet(), de, ate));

        Set<String> existentes = new HashSet<>();
        lancamentos.forEach(l -> existentes.add(chave(l.getConta().getId(), l.getCompetencia())));

        List<ContaLancamento> novos = new ArrayList<>();
        for (Conta conta : contas) {
            LocalDate inicio = conta.getCompetenciaInicio().isAfter(de) ? conta.getCompetenciaInicio() : de;
            LocalDate fim = fimOuSemFim(conta).isBefore(ate) ? fimOuSemFim(conta) : ate;
            for (LocalDate mes = inicio; !mes.isAfter(fim); mes = mes.plusMonths(1)) {
                if (!existentes.contains(chave(conta.getId(), mes))) novos.add(gerar(conta, mes));
            }
        }
        lancamentos.addAll(lancamentoRepository.saveAll(novos));

        // Débito automático: considera paga quando o vencimento chega
        LocalDate hoje = LocalDate.now();
        lancamentos.stream()
                .filter(l -> l.getConta().isDebitoAutomatico() && !l.isPaga() && !l.isBaixaManual()
                        && !l.getVencimento().isAfter(hoje))
                .forEach(l -> {
                    l.setPaga(true);
                    l.setDataPagamento(l.getVencimento());
                });

        return lancamentos.stream()
                .sorted(Comparator.comparing(ContaLancamento::getCompetencia)
                        .thenComparing(ContaLancamento::getVencimento)
                        .thenComparing(l -> l.getConta().getDescricao(), String.CASE_INSENSITIVE_ORDER))
                .map(this::paraResponse)
                .toList();
    }

    @Override
    @Transactional
    public LancamentoResponse atualizarLancamento(UUID id, LancamentoInput input) {
        Assert.notNull(input, "Informe o que alterar");
        ContaLancamento lancamento = lancamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento " + id + " não encontrado"));

        if (input.valor() != null) {
            Assert.isTrue(input.valor().signum() > 0, "O valor deve ser maior que zero");
            lancamento.setValor(input.valor());
            lancamento.setValorEstimado(false);
            if (lancamento.getConta().isValorVariavel()) {
                lancamentoRepository.atualizarEstimativasDepoisDe(
                        lancamento.getConta().getId(), lancamento.getCompetencia(), input.valor());
            }
        }
        if (input.paga() != null) {
            lancamento.setPaga(input.paga());
            lancamento.setBaixaManual(true);
            lancamento.setDataPagamento(input.paga()
                    ? Objects.requireNonNullElse(input.dataPagamento(), LocalDate.now())
                    : null);
        } else if (input.dataPagamento() != null && lancamento.isPaga()) {
            lancamento.setDataPagamento(input.dataPagamento());
        }
        return paraResponse(lancamentoRepository.save(lancamento));
    }

    // ---------------------------------------------------------------- internos

    private Conta buscarConta(UUID id) {
        return contaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta " + id + " não encontrada"));
    }

    /** Valida o input e copia para a conta, calculando o último mês. */
    private void aplicar(Conta conta, ContaInput in) {
        Assert.notNull(in, "Os dados da conta são obrigatórios");
        Assert.notNull(in.pessoaId(), "A pessoa é obrigatória");
        Assert.hasText(in.descricao(), "A descrição é obrigatória");
        Assert.isTrue(in.descricao().trim().length() <= 120, "A descrição deve ter até 120 caracteres");
        Assert.notNull(in.tipo(), "O tipo é obrigatório (RECORRENTE, PARCELADA ou UNICA)");
        Assert.isTrue(in.valor() != null && in.valor().signum() > 0, "O valor deve ser maior que zero");
        Assert.isTrue(in.diaVencimento() != null && in.diaVencimento() >= 1 && in.diaVencimento() <= 31,
                "O dia do vencimento deve estar entre 1 e 31");
        Assert.isTrue(in.jurosMes() == null || in.jurosMes().signum() >= 0, "Os juros não podem ser negativos");

        LocalDate inicio = parse(in.competenciaInicio(), "competenciaInicio");
        LocalDate fim;
        Integer parcelaInicial = null;
        Integer totalParcelas = null;

        switch (in.tipo()) {
            case PARCELADA -> {
                Assert.isTrue(in.totalParcelas() != null && in.totalParcelas() >= 1, "Informe o total de parcelas");
                parcelaInicial = Objects.requireNonNullElse(in.parcelaInicial(), 1);
                totalParcelas = in.totalParcelas();
                Assert.isTrue(parcelaInicial >= 1 && parcelaInicial <= totalParcelas,
                        "A parcela atual deve estar entre 1 e o total de parcelas");
                fim = inicio.plusMonths((long) totalParcelas - parcelaInicial);
            }
            case UNICA -> fim = inicio;
            default -> {
                fim = parseOuNulo(in.competenciaFim(), "competenciaFim");
                Assert.isTrue(fim == null || !fim.isBefore(inicio), "O último mês deve ser depois do primeiro");
            }
        }

        conta.setPessoa(pessoaRepository.findById(in.pessoaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa " + in.pessoaId() + " não encontrada")));
        conta.setCategoria(in.categoriaId() == null ? null : categoriasService.findByUUid(in.categoriaId()));
        conta.setDescricao(in.descricao().trim());
        conta.setTipo(in.tipo());
        conta.setValor(in.valor());
        conta.setDiaVencimento(in.diaVencimento());
        conta.setCompetenciaInicio(inicio);
        conta.setCompetenciaFim(fim);
        conta.setParcelaInicial(parcelaInicial);
        conta.setTotalParcelas(totalParcelas);
        conta.setJurosMes(in.jurosMes());
        conta.setValorVariavel(Boolean.TRUE.equals(in.valorVariavel()));
        conta.setDebitoAutomatico(Boolean.TRUE.equals(in.debitoAutomatico()));
    }

    private ContaLancamento gerar(Conta conta, LocalDate competencia) {
        ContaLancamento l = new ContaLancamento();
        l.setConta(conta);
        l.setCompetencia(competencia);
        l.setVencimento(vencimento(competencia, conta.getDiaVencimento()));
        if (conta.getTipo() == TipoConta.PARCELADA) {
            l.setNumeroParcela(parcelaNoMes(conta, competencia));
        }
        if (conta.isValorVariavel()) {
            // Estimativa: o último valor real informado, ou o valor cadastrado
            l.setValor(lancamentoRepository
                    .findFirstByContaIdAndCompetenciaBeforeAndValorEstimadoFalseOrderByCompetenciaDesc(conta.getId(), competencia)
                    .map(ContaLancamento::getValor)
                    .orElse(conta.getValor()));
            l.setValorEstimado(true);
        } else {
            l.setValor(conta.getValor());
        }
        return l;
    }

    private static int parcelaNoMes(Conta conta, LocalDate competencia) {
        return conta.getParcelaInicial() + (int) mesesEntre(conta.getCompetenciaInicio(), competencia);
    }

    private static LocalDate fimOuSemFim(Conta conta) {
        return conta.getCompetenciaFim() != null ? conta.getCompetenciaFim() : SEM_FIM;
    }

    private static String chave(UUID conta, LocalDate competencia) {
        return conta + "|" + competencia;
    }

    private ContaResponse paraResponse(Conta c, LocalDate mesAtual) {
        boolean ativa = c.getCompetenciaFim() == null || !c.getCompetenciaFim().isBefore(mesAtual);
        Integer parcelaAtual = null;
        Integer restantes = null;
        BigDecimal saldo = null;
        if (c.getTipo() == TipoConta.PARCELADA) {
            if (mesAtual.isBefore(c.getCompetenciaInicio())) {
                restantes = c.getTotalParcelas() - c.getParcelaInicial() + 1;
            } else if (ativa) {
                parcelaAtual = parcelaNoMes(c, mesAtual);
                restantes = c.getTotalParcelas() - parcelaAtual;
            } else {
                restantes = 0;
            }
            saldo = c.getValor().multiply(BigDecimal.valueOf(restantes));
        }
        return new ContaResponse(
                c.getId(),
                c.getPessoa().getId(),
                c.getCategoria() != null ? c.getCategoria().getId() : null,
                c.getCategoria() != null ? c.getCategoria().getNome() : null,
                c.getDescricao(),
                c.getTipo(),
                c.getValor(),
                c.getDiaVencimento(),
                formatar(c.getCompetenciaInicio()),
                formatar(c.getCompetenciaFim()),
                c.getParcelaInicial(),
                c.getTotalParcelas(),
                c.getJurosMes(),
                c.isValorVariavel(),
                c.isDebitoAutomatico(),
                ativa,
                parcelaAtual,
                restantes,
                saldo);
    }

    private LancamentoResponse paraResponse(ContaLancamento l) {
        Conta c = l.getConta();
        return new LancamentoResponse(
                l.getId(),
                c.getId(),
                c.getDescricao(),
                c.getCategoria() != null ? c.getCategoria().getNome() : null,
                c.getTipo(),
                formatar(l.getCompetencia()),
                l.getVencimento(),
                l.getNumeroParcela(),
                c.getTotalParcelas(),
                l.getValor(),
                l.isValorEstimado(),
                l.isPaga(),
                l.getDataPagamento(),
                c.isDebitoAutomatico(),
                c.isValorVariavel());
    }
}
