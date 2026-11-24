package co.javeriana.dw.proyecto.security;

import co.javeriana.dw.proyecto.entidad.RolUsuario;

/**
 * Quién hace la petición, ya validado desde el token. Es el "principal" de Spring Security:
 * los controllers lo reciben con @AuthenticationPrincipal y de aquí sale la empresa,
 * nunca de un parámetro que el cliente pueda falsificar.
 */
public record UsuarioAutenticado(Long id, Long empresaId, String email, RolUsuario rol) {}
