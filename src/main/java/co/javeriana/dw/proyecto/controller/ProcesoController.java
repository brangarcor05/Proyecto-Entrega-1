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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.javeriana.dw.proyecto.dto.historial.HistorialResponse;
import co.javeriana.dw.proyecto.dto.proceso.ActualizarProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.CompartirProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.CrearProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.ProcesoResponse;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import co.javeriana.dw.proyecto.service.ProcesoService;
import jakarta.validation.Valid;

/**
 * Procesos (HU-04 a HU-07 y HU-23).
 *
 * El usuario y la empresa salen SIEMPRE del access token (@AuthenticationPrincipal),
 * nunca de un parametro que el cliente pueda cambiar. Si un cliente antiguo todavia
 * manda empresaId o usuarioId en la URL, se ignoran.
 */
@RestController
@RequestMapping("/api/procesos")
public class ProcesoController {

    private final ProcesoService procesoService;

    public ProcesoController(ProcesoService procesoService) {
        this.procesoService = procesoService;
    }

    /** HU-07 y HU-23: los procesos de la empresa mas los que le compartieron. */
    @GetMapping
    public ResponseEntity<Page<ProcesoResponse>> consultar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) EstadoProceso estado,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(procesoService.consultar(
                actor.empresaId(), nombre, estado, categoria, incluirInactivos, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcesoResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {

        return ResponseEntity.ok(procesoService.obtener(id, actor.empresaId()));
    }

    /** HU-07: "Se puede consultar el historial de cambios del proceso". */
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialResponse>> consultarHistorial(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {

        return ResponseEntity.ok(procesoService.consultarHistorial(id, actor.empresaId()));
    }

    @PostMapping
    public ResponseEntity<ProcesoResponse> crear(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @Valid @RequestBody CrearProcesoRequest request) {

        ProcesoResponse procesoCreado = procesoService.crear(request, actor.id());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(procesoCreado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(procesoCreado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcesoResponse> actualizar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarProcesoRequest request) {

        return ResponseEntity.ok(procesoService.actualizar(id, request, actor.id()));
    }

    /** HU-23: define que otras empresas pueden consultar el proceso, en solo lectura. */
    @PutMapping("/{id}/comparticion")
    public ResponseEntity<ProcesoResponse> compartir(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id,
            @Valid @RequestBody CompartirProcesoRequest request) {

        return ResponseEntity.ok(procesoService.compartir(id, request, actor.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        procesoService.eliminar(id, actor.id());
        return ResponseEntity.noContent().build();
    }
    // --- Diagrama BPMN (XML) ---

    @GetMapping("/{id}/diagrama")
    public ResponseEntity<String> obtenerDiagrama(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {

        String xml = procesoService.obtenerDiagrama(id, actor.empresaId());
        return ResponseEntity.ok()
                .header("Content-Type", "text/plain; charset=UTF-8")
                .body(xml);
    }

    @PutMapping(value = "/{id}/diagrama", consumes = "text/plain")
    public ResponseEntity<Void> guardarDiagrama(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id,
            @RequestBody String xml) {

        procesoService.guardarDiagrama(id, xml, actor.id());
        return ResponseEntity.noContent().build();
    }
}
