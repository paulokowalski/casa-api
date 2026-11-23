package com.kowalski.casaapi.carro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CarroRepository extends JpaRepository<Carro, UUID> {

    @Query("""
            select new com.kowalski.casaapi.carro.CarroResponse(
                carro.id, carro.codigoVeiculo, carro.mesReferencia, carro.marca, carro.modelo, carro.ano, carro.valor)
            from Carro carro
            where carro.codigoVeiculo = :codigoVeiculo
            order by substring(carro.mesReferencia, 4, 4), substring(carro.mesReferencia, 1, 2)
            """)
    List<CarroResponse> historicoDoVeiculo(String codigoVeiculo);
}
