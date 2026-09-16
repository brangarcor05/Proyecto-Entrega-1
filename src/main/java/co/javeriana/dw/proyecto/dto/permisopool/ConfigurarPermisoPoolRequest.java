package co.javeriana.dw.proyecto.dto.permisopool;

import co.javeriana.dw.proyecto.entidad.RolUsuario;
import jakarta.validation.constraints.NotNull;

/**
 * Configuracion de HU-24: que puede hacer cada rol de acceso sobre los pools y
 * las lanes de la empresa. Se guarda una fila por rol.
 */
public record ConfigurarPermisoPoolRequest(

        @NotNull(message = "El rol de acceso es obligatorio")
        RolUsuario rolUsuario,

        boolean puedeCrear,
        boolean puedeEditar,
        boolean puedeEliminar) {
}
