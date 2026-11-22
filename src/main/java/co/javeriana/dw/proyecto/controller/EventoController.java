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

import co.javeriana.dw.proyecto.dto.evento.ActualizarEventoRequest;
import co.javeriana.dw.proyecto.dto.evento.CrearEventoRequest;
import co.javeriana.dw.proyecto.dto.evento.EventoResponse;
import co.javeriana.dw.proyecto.service.EventoService;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import jakarta.validation.Valid;

/**
 * Eventos de inicio y fin del proceso.
 *
 * El usuario y la empresa salen del access token (@AuthenticationPrincipal), nunca de
 * un parametro que el cliente pueda cambiar. Las lecturas exigen que la empresa del
 * token sea la propietaria del proceso o una invitada (HU-23).
 */
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    public ResponseEntity<List<EventoResponse>> listarPorProceso(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @RequestParam Long procesoId) {
        return ResponseEntity.ok(eventoService.listarPorProceso(procesoId, actor.empresaId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtener(id, actor.empresaId()));
    }

    @PostMapping
    public ResponseEntity<EventoResponse> crear(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @Valid @RequestBody CrearEventoRequest request) {

        EventoResponse eventoCreado = eventoService.crear(request, actor.id());
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
            @AuthenticationPrincipal UsuarioAutenticado actor) {

        return ResponseEntity.ok(eventoService.actualizar(id, request, actor.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        eventoService.eliminar(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
