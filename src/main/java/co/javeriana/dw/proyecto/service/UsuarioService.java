package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.autenticacion.LoginRequest;
import co.javeriana.dw.proyecto.dto.autenticacion.SesionResponse;
import co.javeriana.dw.proyecto.dto.usuario.CambiarRolRequest;
import co.javeriana.dw.proyecto.dto.usuario.CrearUsuarioRequest;
import co.javeriana.dw.proyecto.dto.usuario.UsuarioResponse;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.CredencialesInvalidasException;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                           ModelMapper modelMapper) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public Usuario crearAdministradorInicial(Empresa empresa, String nombre, String correo, String passwordPlano) {
        if (usuarioRepository.existsByEmail(correo)) {
            throw new NombreDuplicadoException("Ya existe un usuario con el correo " + correo);
        }
        Usuario admin = Usuario.builder()
                .empresa(empresa)
                .nombre(nombre)
                .email(correo)
                .passwordHash(passwordEncoder.encode(passwordPlano))
                .rolUsuario(RolUsuario.ADMIN)
                .activo(true)
                .build();
        return usuarioRepository.save(admin);
    }

    @Transactional
    public UsuarioResponse invitarUsuario(Empresa empresa, CrearUsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new NombreDuplicadoException("El correo " + request.getEmail() + " ya está registrado");
        }

        
        Usuario usuario = modelMapper.map(request, Usuario.class);
        usuario.setEmpresa(empresa);
        usuario.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setActivo(true);

        return modelMapper.map(usuarioRepository.save(usuario), UsuarioResponse.class);
    }

    @Transactional
    public UsuarioResponse cambiarRolAcceso(Long usuarioId, CambiarRolRequest request) {
        Usuario usuario = obtenerPorId(usuarioId);
        modelMapper.map(request, usuario); 
        return modelMapper.map(usuarioRepository.save(usuario), UsuarioResponse.class);
    }

    @Transactional
    public void desactivar(Long usuarioId) {
        Usuario usuario = obtenerPorId(usuarioId);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    public SesionResponse autenticar(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(CredencialesInvalidasException::new);
        if (!usuario.isActivo() || !passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        return modelMapper.map(usuario, SesionResponse.class);
    }

    public List<UsuarioResponse> listarPorEmpresa(Long empresaId) {
        return usuarioRepository.findByEmpresaId(empresaId).stream()
                .map(u -> modelMapper.map(u, UsuarioResponse.class))
                .toList();
    }

    private Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }
}