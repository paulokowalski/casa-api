package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.Carro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarroRepository extends JpaRepository<Carro, UUID> {

    @Query("""
            select carro
            from Carro carro
            where carro.codigoVeiculo = :codigoVeiculo
            order by substring(carro.mesReferencia, 4, 4), substring(carro.mesReferencia, 1, 2)
            """)
    List<Carro> findByCodigoVeiculoOrderByMesReferencia(String codigoVeiculo);

}
