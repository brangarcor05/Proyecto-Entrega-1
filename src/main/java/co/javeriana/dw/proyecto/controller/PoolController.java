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

import co.javeriana.dw.proyecto.dto.pool.ActualizarPoolRequest;
import co.javeriana.dw.proyecto.dto.pool.CrearPoolRequest;
import co.javeriana.dw.proyecto.dto.pool.PoolResponse;
import co.javeriana.dw.proyecto.service.PoolService;
import jakarta.validation.Valid;

/**
 * Pools del diagrama (HU-21).
 *
 * Mientras no exista el inicio de sesion (HU-03), usuarioId llega como parametro de
 * la peticion. Cuando el login este listo debe salir del usuario autenticado.
 */
@RestController
@RequestMapping("/api/pools")
public class PoolController {

    private final PoolService poolService;

    public PoolController(PoolService poolService) {
        this.poolService = poolService;
    }

    @GetMapping
    public ResponseEntity<List<PoolResponse>> listarPorProceso(@RequestParam Long procesoId) {
        return ResponseEntity.ok(poolService.listarPorProceso(procesoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PoolResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(poolService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<PoolResponse> crear(
            @Valid @RequestBody CrearPoolRequest request,
            @RequestParam Long usuarioId) {

        PoolResponse poolCreado = poolService.crear(request, usuarioId);
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
            @RequestParam Long usuarioId) {

        return ResponseEntity.ok(poolService.actualizar(id, request, usuarioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        poolService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
