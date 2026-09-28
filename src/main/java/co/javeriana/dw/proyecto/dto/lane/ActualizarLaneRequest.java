package co.javeriana.dw.proyecto.dto.lane;

import jakarta.validation.constraints.NotNull;

/**
 * "Renombrar" una lane (HU-22) es reasignarle otro rol de proceso, ya que la lane
 * no tiene nombre propio: muestra el del rol.
 */
public record ActualizarLaneRequest(

        @NotNull(message = "El rol de proceso es obligatorio")
        Long rolProcesoId) {
}
