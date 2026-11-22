package com.kowalski.casaapi.crescimento.diario;

import com.kowalski.casaapi.crescimento.album.CatalogoAlbum;
import com.kowalski.casaapi.crescimento.album.CatalogoAlbum.Lugar;
import com.kowalski.casaapi.crescimento.album.CatalogoAlbum.TipoLugar;
import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.CriancaService;
import com.kowalski.casaapi.crescimento.crianca.Idade;
import com.kowalski.casaapi.crescimento.diario.RegistroDtos.*;
import com.kowalski.casaapi.crescimento.familia.AcessoFamilia;
import com.kowalski.casaapi.crescimento.foto.FotoDtos.FotoResponse;
import com.kowalski.casaapi.crescimento.foto.FotoService;
import com.kowalski.casaapi.crescimento.identidade.DiretorioUsuarios;
import com.kowalski.casaapi.crescimento.medida.Medicao;
import com.kowalski.casaapi.crescimento.medida.MedicaoRepository;
import com.kowalski.casaapi.crescimento.medida.MedicaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistroService {

    private final RegistroRepository registros;
    private final CriancaService criancas;
    private final MedicaoService medicoes;
    private final MedicaoRepository medicaoRepository;
    private final FotoService fotos;
    private final AcessoFamilia acesso;
    private final DiretorioUsuarios usuarios;
    private final Clock relogio;

    // ---------------------------------------------------------------- consultas

    /** Registros e medições avulsas, do mais recente ao mais antigo. {@code tipo} filtra (PESAGEM inclui as medições). */
    @Transactional(readOnly = true)
    public LinhaDoTempoResponse linhaDoTempo(UUID criancaId, TipoRegistro tipo, boolean soComFotos, int pagina, int tamanho) {
        if (pagina < 0) throw new IllegalArgumentException("A página começa em 0");
        if (tamanho < 1 || tamanho > 100) throw new IllegalArgumentException("O tamanho da página vai de 1 a 100");
        Crianca c = criancas.carregar(criancaId, false);
        List<Registro> regs = registros.findByCriancaIdOrderByDataDescCriadoEmDesc(criancaId);
        List<Medicao> meds = medicaoRepository.findByCriancaIdOrderByDataAscCriadoEmAsc(criancaId);
        Map<UUID, int[]> ganhos = ganhos(meds);
        Map<UUID, Medicao> medPorId = meds.stream().collect(Collectors.toMap(Medicao::getId, m -> m));
        Set<UUID> ligadas = regs.stream().map(Registro::getMedicaoId).filter(Objects::nonNull).collect(Collectors.toSet());

        record Item(LocalDate data, Instant criadoEm, Registro reg, Medicao med) {
        }
        List<Item> todos = new ArrayList<>();
        regs.stream().filter(r -> tipo == null || r.getTipo() == tipo)
                .forEach(r -> todos.add(new Item(r.getData(), r.getCriadoEm(), r, null)));
        if (!soComFotos && (tipo == null || tipo == TipoRegistro.PESAGEM))
            meds.stream().filter(m -> !ligadas.contains(m.getId()))
                    .forEach(m -> todos.add(new Item(m.getData(), m.getCriadoEm(), null, m)));
        Map<UUID, List<FotoResponse>> fotosDe;
        List<Item> filtrados = todos;
        if (soComFotos) {
            fotosDe = fotos.doRegistros(regs.stream().map(Registro::getId).toList());
            filtrados = todos.stream().filter(i -> !fotosDe.getOrDefault(i.reg().getId(), List.of()).isEmpty()).toList();
        } else {
            fotosDe = null;
        }
        filtrados = filtrados.stream().sorted(Comparator.comparing(Item::data).thenComparing(Item::criadoEm).reversed()).toList();

        int ini = Math.min(filtrados.size(), pagina * tamanho), fim = Math.min(filtrados.size(), ini + tamanho);
        List<Item> pag = filtrados.subList(ini, fim);
        List<Registro> regsPag = pag.stream().map(Item::reg).filter(Objects::nonNull).toList();
        Map<UUID, List<FotoResponse>> fotosPag = fotosDe != null ? fotosDe : fotos.doRegistros(regsPag.stream().map(Registro::getId).toList());
        Map<Long, String> nomes = nomes(regsPag);
        List<ItemLinhaDoTempo> itens = pag.stream().map(i -> {
            Medicao m = i.reg() != null ? (i.reg().getMedicaoId() == null ? null : medPorId.get(i.reg().getMedicaoId())) : i.med();
            int[] g = m == null ? null : ganhos.get(m.getId());
            RegistroResponse rr = i.reg() == null ? null : resposta(c, i.reg(), m, fotosPag.getOrDefault(i.reg().getId(), List.of()), nomes);
            return new ItemLinhaDoTempo(i.reg() != null ? "REGISTRO" : "MEDICAO", i.reg() != null ? i.reg().getTipo() : TipoRegistro.PESAGEM,
                    i.data(), Idade.dias(c.getNascimento(), i.data()), Idade.texto(c.getNascimento(), i.data()), mesDeVida(c, i.data()),
                    rr, i.reg() == null ? medicoes.resposta(c, m) : null, g == null ? null : g[0], g == null ? null : g[1]);
        }).toList();
        return new LinhaDoTempoResponse(itens, pagina, tamanho, filtrados.size(), fim < filtrados.size());
    }

    @Transactional(readOnly = true)
    public RegistroResponse buscar(UUID criancaId, UUID id) {
        Crianca c = criancas.carregar(criancaId, false);
        return resposta(c, daCrianca(criancaId, id));
    }

    // ---------------------------------------------------------------- alterações

    @Transactional
    public RegistroResponse criar(UUID criancaId, RegistroInput dados) {
        Crianca c = criancas.carregar(criancaId, true);
        return salvar(c, null, dados);
    }

    @Transactional
    public RegistroResponse atualizar(UUID criancaId, UUID id, RegistroInput dados) {
        Crianca c = criancas.carregar(criancaId, true);
        return salvar(c, daCrianca(criancaId, id), dados);
    }

    @Transactional
    public void excluir(UUID criancaId, UUID id) {
        Crianca c = criancas.carregar(criancaId, true);
        excluir(c, daCrianca(criancaId, id));
    }

    /** Apaga o registro com as fotos e as medidas dele. O acesso já foi conferido. */
    @Transactional
    public void excluir(Crianca c, Registro r) {
        fotos.excluirDoRegistro(c, r.getId());
        UUID medicaoId = r.getMedicaoId();
        registros.delete(r);
        registros.flush();
        medicoes.excluirDoRegistro(medicaoId);
    }

    /**
     * Cria ({@code existente == null}) ou altera um registro. O acesso de escrita já foi conferido por quem chama.
     */
    @Transactional
    public RegistroResponse salvar(Crianca c, Registro existente, RegistroInput d) {
        if (d == null) throw new IllegalArgumentException("Informe os dados do registro");
        LocalDate hoje = LocalDate.now(relogio);
        if (d.data() == null) throw new IllegalArgumentException("Informe a data");
        if (d.data().isBefore(c.getNascimento())) throw new IllegalArgumentException("A data é anterior ao nascimento");
        if (d.data().isAfter(hoje)) throw new IllegalArgumentException("A data não pode ser no futuro");
        if (d.tipo() == null) throw new IllegalArgumentException("Informe o tipo do registro");
        String texto = limpo(d.texto(), 3000, "O texto pode ter até 3000 caracteres");
        boolean consulta = d.tipo() == TipoRegistro.CONSULTA;
        String profissional = consulta ? limpo(d.profissional(), 80, "O profissional ou local pode ter até 80 caracteres") : null;
        LocalDate retorno = consulta ? d.proximaConsulta() : null;
        if (retorno != null && (retorno.isBefore(d.data()) || retorno.isAfter(d.data().plusYears(2))))
            throw new IllegalArgumentException("O retorno deve ser depois da consulta (até 2 anos)");

        Lugar lugar = null;
        String chave = d.albumChave() == null || d.albumChave().isBlank() ? null : d.albumChave().trim();
        String tituloEspecial = null;
        if (chave != null) {
            tituloEspecial = limpo(d.albumTitulo(), 50, "O nome do momento pode ter até 50 caracteres");
            lugar = CatalogoAlbum.lugar(chave, tituloEspecial).orElseThrow(() -> new IllegalArgumentException("Lugar do álbum desconhecido: " + chave));
            if (lugar.tipo() == TipoLugar.ESPECIAL && tituloEspecial == null) throw new IllegalArgumentException("Dê um nome para o momento especial");
            if (lugar.tipo() != TipoLugar.ESPECIAL) tituloEspecial = null;
            registros.findByCriancaIdAndAlbumChave(c.getId(), chave)
                    .filter(outro -> existente == null || !outro.getId().equals(existente.getId()))
                    .ifPresent(outro -> {
                        throw new IllegalArgumentException("Este lugar do álbum já tem um registro; altere o existente");
                    });
        }

        List<UUID> fotoIds = d.fotoIds();
        if (fotoIds == null) fotoIds = existente == null ? List.of()
                : fotos.doRegistros(List.of(existente.getId())).getOrDefault(existente.getId(), List.of()).stream().map(FotoResponse::id).toList();
        boolean temMedida = d.pesoKg() != null || d.comprimentoCm() != null || d.perimetroCm() != null;
        if (lugar != null && fotoIds.isEmpty()) throw new IllegalArgumentException("Escolha pelo menos uma foto para o álbum");
        if (d.tipo() == TipoRegistro.PESAGEM && !temMedida) throw new IllegalArgumentException("Informe pelo menos uma medida");
        if (d.tipo() == TipoRegistro.MOMENTO && texto == null && fotoIds.isEmpty())
            throw new IllegalArgumentException("Escreva algo ou escolha uma foto");

        Registro r = existente != null ? existente : new Registro();
        if (existente == null) {
            r.setCriancaId(c.getId());
            r.setCriadoPor(acesso.usuarioId());
        } else {
            r.setAtualizadoEm(Instant.now());
        }
        Medicao m = medicoes.gravarDoRegistro(c, r.getMedicaoId(), d.data(), d.pesoKg(), d.comprimentoCm(), d.perimetroCm());
        r.setMedicaoId(m == null ? null : m.getId());
        r.setData(d.data());
        r.setTipo(d.tipo());
        r.setTexto(texto);
        r.setProfissional(profissional);
        r.setProximaConsulta(retorno);
        r.setAlbumChave(chave);
        r.setAlbumTitulo(tituloEspecial);
        try {
            r = registros.saveAndFlush(r);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Este lugar do álbum já tem um registro; altere o existente");
        }
        fotos.definirFotosDoRegistro(c, r.getId(), fotoIds);
        return resposta(c, r);
    }

    /** Registros do álbum por chave (para a tela do álbum). */
    @Transactional(readOnly = true)
    public Map<String, Registro> doAlbum(UUID criancaId) {
        Map<String, Registro> r = new LinkedHashMap<>();
        registros.findByCriancaIdAndAlbumChaveNotNull(criancaId).stream()
                .sorted(Comparator.comparing(Registro::getData)).forEach(x -> r.put(x.getAlbumChave(), x));
        return r;
    }

    // ---------------------------------------------------------------- montagem

    private RegistroResponse resposta(Crianca c, Registro r) {
        Medicao m = r.getMedicaoId() == null ? null : medicaoRepository.findById(r.getMedicaoId()).orElse(null);
        return resposta(c, r, m, fotos.doRegistros(List.of(r.getId())).getOrDefault(r.getId(), List.of()), nomes(List.of(r)));
    }

    private RegistroResponse resposta(Crianca c, Registro r, Medicao m, List<FotoResponse> fs, Map<Long, String> nomes) {
        AlbumRef album = r.getAlbumChave() == null ? null : CatalogoAlbum.lugar(r.getAlbumChave(), r.getAlbumTitulo())
                .map(l -> new AlbumRef(l.chave(), l.titulo(), CatalogoAlbum.rotulo(l))).orElse(null);
        return new RegistroResponse(r.getId(), r.getCriancaId(), r.getData(), Idade.dias(c.getNascimento(), r.getData()),
                Idade.texto(c.getNascimento(), r.getData()), mesDeVida(c, r.getData()), r.getTipo(), r.getTexto(), r.getProfissional(),
                r.getProximaConsulta(), album, m == null ? null : medicoes.resposta(c, m), fs, r.getCriadoPor(),
                r.getCriadoPor() == null ? null : nomes.get(r.getCriadoPor()), r.getCriadoEm(), r.getAtualizadoEm());
    }

    private Map<Long, String> nomes(List<Registro> regs) {
        Set<Long> ids = regs.stream().map(Registro::getCriadoPor).filter(Objects::nonNull).collect(Collectors.toSet());
        return ids.isEmpty() ? Map.of() : usuarios.nomes(ids);
    }

    /** Diferença de peso (gramas, dias) de cada medição para a pesagem anterior. */
    static Map<UUID, int[]> ganhos(List<Medicao> meds) {
        Map<UUID, int[]> r = new HashMap<>();
        Medicao ant = null;
        for (Medicao m : meds.stream().filter(x -> x.getPesoKg() != null).sorted(Comparator.comparing(Medicao::getData)).toList()) {
            if (ant != null) r.put(m.getId(), new int[]{(int) Math.round((m.getPesoKg() - ant.getPesoKg()) * 1000), Idade.dias(ant.getData(), m.getData())});
            ant = m;
        }
        return r;
    }

    static int mesDeVida(Crianca c, LocalDate data) {
        return Idade.mesesCompletos(c.getNascimento(), data);
    }

    private Registro daCrianca(UUID criancaId, UUID id) {
        return registros.findById(id).filter(r -> r.getCriancaId().equals(criancaId))
                .orElseThrow(() -> new NaoEncontradoException("Registro não encontrado"));
    }

    private static String limpo(String s, int max, String mensagem) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        if (s.length() > max) throw new IllegalArgumentException(mensagem);
        return s;
    }
}
