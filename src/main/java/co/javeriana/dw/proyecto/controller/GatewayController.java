package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.gateway.ActualizarGatewayRequest;
import co.javeriana.dw.proyecto.dto.gateway.GatewayResponse;
import co.javeriana.dw.proyecto.dto.gateway.CrearGatewayRequest;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import co.javeriana.dw.proyecto.service.GatewayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Gateways del diagrama (HU-14 a HU-16).
 *
 * El usuario y la empresa salen del access token (@AuthenticationPrincipal). Las reglas
 * de permisos, de empresa propietaria y de historial se aplican en GatewayService.
 */
@RestController
@RequestMapping("/api/gateways")
public class GatewayController {
    private final GatewayService gatewayService;

    public GatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @PostMapping
    public ResponseEntity<GatewayResponse> crear(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @Valid @RequestBody CrearGatewayRequest request) {
        GatewayResponse response = gatewayService.crear(request, actor.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GatewayResponse> obtener(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        return ResponseEntity.ok(gatewayService.obtenerPorId(id, actor.empresaId()));
    }

    @GetMapping("/proceso/{procesoId}")
    public ResponseEntity<List<GatewayResponse>> listarPorProceso(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long procesoId) {
        return ResponseEntity.ok(gatewayService.listarPorProceso(procesoId, actor.empresaId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GatewayResponse> actualizar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarGatewayRequest request) {
        return ResponseEntity.ok(gatewayService.actualizar(id, request, actor.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        gatewayService.eliminar(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
