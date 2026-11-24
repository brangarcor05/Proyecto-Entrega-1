package co.javeriana.dw.proyecto.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import jakarta.validation.Valid;

/**
 * Lanes del diagrama (HU-22).
 *
 * El usuario y la empresa salen del access token (@AuthenticationPrincipal), nunca de
 * un parametro que el cliente pueda cambiar. Las lecturas exigen que la empresa del
 * token sea la propietaria del proceso o una invitada (HU-23).
 */
@RestController
@RequestMapping("/api/lanes")
public class LaneController {

    private final LaneService laneService;

    public LaneController(LaneService laneService) {
        this.laneService = laneService;
    }

    @GetMapping
    public ResponseEntity<List<LaneResponse>> listarPorPool(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @RequestParam Long poolId) {
        return ResponseEntity.ok(laneService.listarPorPool(poolId, actor.empresaId()));
    }

    @PostMapping
    public ResponseEntity<LaneResponse> crear(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @Valid @RequestBody CrearLaneRequest request) {

        LaneResponse creada = laneService.crear(request, actor.id());
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
            @AuthenticationPrincipal UsuarioAutenticado actor) {

        return ResponseEntity.ok(laneService.actualizar(id, request, actor.id()));
    }

    /** HU-22: reordenar todas las lanes de un pool en un solo llamado. */
    @PutMapping("/reordenar")
    public ResponseEntity<List<LaneResponse>> reordenar(
            @RequestParam Long poolId,
            @Valid @RequestBody ReordenarLanesRequest request,
            @AuthenticationPrincipal UsuarioAutenticado actor) {

        return ResponseEntity.ok(laneService.reordenar(poolId, request, actor.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        laneService.eliminar(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
