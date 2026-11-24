package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.mensaje.ActualizarEventoMensajeEnvioRequest;
import co.javeriana.dw.proyecto.dto.mensaje.EventoMensajeEnvioResponse;
import co.javeriana.dw.proyecto.dto.mensaje.CrearEventoMensajeEnvioRequest;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import co.javeriana.dw.proyecto.service.EventoMensajeEnvioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Message Throw entre pools (HU-25, HU-28).
 *
 * El usuario y la empresa salen del access token (@AuthenticationPrincipal). Las reglas
 * de permisos, de empresa propietaria y de historial se aplican en EventoMensajeEnvioService.
 */
@RestController
@RequestMapping("/api/eventos-mensaje-envio")
public class EventoMensajeEnvioController {
    private final EventoMensajeEnvioService eventoMensajeEnvioService;

    public EventoMensajeEnvioController(EventoMensajeEnvioService eventoMensajeEnvioService) {
        this.eventoMensajeEnvioService = eventoMensajeEnvioService;
    }

    @PostMapping
    public ResponseEntity<EventoMensajeEnvioResponse> crear(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @Valid @RequestBody CrearEventoMensajeEnvioRequest request) {
        EventoMensajeEnvioResponse response = eventoMensajeEnvioService.crear(request, actor.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoMensajeEnvioResponse> obtener(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        return ResponseEntity.ok(eventoMensajeEnvioService.obtenerPorId(id, actor.empresaId()));
    }

    @GetMapping("/proceso/{procesoId}")
    public ResponseEntity<List<EventoMensajeEnvioResponse>> listarPorProceso(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long procesoId) {
        return ResponseEntity.ok(eventoMensajeEnvioService.listarPorProceso(procesoId, actor.empresaId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoMensajeEnvioResponse> actualizar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEventoMensajeEnvioRequest request) {
        return ResponseEntity.ok(eventoMensajeEnvioService.actualizar(id, request, actor.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        eventoMensajeEnvioService.eliminar(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
