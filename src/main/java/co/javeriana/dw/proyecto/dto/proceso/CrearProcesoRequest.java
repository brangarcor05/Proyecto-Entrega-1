package co.javeriana.dw.proyecto.dto.proceso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos que el cliente envía para crear un proceso (HU-04).
 * El estado y el campo activo no se reciben: todo proceso nace en BORRADOR y activo.
 */
public record CrearProcesoRequest(

        // Mientras no exista el inicio de sesión (HU-03) la empresa llega en la petición.
        // Cuando haya usuario autenticado debe tomarse de la sesión y salir de este DTO.
        @NotNull(message = "La empresa es obligatoria")
        Long empresaId,

        @NotBlank(message = "El nombre del proceso es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
        String descripcion,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 100, message = "La categoría no puede superar los 100 caracteres")
        String categoria) {
}
