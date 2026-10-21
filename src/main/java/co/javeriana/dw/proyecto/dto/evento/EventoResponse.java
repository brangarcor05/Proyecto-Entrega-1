package co.javeriana.dw.proyecto.dto.evento;

import co.javeriana.dw.proyecto.entidad.Evento;

/** Aplana proceso y pool a sus ids para no arrastrar las entidades completas. */
public record EventoResponse(
        Long id,
        Long procesoId,
        Long poolId,
        String nombre,
        String tipo,
        Double posicionX,
        Double posicionY,
        boolean activo) {

    /** Debe invocarse dentro de una transaccion: proceso y pool se cargan de forma perezosa. */
    public static EventoResponse desde(Evento evento) {
        return new EventoResponse(
                evento.getId(),
                evento.getProceso().getId(),
                evento.getPool().getId(),
                evento.getNombre(),
                evento.getTipo(),
                evento.getPosicionX(),
                evento.getPosicionY(),
                evento.isActivo());
    }
}
