package co.javeriana.dw.proyecto.dto.autenticacion;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de POST /api/auth/refresh y POST /api/auth/logout. */
public record RefreshTokenRequest(
        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken) {
}
