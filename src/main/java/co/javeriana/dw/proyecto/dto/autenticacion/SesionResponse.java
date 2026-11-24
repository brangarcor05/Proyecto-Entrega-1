package co.javeriana.dw.proyecto.dto.autenticacion;

import co.javeriana.dw.proyecto.entidad.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de login y de renovación de sesión (HU-03).
 *
 * accessToken: JWT de corta duración, va en la cabecera Authorization de cada petición.
 * refreshToken: token opaco de larga duración, solo se envía a /api/auth/refresh y
 * /api/auth/logout. Cada renovación entrega un par nuevo y el refresh anterior deja de servir.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SesionResponse {
    private Long usuarioId;
    private String nombre;
    private String email;
    private RolUsuario rolUsuario;
    private Long empresaId;
    private String empresaNombre;

    private String tokenType = "Bearer";
    private String accessToken;
    /** Segundos que dura el access token. */
    private long expiresIn;
    private String refreshToken;
    /** Segundos que dura el refresh token. */
    private long refreshExpiresIn;
}
