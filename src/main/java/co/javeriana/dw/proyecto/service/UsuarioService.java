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
import co.javeriana.dw.proyecto.security.JwtService;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final JwtService jwtService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          ModelMapper modelMapper, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.jwtService = jwtService;
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
        // El mapper no cifra: el hash se pone a mano a partir de la contraseña inicial del admin
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);
        return modelMapper.map(usuarioRepository.save(usuario), UsuarioResponse.class);
    }

    // Ahora recibe la empresa de QUIEN HACE la petición (sale del token), no del cliente
    @Transactional
    public UsuarioResponse cambiarRolAcceso(Long usuarioId, Long empresaId, CambiarRolRequest request) {
        Usuario usuario = obtenerDeEmpresa(usuarioId, empresaId);
        usuario.setRolUsuario(request.getRolUsuario());
        return modelMapper.map(usuarioRepository.save(usuario), UsuarioResponse.class);
    }

    @Transactional
    public void desactivar(Long usuarioId, Long empresaId) {
        Usuario usuario = obtenerDeEmpresa(usuarioId, empresaId);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    public SesionResponse autenticar(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(CredencialesInvalidasException::new);
        if (!usuario.isActivo() || !passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        SesionResponse sesion = modelMapper.map(usuario, SesionResponse.class);
        sesion.setToken(jwtService.generarToken(usuario));
        return sesion;
    }

    public List<UsuarioResponse> listarPorEmpresa(Long empresaId) {
        return usuarioRepository.findByEmpresaId(empresaId).stream()
                .map(u -> modelMapper.map(u, UsuarioResponse.class))
                .toList();
    }

    /**
     * Aislamiento entre empresas: un admin de la empresa A no puede tocar usuarios de la B
     * aunque adivine el id. Se responde "no encontrado" (404), no "prohibido", para no
     * revelar que ese id existe en otra empresa.
     */
    private Usuario obtenerDeEmpresa(Long usuarioId, Long empresaId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + usuarioId));
        if (!usuario.getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException("Usuario no encontrado: " + usuarioId);
        }
        return usuario;
    }
}
