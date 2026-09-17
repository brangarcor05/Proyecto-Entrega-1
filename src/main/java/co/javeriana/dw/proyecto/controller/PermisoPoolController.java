package co.javeriana.dw.proyecto.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.javeriana.dw.proyecto.dto.permisopool.ConfigurarPermisoPoolRequest;
import co.javeriana.dw.proyecto.dto.permisopool.PermisoPoolResponse;
import co.javeriana.dw.proyecto.service.PermisoPoolService;
import jakarta.validation.Valid;

/**
 * Permisos sobre pools y lanes (HU-24).
 *
 * Mientras no exista el inicio de sesion (HU-03), empresaId y usuarioId llegan como
 * parametros de la peticion. Cuando el login este listo deben salir del usuario
 * autenticado y desaparecer de la firma de estos metodos.
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
    public ResponseEntity<List<PermisoPoolResponse>> consultar(@RequestParam Long empresaId) {
        return ResponseEntity.ok(permisoPoolService.consultar(empresaId));
    }

    /** Configura un rol. La empresa se toma del administrador que hace la peticion. */
    @PutMapping
    public ResponseEntity<PermisoPoolResponse> configurar(
            @Valid @RequestBody ConfigurarPermisoPoolRequest request,
            @RequestParam Long usuarioId) {

        return ResponseEntity.ok(permisoPoolService.configurar(request, usuarioId));
    }
}
