package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.FaturaPaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaturaPagaRepository extends JpaRepository<FaturaPaga, FaturaPaga.Id> {
}
