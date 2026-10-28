package co.javeriana.dw.proyecto.controller;

import java.net.URI;
import java.util.List;

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

import co.javeriana.dw.proyecto.dto.lane.ActualizarLaneRequest;
import co.javeriana.dw.proyecto.dto.lane.CrearLaneRequest;
import co.javeriana.dw.proyecto.dto.lane.LaneResponse;
import co.javeriana.dw.proyecto.dto.lane.ReordenarLanesRequest;
import co.javeriana.dw.proyecto.service.LaneService;
import jakarta.validation.Valid;

/**
 * Lanes del diagrama (HU-22).
 *
 * Mientras no exista el inicio de sesion (HU-03), usuarioId llega como parametro de
 * la peticion. Cuando el login este listo debe salir del usuario autenticado.
 */
@RestController
@RequestMapping("/api/lanes")
public class LaneController {

    private final LaneService laneService;

    public LaneController(LaneService laneService) {
        this.laneService = laneService;
    }

    @GetMapping
    public ResponseEntity<List<LaneResponse>> listarPorPool(@RequestParam Long poolId) {
        return ResponseEntity.ok(laneService.listarPorPool(poolId));
    }

    @PostMapping
    public ResponseEntity<LaneResponse> crear(
            @Valid @RequestBody CrearLaneRequest request,
            @RequestParam Long usuarioId) {

        LaneResponse creada = laneService.crear(request, usuarioId);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LaneResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarLaneRequest request,
            @RequestParam Long usuarioId) {

        return ResponseEntity.ok(laneService.actualizar(id, request, usuarioId));
    }

    /** HU-22: reordenar todas las lanes de un pool en un solo llamado. */
    @PutMapping("/reordenar")
    public ResponseEntity<List<LaneResponse>> reordenar(
            @RequestParam Long poolId,
            @Valid @RequestBody ReordenarLanesRequest request,
            @RequestParam Long usuarioId) {

        return ResponseEntity.ok(laneService.reordenar(poolId, request, usuarioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        laneService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
