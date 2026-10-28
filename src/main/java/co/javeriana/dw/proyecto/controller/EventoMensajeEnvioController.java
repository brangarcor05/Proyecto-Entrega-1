package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.mensaje.ActualizarEventoMensajeEnvioRequest;
import co.javeriana.dw.proyecto.dto.mensaje.CrearEventoMensajeEnvioRequest;
import co.javeriana.dw.proyecto.dto.mensaje.EventoMensajeEnvioResponse;
import co.javeriana.dw.proyecto.service.EventoMensajeEnvioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/eventos-mensaje-envio")
public class EventoMensajeEnvioController {

    private final EventoMensajeEnvioService eventoMensajeEnvioService;

    public EventoMensajeEnvioController(EventoMensajeEnvioService eventoMensajeEnvioService) {
        this.eventoMensajeEnvioService = eventoMensajeEnvioService;
    }

    @PostMapping
    public ResponseEntity<EventoMensajeEnvioResponse> crear(@Valid @RequestBody CrearEventoMensajeEnvioRequest request) {
        EventoMensajeEnvioResponse response = eventoMensajeEnvioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoMensajeEnvioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(eventoMensajeEnvioService.obtenerPorId(id));
    }

    @GetMapping("/proceso/{procesoId}")
    public ResponseEntity<List<EventoMensajeEnvioResponse>> listarPorProceso(@PathVariable Long procesoId) {
        return ResponseEntity.ok(eventoMensajeEnvioService.listarPorProceso(procesoId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoMensajeEnvioResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarEventoMensajeEnvioRequest request) {
        return ResponseEntity.ok(eventoMensajeEnvioService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        eventoMensajeEnvioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}