package co.javeriana.dw.proyecto.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.NodoProceso;

/**
 * NodoProceso es la clase padre de actividades, gateways, eventos y eventos de mensaje.
 * Consultarla permite saber si un pool tiene elementos adentro sin preguntarle
 * a cada repositorio hijo por separado.
 */
public interface NodoProcesoRepository extends JpaRepository<NodoProceso, Long> {

    boolean existsByPoolIdAndActivoTrue(Long poolId);

    Optional<NodoProceso> findByIdAndActivoTrue(Long id);
}
