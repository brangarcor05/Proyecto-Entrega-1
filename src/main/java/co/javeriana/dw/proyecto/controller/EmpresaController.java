package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.empresa.CrearEmpresaRequest;
import co.javeriana.dw.proyecto.dto.empresa.EmpresaResponse;
import co.javeriana.dw.proyecto.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(empresaService.obtenerPorId(id));
    }
}