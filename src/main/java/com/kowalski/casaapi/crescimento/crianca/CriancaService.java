package com.kowalski.casaapi.crescimento.crianca;

import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.crianca.CriancaDtos.*;
import com.kowalski.casaapi.crescimento.familia.AcessoFamilia;
import com.kowalski.casaapi.crescimento.exame.ExameService;
import com.kowalski.casaapi.crescimento.foto.FotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CriancaService {

    private final CriancaRepository criancas;
    private final AcessoFamilia acesso;
    private final FotoService fotos;
    private final ExameService exames;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public Crianca carregar(UUID id, boolean escrita) {
        Crianca c = criancas.findById(id).orElseThrow(() -> new NaoEncontradoException("Criança não encontrada"));
        if (escrita) acesso.exigirEscrita(c.getFamiliaId());
        else acesso.exigirLeitura(c.getFamiliaId());
        return c;
    }

    @Transactional(readOnly = true)
    public List<CriancaResponse> listar(UUID familiaId) {
        acesso.exigirLeitura(familiaId);
        boolean escrita = acesso.podeEscrever(familiaId);
        return criancas.findByFamiliaIdOrderByNascimento(familiaId).stream().map(c -> resposta(c, escrita)).toList();
    }

    @Transactional(readOnly = true)
    public CriancaResponse buscar(UUID id) {
        Crianca c = carregar(id, false);
        return resposta(c, acesso.podeEscrever(c.getFamiliaId()));
    }

    @Transactional
    public CriancaResponse criar(UUID familiaId, CriancaInput dados) {
        acesso.exigirEscrita(familiaId);
        Crianca c = new Crianca();
        c.setFamiliaId(familiaId);
        aplicar(c, dados);
        if (dados.fotoId() != null) throw new IllegalArgumentException("Envie a foto de perfil depois de cadastrar a criança");
        return resposta(criancas.save(c), true);
    }

    @Transactional
    public CriancaResponse atualizar(UUID id, CriancaInput dados) {
        Crianca c = carregar(id, true);
        aplicar(c, dados);
        if (dados.fotoId() != null) fotos.exigirDaCrianca(dados.fotoId(), c.getId());
        c.setFotoId(dados.fotoId());
        c.setAtualizadoEm(Instant.now());
        return resposta(criancas.save(c), true);
    }

    @Transactional
    public void excluir(UUID id) {
        Crianca c = carregar(id, true);
        c.setFotoId(null);
        criancas.saveAndFlush(c);
        fotos.excluirTodasDaCrianca(c.getId());
        exames.excluirTodosDaCrianca(c.getId());
        criancas.delete(c);
    }

    private void aplicar(Crianca c, CriancaInput d) {
        if (d == null) throw new IllegalArgumentException("Informe os dados da criança");
        String nome = d.nome() == null ? "" : d.nome().trim();
        if (nome.isEmpty()) throw new IllegalArgumentException("Informe o nome da criança");
        if (nome.length() > 60) throw new IllegalArgumentException("O nome pode ter até 60 caracteres");
        if (d.nascimento() == null) throw new IllegalArgumentException("Informe a data de nascimento");
        if (d.nascimento().isAfter(LocalDate.now(relogio))) throw new IllegalArgumentException("A data de nascimento não pode ser no futuro");
        if (d.sexo() == null) throw new IllegalArgumentException("Informe o sexo (as curvas da OMS são diferentes para meninos e meninas)");
        faixa(d.idadeGestacionalSemanas(), 22, 44, "A idade gestacional deve estar entre 22 e 44 semanas");
        faixa(d.pesoNascerKg(), 0.3, 7, "O peso ao nascer deve estar em kg, por exemplo 3,25");
        faixa(d.comprimentoNascerCm(), 20, 65, "O comprimento ao nascer deve estar em cm, por exemplo 49,5");
        faixa(d.perimetroNascerCm(), 18, 45, "O perímetro cefálico ao nascer deve estar em cm, por exemplo 34,5");
        c.setNome(nome);
        c.setNascimento(d.nascimento());
        c.setSexo(d.sexo());
        c.setIdadeGestacionalSemanas(d.idadeGestacionalSemanas());
        c.setPesoNascerKg(d.pesoNascerKg());
        c.setComprimentoNascerCm(d.comprimentoNascerCm());
        c.setPerimetroNascerCm(d.perimetroNascerCm());
    }

    static void faixa(Double v, double min, double max, String mensagem) {
        if (v != null && (v.isNaN() || v < min || v > max)) throw new IllegalArgumentException(mensagem);
    }

    private CriancaResponse resposta(Crianca c, boolean podeEditar) {
        LocalDate hoje = LocalDate.now(relogio);
        return new CriancaResponse(c.getId(), c.getFamiliaId(), c.getNome(), c.getNascimento(), c.getSexo(),
                c.getIdadeGestacionalSemanas(), c.getPesoNascerKg(), c.getComprimentoNascerCm(), c.getPerimetroNascerCm(),
                Idade.prematuro(c), Idade.dias(c.getNascimento(), hoje), Idade.diasCorrigidos(c, hoje),
                Idade.texto(c.getNascimento(), hoje), c.getFotoId() == null ? null : fotos.resposta(c.getFotoId()), podeEditar);
    }
}
