package co.javeriana.dw.proyecto.dto.arco;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de un flujo de secuencia (HU-11): la flecha continua que fija el orden
 * entre dos elementos del diagrama.
 *
 * El origen y el destino pueden ser actividades, gateways o eventos, por eso se
 * reciben como ids de nodo sin distinguir el tipo.
 */
public record CrearArcoRequest(

        @NotNull(message = "El proceso es obligatorio")
        Long procesoId,

        @NotNull(message = "El nodo de origen es obligatorio")
        Long origenId,

        @NotNull(message = "El nodo de destino es obligatorio")
        Long destinoId,

        @Size(max = 150, message = "La etiqueta no puede superar los 150 caracteres")
        String etiqueta,

        /** Solo aplica cuando el arco sale de un gateway (HU-12, HU-14). */
        @Size(max = 500, message = "La condicion no puede superar los 500 caracteres")
        String condicion) {
}
