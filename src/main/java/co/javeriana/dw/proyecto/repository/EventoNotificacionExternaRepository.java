package co.javeriana.dw.proyecto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.EventoNotificacionExterna;

public interface EventoNotificacionExternaRepository extends JpaRepository<EventoNotificacionExterna, Long> {
    List<EventoNotificacionExterna> findByProcesoIdAndActivoTrue(Long procesoId);
}
