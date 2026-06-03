package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.TipoLancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TipoLancamentoRepository extends JpaRepository<TipoLancamento, UUID> { }
