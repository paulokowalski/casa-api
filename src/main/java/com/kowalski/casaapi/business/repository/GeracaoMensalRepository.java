package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.GeracaoMensal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeracaoMensalRepository extends JpaRepository<GeracaoMensal, UUID> {

    Optional<GeracaoMensal> findByMes(String mes);

}
