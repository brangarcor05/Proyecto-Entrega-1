package co.javeriana.dw.proyecto.dto.pool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos editables de un pool (HU-21). No incluye el proceso, porque un pool no
 * cambia de diagrama, ni esPropietario, que lo define el sistema.
 */
public record ActualizarPoolRequest(

        @NotBlank(message = "El nombre del pool es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        boolean cajaNegra) {
}
