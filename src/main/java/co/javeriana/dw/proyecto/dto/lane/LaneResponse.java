package co.javeriana.dw.proyecto.dto.lane;

import co.javeriana.dw.proyecto.entidad.Lane;

/**
 * Una lane del diagrama. Incluye rolProcesoNombre para que el front no tenga que
 * pedir el catálogo de roles aparte solo para mostrar la etiqueta de la banda.
 */
public record LaneResponse(
        Long id,
        Long poolId,
        Long rolProcesoId,
        String rolProcesoNombre,
        Integer orden,
        boolean activo) {

    /** Debe invocarse dentro de una transacción: pool y rolProceso se cargan de forma perezosa. */
    public static LaneResponse desde(Lane lane) {
        return new LaneResponse(
                lane.getId(),
                lane.getPool().getId(),
                lane.getRolProceso().getId(),
                lane.getRolProceso().getNombre(),
                lane.getOrden(),
                lane.isActivo());
    }
}
