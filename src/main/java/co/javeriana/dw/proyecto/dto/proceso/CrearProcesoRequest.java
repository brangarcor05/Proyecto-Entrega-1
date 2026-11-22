package co.javeriana.dw.proyecto.dto.proceso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que el cliente envía para crear un proceso (HU-04).
 * El estado y el campo activo no se reciben: todo proceso nace en BORRADOR y activo.
 */
public record CrearProcesoRequest(

        // Se conserva solo por compatibilidad con clientes anteriores: el servicio lo
        // IGNORA y crea el proceso en la empresa del usuario autenticado (token).
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
