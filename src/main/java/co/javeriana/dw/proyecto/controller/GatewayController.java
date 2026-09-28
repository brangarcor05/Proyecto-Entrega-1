package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.gateway.ActualizarGatewayRequest;
import co.javeriana.dw.proyecto.dto.gateway.CrearGatewayRequest;
import co.javeriana.dw.proyecto.dto.gateway.GatewayResponse;
import co.javeriana.dw.proyecto.service.GatewayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/gateways")
public class GatewayController {

    private final GatewayService gatewayService;

    public GatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @PostMapping
    public ResponseEntity<GatewayResponse> crear(@Valid @RequestBody CrearGatewayRequest request) {
        GatewayResponse response = gatewayService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GatewayResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(gatewayService.obtenerPorId(id));
    }

    @GetMapping("/proceso/{procesoId}")
    public ResponseEntity<List<GatewayResponse>> listarPorProceso(@PathVariable Long procesoId) {
        return ResponseEntity.ok(gatewayService.listarPorProceso(procesoId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GatewayResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarGatewayRequest request) {
        return ResponseEntity.ok(gatewayService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        gatewayService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}