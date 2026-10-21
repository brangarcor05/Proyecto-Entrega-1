package co.javeriana.dw.proyecto.repository;



import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.EventoMensajeRecepcion;
import co.javeriana.dw.proyecto.entidad.VarianteMensajeCatch;

public interface EventoMensajeRecepcionRepository extends JpaRepository<EventoMensajeRecepcion, Long> {
    List<EventoMensajeRecepcion> findByProcesoIdAndActivoTrue(Long procesoId);

    // HU-27: validar existencia del Throw correspondiente
    Optional<EventoMensajeRecepcion> findByNombreMensajeAndProcesoId(String nombreMensaje, Long procesoId);

    List<EventoMensajeRecepcion> findByProcesoIdAndVariante(Long procesoId, VarianteMensajeCatch variante);

    // HU-28: detectar mensajes ambiguos (mismo nombre + misma clave) en un proceso
    List<EventoMensajeRecepcion> findByProcesoIdAndNombreMensajeAndClaveCorrelacion(
            Long procesoId, String nombreMensaje, String claveCorrelacion);
}