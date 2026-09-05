package co.javeriana.dw.proyecto.dto.usuario;

import co.javeriana.dw.proyecto.entidad.RolUsuario;


public record ActualizarUsuarioRequest(
        String nombre,
        String email,
        RolUsuario rol,
        boolean activo) {
}
