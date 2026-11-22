package com.kowalski.casaapi.crescimento.album;

import com.kowalski.casaapi.crescimento.album.AlbumDtos.*;
import com.kowalski.casaapi.crescimento.album.CatalogoAlbum.Lugar;
import com.kowalski.casaapi.crescimento.album.CatalogoAlbum.TipoLugar;
import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.CriancaService;
import com.kowalski.casaapi.crescimento.crianca.Idade;
import com.kowalski.casaapi.crescimento.diario.Registro;
import com.kowalski.casaapi.crescimento.diario.RegistroDtos.RegistroInput;
import com.kowalski.casaapi.crescimento.diario.RegistroDtos.RegistroResponse;
import com.kowalski.casaapi.crescimento.diario.RegistroService;
import com.kowalski.casaapi.crescimento.diario.TipoRegistro;
import com.kowalski.casaapi.crescimento.foto.FotoDtos.FotoResponse;
import com.kowalski.casaapi.crescimento.foto.FotoService;
import com.kowalski.casaapi.crescimento.medida.Medicao;
import com.kowalski.casaapi.crescimento.medida.MedicaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AlbumService {

    /** Mêsversários futuros aparecem até esta antecedência. */
    static final int DIAS_ANTECEDENCIA = 45;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final CriancaService criancas;
    private final RegistroService registros;
    private final FotoService fotos;
    private final MedicaoRepository medicoes;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public AlbumResponse album(UUID criancaId) {
        Crianca c = criancas.carregar(criancaId, false);
        LocalDate hoje = LocalDate.now(relogio);
        Map<String, Registro> regs = registros.doAlbum(criancaId);
        Map<UUID, List<FotoResponse>> fs = fotos.doRegistros(regs.values().stream().map(Registro::getId).toList());
        List<Medicao> meds = medicoes.findByCriancaIdOrderByDataAscCriadoEmAsc(criancaId);

        List<LugarResponse> mes = new ArrayList<>();
        int comFoto = 0, passados = 0;
        for (Lugar l : CatalogoAlbum.mesversarios(c.getNascimento(), hoje)) {
            LocalDate prevista = l.data(c.getNascimento());
            int dias = Idade.dias(hoje, prevista);
            Registro r = regs.get(l.chave());
            List<FotoResponse> f = r == null ? List.of() : fs.getOrDefault(r.getId(), List.of());
            if (!prevista.isAfter(hoje)) passados++;
            if (!f.isEmpty()) comFoto++;
            if (dias > DIAS_ANTECEDENCIA && f.isEmpty()) continue;
            Situacao s = !f.isEmpty() ? Situacao.FEITO : Math.abs(dias) <= 3 ? Situacao.HOJE : dias > 0 ? Situacao.FUTURO : Situacao.PENDENTE;
            mes.add(lugar(l, prevista, s, dias, r, f, meds));
        }
        List<LugarResponse> primeiras = CatalogoAlbum.PRIMEIRAS_VEZES.stream().map(l -> {
            Registro r = regs.get(l.chave());
            List<FotoResponse> f = r == null ? List.of() : fs.getOrDefault(r.getId(), List.of());
            return lugar(l, null, f.isEmpty() ? Situacao.PENDENTE : Situacao.FEITO, null, r, f, meds);
        }).toList();
        List<LugarResponse> especiais = regs.values().stream().filter(r -> r.getAlbumChave().startsWith("especial-"))
                .map(r -> {
                    Lugar l = CatalogoAlbum.lugar(r.getAlbumChave(), r.getAlbumTitulo()).orElseThrow();
                    List<FotoResponse> f = fs.getOrDefault(r.getId(), List.of());
                    return lugar(l, null, f.isEmpty() ? Situacao.PENDENTE : Situacao.FEITO, null, r, f, meds);
                }).toList();
        int primeirasFeitas = (int) primeiras.stream().filter(p -> p.situacao() == Situacao.FEITO).count();
        return new AlbumResponse(criancaId, (int) fotos.contar(criancaId), comFoto, passados, primeirasFeitas,
                chamada(c, hoje, regs, fs), mes, primeiras, especiais);
    }

    /** Cria ou altera as fotos de um lugar do álbum. */
    @Transactional
    public RegistroResponse salvar(UUID criancaId, String chave, AlbumInput dados) {
        Crianca c = criancas.carregar(criancaId, true);
        if (dados == null) throw new IllegalArgumentException("Informe as fotos");
        Registro existente = registros.doAlbum(criancaId).get(chave);
        String titulo = dados.albumTitulo() != null ? dados.albumTitulo() : existente != null ? existente.getAlbumTitulo() : null;
        Lugar l = CatalogoAlbum.lugar(chave, titulo).orElseThrow(() -> new NaoEncontradoException("Lugar do álbum desconhecido: " + chave));
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate data = dados.data();
        if (data == null) data = existente != null ? existente.getData() : padrao(l.data(c.getNascimento()), c.getNascimento(), hoje);

        Medicao m = existente == null || existente.getMedicaoId() == null ? null : medicoes.findById(existente.getMedicaoId()).orElse(null);
        TipoRegistro tipo = existente != null && existente.getTipo() == TipoRegistro.CONSULTA ? TipoRegistro.CONSULTA
                : dados.pesoKg() != null || (m != null && (m.getComprimentoCm() != null || m.getPerimetroCm() != null)) ? TipoRegistro.PESAGEM
                : TipoRegistro.MOMENTO;
        RegistroInput in = new RegistroInput(data, tipo, dados.texto(),
                existente != null ? existente.getProfissional() : null, existente != null ? existente.getProximaConsulta() : null,
                chave, titulo, dados.pesoKg(), m == null ? null : m.getComprimentoCm(), m == null ? null : m.getPerimetroCm(),
                dados.fotoIds());
        return registros.salvar(c, existente, in);
    }

    /** Novo momento especial (o nome vem em albumTitulo). */
    @Transactional
    public RegistroResponse novoEspecial(UUID criancaId, AlbumInput dados) {
        byte[] b = new byte[5];
        ALEATORIO.nextBytes(b);
        return salvar(criancaId, "especial-" + HexFormat.of().formatHex(b), dados);
    }

    @Transactional
    public void excluir(UUID criancaId, String chave) {
        Crianca c = criancas.carregar(criancaId, true);
        Registro r = registros.doAlbum(criancaId).get(chave);
        if (r == null) throw new NaoEncontradoException("Este lugar do álbum não tem registro");
        registros.excluir(c, r);
    }

    private static LocalDate padrao(LocalDate prevista, LocalDate nascimento, LocalDate hoje) {
        if (prevista == null || prevista.isAfter(hoje)) return hoje;
        return prevista.isBefore(nascimento) ? nascimento : prevista;
    }

    private LugarResponse lugar(Lugar l, LocalDate prevista, Situacao s, Integer dias, Registro r, List<FotoResponse> f, List<Medicao> meds) {
        Double peso = null;
        if (r != null && l.tipo() == TipoLugar.MESVERSARIO) peso = pesoEm(r, meds);
        return new LugarResponse(l.chave(), l.tipo(), l.titulo(), l.numero(), l.unidade(), prevista, s, dias,
                r == null ? null : r.getId(), r == null ? null : r.getData(), r == null ? null : r.getTexto(),
                f.isEmpty() ? null : f.get(f.size() - 1), f.size(), peso);
    }

    /** Peso do próprio registro ou a pesagem mais recente até 21 dias antes. */
    static Double pesoEm(Registro r, List<Medicao> meds) {
        if (r.getMedicaoId() != null)
            for (Medicao m : meds) if (m.getId().equals(r.getMedicaoId()) && m.getPesoKg() != null) return m.getPesoKg();
        Medicao melhor = null;
        for (Medicao m : meds)
            if (m.getPesoKg() != null && !m.getData().isAfter(r.getData()) && Idade.dias(m.getData(), r.getData()) <= 21) melhor = m;
        return melhor == null ? null : melhor.getPesoKg();
    }

    /** O mêsversário que passou há até 10 dias sem foto, o de hoje, ou o próximo em até 7 dias. */
    static Chamada chamada(Crianca c, LocalDate hoje, Map<String, Registro> regs, Map<UUID, List<FotoResponse>> fs) {
        List<Lugar> lugares = CatalogoAlbum.mesversarios(c.getNascimento(), hoje);
        Lugar recente = null;
        for (Lugar l : lugares) {
            LocalDate d = l.data(c.getNascimento());
            Registro r = regs.get(l.chave());
            boolean temFoto = r != null && !fs.getOrDefault(r.getId(), List.of()).isEmpty();
            if (!d.isAfter(hoje) && Idade.dias(d, hoje) <= 10 && !temFoto) recente = l;
        }
        if (recente != null) return chamada(recente, c, hoje);
        return lugares.stream().filter(l -> l.data(c.getNascimento()).isAfter(hoje)).findFirst()
                .filter(l -> Idade.dias(hoje, l.data(c.getNascimento())) <= 7).map(l -> chamada(l, c, hoje)).orElse(null);
    }

    private static Chamada chamada(Lugar l, Crianca c, LocalDate hoje) {
        LocalDate d = l.data(c.getNascimento());
        return new Chamada(l.chave(), l.titulo(), l.numero(), l.unidade(), d, Idade.dias(hoje, d));
    }
}
