package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.empresa.CrearEmpresaRequest;
import co.javeriana.dw.proyecto.dto.empresa.EmpresaResponse;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import co.javeriana.dw.proyecto.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HU-01 (registro, publico) y consultas de empresas para usuarios autenticados.
 * La empresa del usuario sale del access token.
 */
@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @PostMapping
    public ResponseEntity<EmpresaResponse> crear(@Valid @RequestBody CrearEmpresaRequest request) {
        EmpresaResponse response = empresaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Solo la propia empresa: otra se reporta como inexistente para no revelarla. */
    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponse> obtener(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id) {
        if (!id.equals(actor.empresaId())) {
            throw new RecursoNoEncontradoException("Empresa no encontrada: " + id);
        }
        return ResponseEntity.ok(empresaService.obtenerPorId(id));
    }

    /** HU-23: otras empresas con las que se puede compartir un proceso (datos minimos). */
    @GetMapping
    public ResponseEntity<List<EmpresaResponse>> listar(@AuthenticationPrincipal UsuarioAutenticado actor) {
        return ResponseEntity.ok(empresaService.listarParaCompartir(actor.empresaId()));
    }
}
