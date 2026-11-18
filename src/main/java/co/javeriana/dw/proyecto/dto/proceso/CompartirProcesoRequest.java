package co.javeriana.dw.proyecto.dto.proceso;

import java.util.Set;

/**
 * Estado de comparticion de un proceso (HU-23).
 *
 * Con compartido en false la lista se vacia y el proceso vuelve a verlo solo su
 * empresa. Con compartido en true hay que indicar al menos una empresa invitada,
 * y su acceso es siempre de solo lectura.
 */
public record CompartirProcesoRequest(
        boolean compartido,
        Set<Long> empresasIds) {
}
