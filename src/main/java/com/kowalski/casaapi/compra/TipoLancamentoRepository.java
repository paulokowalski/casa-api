package com.kowalski.casaapi.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TipoLancamentoRepository extends JpaRepository<TipoLancamento, UUID> { }
