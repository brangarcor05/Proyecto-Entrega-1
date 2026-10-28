package co.javeriana.dw.proyecto.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.javeriana.dw.proyecto.dto.rolproceso.ActualizarRolProcesoRequest;
import co.javeriana.dw.proyecto.dto.rolproceso.CrearRolProcesoRequest;
import co.javeriana.dw.proyecto.dto.rolproceso.RolProcesoResponse;
import co.javeriana.dw.proyecto.service.RolProcesoService;
import jakarta.validation.Valid;

/**
 * Roles de proceso (HU-17 a HU-20).
 *
 * Mientras no exista el inicio de sesion (HU-03), usuarioId/empresaId llegan como
 * parametros de la peticion. Cuando el login este listo deben salir del usuario
 * autenticado.
 */
@RestController
@RequestMapping("/api/roles-proceso")
public class RolProcesoController {

    private final RolProcesoService rolProcesoService;

    public RolProcesoController(RolProcesoService rolProcesoService) {
        this.rolProcesoService = rolProcesoService;
    }

    /** HU-20: búsqueda por nombre, paginada, solo de la empresa indicada. */
    @GetMapping
    public ResponseEntity<Page<RolProcesoResponse>> consultar(
            @RequestParam Long empresaId,
            @RequestParam(required = false) String nombre,
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(rolProcesoService.consultar(empresaId, nombre, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolProcesoResponse> buscarPorId(
            @PathVariable Long id,
            @RequestParam Long empresaId) {

        return ResponseEntity.ok(rolProcesoService.obtener(id, empresaId));
    }

    @PostMapping
    public ResponseEntity<RolProcesoResponse> crear(
            @Valid @RequestBody CrearRolProcesoRequest request,
            @RequestParam Long usuarioId) {

        RolProcesoResponse creado = rolProcesoService.crear(request, usuarioId);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RolProcesoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarRolProcesoRequest request,
            @RequestParam Long usuarioId) {

        return ResponseEntity.ok(rolProcesoService.actualizar(id, request, usuarioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        rolProcesoService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
