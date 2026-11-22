package com.kowalski.casaapi.crescimento.agenda;

import com.kowalski.casaapi.crescimento.album.CatalogoAlbum;
import com.kowalski.casaapi.crescimento.album.CatalogoAlbum.Lugar;
import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.CriancaService;
import com.kowalski.casaapi.crescimento.crianca.Idade;
import com.kowalski.casaapi.crescimento.diario.RegistroRepository;
import com.kowalski.casaapi.crescimento.vacina.VacinaAplicadaRepository;
import com.kowalski.casaapi.crescimento.vacina.VacinaDtos.DoseResponse;
import com.kowalski.casaapi.crescimento.vacina.VacinaDtos.SituacaoVacina;
import com.kowalski.casaapi.crescimento.vacina.VacinaService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

/** O que vem pela frente: vacinas (e as atrasadas), a próxima consulta e o próximo mêsversário. */
@Service
@RequiredArgsConstructor
public class AgendaService {

    static final int DIAS_A_FRENTE = 60;

    public enum TipoCompromisso { VACINA, CONSULTA, MESVERSARIO }

    @Schema(description = "Compromisso da agenda")
    public record Compromisso(TipoCompromisso tipo, LocalDate data,
                              @Schema(description = "Dias até a data (negativo = atrasado)") int dias,
                              boolean atrasado, String titulo, String detalhe,
                              @Schema(description = "Ids das vacinas ou a chave do álbum") List<String> referencias) {
    }

    private final CriancaService criancas;
    private final VacinaAplicadaRepository vacinas;
    private final RegistroRepository registros;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public List<Compromisso> agenda(UUID criancaId) {
        Crianca c = criancas.carregar(criancaId, false);
        LocalDate hoje = LocalDate.now(relogio);
        return montar(c, VacinaService.montar(c, vacinas.findByCriancaId(criancaId), hoje).grupos().stream()
                .flatMap(g -> g.doses().stream()).toList(), registros.proximaConsulta(criancaId, hoje), hoje);
    }

    public static List<Compromisso> montar(Crianca c, List<DoseResponse> doses, LocalDate proximaConsulta, LocalDate hoje) {
        List<Compromisso> r = new ArrayList<>();
        Map<LocalDate, List<DoseResponse>> porData = new TreeMap<>();
        for (DoseResponse d : doses) {
            boolean pendente = d.situacao() == SituacaoVacina.ATRASADA || d.situacao() == SituacaoVacina.TOMAR_AGORA
                    || (d.situacao() == SituacaoVacina.PROXIMA || d.situacao() == SituacaoVacina.FUTURA) && !d.dataPrevista().isAfter(hoje.plusDays(DIAS_A_FRENTE));
            if (pendente) porData.computeIfAbsent(d.dataPrevista().isBefore(hoje) ? hoje : d.dataPrevista(), k -> new ArrayList<>()).add(d);
        }
        porData.forEach((data, ds) -> {
            boolean atrasado = ds.stream().anyMatch(d -> d.dataPrevista().isBefore(hoje));
            LocalDate mais = ds.stream().map(DoseResponse::dataPrevista).min(Comparator.naturalOrder()).orElse(data);
            r.add(new Compromisso(TipoCompromisso.VACINA, atrasado ? mais : data, Idade.dias(hoje, atrasado ? mais : data), atrasado,
                    ds.size() == 1 ? "Vacina: " + ds.get(0).vacina() : ds.size() + " vacinas",
                    String.join(", ", ds.stream().map(d -> d.vacina() + " (" + d.dose() + ")").toList()),
                    ds.stream().map(DoseResponse::id).toList()));
        });
        if (proximaConsulta != null)
            r.add(new Compromisso(TipoCompromisso.CONSULTA, proximaConsulta, Idade.dias(hoje, proximaConsulta), false,
                    "Consulta de retorno", null, List.of()));
        CatalogoAlbum.mesversarios(c.getNascimento(), hoje).stream()
                .filter(l -> l.data(c.getNascimento()).isAfter(hoje.minusDays(1)))
                .min(Comparator.comparing((Lugar l) -> l.data(c.getNascimento())))
                .filter(l -> !l.data(c.getNascimento()).isAfter(hoje.plusDays(DIAS_A_FRENTE)))
                .ifPresent(l -> r.add(new Compromisso(TipoCompromisso.MESVERSARIO, l.data(c.getNascimento()),
                        Idade.dias(hoje, l.data(c.getNascimento())), false, CatalogoAlbum.rotulo(l), "Hora da foto do álbum", List.of(l.chave()))));
        r.sort(Comparator.comparing(Compromisso::data).thenComparing(Compromisso::tipo));
        return r;
    }
}
