package co.javeriana.dw.proyecto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.EventoMensajeEnvio;

public interface EventoMensajeEnvioRepository extends JpaRepository<EventoMensajeEnvio, Long> {
    List<EventoMensajeEnvio> findByProcesoIdAndActivoTrue(Long procesoId);

    // HU-25: validar que exista un Catch con el mismo nombre en el pool destino
    Optional<EventoMensajeEnvio> findByNombreMensajeAndProcesoId(String nombreMensaje, Long procesoId);
}