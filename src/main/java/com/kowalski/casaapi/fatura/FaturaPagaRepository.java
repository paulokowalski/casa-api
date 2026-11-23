package com.kowalski.casaapi.fatura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaturaPagaRepository extends JpaRepository<FaturaPaga, FaturaPaga.Id> {
}
