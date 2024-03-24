package com.kowalski.casaapi.domain.repository;

import com.kowalski.casaapi.domain.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompraRepository extends JpaRepository<Compra, UUID> {

//    @Query(value = " SELECT p.nm_product as nomeProduto, ip.vl_installment as valor, ip.dt_installment as mes " +
//            " from installment_purchase ip " +
//            " join purchase p on p.purchase_id = ip.purchase_id " +
//            " where date_part('month', ip.dt_installment) = :mounth " +
//            " and p.nm_person_purchase = :person " +
//            " order by p.nm_person_purchase, date_part('month', ip.dt_installment) ",
//            nativeQuery = true)
//    List<CompraResponse> findAllByMounthAndPerson(Double mounth, String person);


}
