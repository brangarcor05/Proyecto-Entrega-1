package co.javeriana.dw.proyecto.dto.rolproceso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos editables de un rol de proceso (HU-18): nombre y descripción. */
public record ActualizarRolProcesoRequest(

        @NotBlank(message = "El nombre del rol es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion) {
}
