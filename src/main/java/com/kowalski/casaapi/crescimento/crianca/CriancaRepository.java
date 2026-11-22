package com.kowalski.casaapi.crescimento.crianca;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CriancaRepository extends JpaRepository<Crianca, UUID> {

    List<Crianca> findByFamiliaIdOrderByNascimento(UUID familiaId);
}
