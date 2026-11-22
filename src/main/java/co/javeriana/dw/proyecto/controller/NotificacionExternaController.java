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

import co.javeriana.dw.proyecto.dto.notificacionexterna.ActualizarNotificacionExternaRequest;
import co.javeriana.dw.proyecto.dto.notificacionexterna.CrearNotificacionExternaRequest;
import co.javeriana.dw.proyecto.dto.notificacionexterna.NotificacionExternaResponse;
import co.javeriana.dw.proyecto.service.NotificacionExternaService;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import jakarta.validation.Valid;

/**
 * Notificaciones hacia sistemas externos.
 *
 * El usuario y la empresa salen del access token (@AuthenticationPrincipal), nunca de
 * un parametro que el cliente pueda cambiar. Las lecturas exigen que la empresa del
 * token sea la propietaria del proceso o una invitada (HU-23).
 */
@RestController
@RequestMapping("/api/notificaciones-externas")
public class NotificacionExternaController {

    private final NotificacionExternaService notificacionExternaService;

    public NotificacionExternaController(NotificacionExternaService notificacionExternaService) {
        this.notificacionExternaService = notificacionExternaService;
    }

    @GetMapping
    public ResponseEntity<List<NotificacionExternaResponse>> listarPorProceso(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @RequestParam Long procesoId) {
        return ResponseEntity.ok(notificacionExternaService.listarPorProceso(procesoId, actor.empresaId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificacionExternaResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        return ResponseEntity.ok(notificacionExternaService.obtener(id, actor.empresaId()));
    }

    @PostMapping
    public ResponseEntity<NotificacionExternaResponse> crear(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @Valid @RequestBody CrearNotificacionExternaRequest request) {

        NotificacionExternaResponse creada = notificacionExternaService.crear(request, actor.id());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificacionExternaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarNotificacionExternaRequest request,
            @AuthenticationPrincipal UsuarioAutenticado actor) {

        return ResponseEntity.ok(notificacionExternaService.actualizar(id, request, actor.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        notificacionExternaService.eliminar(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}