package co.javeriana.dw.proyecto.dto.historial;

import java.time.LocalDateTime;

import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.Historial;

/**
 * Un cambio registrado sobre un proceso (HU-05, HU-06 y HU-07):
 * que se hizo, quien lo hizo y cuando.
 */
public record HistorialResponse(
        Long id,
        String entidadTipo,
        Long entidadId,
        AccionHistorial accion,
        String detalle,
        LocalDateTime fecha,
        Long usuarioId,
        String usuarioNombre) {

    /** Debe invocarse dentro de una transaccion: el usuario se carga de forma perezosa. */
    public static HistorialResponse desde(Historial historial) {
        return new HistorialResponse(
                historial.getId(),
                historial.getEntidadTipo(),
                historial.getEntidadId(),
                historial.getAccion(),
                historial.getDetalle(),
                historial.getFecha(),
                historial.getUsuario().getId(),
                historial.getUsuario().getNombre());
    }
}
