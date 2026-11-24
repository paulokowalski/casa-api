package com.kowalski.casaapi.despesa;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class DespesaRepository {

    private static final String TOTAIS_DO_MES = """
            select new com.kowalski.casaapi.despesa.DespesaResponse(
                sum(cp.valorParcela),
                sum(case when c.numeroParcelas > 1 then cp.valorParcela else 0 end),
                sum(case when cp.numeroParcela = 1 and c.numeroParcelas = 1 and cp.valorParcela > 0 then cp.valorParcela else 0 end))
            from CompraParcela cp
            join cp.compra c
            join c.pessoa p
            where upper(p.nome) = upper(:nomePessoa)
              and cp.dataParcela >= :inicio and cp.dataParcela < :fim
            """;

    private final EntityManager entityManager;

    public DespesaResponse totaisDoMes(String nomePessoa, LocalDate inicioDoMes) {
        return entityManager.createQuery(TOTAIS_DO_MES, DespesaResponse.class)
                .setParameter("nomePessoa", nomePessoa)
                .setParameter("inicio", inicioDoMes)
                .setParameter("fim", inicioDoMes.plusMonths(1))
                .getSingleResult();
    }
}
