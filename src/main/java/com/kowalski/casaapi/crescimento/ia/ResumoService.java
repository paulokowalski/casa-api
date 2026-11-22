package com.kowalski.casaapi.crescimento.ia;

import com.kowalski.casaapi.crescimento.comum.CrescimentoConfig;
import com.kowalski.casaapi.crescimento.comum.IaIndisponivelException;
import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.CriancaService;
import com.kowalski.casaapi.crescimento.crianca.Idade;
import com.kowalski.casaapi.crescimento.crianca.Sexo;
import com.kowalski.casaapi.crescimento.diario.Registro;
import com.kowalski.casaapi.crescimento.diario.RegistroRepository;
import com.kowalski.casaapi.crescimento.familia.AcessoFamilia;
import com.kowalski.casaapi.crescimento.ia.ResumoDtos.PerguntaInput;
import com.kowalski.casaapi.crescimento.ia.ResumoDtos.PerguntaResponse;
import com.kowalski.casaapi.crescimento.ia.ResumoDtos.ResumoResponse;
import com.kowalski.casaapi.crescimento.ia.ResumoDtos.ResumosResponse;
import com.kowalski.casaapi.crescimento.marco.CatalogoMarcos;
import com.kowalski.casaapi.crescimento.marco.MarcoAtingido;
import com.kowalski.casaapi.crescimento.marco.MarcoAtingidoRepository;
import com.kowalski.casaapi.crescimento.medida.AvaliacaoCrescimento;
import com.kowalski.casaapi.crescimento.medida.Medicao;
import com.kowalski.casaapi.crescimento.medida.MedicaoRepository;
import com.kowalski.casaapi.crescimento.medida.MedidaDtos.Alerta;
import com.kowalski.casaapi.crescimento.medida.MedidaDtos.MedicaoResponse;
import com.kowalski.casaapi.crescimento.medida.MedidaDtos.MedidaAvaliada;
import com.kowalski.casaapi.crescimento.vacina.VacinaAplicadaRepository;
import com.kowalski.casaapi.crescimento.vacina.VacinaDtos.DoseResponse;
import com.kowalski.casaapi.crescimento.vacina.VacinaDtos.SituacaoVacina;
import com.kowalski.casaapi.crescimento.vacina.VacinaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Resumo de um mês de vida com perguntas para a próxima consulta, gerado pela IA só quando alguém pede.
 * Vão para a IA só dados em texto (medidas, textos do diário, marcos e vacinas): nunca fotos nem links (este pacote não
 * usa o pacote foto; ArquiteturaCrescimentoTest confere), o nome da criança vira "a criança" e o dos profissionais
 * não vai. Limite de {@value #LIMITE_DIA_POR_CRIANCA} gerações por criança em 24 h.
 */
@Service
@RequiredArgsConstructor
public class ResumoService {

    static final int MAX_TEXTO = 4000;
    static final int MAX_PERGUNTAS = 6;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Quantos resumos uma criança pode gerar em 24 h (contando os refeitos). */
    static final int LIMITE_DIA_POR_CRIANCA = 5;
    /** Quantas perguntas à IA uma criança pode ter em 24 h. */
    static final int LIMITE_PERGUNTAS_DIA = 20;
    static final int PERGUNTA_MAX = 500;
    static final int CONVERSA_NO_CONTEXTO = 4;
    private static final Pattern LINK = Pattern.compile("(?i)\\b(?:https?|ftp|file|data):\\S+|\\bwww\\.\\S+");

    private final CriancaService criancas;
    private final AcessoFamilia acesso;
    private final ResumoMesRepository resumos;
    private final PerguntaIaRepository conversas;
    private final MedicaoRepository medicoes;
    private final RegistroRepository registros;
    private final MarcoAtingidoRepository marcos;
    private final VacinaAplicadaRepository vacinas;
    private final AvaliacaoCrescimento avaliacao;
    private final GeradorIa gerador;
    private final Clock relogio;
    private final Map<UUID, Deque<Instant>> usos = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Instant>> usosPerguntas = new ConcurrentHashMap<>();

    /** O que vai para a IA (tudo já filtrado e avaliado). */
    record Entrada(Crianca crianca, int mes, LocalDate hoje, List<MedicaoResponse> medidas, List<Registro> registros,
                   List<MarcoAtingido> marcos, List<DoseResponse> doses, List<Alerta> alertas) {
    }

    @Transactional(readOnly = true)
    public ResumosResponse listar(UUID criancaId) {
        Crianca c = criancas.carregar(criancaId, false);
        LocalDate hoje = LocalDate.now(relogio);
        Map<Integer, List<PerguntaResponse>> porMes = new HashMap<>();
        conversas.findByCriancaIdOrderByFeitaEmAsc(criancaId)
                .forEach(q -> porMes.computeIfAbsent(q.getMes(), k -> new ArrayList<>()).add(resposta(q)));
        return new ResumosResponse(gerador.disponivel(), Idade.mesesCompletos(c.getNascimento(), hoje),
                resumos.findByCriancaIdOrderByMesDesc(criancaId).stream().map(r -> resposta(r, porMes.getOrDefault(r.getMes(), List.of()))).toList());
    }

    /** Sem transação: a IA leva alguns segundos e não deve segurar conexão do banco. */
    public ResumoResponse gerar(UUID criancaId, int mes) {
        Crianca c = criancas.carregar(criancaId, true);
        LocalDate hoje = LocalDate.now(relogio);
        int atual = Idade.mesesCompletos(c.getNascimento(), hoje);
        if (mes < 0 || mes > atual) throw new IllegalArgumentException("Escolha um mês de vida entre o 1º e o atual (" + rotulo(atual) + ")");
        if (!gerador.disponivel()) throw new IaIndisponivelException("O resumo com IA não está configurado neste servidor.", null);

        Entrada e = entrada(c, mes, hoje, atual);
        reservar(usos, criancaId, LIMITE_DIA_POR_CRIANCA, "Limite de " + LIMITE_DIA_POR_CRIANCA + " resumos por dia para esta criança. Tente amanhã.");
        GeradorIa.Resultado r = gerador.resumirMes(prompt(e));

        String texto = r.resumo() == null ? "" : semLinks(r.resumo()).strip();
        if (texto.isBlank()) throw new IaIndisponivelException("A IA devolveu um resumo vazio. Tente de novo.", null);
        List<String> perguntas = r.perguntas() == null ? List.of() : r.perguntas().stream().filter(Objects::nonNull)
                .map(x -> semLinks(x).replace('\n', ' ').strip()).filter(x -> !x.isBlank()).limit(MAX_PERGUNTAS).toList();

        ResumoMes rm = resumos.findByCriancaIdAndMes(criancaId, mes).orElseGet(ResumoMes::new);
        rm.setCriancaId(criancaId);
        rm.setMes(mes);
        rm.setInicio(inicio(c, mes));
        rm.setFim(fim(c, mes));
        rm.setTexto(corta(texto));
        rm.setPerguntas(corta(String.join("\n", perguntas)));
        rm.setModelo(r.modelo());
        rm.setGeradoPor(acesso.usuarioId());
        rm.setGeradoEm(relogio.instant());
        return resposta(resumos.save(rm), conversas.findByCriancaIdAndMesOrderByFeitaEmAsc(criancaId, mes).stream().map(ResumoService::resposta).toList());
    }

    /**
     * Pergunta dos pais sobre a criança neste mês. Precisa do resumo do mês; a IA recebe os mesmos dados do resumo, o
     * resumo e as últimas perguntas, e recusa assuntos que não sejam a criança e seus cuidados. Sem transação.
     */
    public PerguntaResponse perguntar(UUID criancaId, int mes, PerguntaInput dados) {
        Crianca c = criancas.carregar(criancaId, true);
        String pergunta = dados == null || dados.pergunta() == null ? "" : dados.pergunta().strip();
        if (pergunta.length() < 3) throw new IllegalArgumentException("Escreva a pergunta");
        if (pergunta.length() > PERGUNTA_MAX) throw new IllegalArgumentException("A pergunta pode ter até " + PERGUNTA_MAX + " caracteres");
        ResumoMes rm = resumos.findByCriancaIdAndMes(criancaId, mes)
                .orElseThrow(() -> new NaoEncontradoException("Gere o resumo do " + rotulo(mes) + " antes de fazer perguntas"));
        if (!gerador.disponivel()) throw new IaIndisponivelException("A IA não está configurada neste servidor.", null);
        LocalDate hoje = LocalDate.now(relogio);
        List<PerguntaIa> anteriores = conversas.findByCriancaIdAndMesOrderByFeitaEmAsc(criancaId, mes);
        String contexto = contextoPergunta(prompt(entrada(c, mes, hoje, Idade.mesesCompletos(c.getNascimento(), hoje))), rm,
                anteriores.subList(Math.max(0, anteriores.size() - CONVERSA_NO_CONTEXTO), anteriores.size()));

        reservar(usosPerguntas, criancaId, LIMITE_PERGUNTAS_DIA, "Limite de " + LIMITE_PERGUNTAS_DIA + " perguntas por dia para esta criança. Tente amanhã.");
        GeradorIa.Resposta r = gerador.responder(contexto, anonimizar(pergunta, c.getNome()));
        if (r.foraDoAssunto())
            throw new IllegalArgumentException("Só respondo perguntas sobre " + primeiroNome(c.getNome())
                    + ": crescimento, desenvolvimento, saúde, alimentação, sono e cuidados.");
        String resposta = r.resposta() == null ? "" : semLinks(r.resposta()).strip();
        if (resposta.isBlank()) throw new IaIndisponivelException("A IA devolveu uma resposta vazia. Tente de novo.", null);

        PerguntaIa q = new PerguntaIa();
        q.setCriancaId(criancaId);
        q.setMes(mes);
        q.setPergunta(pergunta);
        q.setResposta(resposta.length() <= 3000 ? resposta : resposta.substring(0, 2999) + "…");
        q.setModelo(r.modelo());
        q.setFeitaPor(acesso.usuarioId());
        q.setFeitaEm(relogio.instant());
        return resposta(conversas.save(q));
    }

    @Transactional
    public void excluirPergunta(UUID criancaId, UUID id) {
        criancas.carregar(criancaId, true);
        PerguntaIa q = conversas.findById(id).filter(x -> x.getCriancaId().equals(criancaId))
                .orElseThrow(() -> new NaoEncontradoException("Pergunta não encontrada"));
        conversas.delete(q);
    }

    static String contextoPergunta(String dadosDoMes, ResumoMes rm, List<PerguntaIa> anteriores) {
        StringBuilder p = new StringBuilder(dadosDoMes);
        p.append("\nResumo do mês já entregue aos pais:\n").append(rm.getTexto()).append('\n');
        if (rm.getPerguntas() != null && !rm.getPerguntas().isBlank())
            p.append("Perguntas sugeridas para o pediatra:\n").append(rm.getPerguntas()).append('\n');
        if (!anteriores.isEmpty()) {
            p.append("\nPerguntas anteriores dos pais sobre este mês:\n");
            anteriores.forEach(q -> p.append("Pergunta: ").append(q.getPergunta().replace('\n', ' ')).append("\nResposta: ")
                    .append(q.getResposta().replace('\n', ' ')).append('\n'));
        }
        return p.toString();
    }

    private Entrada entrada(Crianca c, int mes, LocalDate hoje, int atual) {
        List<Medicao> todas = medicoes.findByCriancaIdOrderByDataAscCriadoEmAsc(c.getId());
        return new Entrada(c, mes, hoje,
                todas.stream().map(m -> avaliacao.resposta(c, m)).toList(),
                registros.findByCriancaIdOrderByDataDescCriadoEmDesc(c.getId()),
                marcos.findByCriancaId(c.getId()),
                VacinaService.montar(c, vacinas.findByCriancaId(c.getId()), hoje).grupos().stream().flatMap(g -> g.doses().stream()).toList(),
                mes == atual ? avaliacao.resumo(c, todas, hoje).alertas() : List.of());
    }

    private static String primeiroNome(String nome) {
        return nome == null || nome.isBlank() ? "a criança" : nome.strip().split("\\s+")[0];
    }

    @Transactional
    public void excluir(UUID criancaId, int mes) {
        criancas.carregar(criancaId, true);
        ResumoMes rm = resumos.findByCriancaIdAndMes(criancaId, mes).orElseThrow(() -> new NaoEncontradoException("Resumo não encontrado"));
        conversas.deleteByCriancaIdAndMes(criancaId, mes);
        resumos.delete(rm);
    }

    /** Conta um uso da criança; recusa se já passou do limite nas últimas 24 h. */
    void reservar(Map<UUID, Deque<Instant>> mapa, UUID criancaId, int limite, String mensagem) {
        Instant agora = relogio.instant();
        Deque<Instant> fila = mapa.computeIfAbsent(criancaId, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && fila.peekFirst().isBefore(agora.minus(Duration.ofDays(1)))) fila.pollFirst();
            if (fila.size() >= limite) throw new IaIndisponivelException(mensagem, null);
            fila.addLast(agora);
        }
    }

    /** Limite de resumos por criança (usado nos testes). */
    void reservar(UUID criancaId) {
        reservar(usos, criancaId, LIMITE_DIA_POR_CRIANCA, "Limite de resumos por dia");
    }

    static String semLinks(String s) {
        return s == null ? null : LINK.matcher(s).replaceAll("[link]");
    }

    static PerguntaResponse resposta(PerguntaIa q) {
        return new PerguntaResponse(q.getId(), q.getPergunta(), q.getResposta(), q.getFeitaEm());
    }

    static ResumoResponse resposta(ResumoMes r) {
        return resposta(r, List.of());
    }

    static ResumoResponse resposta(ResumoMes r, List<PerguntaResponse> conversa) {
        List<String> perguntas = r.getPerguntas() == null || r.getPerguntas().isBlank() ? List.of() : List.of(r.getPerguntas().split("\n"));
        boolean parcial = r.getGeradoEm().atZone(CrescimentoConfig.FUSO).toLocalDate().isBefore(r.getFim().plusDays(1));
        return new ResumoResponse(r.getMes(), rotulo(r.getMes()), r.getInicio(), r.getFim(), r.getTexto(), perguntas, r.getModelo(), r.getGeradoEm(), parcial, conversa);
    }

    static String rotulo(int mes) {
        return (mes + 1) + "º mês";
    }

    static LocalDate inicio(Crianca c, int mes) {
        return c.getNascimento().plusMonths(mes);
    }

    static LocalDate fim(Crianca c, int mes) {
        return c.getNascimento().plusMonths(mes + 1L).minusDays(1);
    }

    private static String corta(String s) {
        return s.length() <= MAX_TEXTO ? s : s.substring(0, MAX_TEXTO - 1) + "…";
    }

    /** Troca o nome da criança (completo e o primeiro nome) por "a criança" e tira links (ex.: de fotos). */
    static String anonimizar(String texto, String nome) {
        if (texto == null || nome == null || nome.isBlank()) return semLinks(texto);
        String r = semLinks(texto);
        String completo = nome.strip();
        String primeiro = completo.split("\\s+")[0];
        for (String n : completo.equals(primeiro) ? List.of(completo) : List.of(completo, primeiro))
            if (n.length() >= 2)
                r = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(n) + "(?![\\p{L}\\p{N}])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                        .matcher(r).replaceAll("a criança");
        return r;
    }

    static String prompt(Entrada e) {
        Crianca c = e.crianca();
        LocalDate ini = inicio(c, e.mes());
        LocalDate fimMes = fim(c, e.mes());
        boolean parcial = fimMes.isAfter(e.hoje());
        LocalDate fim = parcial ? e.hoje() : fimMes;
        String nome = c.getNome();
        StringBuilder p = new StringBuilder();

        p.append("Criança: ").append(c.getSexo() == Sexo.F ? "menina" : c.getSexo() == Sexo.M ? "menino" : "sexo não informado")
                .append(", nasceu em ").append(DATA.format(c.getNascimento()));
        if (Idade.prematuro(c))
            p.append(", prematura de ").append(String.format(Locale.ROOT, "%.0f", c.getIdadeGestacionalSemanas()))
                    .append(" semanas (curvas e marcos usam a idade corrigida até os 2 anos)");
        p.append(".\nIdade hoje (").append(DATA.format(e.hoje())).append("): ").append(Idade.texto(c.getNascimento(), e.hoje())).append(".\n");
        p.append("Período do resumo: ").append(rotulo(e.mes())).append(" de vida, de ").append(DATA.format(ini)).append(" a ")
                .append(DATA.format(fimMes)).append(parcial ? " (ainda em andamento: dados até hoje)" : "").append(".\n\n");

        // medidas do período, com a última anterior para comparar
        List<MedicaoResponse> doPeriodo = e.medidas().stream().filter(m -> dentro(m.data(), ini, fim)).toList();
        Optional<MedicaoResponse> anterior = e.medidas().stream().filter(m -> m.data().isBefore(ini) && m.peso() != null && m.peso().valor() != null)
                .max(Comparator.comparing(MedicaoResponse::data));
        p.append("Medidas do período:\n");
        if (doPeriodo.isEmpty()) p.append("- nenhuma medida registrada no período\n");
        anterior.ifPresent(m -> p.append("- (antes do período) ").append(medida(m)).append('\n'));
        doPeriodo.forEach(m -> p.append("- ").append(medida(m)).append('\n'));
        List<MedicaoResponse> pesos = new ArrayList<>(anterior.stream().toList());
        doPeriodo.stream().filter(m -> m.peso() != null && m.peso().valor() != null).forEach(pesos::add);
        if (pesos.size() >= 2) {
            MedicaoResponse a = pesos.get(0), b = pesos.get(pesos.size() - 1);
            int g = (int) Math.round((b.peso().valor() - a.peso().valor()) * 1000);
            int d = Idade.dias(a.data(), b.data());
            if (d > 0) p.append("Ganho de peso: ").append(g >= 0 ? "+" : "").append(g).append(" g em ").append(d).append(" dias (")
                    .append(Math.round((double) g / d)).append(" g/dia).\n");
        }
        if (!e.alertas().isEmpty()) {
            p.append("Alertas de crescimento hoje:\n");
            e.alertas().forEach(a -> p.append("- ").append(a.titulo()).append(": ").append(anonimizar(a.texto(), nome)).append('\n'));
        }

        p.append("\nDiário do período:\n");
        List<Registro> regs = e.registros().stream().filter(r -> dentro(r.getData(), ini, fim)).sorted(Comparator.comparing(Registro::getData)).toList();
        if (regs.isEmpty()) p.append("- nenhuma anotação no período\n");
        for (Registro r : regs) {
            p.append("- ").append(DATA.format(r.getData())).append(" [").append(switch (r.getTipo()) {
                case PESAGEM -> "pesagem";
                case CONSULTA -> "consulta";
                case MOMENTO -> "momento";
            }).append(']');
            if (r.getAlbumTitulo() != null) p.append(" (álbum: ").append(anonimizar(r.getAlbumTitulo(), nome)).append(')');
            if (r.getTexto() != null && !r.getTexto().isBlank()) p.append(' ').append(anonimizar(r.getTexto().strip(), nome).replace('\n', ' '));
            if (r.getProximaConsulta() != null) p.append(" (retorno marcado para ").append(DATA.format(r.getProximaConsulta())).append(')');
            p.append('\n');
        }

        p.append("\nMarcos do desenvolvimento observados no período:\n");
        List<MarcoAtingido> noPeriodo = e.marcos().stream().filter(m -> dentro(m.getAtingidoEm(), ini, fim))
                .sorted(Comparator.comparing(MarcoAtingido::getAtingidoEm)).toList();
        if (noPeriodo.isEmpty()) p.append("- nenhum marcado no período\n");
        for (MarcoAtingido m : noPeriodo) {
            p.append("- ").append(DATA.format(m.getAtingidoEm())).append(": ")
                    .append(CatalogoMarcos.marco(m.getMarcoId()).map(CatalogoMarcos.Marco::texto).orElse(m.getMarcoId()));
            if (m.getNota() != null && !m.getNota().isBlank()) p.append(" (nota: ").append(anonimizar(m.getNota().strip(), nome)).append(')');
            p.append('\n');
        }
        Set<String> atingidos = new HashSet<>();
        e.marcos().stream().filter(m -> !m.getAtingidoEm().isAfter(fim)).forEach(m -> atingidos.add(m.getMarcoId()));
        CatalogoMarcos.Faixa faixa = CatalogoMarcos.faixaDaIdade(Math.max(0, Idade.diasCurva(c, fim)) / 30.4375);
        List<String> faltam = CatalogoMarcos.MARCOS.stream().filter(m -> m.faixaId().equals(faixa.id()) && !atingidos.contains(m.id()))
                .map(CatalogoMarcos.Marco::texto).toList();
        p.append("Marcos da fase \"").append(faixa.nome()).append("\" ainda não marcados pela família:")
                .append(faltam.isEmpty() ? " nenhum\n" : "\n");
        faltam.forEach(t -> p.append("- ").append(t).append('\n'));

        p.append("\nVacinas aplicadas no período:\n");
        List<DoseResponse> aplicadas = e.doses().stream().filter(d -> d.aplicadaEm() != null && dentro(d.aplicadaEm(), ini, fim))
                .sorted(Comparator.comparing(DoseResponse::aplicadaEm)).toList();
        if (aplicadas.isEmpty()) p.append("- nenhuma\n");
        aplicadas.forEach(d -> p.append("- ").append(DATA.format(d.aplicadaEm())).append(": ").append(d.vacina()).append(" (").append(d.dose()).append(")\n"));
        List<DoseResponse> pendentes = e.doses().stream().filter(d -> d.situacao() == SituacaoVacina.ATRASADA || d.situacao() == SituacaoVacina.TOMAR_AGORA
                || d.situacao() == SituacaoVacina.PROXIMA).sorted(Comparator.comparing(DoseResponse::dataPrevista)).toList();
        p.append("Vacinas do calendário sem aplicação registrada no app, com data recomendada já passada ou nos próximos 30 dias ")
                .append("(podem ter sido aplicadas e só não registradas; confirme na caderneta):").append(pendentes.isEmpty() ? " nenhuma\n" : "\n");
        pendentes.forEach(d -> p.append("- ").append(d.vacina()).append(" (").append(d.dose()).append("): ").append(switch (d.situacao()) {
            case ATRASADA -> "data recomendada passou";
            case TOMAR_AGORA -> "data recomendada passou há pouco";
            default -> "prevista";
        }).append(", data recomendada ").append(DATA.format(d.dataPrevista())).append('\n'));

        return p.toString();
    }

    private static boolean dentro(LocalDate d, LocalDate ini, LocalDate fim) {
        return d != null && !d.isBefore(ini) && !d.isAfter(fim);
    }

    private static String medida(MedicaoResponse m) {
        List<String> partes = new ArrayList<>();
        valor(partes, "peso", m.peso(), "kg", 3);
        valor(partes, "comprimento", m.comprimento(), "cm", 1);
        valor(partes, "perímetro cefálico", m.perimetro(), "cm", 1);
        return DATA.format(m.data()) + " (" + m.idade() + "): " + (partes.isEmpty() ? "sem valores" : String.join(", ", partes));
    }

    private static void valor(List<String> partes, String nome, MedidaAvaliada a, String unidade, int casas) {
        if (a == null || a.valor() == null) return;
        String s = nome + " " + String.format(Locale.ROOT, "%." + casas + "f", a.valor()).replace('.', ',') + " " + unidade;
        if (a.percentil() != null) s += " (percentil " + Math.round(a.percentil()) + (a.faixa() == null ? "" : ", " + switch (a.faixa()) {
            case ABAIXO -> "abaixo do P3";
            case LIMITE_INFERIOR -> "entre P3 e P15";
            case ESPERADA -> "entre P15 e P85";
            case LIMITE_SUPERIOR -> "entre P85 e P97";
            case ACIMA -> "acima do P97";
        }) + ")";
        partes.add(s);
    }
}
