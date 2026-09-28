package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.actividad.ActualizarActividadRequest;
import co.javeriana.dw.proyecto.dto.actividad.ActividadResponse;
import co.javeriana.dw.proyecto.dto.actividad.CrearActividadRequest;
import co.javeriana.dw.proyecto.service.ActividadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/actividades")
public class ActividadController {
    private final ActividadService actividadService;

    public ActividadController(ActividadService actividadService) {
        this.actividadService = actividadService;
    }

    @PostMapping
    public ResponseEntity<ActividadResponse> crear(@Valid @RequestBody CrearActividadRequest request,
            @RequestParam Long usuarioId) {
        ActividadResponse response = actividadService.crear(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActividadResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(actividadService.obtenerPorId(id));
    }

    @GetMapping("/proceso/{procesoId}")
    public ResponseEntity<List<ActividadResponse>> listarPorProceso(@PathVariable Long procesoId) {
        return ResponseEntity.ok(actividadService.listarPorProceso(procesoId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActividadResponse> actualizar(@PathVariable Long id,
            @Valid @RequestBody ActualizarActividadRequest request, @RequestParam Long usuarioId) {
        return ResponseEntity.ok(actividadService.actualizar(id, request, usuarioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        actividadService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
