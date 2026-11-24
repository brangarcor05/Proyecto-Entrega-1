package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.actividad.ActualizarActividadRequest;
import co.javeriana.dw.proyecto.dto.actividad.ActividadResponse;
import co.javeriana.dw.proyecto.dto.actividad.CrearActividadRequest;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import co.javeriana.dw.proyecto.service.ActividadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Actividades del diagrama (HU-08 a HU-10).
 *
 * El usuario y la empresa salen del access token (@AuthenticationPrincipal). Las reglas
 * de permisos, de empresa propietaria y de historial se aplican en ActividadService.
 */
@RestController
@RequestMapping("/api/actividades")
public class ActividadController {
    private final ActividadService actividadService;

    public ActividadController(ActividadService actividadService) {
        this.actividadService = actividadService;
    }

    @PostMapping
    public ResponseEntity<ActividadResponse> crear(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @Valid @RequestBody CrearActividadRequest request) {
        ActividadResponse response = actividadService.crear(request, actor.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActividadResponse> obtener(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        return ResponseEntity.ok(actividadService.obtenerPorId(id, actor.empresaId()));
    }

    @GetMapping("/proceso/{procesoId}")
    public ResponseEntity<List<ActividadResponse>> listarPorProceso(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long procesoId) {
        return ResponseEntity.ok(actividadService.listarPorProceso(procesoId, actor.empresaId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActividadResponse> actualizar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarActividadRequest request) {
        return ResponseEntity.ok(actividadService.actualizar(id, request, actor.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        actividadService.eliminar(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
