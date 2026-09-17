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

import co.javeriana.dw.proyecto.dto.arco.ActualizarArcoRequest;
import co.javeriana.dw.proyecto.dto.arco.ArcoEliminadoResponse;
import co.javeriana.dw.proyecto.dto.arco.ArcoResponse;
import co.javeriana.dw.proyecto.dto.arco.CrearArcoRequest;
import co.javeriana.dw.proyecto.service.ArcoService;
import jakarta.validation.Valid;

/**
 * Arcos del diagrama (HU-11 a HU-13).
 *
 * Mientras no exista el inicio de sesion (HU-03), usuarioId llega como parametro de
 * la peticion. Cuando el login este listo debe salir del usuario autenticado.
 */
@RestController
@RequestMapping("/api/arcos")
public class ArcoController {

    private final ArcoService arcoService;

    public ArcoController(ArcoService arcoService) {
        this.arcoService = arcoService;
    }

    @GetMapping
    public ResponseEntity<List<ArcoResponse>> listarPorProceso(@RequestParam Long procesoId) {
        return ResponseEntity.ok(arcoService.listarPorProceso(procesoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArcoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(arcoService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<ArcoResponse> crear(
            @Valid @RequestBody CrearArcoRequest request,
            @RequestParam Long usuarioId) {

        ArcoResponse arcoCreado = arcoService.crear(request, usuarioId);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(arcoCreado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(arcoCreado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArcoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarArcoRequest request,
            @RequestParam Long usuarioId) {

        return ResponseEntity.ok(arcoService.actualizar(id, request, usuarioId));
    }

    /**
     * Devuelve 200 con cuerpo en vez del 204 habitual: HU-13 pide advertir si la
     * eliminacion deja un elemento sin camino de entrada o de salida, y esas
     * advertencias viajan en la respuesta.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ArcoEliminadoResponse> eliminar(
            @PathVariable Long id,
            @RequestParam Long usuarioId) {

        return ResponseEntity.ok(arcoService.eliminar(id, usuarioId));
    }
}
