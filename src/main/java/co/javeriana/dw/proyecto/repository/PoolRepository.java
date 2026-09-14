package co.javeriana.dw.proyecto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.Pool;

public interface PoolRepository extends JpaRepository<Pool, Long> {
    List<Pool> findByProcesoId(Long procesoId);
    Optional<Pool> findByProcesoIdAndEsPropietarioTrue(Long procesoId);
}