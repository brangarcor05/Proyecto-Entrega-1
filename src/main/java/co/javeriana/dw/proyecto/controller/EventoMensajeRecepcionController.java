package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.mensaje.ActualizarEventoMensajeRecepcionRequest;
import co.javeriana.dw.proyecto.dto.mensaje.CrearEventoMensajeRecepcionRequest;
import co.javeriana.dw.proyecto.dto.mensaje.EventoMensajeRecepcionResponse;
import co.javeriana.dw.proyecto.service.EventoMensajeRecepcionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventos-mensaje-recepcion")
public class EventoMensajeRecepcionController {

    private final EventoMensajeRecepcionService eventoMensajeRecepcionService;

    public EventoMensajeRecepcionController(EventoMensajeRecepcionService eventoMensajeRecepcionService) {
        this.eventoMensajeRecepcionService = eventoMensajeRecepcionService;
    }

    @PostMapping
    public ResponseEntity<EventoMensajeRecepcionResponse> crear(
            @Valid @RequestBody CrearEventoMensajeRecepcionRequest request, @RequestParam Long usuarioId) {
        EventoMensajeRecepcionResponse response = eventoMensajeRecepcionService.crear(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoMensajeRecepcionResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(eventoMensajeRecepcionService.obtenerPorId(id));
    }

    @GetMapping("/proceso/{procesoId}")
    public ResponseEntity<List<EventoMensajeRecepcionResponse>> listarPorProceso(@PathVariable Long procesoId) {
        return ResponseEntity.ok(eventoMensajeRecepcionService.listarPorProceso(procesoId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoMensajeRecepcionResponse> actualizar(@PathVariable Long id,
            @Valid @RequestBody ActualizarEventoMensajeRecepcionRequest request, @RequestParam Long usuarioId) {
        return ResponseEntity.ok(eventoMensajeRecepcionService.actualizar(id, request, usuarioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        eventoMensajeRecepcionService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
