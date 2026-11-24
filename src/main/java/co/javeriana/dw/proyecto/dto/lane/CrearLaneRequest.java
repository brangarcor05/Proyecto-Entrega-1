package co.javeriana.dw.proyecto.dto.lane;

import jakarta.validation.constraints.NotNull;

/**
 * Alta de una lane dentro de un pool (HU-22). No lleva nombre propio: su etiqueta en
 * el diagrama es el nombre del rol de proceso asociado. El orden lo calcula el
 * service (se agrega al final de las que ya existen en el pool).
 */
public record CrearLaneRequest(

        @NotNull(message = "El pool es obligatorio")
        Long poolId,

        @NotNull(message = "El rol de proceso es obligatorio")
        Long rolProcesoId) {
}
