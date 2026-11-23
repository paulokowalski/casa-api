package com.kowalski.casaapi.crescimento.exame;

import com.kowalski.casaapi.crescimento.comum.FotoInvalidaException;
import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.CriancaRepository;
import com.kowalski.casaapi.crescimento.exame.ExameDtos.ArquivoExame;
import com.kowalski.casaapi.crescimento.exame.ExameDtos.ExameInput;
import com.kowalski.casaapi.crescimento.exame.ExameDtos.ExameResponse;
import com.kowalski.casaapi.crescimento.familia.AcessoFamilia;
import com.kowalski.casaapi.crescimento.foto.FotoStorage;
import com.kowalski.casaapi.crescimento.foto.ProcessadorImagem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Exames da criança: PDF ou imagem enviados pelos pais. Só membros da família veem; só responsáveis enviam, editam e
 * apagam. Os arquivos só saem por {@link #arquivo} com login (não há link público). Imagens são regravadas em JPEG,
 * endireitadas e sem metadados (a localização GPS some); PDFs são guardados como vieram.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExameService {

    public static final long TAMANHO_MAXIMO = 25L * 1024 * 1024;
    static final int MAX_TITULO = 120, MAX_OBSERVACAO = 1000, MAX_NOME_ARQUIVO = 200;

    private final ExameRepository exames;
    private final CriancaRepository criancas;
    private final AcessoFamilia acesso;
    private final FotoStorage storage;
    private final ProcessadorImagem processador;
    private final Clock relogio;

    @Transactional
    public ExameResponse enviar(UUID criancaId, byte[] dados, String nomeArquivo, ExameInput d) {
        Crianca c = crianca(criancaId, true);
        if (dados == null || dados.length == 0) throw new IllegalArgumentException("Escolha o arquivo do exame");
        if (dados.length > TAMANHO_MAXIMO) throw new IllegalArgumentException("O arquivo é grande demais. O limite é de 25 MB.");
        Exame e = new Exame();
        aplicar(e, c, d);
        String sha = sha256(dados);
        var existente = exames.findByCriancaIdAndSha256(criancaId, sha);
        if (existente.isPresent()) return resposta(existente.get(), true);

        TipoExame tipo = detectar(dados);
        byte[] gravar = tipo == TipoExame.PDF ? dados : processador.processarOriginal(dados);
        String chave = "exames/" + c.getFamiliaId() + "/" + c.getId() + "/" + UUID.randomUUID() + tipo.extensao();
        storage.gravar(chave, gravar);
        try {
            e.setFamiliaId(c.getFamiliaId());
            e.setCriancaId(c.getId());
            e.setNomeArquivo(nomeArquivo(nomeArquivo, tipo));
            e.setTipo(tipo);
            e.setChave(chave);
            e.setBytes((long) gravar.length);
            e.setSha256(sha);
            e.setEnviadoPor(acesso.usuarioId());
            e = exames.saveAndFlush(e);
            apagarSeDesfizer(chave);
            return resposta(e, false);
        } catch (RuntimeException ex) {
            apagarArquivo(chave);
            throw ex;
        }
    }

    /**
     * O exame já guardado com este mesmo arquivo. Usado quando dois envios iguais chegam juntos (clique duplo): o
     * segundo esbarra na restrição única do banco depois da checagem de {@link #enviar}, e a resposta passa a ser o
     * exame do primeiro, com {@code jaExistia=true}, em vez de erro.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<ExameResponse> jaEnviado(UUID criancaId, byte[] dados) {
        crianca(criancaId, false);
        if (dados == null || dados.length == 0) return java.util.Optional.empty();
        return exames.findByCriancaIdAndSha256(criancaId, sha256(dados)).map(e -> resposta(e, true));
    }

    @Transactional(readOnly = true)
    public List<ExameResponse> listar(UUID criancaId) {
        crianca(criancaId, false);
        return exames.findByCriancaIdOrderByDataDescCriadoEmDesc(criancaId).stream().map(e -> resposta(e, false)).toList();
    }

    @Transactional
    public ExameResponse atualizar(UUID criancaId, UUID id, ExameInput d) {
        Crianca c = crianca(criancaId, true);
        Exame e = carregar(c, id);
        aplicar(e, c, d);
        return resposta(exames.saveAndFlush(e), false);
    }

    @Transactional
    public void excluir(UUID criancaId, UUID id) {
        Crianca c = crianca(criancaId, true);
        Exame e = carregar(c, id);
        exames.delete(e);
        apagarDepoisDeConfirmar(List.of(e.getChave()));
    }

    /** Chamado ao excluir a criança (o acesso já foi conferido). */
    @Transactional
    public void excluirTodosDaCrianca(UUID criancaId) {
        List<Exame> lista = exames.findByCriancaIdOrderByDataDescCriadoEmDesc(criancaId);
        exames.deleteAll(lista);
        apagarDepoisDeConfirmar(lista.stream().map(Exame::getChave).toList());
    }

    /** O arquivo do exame, para quem é da família (exige login). */
    @Transactional(readOnly = true)
    public ArquivoExame arquivo(UUID criancaId, UUID id) {
        Crianca c = crianca(criancaId, false);
        Exame e = carregar(c, id);
        return new ArquivoExame(storage.ler(e.getChave()), e.getTipo(), e.getNomeArquivo());
    }

    /** Pelo conteúdo, não pelo nome ou pelo tipo informado pelo navegador. */
    static TipoExame detectar(byte[] d) {
        if (d.length >= 5 && d[0] == '%' && d[1] == 'P' && d[2] == 'D' && d[3] == 'F' && d[4] == '-') return TipoExame.PDF;
        boolean jpeg = d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF;
        boolean png = d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G';
        if (jpeg || png) return TipoExame.JPEG;
        throw new FotoInvalidaException("Formato não suportado. Envie o exame em PDF, JPG ou PNG.");
    }

    /** Só o nome (sem pastas nem caracteres de controle), com a extensão do que foi guardado. */
    static String nomeArquivo(String original, TipoExame tipo) {
        String n = original == null ? "" : original.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"]", "").strip();
        int ponto = n.lastIndexOf('.');
        if (ponto > 0) n = n.substring(0, ponto);
        if (n.isBlank()) n = "exame";
        int max = MAX_NOME_ARQUIVO - tipo.extensao().length();
        if (n.length() > max) n = n.substring(0, max);
        return n + tipo.extensao();
    }

    private void aplicar(Exame e, Crianca c, ExameInput d) {
        String titulo = d == null || d.titulo() == null ? "" : d.titulo().strip();
        if (titulo.isEmpty()) throw new IllegalArgumentException("Dê um nome ao exame (ex.: Hemograma)");
        if (titulo.length() > MAX_TITULO) throw new IllegalArgumentException("O nome do exame pode ter até " + MAX_TITULO + " caracteres");
        if (d.data() == null) throw new IllegalArgumentException("Informe a data do exame");
        if (d.data().isAfter(LocalDate.now(relogio))) throw new IllegalArgumentException("A data do exame não pode ser no futuro");
        if (c.getNascimento() != null && d.data().isBefore(c.getNascimento().minusMonths(10)))
            throw new IllegalArgumentException("A data do exame é anterior à gestação");
        String obs = d.observacao() == null ? null : d.observacao().strip();
        if (obs != null && obs.length() > MAX_OBSERVACAO)
            throw new IllegalArgumentException("A observação pode ter até " + MAX_OBSERVACAO + " caracteres");
        e.setTitulo(titulo);
        e.setData(d.data());
        e.setObservacao(obs == null || obs.isEmpty() ? null : obs);
    }

    private static ExameResponse resposta(Exame e, boolean jaExistia) {
        return new ExameResponse(e.getId(), e.getCriancaId(), e.getTitulo(), e.getData(), e.getObservacao(),
                e.getNomeArquivo(), e.getTipo(), e.getBytes(), e.getCriadoEm(), jaExistia);
    }

    private Crianca crianca(UUID id, boolean escrita) {
        Crianca c = criancas.findById(id).orElseThrow(() -> new NaoEncontradoException("Criança não encontrada"));
        if (escrita) acesso.exigirEscrita(c.getFamiliaId());
        else acesso.exigirLeitura(c.getFamiliaId());
        return c;
    }

    private Exame carregar(Crianca c, UUID id) {
        return exames.findById(id).filter(e -> e.getCriancaId().equals(c.getId()))
                .orElseThrow(() -> new NaoEncontradoException("Exame não encontrado"));
    }

    /** Se a transação for desfeita depois de gravar o arquivo, ele não fica órfão no disco. */
    private void apagarSeDesfizer(String chave) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) apagarArquivo(chave);
            }
        });
    }

    /** Só apaga os arquivos quando a exclusão no banco foi confirmada. */
    private void apagarDepoisDeConfirmar(List<String> chaves) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            chaves.forEach(this::apagarArquivo);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chaves.forEach(ExameService.this::apagarArquivo);
            }
        });
    }

    private void apagarArquivo(String chave) {
        try {
            storage.apagar(chave);
        } catch (RuntimeException e) {
            log.warn("Não apaguei {}: {}", chave, e.getMessage());
        }
    }

    static String sha256(byte[] dados) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dados));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
