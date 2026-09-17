package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.empresa.CrearEmpresaRequest;
import co.javeriana.dw.proyecto.dto.empresa.EmpresaResponse;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioService usuarioService;
    private final ModelMapper modelMapper;

    public EmpresaService(EmpresaRepository empresaRepository, UsuarioService usuarioService,
                           ModelMapper modelMapper) {
        this.empresaRepository = empresaRepository;
        this.usuarioService = usuarioService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public EmpresaResponse crear(CrearEmpresaRequest request) {
        if (empresaRepository.existsByRuc(request.getRuc())) {
            throw new NombreDuplicadoException("Ya existe una empresa registrada con el RUC " + request.getRuc());
        }
        if (empresaRepository.existsByEmail(request.getEmail())) {
            throw new NombreDuplicadoException("Ya existe una empresa registrada con el correo " + request.getEmail());
        }

        // adminNombre/adminPassword no existen en Empresa, ModelMapper simplemente los
        // ignora al no encontrar destino — no hace falta excluirlos a mano.
        Empresa empresa = modelMapper.map(request, Empresa.class);
        empresa = empresaRepository.save(empresa);

        Usuario admin = usuarioService.crearAdministradorInicial(
                empresa, request.getAdminNombre(), request.getEmail(), request.getAdminPassword());

        empresa.setAdminUsuarioId(admin.getId());
        empresa = empresaRepository.save(empresa);

        return modelMapper.map(empresa, EmpresaResponse.class);
    }

    public EmpresaResponse obtenerPorId(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada: " + id));
        return modelMapper.map(empresa, EmpresaResponse.class);
    }
}