package co.javeriana.dw.proyecto.dto.pool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de un participante del diagrama (HU-21): un cliente, un proveedor o un
 * sistema externo. El pool de la empresa propietaria no se crea por aqui: lo
 * genera ProcesoService al crear el proceso, y es unico.
 */
public record CrearPoolRequest(

        @NotNull(message = "El proceso es obligatorio")
        Long procesoId,

        @NotBlank(message = "El nombre del pool es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        /** Participante externo del que no se modela el flujo interno. */
        boolean cajaNegra) {
}
