package co.javeriana.dw.proyecto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.Actividad;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    boolean existsByNombreAndProcesoId(String nombre, Long procesoId);
    List<Actividad> findByProcesoIdAndActivoTrue(Long procesoId);
    boolean existsByLaneIdAndActivoTrue(Long laneId);
}