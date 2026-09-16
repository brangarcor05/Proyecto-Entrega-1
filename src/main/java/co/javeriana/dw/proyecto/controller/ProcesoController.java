package co.javeriana.dw.proyecto.controller;

import java.net.URI;
import java.util.List;

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

import co.javeriana.dw.proyecto.dto.historial.HistorialResponse;
import co.javeriana.dw.proyecto.dto.proceso.ActualizarProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.CrearProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.ProcesoResponse;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.service.ProcesoService;
import jakarta.validation.Valid;

/**
 * Mientras no exista el inicio de sesion (HU-03), empresaId y usuarioId llegan como
 * parametros de la peticion. Cuando el login este listo deben salir del usuario
 * autenticado y desaparecer de la firma de estos metodos.
 */
@RestController
@RequestMapping("/api/procesos")
public class ProcesoController {

    private final ProcesoService procesoService;

    public ProcesoController(ProcesoService procesoService) {
        this.procesoService = procesoService;
    }

    @GetMapping
    public ResponseEntity<Page<ProcesoResponse>> consultar(
            @RequestParam Long empresaId,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) EstadoProceso estado,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(procesoService.consultar(
                empresaId, nombre, estado, categoria, incluirInactivos, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcesoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(procesoService.obtener(id));
    }

    /** HU-07: "Se puede consultar el historial de cambios del proceso". */
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialResponse>> consultarHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(procesoService.consultarHistorial(id));
    }

    @PostMapping
    public ResponseEntity<ProcesoResponse> crear(
            @Valid @RequestBody CrearProcesoRequest request,
            @RequestParam Long usuarioId) {

        ProcesoResponse procesoCreado = procesoService.crear(request, usuarioId);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(procesoCreado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(procesoCreado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcesoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarProcesoRequest request,
            @RequestParam Long usuarioId) {

        return ResponseEntity.ok(procesoService.actualizar(id, request, usuarioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        procesoService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
