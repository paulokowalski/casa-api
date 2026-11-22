package com.kowalski.casaapi.crescimento.foto;

import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.comum.SemPermissaoException;
import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.CriancaRepository;
import com.kowalski.casaapi.crescimento.familia.AcessoFamilia;
import com.kowalski.casaapi.crescimento.foto.FotoDtos.FotoResponse;
import com.kowalski.casaapi.crescimento.foto.FotoDtos.FotoUrls;
import com.kowalski.casaapi.crescimento.foto.ProcessadorImagem.ImagemProcessada;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FotoService {

    public static final long TAMANHO_MAXIMO = 25L * 1024 * 1024;
    public static final int MAX_FOTOS_REGISTRO = 30;

    private final FotoRepository fotos;
    private final CriancaRepository criancas;
    private final AcessoFamilia acesso;
    private final FotoStorage storage;
    private final ProcessadorImagem processador;
    private final LinkAssinado links;
    private final Clock relogio;

    @Transactional
    public FotoResponse enviar(UUID criancaId, byte[] dados) {
        Crianca c = crianca(criancaId, true);
        if (dados == null || dados.length == 0) throw new IllegalArgumentException("Escolha uma foto");
        if (dados.length > TAMANHO_MAXIMO) throw new IllegalArgumentException("A foto é grande demais. O limite é de 25 MB por foto.");
        String sha = sha256(dados);
        var existente = fotos.findByCriancaIdAndSha256(criancaId, sha);
        if (existente.isPresent()) return resposta(existente.get(), true);

        ImagemProcessada img = processador.processar(dados);
        int ano = img.tiradaEm() != null ? img.tiradaEm().getYear() : LocalDate.now(relogio).getYear();
        String chave = c.getFamiliaId() + "/" + c.getId() + "/" + ano + "/" + UUID.randomUUID();
        long total = 0;
        try {
            for (var e : img.arquivos().entrySet()) {
                storage.gravar(chave + e.getKey().sufixo(), e.getValue());
                total += e.getValue().length;
            }
            Foto f = new Foto();
            f.setFamiliaId(c.getFamiliaId());
            f.setCriancaId(c.getId());
            f.setChave(chave);
            f.setLargura(img.largura());
            f.setAltura(img.altura());
            f.setBytes(total);
            f.setSha256(sha);
            f.setTiradaEm(img.tiradaEm());
            f.setEnviadaPor(acesso.usuarioId());
            f = fotos.saveAndFlush(f);
            apagarSeDesfizer(chave);
            return resposta(f, false);
        } catch (RuntimeException e) {
            apagarArquivos(chave);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<FotoResponse> listar(UUID criancaId) {
        crianca(criancaId, false);
        return fotos.findByCriancaIdOrderByCriadaEmDesc(criancaId).stream().map(f -> resposta(f, false)).toList();
    }

    @Transactional(readOnly = true)
    public FotoResponse buscar(UUID id) {
        return resposta(carregar(id, false), false);
    }

    @Transactional
    public void excluir(UUID id) {
        Foto f = carregar(id, true);
        criancas.findById(f.getCriancaId()).filter(c -> id.equals(c.getFotoId())).ifPresent(c -> {
            c.setFotoId(null);
            criancas.saveAndFlush(c);
        });
        fotos.delete(f);
        apagarDepoisDeConfirmar(List.of(f.getChave()));
    }

    /** Chamado ao excluir a criança (o acesso já foi conferido). */
    @Transactional
    public void excluirTodasDaCrianca(UUID criancaId) {
        List<Foto> lista = fotos.findByCriancaIdOrderByCriadaEmDesc(criancaId);
        fotos.deleteAll(lista);
        apagarDepoisDeConfirmar(lista.stream().map(Foto::getChave).toList());
    }

    /**
     * Coloca as fotos no registro, na ordem dada. Fotos que estavam no registro e saíram da lista são apagadas
     * (a de perfil da criança só é desligada do registro). O acesso já foi conferido por quem chama.
     */
    @Transactional
    public void definirFotosDoRegistro(Crianca c, UUID registroId, List<UUID> ids) {
        List<UUID> lista = ids == null ? List.of() : ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (lista.size() > MAX_FOTOS_REGISTRO) throw new IllegalArgumentException("Cada registro pode ter até " + MAX_FOTOS_REGISTRO + " fotos");
        java.util.Map<UUID, Foto> porId = new java.util.HashMap<>();
        fotos.findAllById(lista).forEach(f -> porId.put(f.getId(), f));
        for (int i = 0; i < lista.size(); i++) {
            Foto f = porId.get(lista.get(i));
            if (f == null || !f.getCriancaId().equals(c.getId())) throw new IllegalArgumentException("Foto não encontrada para esta criança");
            if (f.getRegistroId() != null && !f.getRegistroId().equals(registroId))
                throw new IllegalArgumentException("Uma das fotos já está em outro registro");
            f.setRegistroId(registroId);
            f.setOrdem((short) i);
        }
        fotos.saveAll(porId.values());
        for (Foto f : fotos.findByRegistroIdOrderByOrdemAsc(registroId))
            if (!lista.contains(f.getId())) tirarDoRegistro(c, f);
    }

    /** Ao excluir um registro: apaga as fotos dele (menos a de perfil, que só fica solta). */
    @Transactional
    public void excluirDoRegistro(Crianca c, UUID registroId) {
        fotos.findByRegistroIdOrderByOrdemAsc(registroId).forEach(f -> tirarDoRegistro(c, f));
    }

    /** Fotos de cada registro, na ordem, com links novos. */
    @Transactional(readOnly = true)
    public java.util.Map<UUID, List<FotoResponse>> doRegistros(java.util.Collection<UUID> registroIds) {
        java.util.Map<UUID, List<FotoResponse>> r = new java.util.HashMap<>();
        if (registroIds.isEmpty()) return r;
        for (Foto f : fotos.findByRegistroIdInOrderByOrdemAsc(registroIds))
            r.computeIfAbsent(f.getRegistroId(), k -> new java.util.ArrayList<>()).add(resposta(f, false));
        return r;
    }

    private void tirarDoRegistro(Crianca c, Foto f) {
        if (f.getId().equals(c.getFotoId())) {
            f.setRegistroId(null);
            f.setOrdem(null);
            fotos.save(f);
        } else {
            fotos.delete(f);
            apagarDepoisDeConfirmar(List.of(f.getChave()));
        }
    }

    public long contar(UUID criancaId) {
        return fotos.countByCriancaId(criancaId);
    }

    public void exigirDaCrianca(UUID fotoId, UUID criancaId) {
        fotos.findById(fotoId).filter(f -> f.getCriancaId().equals(criancaId))
                .orElseThrow(() -> new IllegalArgumentException("A foto de perfil precisa ser uma foto desta criança"));
    }

    /** Resposta com links novos; sem conferir acesso (quem chama já conferiu). */
    public FotoResponse resposta(UUID fotoId) {
        return fotos.findById(fotoId).map(f -> resposta(f, false)).orElse(null);
    }

    /** Arquivo de um link assinado. Não exige login: a assinatura é a autorização. */
    @Transactional(readOnly = true)
    public Resource arquivo(UUID id, Variante v, long exp, String sig) {
        if (!links.valido(id, v, exp, sig)) throw new SemPermissaoException("Link da foto inválido ou vencido");
        Foto f = fotos.findById(id).orElseThrow(() -> new NaoEncontradoException("Foto não encontrada"));
        return storage.ler(f.getChave() + v.sufixo());
    }

    private FotoResponse resposta(Foto f, boolean jaExistia) {
        String base = caminhoApi() + "/v1/crescimento/fotos/" + f.getId() + "/arquivo";
        LinkAssinado.Assinatura mini = links.assinar(f.getId(), Variante.MINI);
        LinkAssinado.Assinatura media = links.assinar(f.getId(), Variante.MEDIA);
        LinkAssinado.Assinatura orig = links.assinar(f.getId(), Variante.ORIGINAL);
        FotoUrls urls = new FotoUrls(url(base, Variante.MINI, mini), url(base, Variante.MEDIA, media),
                url(base, Variante.ORIGINAL, orig), links.vencimento(mini));
        return new FotoResponse(f.getId(), f.getCriancaId(), f.getLargura(), f.getAltura(), f.getTiradaEm(),
                f.getCriadaEm(), f.getEnviadaPor(), f.getRegistroId(), jaExistia, urls);
    }

    private static String url(String base, Variante v, LinkAssinado.Assinatura a) {
        return base + "?v=" + v.name() + "&exp=" + a.exp() + "&sig=" + a.sig();
    }

    /** Prefixo da API como o cliente a enxerga (ex.: /casa-api), para os links funcionarem atrás do proxy. */
    private static String caminhoApi() {
        try {
            String p = ServletUriComponentsBuilder.fromCurrentContextPath().build().getPath();
            return p == null ? "" : p;
        } catch (IllegalStateException semRequisicao) {
            return "";
        }
    }

    private Crianca crianca(UUID id, boolean escrita) {
        Crianca c = criancas.findById(id).orElseThrow(() -> new NaoEncontradoException("Criança não encontrada"));
        if (escrita) acesso.exigirEscrita(c.getFamiliaId());
        else acesso.exigirLeitura(c.getFamiliaId());
        return c;
    }

    private Foto carregar(UUID id, boolean escrita) {
        Foto f = fotos.findById(id).orElseThrow(() -> new NaoEncontradoException("Foto não encontrada"));
        if (escrita) acesso.exigirEscrita(f.getFamiliaId());
        else acesso.exigirLeitura(f.getFamiliaId());
        return f;
    }

    /** Se a transação for desfeita depois de gravar os arquivos, eles não ficam órfãos no disco. */
    private void apagarSeDesfizer(String chave) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) apagarArquivos(chave);
            }
        });
    }

    /** Só apaga os arquivos quando a exclusão no banco foi confirmada. */
    private void apagarDepoisDeConfirmar(List<String> chaves) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            chaves.forEach(this::apagarArquivos);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chaves.forEach(FotoService.this::apagarArquivos);
            }
        });
    }

    private void apagarArquivos(String chave) {
        for (Variante v : Variante.values()) {
            try {
                storage.apagar(chave + v.sufixo());
            } catch (RuntimeException e) {
                log.warn("Não apaguei {}{}: {}", chave, v.sufixo(), e.getMessage());
            }
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
