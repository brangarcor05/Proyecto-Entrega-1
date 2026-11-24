package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.mensaje.ActualizarEventoMensajeRecepcionRequest;
import co.javeriana.dw.proyecto.dto.mensaje.EventoMensajeRecepcionResponse;
import co.javeriana.dw.proyecto.dto.mensaje.CrearEventoMensajeRecepcionRequest;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import co.javeriana.dw.proyecto.service.EventoMensajeRecepcionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Message Catch (HU-27, HU-28).
 *
 * El usuario y la empresa salen del access token (@AuthenticationPrincipal). Las reglas
 * de permisos, de empresa propietaria y de historial se aplican en EventoMensajeRecepcionService.
 */
@RestController
@RequestMapping("/api/eventos-mensaje-recepcion")
public class EventoMensajeRecepcionController {
    private final EventoMensajeRecepcionService eventoMensajeRecepcionService;

    public EventoMensajeRecepcionController(EventoMensajeRecepcionService eventoMensajeRecepcionService) {
        this.eventoMensajeRecepcionService = eventoMensajeRecepcionService;
    }

    @PostMapping
    public ResponseEntity<EventoMensajeRecepcionResponse> crear(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @Valid @RequestBody CrearEventoMensajeRecepcionRequest request) {
        EventoMensajeRecepcionResponse response = eventoMensajeRecepcionService.crear(request, actor.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoMensajeRecepcionResponse> obtener(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        return ResponseEntity.ok(eventoMensajeRecepcionService.obtenerPorId(id, actor.empresaId()));
    }

    @GetMapping("/proceso/{procesoId}")
    public ResponseEntity<List<EventoMensajeRecepcionResponse>> listarPorProceso(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long procesoId) {
        return ResponseEntity.ok(eventoMensajeRecepcionService.listarPorProceso(procesoId, actor.empresaId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoMensajeRecepcionResponse> actualizar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEventoMensajeRecepcionRequest request) {
        return ResponseEntity.ok(eventoMensajeRecepcionService.actualizar(id, request, actor.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        eventoMensajeRecepcionService.eliminar(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
