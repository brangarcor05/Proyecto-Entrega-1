package co.javeriana.dw.proyecto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.Lane;

public interface LaneRepository extends JpaRepository<Lane, Long> {
    List<Lane> findByPoolIdAndActivoTrueOrderByOrden(Long poolId);

    // HU-19/HU-24: no se puede eliminar un rol asociado a una lane
    boolean existsByRolProcesoIdAndActivoTrue(Long rolProcesoId);

    boolean existsByPoolIdAndRolProcesoId(Long poolId, Long rolProcesoId);

    // HU-21: un pool con lanes no se puede eliminar ni volver caja negra
    boolean existsByPoolIdAndActivoTrue(Long poolId);
}