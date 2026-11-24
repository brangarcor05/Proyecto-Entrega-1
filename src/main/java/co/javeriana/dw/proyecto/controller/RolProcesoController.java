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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.javeriana.dw.proyecto.dto.rolproceso.ActualizarRolProcesoRequest;
import co.javeriana.dw.proyecto.dto.rolproceso.CrearRolProcesoRequest;
import co.javeriana.dw.proyecto.dto.rolproceso.RolProcesoResponse;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import co.javeriana.dw.proyecto.service.RolProcesoService;
import jakarta.validation.Valid;

/**
 * Roles de proceso (HU-17 a HU-20).
 *
 * La empresa y el usuario salen SIEMPRE del access token (@AuthenticationPrincipal), nunca
 * de un parametro que el cliente pueda cambiar. Asi un usuario solo ve y modifica el
 * catalogo de su propia empresa (HU-20: "solo aparecen los roles de la empresa del
 * usuario autenticado").
 *
 * Permisos:
 *  - Consultar (GET): cualquier usuario autenticado (ADMIN, EDITOR, LECTURA). Todos
 *    necesitan el catalogo para ver y nombrar lanes.
 *  - Crear, editar, eliminar: solo ADMIN (HU-17 y HU-19). @PreAuthorize corta la peticion
 *    con 403 antes de llegar al servicio; el servicio vuelve a comprobar el rol contra la
 *    base de datos por si cambio despues de emitirse el token.
 */
@RestController
@RequestMapping("/api/roles-proceso")
public class RolProcesoController {

    private final RolProcesoService rolProcesoService;

    public RolProcesoController(RolProcesoService rolProcesoService) {
        this.rolProcesoService = rolProcesoService;
    }

    /** HU-20: busqueda por nombre, paginada, solo de la empresa del usuario autenticado. */
    @GetMapping
    public ResponseEntity<Page<RolProcesoResponse>> consultar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @RequestParam(required = false) String nombre,
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(rolProcesoService.consultar(actor.empresaId(), nombre, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolProcesoResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {

        return ResponseEntity.ok(rolProcesoService.obtener(id, actor.empresaId()));
    }

    /** HU-17: solo el administrador. El rol queda en la empresa del administrador. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RolProcesoResponse> crear(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @Valid @RequestBody CrearRolProcesoRequest request) {

        RolProcesoResponse creado = rolProcesoService.crear(request, actor.id());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    /** HU-18. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RolProcesoResponse> actualizar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarRolProcesoRequest request) {

        return ResponseEntity.ok(rolProcesoService.actualizar(id, request, actor.id()));
    }

    /** HU-19: eliminacion logica, bloqueada si el rol esta en uso. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {

        rolProcesoService.eliminar(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
