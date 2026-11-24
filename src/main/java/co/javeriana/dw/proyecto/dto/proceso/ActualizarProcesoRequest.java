package co.javeriana.dw.proyecto.dto.proceso;

import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos editables de un proceso (HU-05). No incluye la empresa:
 * un proceso no cambia de empresa, ni el campo activo, que solo cambia al eliminar.
 */
public record ActualizarProcesoRequest(

        @NotBlank(message = "El nombre del proceso es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
        String descripcion,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 100, message = "La categoría no puede superar los 100 caracteres")
        String categoria,

        @NotNull(message = "El estado es obligatorio")
        EstadoProceso estado) {
}
