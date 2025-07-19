package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.ListaCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListaCompraRepository extends JpaRepository<ListaCompra, UUID> {


    Optional<ListaCompra> findByItem(@Param("item") String item);

}
