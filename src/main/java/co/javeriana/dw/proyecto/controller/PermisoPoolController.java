package co.javeriana.dw.proyecto.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.javeriana.dw.proyecto.dto.permisopool.ConfigurarPermisoPoolRequest;
import co.javeriana.dw.proyecto.dto.permisopool.PermisoPoolResponse;
import co.javeriana.dw.proyecto.service.PermisoPoolService;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import jakarta.validation.Valid;

/**
 * Permisos sobre pools y lanes (HU-24).
 *
 * La empresa y el usuario salen del access token: cada organizacion solo consulta y
 * configura sus propios permisos.
 */
@RestController
@RequestMapping("/api/permisos-pool")
public class PermisoPoolController {

    private final PermisoPoolService permisoPoolService;

    public PermisoPoolController(PermisoPoolService permisoPoolService) {
        this.permisoPoolService = permisoPoolService;
    }

    /** Los tres roles de acceso con lo que puede hacer cada uno sobre pools y lanes. */
    @GetMapping
    public ResponseEntity<List<PermisoPoolResponse>> consultar(@AuthenticationPrincipal UsuarioAutenticado actor) {
        return ResponseEntity.ok(permisoPoolService.consultar(actor.empresaId()));
    }

    /** Configura un rol. La empresa se toma del administrador que hace la peticion. */
    @PutMapping
    public ResponseEntity<PermisoPoolResponse> configurar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @Valid @RequestBody ConfigurarPermisoPoolRequest request) {

        return ResponseEntity.ok(permisoPoolService.configurar(request, actor.id()));
    }
}
