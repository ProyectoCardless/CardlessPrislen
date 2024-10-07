/*
 * Repositorio para la entidad Transaccion
 */
package com.banco.CajerosCardless.repositories;

import com.banco.CajerosCardless.models.Cuenta;
import com.banco.CajerosCardless.models.Transaccion;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    List<Transaccion> findByCuenta(Cuenta cuenta);

    // Método para contar cuántas transacciones tiene una cuenta
    long countByCuenta(Cuenta cuenta);
    @Query("SELECT SUM(t.monto) FROM Transaccion t WHERE t.cuenta.id = :cuentaId AND t.tipo = :tipo")
    BigDecimal sumarPorTipo(@Param("cuentaId") Integer cuentaId, @Param("tipo") Transaccion.Tipo tipo);
}
