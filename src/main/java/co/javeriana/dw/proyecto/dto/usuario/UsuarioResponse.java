package co.javeriana.dw.proyecto.dto.usuario;

import co.javeriana.dw.proyecto.entidad.RolUsuario;

/**
 * Información pública que la API puede devolver sobre un usuario.
 * Nunca contiene la contraseña ni otra credencial.
 */
public record UsuarioResponse(
        Long id,
        Long empresaId,
        String nombre,
        String email,
        RolUsuario rol,
        boolean activo) {
}
