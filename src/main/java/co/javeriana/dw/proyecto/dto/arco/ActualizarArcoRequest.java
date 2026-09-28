package co.javeriana.dw.proyecto.dto.arco;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos editables de un arco (HU-12). No incluye el proceso, porque un arco no
 * cambia de diagrama, pero si el origen y el destino: reconectar un arco es una
 * edicion valida y se le aplican las mismas validaciones de la creacion.
 */
public record ActualizarArcoRequest(

        @NotNull(message = "El nodo de origen es obligatorio")
        Long origenId,

        @NotNull(message = "El nodo de destino es obligatorio")
        Long destinoId,

        @Size(max = 150, message = "La etiqueta no puede superar los 150 caracteres")
        String etiqueta,

        @Size(max = 500, message = "La condicion no puede superar los 500 caracteres")
        String condicion) {
}
