package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.usuario.CambiarRolRequest;
import co.javeriana.dw.proyecto.dto.usuario.CrearUsuarioRequest;
import co.javeriana.dw.proyecto.dto.usuario.UsuarioResponse;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import co.javeriana.dw.proyecto.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // HU-02: solo el administrador gestiona usuarios. La empresa sale del token.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> invitar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @Valid @RequestBody CrearUsuarioRequest request) {
        Empresa empresa = new Empresa();
        empresa.setId(actor.empresaId());
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.invitarUsuario(empresa, request));
    }

    @PatchMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> cambiarRol(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id,
            @Valid @RequestBody CambiarRolRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarRolAcceso(id, actor.empresaId(), request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        usuarioService.desactivar(id, actor.empresaId());
        return ResponseEntity.noContent().build();
    }

    // Listar: cualquier usuario autenticado, pero SOLO ve los de su propia empresa
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar(@AuthenticationPrincipal UsuarioAutenticado actor) {
        return ResponseEntity.ok(usuarioService.listarPorEmpresa(actor.empresaId()));
    }
}
