package co.javeriana.dw.proyecto.dto.rolproceso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta de un rol de proceso (HU-17): el catalogo de funciones de la empresa
 * (Analista, Supervisor, Auditor...) que luego se usa para nombrar lanes.
 *
 * No lleva empresaId: el rol se crea en la empresa del usuario autenticado (sale del token).
 */
public record CrearRolProcesoRequest(

        @NotBlank(message = "El nombre del rol es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion) {
}
