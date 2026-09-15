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

import co.javeriana.dw.proyecto.dto.proceso.ActualizarProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.CrearProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.ProcesoResponse;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.service.ProcesoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/procesos")
public class ProcesoController {
    private final ProcesoService procesoService;

    public ProcesoController(ProcesoService procesoService) {
        this.procesoService = procesoService;
    }

    /**
     * HU-07. El parámetro empresaId acota el listado a una sola empresa; cuando exista
     * el inicio de sesión (HU-03) debe tomarse del usuario autenticado y dejar de ser público.
     */
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

    @PostMapping
    public ResponseEntity<ProcesoResponse> crear(@Valid @RequestBody CrearProcesoRequest request) {
        ProcesoResponse procesoCreado = procesoService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(procesoCreado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(procesoCreado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcesoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarProcesoRequest request) {
        return ResponseEntity.ok(procesoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        procesoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
