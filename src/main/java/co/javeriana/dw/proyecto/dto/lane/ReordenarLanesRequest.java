package co.javeriana.dw.proyecto.dto.lane;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/**
 * Reordenar lanes (HU-22) es una operación de conjunto: se manda el id de cada lane
 * del pool en el orden final deseado, y el service reasigna orden = posición en la
 * lista. Debe incluir exactamente las lanes activas del pool, ni más ni menos.
 */
public record ReordenarLanesRequest(

        @NotEmpty(message = "Debe incluir al menos una lane")
        List<Long> laneIdsEnOrden) {
}
