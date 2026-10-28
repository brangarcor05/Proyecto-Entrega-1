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

import co.javeriana.dw.proyecto.dto.notificacionexterna.ActualizarNotificacionExternaRequest;
import co.javeriana.dw.proyecto.dto.notificacionexterna.CrearNotificacionExternaRequest;
import co.javeriana.dw.proyecto.dto.notificacionexterna.NotificacionExternaResponse;
import co.javeriana.dw.proyecto.service.NotificacionExternaService;
import jakarta.validation.Valid;

/**
 * Notificaciones hacia sistemas externos.
 *
 * Mientras no exista el inicio de sesion (HU-03), usuarioId llega como parametro de
 * la peticion. Cuando el login este listo debe salir del usuario autenticado.
 */
@RestController
@RequestMapping("/api/notificaciones-externas")
public class NotificacionExternaController {

    private final NotificacionExternaService notificacionExternaService;

    public NotificacionExternaController(NotificacionExternaService notificacionExternaService) {
        this.notificacionExternaService = notificacionExternaService;
    }

    @GetMapping
    public ResponseEntity<List<NotificacionExternaResponse>> listarPorProceso(@RequestParam Long procesoId) {
        return ResponseEntity.ok(notificacionExternaService.listarPorProceso(procesoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificacionExternaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionExternaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<NotificacionExternaResponse> crear(
            @Valid @RequestBody CrearNotificacionExternaRequest request,
            @RequestParam Long usuarioId) {

        NotificacionExternaResponse creada = notificacionExternaService.crear(request, usuarioId);
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
            @RequestParam Long usuarioId) {

        return ResponseEntity.ok(notificacionExternaService.actualizar(id, request, usuarioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        notificacionExternaService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}