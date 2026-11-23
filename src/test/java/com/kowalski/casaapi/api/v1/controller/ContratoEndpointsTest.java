package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.response.*;
import com.kowalski.casaapi.business.auth.AuthService;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import com.kowalski.casaapi.comum.MonitorIndisponivelException;
import com.kowalski.casaapi.carro.CarroResponse;
import com.kowalski.casaapi.carro.CarroService;
import com.kowalski.casaapi.compra.*;
import com.kowalski.casaapi.conta.*;
import com.kowalski.casaapi.fatura.*;
import com.kowalski.casaapi.geracao.*;
import com.kowalski.casaapi.renda.*;
import com.kowalski.casaapi.pessoa.Pessoa;
import com.kowalski.casaapi.business.monitor.MonitorService;
import com.kowalski.casaapi.pessoa.PessoaRepository;
import com.kowalski.casaapi.pessoa.PessoaResponse;
import com.kowalski.casaapi.pessoa.PessoaService;
import com.kowalski.casaapi.business.service.*;
import com.kowalski.casaapi.support.Login;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import({UsuarioLogado.class, com.kowalski.casaapi.comum.RelogioConfig.class, com.kowalski.casaapi.crescimento.comum.CrescimentoConfig.class})
class ContratoEndpointsTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired MockMvc mvc;

    @MockitoBean CompraService compraService;
    @MockitoBean DespesaService despesaService;
    @MockitoBean FiltroService filtroService;
    @MockitoBean FaturaService faturaService;
    @MockitoBean RendaService rendaService;
    @MockitoBean GeracaoService geracaoService;
    @MockitoBean PessoaService pessoaService;
    @MockitoBean ContaService contaService;
    @MockitoBean CarroService carroService;
    @MockitoBean AuthService authService;
    @MockitoBean PessoaRepository pessoaRepository;
    @MockitoBean MonitorService monitorService;
    @MockitoBean com.kowalski.casaapi.business.monitor.InternetService internetService;
    @MockitoBean com.kowalski.casaapi.business.monitor.VentilacaoService ventilacaoService;
    @MockitoBean com.kowalski.casaapi.business.passagens.PassagensService passagensService;
    @MockitoBean com.kowalski.casaapi.business.passagens.RotasPassagemService rotasPassagemService;
    // módulo crescimento: controllers carregados pelo @WebMvcTest
    @MockitoBean com.kowalski.casaapi.crescimento.familia.FamiliaService familiaService;
    @MockitoBean com.kowalski.casaapi.crescimento.crianca.CriancaService criancaService;
    @MockitoBean com.kowalski.casaapi.crescimento.medida.MedicaoService medicaoService;
    @MockitoBean com.kowalski.casaapi.crescimento.foto.FotoService fotoService;
    @MockitoBean com.kowalski.casaapi.crescimento.exame.ExameService exameService;
    @MockitoBean com.kowalski.casaapi.crescimento.diario.RegistroService registroService;
    @MockitoBean com.kowalski.casaapi.crescimento.album.AlbumService albumService;
    @MockitoBean com.kowalski.casaapi.crescimento.marco.MarcoService marcoService;
    @MockitoBean com.kowalski.casaapi.crescimento.vacina.VacinaService vacinaService;
    @MockitoBean com.kowalski.casaapi.crescimento.agenda.AgendaService agendaService;

    @BeforeEach
    void ana() {
        Login.comoAna();
    }

    @AfterEach
    void sair() {
        Login.sair();
    }

    private static final String COMPRA = """
            {"nomeProduto":"tv","valorProduto":100,"dataCompra":"2026-01-10","numeroParcelas":2,
             "nomePessoaCompra":"ANA","tipoLancamentoId":"%s","categoriaId":"%s"}
            """.formatted(UUID.randomUUID(), UUID.randomUUID());

    @Nested
    class Compras {

        @Test
        void listar() throws Exception {
            mvc.perform(get("/v1/compras")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
            verify(compraService).buscarTodos(null, null);
        }

        @Test
        void listarComFiltros() throws Exception {
            mvc.perform(get("/v1/compras").param("pessoa", "ANA").param("cartao", "NUBANK")).andExpect(status().isOk());
            verify(compraService).buscarTodos("ANA", "NUBANK");
        }

        @Test
        void parcelasDoPeriodoUsaTODOSComoPadrao() throws Exception {
            when(compraService.buscarPorPeriodo(any(), any(), any(), any()))
                    .thenReturn(new CompraResponse(List.of()));

            mvc.perform(get("/v1/compras/parcelas").param("de", "2026-08").param("ate", "2027-11").param("pessoa", "ANA"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.compras").isArray());
            verify(compraService).buscarPorPeriodo("2026-08", "2027-11", "ANA", "TODOS");
        }

        @Test
        void parcelasDoPeriodoSemDeDa400() throws Exception {
            mvc.perform(get("/v1/compras/parcelas").param("ate", "2027-11").param("pessoa", "ANA"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void parcelasDoMesUsaTODOSComoPadrao() throws Exception {
            when(compraService.buscarPorMesENome(any(), any(), any(), any(), any()))
                    .thenReturn(new CompraResponse(List.of()));

            mvc.perform(get("/v1/compras/parcelas/2026/11").param("pessoa", "ANA"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.compras").isArray());
            verify(compraService).buscarPorMesENome("2026", "11", "ANA", "TODOS", "TODOS");
        }

        @Test
        void parcelasDoMesComCartaoEUltimaParcela() throws Exception {
            mvc.perform(get("/v1/compras/parcelas/2026/01")
                    .param("pessoa", "ANA").param("cartao", "NUBANK").param("ultimaParcela", "SIM"));
            verify(compraService).buscarPorMesENome("2026", "01", "ANA", "NUBANK", "SIM");
        }

        @Test
        void parcelasSemPessoaDa400ComMensagem() throws Exception {
            mvc.perform(get("/v1/compras/parcelas/2026/11"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensagem").value("O parâmetro pessoa é obrigatório"));
        }

        @Test
        void parcelasDeOutraPessoaDa403() throws Exception {
            mvc.perform(get("/v1/compras/parcelas/2026/11").param("pessoa", "BRUNO"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.mensagem").exists());
            verifyNoInteractions(compraService);
        }

        @Test
        void criarDa201ComMensagem() throws Exception {
            mvc.perform(post("/v1/compras").contentType(JSON).content(COMPRA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mensagem").value("Cadastro realizado com sucesso."));
            verify(compraService).salvar(any());
        }

        @Test
        void criarInvalidaDa400ComAMensagemDoServico() throws Exception {
            doThrow(new IllegalArgumentException("O nome do produto é obrigatório")).when(compraService).salvar(any());

            mvc.perform(post("/v1/compras").contentType(JSON).content(COMPRA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensagem").value("O nome do produto é obrigatório"));
        }

        @Test
        void corpoIlegivelDa400() throws Exception {
            mvc.perform(post("/v1/compras").contentType(JSON).content("{nao é json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensagem").value("Dados inválidos na requisição"));
        }

        @Test
        void editar() throws Exception {
            UUID id = UUID.randomUUID();
            mvc.perform(put("/v1/compras/" + id).contentType(JSON).content(COMPRA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensagem").value("Compra editada com sucesso."));
            verify(compraService).editar(eq(id), any());
        }

        @Test
        void idInvalidoDa400() throws Exception {
            mvc.perform(delete("/v1/compras/abc")).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensagem").exists());
        }

        @Test
        void excluirDa204() throws Exception {
            UUID id = UUID.randomUUID();
            mvc.perform(delete("/v1/compras/" + id)).andExpect(status().isNoContent());
            verify(compraService).remover(id);
        }

        @Test
        void excluirInexistenteDa404() throws Exception {
            doThrow(new RecursoNaoEncontradoException("Compra não encontrada")).when(compraService).remover(any());

            mvc.perform(delete("/v1/compras/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.mensagem").value("Compra não encontrada"));
        }

        @Test
        void urlAntigaNaoExisteMais() throws Exception {
            mvc.perform(get("/v1/compra/2026/11/ANA/TODOS/TODOS")).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.mensagem").exists());
        }
    }

    @Nested
    class Despesas {

        @Test
        void doAno() throws Exception {
            when(despesaService.buscarDespesasAnuais("2026", "ANA")).thenReturn(new DespesaAnualResponse("ANA", "2026",
                    Map.of("01", new DespesaResponse(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO))));

            mvc.perform(get("/v1/despesas/2026").param("pessoa", "ANA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.despesasPorMes.01.valorMes").value(1));
        }

        @Test
        void anoSemDespesaDa404() throws Exception {
            when(despesaService.buscarDespesasAnuais("2026", "ANA")).thenReturn(new DespesaAnualResponse("ANA", "2026", Map.of()));
            mvc.perform(get("/v1/despesas/2026").param("pessoa", "ANA")).andExpect(status().isNotFound());
        }
    }

    @Nested
    class Filtros {

        private final List<FiltroResponse> itens = List.of(new FiltroResponse("A", "a"));

        @Test
        void cadaFiltroChamaOServicoCerto() throws Exception {
            when(filtroService.buscarAnos()).thenReturn(itens);
            when(filtroService.buscarMeses("2026")).thenReturn(itens);
            when(filtroService.buscarCartoes("2026", "11", "ANA")).thenReturn(itens);
            when(filtroService.buscarCategorias()).thenReturn(itens);
            when(filtroService.buscarTiposDeLancamentos()).thenReturn(itens);

            for (String url : List.of("/v1/filtros/anos", "/v1/filtros/meses?ano=2026",
                    "/v1/filtros/cartoes?ano=2026&mes=11&pessoa=ANA", "/v1/filtros/categorias", "/v1/filtros/tipos-lancamento")) {
                mvc.perform(get(url)).andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].codigo").value("A"))
                        .andExpect(jsonPath("$[0].descricao").value("a"));
            }
        }

        @Test
        void pessoasFiltraParaQuemNaoEAdmin() throws Exception {
            when(filtroService.buscarPessoas("2026", "11"))
                    .thenReturn(List.of(new FiltroResponse("ANA", "ANA"), new FiltroResponse("BRUNO", "BRUNO")));

            mvc.perform(get("/v1/filtros/pessoas").param("ano", "2026").param("mes", "11"))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].codigo").value("ANA"));

            Login.comoAdmin();
            mvc.perform(get("/v1/filtros/pessoas").param("ano", "2026").param("mes", "11"))
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        void cartoesDeOutraPessoaDa403() throws Exception {
            mvc.perform(get("/v1/filtros/cartoes").param("ano", "2026").param("mes", "11").param("pessoa", "BRUNO"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class FaturasERendas {

        @Test
        void fatura() throws Exception {
            when(faturaService.obter(1L, LocalDate.of(2026, 11, 1))).thenReturn(new FaturaResponse("2026-11", true, LocalDate.of(2026, 11, 5)));

            mvc.perform(get("/v1/faturas/2026/11").param("pessoaId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.competencia").value("2026-11"))
                    .andExpect(jsonPath("$.paga").value(true))
                    .andExpect(jsonPath("$.dataPagamento").value("2026-11-05"));

            mvc.perform(put("/v1/faturas/2026/11").param("pessoaId", "1").contentType(JSON).content("{\"paga\":false}"))
                    .andExpect(status().isOk());
            verify(faturaService).salvar(eq(1L), eq(LocalDate.of(2026, 11, 1)), any());
        }

        @Test
        void renda() throws Exception {
            when(rendaService.obter(1L, LocalDate.of(2026, 2, 1)))
                    .thenReturn(new RendaResponse("2026-02", new BigDecimal("5000.00"), "2026-01", true));

            mvc.perform(get("/v1/rendas/2026/2").param("pessoaId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valor").value(5000.0))
                    .andExpect(jsonPath("$.herdada").value(true));

            mvc.perform(put("/v1/rendas/2026/2").param("pessoaId", "1").contentType(JSON).content("{\"valor\":10}"))
                    .andExpect(status().isOk());
            verify(rendaService).salvar(eq(1L), eq(LocalDate.of(2026, 2, 1)), any());
        }

        @Test
        void dadosDeOutraPessoaDa403() throws Exception {
            mvc.perform(get("/v1/faturas/2026/11").param("pessoaId", "2")).andExpect(status().isForbidden());
            mvc.perform(get("/v1/rendas/2026/11").param("pessoaId", "2")).andExpect(status().isForbidden());
        }

        @Test
        void mesInvalidoDa400() throws Exception {
            mvc.perform(get("/v1/rendas/2026/13").param("pessoaId", "1"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensagem").value("Mês inválido: 13"));
            mvc.perform(get("/v1/faturas/2026/xx").param("pessoaId", "1")).andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Contas {

        @Test
        void listarECriar() throws Exception {
            mvc.perform(get("/v1/contas").param("pessoaId", "1")).andExpect(status().isOk());
            verify(contaService).listar(1L);

            mvc.perform(post("/v1/contas").contentType(JSON).content("{\"descricao\":\"x\"}")).andExpect(status().isCreated());
        }

        @Test
        void lancamentosConvertemAsCompetencias() throws Exception {
            mvc.perform(get("/v1/contas/lancamentos").param("pessoaId", "1").param("de", "2026-01").param("ate", "2026-03"))
                    .andExpect(status().isOk());
            verify(contaService).lancamentos(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1));
        }

        @Test
        void competenciaMalFormadaDa400() throws Exception {
            mvc.perform(get("/v1/contas/lancamentos").param("pessoaId", "1").param("de", "jan").param("ate", "2026-03"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensagem").value("O campo de deve estar no formato aaaa-mm"));
        }

        @Test
        void excluirDa204() throws Exception {
            UUID id = UUID.randomUUID();
            mvc.perform(delete("/v1/contas/" + id).param("aPartirDe", "2026-05")).andExpect(status().isNoContent());
            verify(contaService).excluir(id, LocalDate.of(2026, 5, 1));
        }
    }

    @Nested
    class GeracaoSolar {

        @Test
        void doMesPassaAnoEMesNaOrdem() throws Exception {
            when(geracaoService.doMes(2025, 3)).thenReturn(new GeracaoMensalResponse(List.of(), Year.of(2025), Month.MARCH));

            mvc.perform(get("/v1/geracao-solar/2025/3"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("\"ano\":\"2025\"")))
                    .andExpect(content().string(containsString("\"mes\":\"MARCH\"")));
        }

        @Test
        void doAno() throws Exception {
            when(geracaoService.doAno(2025)).thenReturn(List.of(new GeracaoMesAnoResponse(Month.JANUARY, Year.of(2025), 3.5)));

            mvc.perform(get("/v1/geracao-solar/2025"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("\"month\":\"JANUARY\"")))
                    .andExpect(content().string(containsString("\"year\":\"2025\"")))
                    .andExpect(jsonPath("$[0].value").value(3.5));
        }

        @Test
        void doDia() throws Exception {
            when(geracaoService.doDia(LocalDate.of(2025, 1, 2))).thenReturn(new GeracaoDiariaResponse(List.of(), null));

            mvc.perform(get("/v1/geracao-solar").param("data", "2025-01-02")).andExpect(status().isOk());
            mvc.perform(get("/v1/geracao-solar").param("data", "02/01/2025")).andExpect(status().isBadRequest());
        }

        @Test
        void integracaoEnviaLeituraERecebe200() throws Exception {
            mvc.perform(post("/v1/geracao-solar").contentType(JSON).content("{\"gerado\":1.5,\"potencia\":300}"))
                    .andExpect(status().isOk());
            verify(geracaoService).registrar(new GeradoDTO(1.5, 300.0));
        }
    }

    @Nested
    class Servidor {

        private final ServidorEstatisticasResponse.Faixa faixa = new ServidorEstatisticasResponse.Faixa(1.0, 2.0, 3.0);
        private final ServidorEstatisticasResponse resumo =
                new ServidorEstatisticasResponse(1440, faixa, faixa, faixa, faixa, 5.0, 0.2, 10L, 20L, 30L, 40L, 58.0);

        @Test
        void agora() throws Exception {
            when(monitorService.agora()).thenReturn(new ServidorAgoraResponse(null, "casa", null, null, 60,
                    new ServidorAgoraResponse.Cpu(12.5, 0.1, List.of()), new ServidorAgoraResponse.Carga(1, 1, 1, 100),
                    new ServidorAgoraResponse.Memoria(100, 50, 50, 0, 0, 0), List.of(), null, List.of(), List.of(),
                    new ServidorAgoraResponse.DiscoIo(0, 0),
                    List.of(new ServidorContainerResponse("jellyfin", "linuxserver/jellyfin", "running", "ligado há 2 horas",
                            null, 11.6, 100L, 1000L, 10.0, null, null, 0.0, 0.0, 30)),
                    new ServidorAgoraResponse.Pressao(1.5, 0.0, 0.0, 0.4, 0.2), 3.0, List.of(), 100.0, List.of(), 1.2, 0));

            mvc.perform(get("/v1/servidor/agora"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.host").value("casa"))
                    .andExpect(jsonPath("$.cpu.usoPct").value(12.5))
                    .andExpect(jsonPath("$.memoria.pct").value(50.0))
                    .andExpect(jsonPath("$.containers[0].nome").value("jellyfin"))
                    .andExpect(jsonPath("$.containers[0].cpuPct").value(11.6))
                    .andExpect(jsonPath("$.pressao.cpu").value(1.5))
                    .andExpect(jsonPath("$.cpuLimitePct").value(100.0));
        }

        @Test
        void diaUsaHojeComoPadrao() throws Exception {
            when(monitorService.dia(any())).thenAnswer(i -> new ServidorDiaResponse(i.getArgument(0), resumo, List.of(), List.of(), List.of(), List.of(), List.of()));

            mvc.perform(get("/v1/servidor/dia")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(LocalDate.now().toString()))
                    .andExpect(jsonPath("$.resumo.cpu.max").value(3.0))
                    .andExpect(jsonPath("$.resumo.discoTemperaturaMax").value(58.0));
            mvc.perform(get("/v1/servidor/dia").param("data", "2026-11-01")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("2026-11-01"));
            mvc.perform(get("/v1/servidor/dia").param("data", "01/11/2026")).andExpect(status().isBadRequest());
        }

        @Test
        void diasUsaOsUltimos30DiasComoPadrao() throws Exception {
            when(monitorService.dias(any(), any())).thenReturn(List.of(new ServidorResumoDiaResponse(LocalDate.of(2026, 11, 15), resumo)));

            mvc.perform(get("/v1/servidor/dias")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].data").value("2026-11-15"))
                    .andExpect(jsonPath("$[0].resumo.amostras").value(1440));
            verify(monitorService).dias(LocalDate.now().minusDays(29), LocalDate.now());

            mvc.perform(get("/v1/servidor/dias").param("de", "2026-11-01").param("ate", "2026-11-10"));
            verify(monitorService).dias(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10));
        }

        @Test
        void bancoForaDoArDa503ComMensagem() throws Exception {
            when(monitorService.dia(any())).thenThrow(new MonitorIndisponivelException("O banco do monitoramento (MariaDB) está indisponível", null));

            mvc.perform(get("/v1/servidor/dia"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.mensagem").value("O banco do monitoramento (MariaDB) está indisponível"));
        }
    }

    @Nested
    class Pessoas {

        @Test
        void buscarInexistenteDa404ComMensagem() throws Exception {
            doThrow(new RecursoNaoEncontradoException("Pessoa não encontrada")).when(pessoaService).excluir(99L);

            mvc.perform(delete("/v1/pessoas/99")).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.mensagem").value("Pessoa não encontrada"));
        }

        @Test
        void criarAtualizarExcluir() throws Exception {
            PessoaResponse ana = PessoaResponse.de(Login.pessoa(1L, "ANA"));
            when(pessoaService.criar(any())).thenReturn(ana);
            when(pessoaService.atualizar(eq(1L), any())).thenReturn(ana);
            String corpo = "{\"nome\":\"Ana\",\"email\":\"ana@x.com\"}";

            mvc.perform(post("/v1/pessoas").contentType(JSON).content(corpo)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nome").value("ANA"))
                    .andExpect(jsonPath("$.senhaHash").doesNotExist());
            mvc.perform(put("/v1/pessoas/1").contentType(JSON).content(corpo)).andExpect(status().isOk());
            mvc.perform(delete("/v1/pessoas/1")).andExpect(status().isNoContent());
            verify(pessoaService).excluir(1L);
        }
    }

    @Nested
    class ContasEdicao {

        @Test
        void editarRepassaOMesDeInicio() throws Exception {
            UUID id = UUID.randomUUID();
            mvc.perform(put("/v1/contas/" + id).param("aPartirDe", "2026-12").contentType(JSON).content("{\"descricao\":\"x\"}"))
                    .andExpect(status().isOk());
            verify(contaService).editar(eq(id), any(), eq(LocalDate.of(2026, 12, 1)));
        }

        @Test
        void editarSemMesDeInicioPassaNulo() throws Exception {
            UUID id = UUID.randomUUID();
            mvc.perform(put("/v1/contas/" + id).contentType(JSON).content("{\"descricao\":\"x\"}")).andExpect(status().isOk());
            verify(contaService).editar(eq(id), any(), isNull());
        }

        @Test
        void atualizarLancamento() throws Exception {
            UUID id = UUID.randomUUID();
            mvc.perform(patch("/v1/contas/lancamentos/" + id).contentType(JSON).content("{\"paga\":true}"))
                    .andExpect(status().isOk());
            verify(contaService).atualizarLancamento(eq(id), any());
        }
    }

    @Nested
    class PessoasLista {

        @Test
        void listarDevolveOsCamposSemASenha() throws Exception {
            Pessoa ana = Login.pessoa(1L, "ANA");
            ana.setSenhaHash("hash");
            when(pessoaService.listar()).thenReturn(List.of(PessoaResponse.de(ana)));

            mvc.perform(get("/v1/pessoas")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].nome").value("ANA"))
                    .andExpect(jsonPath("$[0].email").value("ana@exemplo.com"))
                    .andExpect(jsonPath("$[0].papel").value("USUARIO"))
                    .andExpect(jsonPath("$[0].ativo").value(true))
                    .andExpect(jsonPath("$[0].temAcesso").value(true))
                    .andExpect(jsonPath("$[0].senhaHash").doesNotExist());
        }
    }

    @Nested
    class ServidorHostEEventos {

        @Test
        void hostDevolveOMapaDoServico() throws Exception {
            when(monitorService.host()).thenReturn(Map.of("desatualizado", false));
            mvc.perform(get("/v1/servidor/host")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.desatualizado").value(false));
        }

        @Test
        void eventosUsaOsUltimos7DiasComoPadrao() throws Exception {
            when(monitorService.eventos(any(), any())).thenReturn(List.of(
                    new ServidorEventoResponse(java.time.LocalDateTime.of(2026, 11, 15, 3, 41), "backup", "sucesso", "ok")));

            mvc.perform(get("/v1/servidor/eventos")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].quando").value("2026-11-15T03:41:00"))
                    .andExpect(jsonPath("$[0].tipo").value("backup"))
                    .andExpect(jsonPath("$[0].nivel").value("sucesso"))
                    .andExpect(jsonPath("$[0].mensagem").value("ok"));
            verify(monitorService).eventos(LocalDate.now().minusDays(6), LocalDate.now());

            mvc.perform(get("/v1/servidor/eventos").param("de", "2026-11-01").param("ate", "2026-11-10"));
            verify(monitorService).eventos(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10));
        }
    }

    @Nested
    class Internet {

        @Test
        void agoraDevolveOMapaDoServico() throws Exception {
            when(internetService.agora()).thenReturn(Map.of("desatualizado", true));
            mvc.perform(get("/v1/servidor/internet")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.desatualizado").value(true));
        }

        @Test
        void diaUsaHojeComoPadrao() throws Exception {
            mvc.perform(get("/v1/servidor/internet/dia")).andExpect(status().isOk());
            verify(internetService).dia(LocalDate.now());
            mvc.perform(get("/v1/servidor/internet/dia").param("data", "2026-11-01")).andExpect(status().isOk());
            verify(internetService).dia(LocalDate.of(2026, 11, 1));
        }

        @Test
        void diasEVelocidadeUsamOsUltimos30Dias() throws Exception {
            mvc.perform(get("/v1/servidor/internet/dias")).andExpect(status().isOk());
            verify(internetService).dias(LocalDate.now().minusDays(29), LocalDate.now());
            mvc.perform(get("/v1/servidor/internet/velocidade")).andExpect(status().isOk());
            verify(internetService).velocidade(LocalDate.now().minusDays(29), LocalDate.now());

            mvc.perform(get("/v1/servidor/internet/velocidade").param("de", "2026-11-01").param("ate", "2026-11-10"));
            verify(internetService).velocidade(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10));
        }

        @Test
        void dataInvalidaDa400() throws Exception {
            mvc.perform(get("/v1/servidor/internet/dia").param("data", "01/11/2026")).andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Ventilacao {

        @Test
        void diasUsaOsUltimos30Dias() throws Exception {
            mvc.perform(get("/v1/servidor/ventilacao/dias")).andExpect(status().isOk());
            verify(ventilacaoService).dias(LocalDate.now().minusDays(29), LocalDate.now());
        }
    }

    @Nested
    class Passagens {

        private static final String ROTA = "{\"nome\":\"FOR-GRU\",\"origem\":[\"FOR\"],\"destino\":[\"GRU\"]}";

        @Test
        void listarUsa30DiasComoPadrao() throws Exception {
            mvc.perform(get("/v1/passagens")).andExpect(status().isOk());
            verify(passagensService).listar(30);
            mvc.perform(get("/v1/passagens").param("dias", "0")).andExpect(status().isOk());
            verify(passagensService).listar(0);
        }

        @Test
        void criarEditarERemoverRota() throws Exception {
            when(rotasPassagemService.criar(any())).thenReturn(Map.of("id", 7));
            mvc.perform(post("/v1/passagens/rotas").contentType(JSON).content(ROTA))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(7));
            verify(rotasPassagemService).criar(argThat(r -> "FOR-GRU".equals(r.nome()) && r.origem().equals(List.of("FOR"))));

            mvc.perform(put("/v1/passagens/rotas/7").contentType(JSON).content(ROTA)).andExpect(status().isOk());
            verify(rotasPassagemService).atualizar(eq(7L), any());

            mvc.perform(delete("/v1/passagens/rotas/7")).andExpect(status().isNoContent());
            verify(rotasPassagemService).remover(7L, false);
            mvc.perform(delete("/v1/passagens/rotas/7").param("historico", "true")).andExpect(status().isNoContent());
            verify(rotasPassagemService).remover(7L, true);
        }
    }

    @Nested
    class Geral {

        @Test
        void carroPorCodigoFipe() throws Exception {
            UUID id = UUID.randomUUID();
            when(carroService.historico("001")).thenReturn(List.of(
                    new CarroResponse(id, "001", "01/2026", "VW", "Gol", "2020", 55000.5)));

            mvc.perform(get("/v1/carros/001")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(id.toString()))
                    .andExpect(jsonPath("$[0].codigoVeiculo").value("001"))
                    .andExpect(jsonPath("$[0].mesReferencia").value("01/2026"))
                    .andExpect(jsonPath("$[0].marca").value("VW"))
                    .andExpect(jsonPath("$[0].modelo").value("Gol"))
                    .andExpect(jsonPath("$[0].ano").value("2020"))
                    .andExpect(jsonPath("$[0].valor").value(55000.5));
        }

        @Test
        void erroInesperadoDa500SemDetalhesInternos() throws Exception {
            when(carroService.historico(any())).thenThrow(new IllegalStateException("senha do banco: xyz"));

            mvc.perform(get("/v1/carros/001"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.mensagem").value("Erro inesperado. Tente novamente."));
        }

        @Test
        void metodoNaoSuportadoDa405() throws Exception {
            mvc.perform(patch("/v1/compras")).andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.mensagem").exists());
        }
    }
}
