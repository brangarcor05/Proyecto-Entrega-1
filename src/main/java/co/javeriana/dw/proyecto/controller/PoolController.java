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

import co.javeriana.dw.proyecto.dto.pool.ActualizarPoolRequest;
import co.javeriana.dw.proyecto.dto.pool.CrearPoolRequest;
import co.javeriana.dw.proyecto.dto.pool.PoolResponse;
import co.javeriana.dw.proyecto.service.PoolService;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import jakarta.validation.Valid;

/**
 * Pools del diagrama (HU-21).
 *
 * El usuario y la empresa salen del access token (@AuthenticationPrincipal), nunca de
 * un parametro que el cliente pueda cambiar. Las lecturas exigen que la empresa del
 * token sea la propietaria del proceso o una invitada (HU-23).
 */
@RestController
@RequestMapping("/api/pools")
public class PoolController {

    private final PoolService poolService;

    public PoolController(PoolService poolService) {
        this.poolService = poolService;
    }

    @GetMapping
    public ResponseEntity<List<PoolResponse>> listarPorProceso(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @RequestParam Long procesoId) {
        return ResponseEntity.ok(poolService.listarPorProceso(procesoId, actor.empresaId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PoolResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        return ResponseEntity.ok(poolService.obtener(id, actor.empresaId()));
    }

    @PostMapping
    public ResponseEntity<PoolResponse> crear(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @Valid @RequestBody CrearPoolRequest request) {

        PoolResponse poolCreado = poolService.crear(request, actor.id());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(poolCreado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(poolCreado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PoolResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPoolRequest request,
            @AuthenticationPrincipal UsuarioAutenticado actor) {

        return ResponseEntity.ok(poolService.actualizar(id, request, actor.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        poolService.eliminar(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
