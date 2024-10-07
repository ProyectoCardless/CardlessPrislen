package com.banco.CajerosCardless.repositories;

import com.banco.CajerosCardless.models.Comision;
import com.banco.CajerosCardless.models.Cuenta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ComisionRepository extends JpaRepository<Comision, Integer> {
    
    Comision findByCuenta(Cuenta cuenta);
    // Obtener el total de comisiones para una cuenta específica
    @Query("SELECT c.montoTotal FROM Comision c WHERE c.cuenta.id = :cuentaId")
    BigDecimal findTotalComisionesByCuentaId(@Param("cuentaId") Integer cuentaId);

    // Obtener el total de comisiones por retiros para una cuenta específica
    @Query("SELECT c.montoPorRetiros FROM Comision c WHERE c.cuenta.id = :cuentaId")
    BigDecimal findComisionesPorRetirosByCuentaId(@Param("cuentaId") Integer cuentaId);

    // Obtener el total de comisiones por depósitos para una cuenta específica
    @Query("SELECT c.montoPorDepositos FROM Comision c WHERE c.cuenta.id = :cuentaId")
    BigDecimal findComisionesPorDepositosByCuentaId(@Param("cuentaId") Integer cuentaId);
}
