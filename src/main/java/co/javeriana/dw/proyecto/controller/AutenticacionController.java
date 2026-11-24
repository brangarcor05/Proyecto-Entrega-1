package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.autenticacion.LoginRequest;
import co.javeriana.dw.proyecto.dto.autenticacion.RefreshTokenRequest;
import co.javeriana.dw.proyecto.dto.autenticacion.SesionResponse;
import co.javeriana.dw.proyecto.service.AutenticacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-03. Los tres endpoints son públicos (no exigen access token): login porque aún no
 * hay sesión, y refresh/logout porque se usan justamente cuando el access token ya venció.
 */
@RestController
@RequestMapping("/api/auth")
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    public AutenticacionController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @PostMapping("/login")
    public ResponseEntity<SesionResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(autenticacionService.iniciarSesion(request));
    }

    /** Cambia un refresh token válido por un par nuevo (access + refresh). */
    @PostMapping("/refresh")
    public ResponseEntity<SesionResponse> renovar(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(autenticacionService.renovar(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        autenticacionService.cerrarSesion(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
