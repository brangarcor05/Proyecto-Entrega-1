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

import co.javeriana.dw.proyecto.dto.evento.ActualizarEventoRequest;
import co.javeriana.dw.proyecto.dto.evento.CrearEventoRequest;
import co.javeriana.dw.proyecto.dto.evento.EventoResponse;
import co.javeriana.dw.proyecto.service.EventoService;
import jakarta.validation.Valid;

/**
 * Eventos de inicio y fin del proceso.
 *
 * Mientras no exista el inicio de sesion (HU-03), usuarioId llega como parametro de
 * la peticion. Cuando el login este listo debe salir del usuario autenticado.
 */
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    public ResponseEntity<List<EventoResponse>> listarPorProceso(@RequestParam Long procesoId) {
        return ResponseEntity.ok(eventoService.listarPorProceso(procesoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<EventoResponse> crear(
            @Valid @RequestBody CrearEventoRequest request,
            @RequestParam Long usuarioId) {

        EventoResponse eventoCreado = eventoService.crear(request, usuarioId);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(eventoCreado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(eventoCreado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEventoRequest request,
            @RequestParam Long usuarioId) {

        return ResponseEntity.ok(eventoService.actualizar(id, request, usuarioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        eventoService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
