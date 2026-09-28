package co.javeriana.dw.proyecto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.javeriana.dw.proyecto.entidad.Lane;

public interface LaneRepository extends JpaRepository<Lane, Long> {
    List<Lane> findByPoolIdAndActivoTrueOrderByOrden(Long poolId);

    Optional<Lane> findByIdAndActivoTrue(Long id);

    // HU-19/HU-24: no se puede eliminar un rol asociado a una lane
    boolean existsByRolProcesoIdAndActivoTrue(Long rolProcesoId);

    boolean existsByPoolIdAndRolProcesoId(Long poolId, Long rolProcesoId);

    // HU-21: un pool con lanes no se puede eliminar ni volver caja negra
    boolean existsByPoolIdAndActivoTrue(Long poolId);

    /**
     * HU-19/HU-20: en que procesos esta usado un rol, para poder decir "no se puede
     * eliminar, esta en uso en Compras y en Ventas" en vez de un rechazo generico.
     */
    @Query("select distinct l.pool.proceso.nombre from Lane l "
            + "where l.rolProceso.id = :rolProcesoId and l.activo = true and l.pool.activo = true")
    List<String> nombresDeProcesosQueUsanElRol(@Param("rolProcesoId") Long rolProcesoId);
}