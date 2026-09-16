package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.exception.CredencialesInvalidasException;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Lo llama EmpresaService al registrar la empresa (HU-01): "se genera un usuario
    // administrador inicial con las credenciales del representante"
    @Transactional
    public Usuario crearAdministradorInicial(Empresa empresa, String correo, String passwordPlano) {
        if (usuarioRepository.existsByEmail(correo)) {
            throw new NombreDuplicadoException("Ya existe un usuario con el correo " + correo);
        }
        Usuario admin = new Usuario();
        admin.setEmail(correo);
        admin.setPasswordHash(passwordEncoder.encode(passwordPlano));
        admin.setRolUsuario(RolUsuario.ADMIN);
        admin.setEmpresa(empresa);
        return usuarioRepository.save(admin);
    }

    @Transactional
    public Usuario invitarUsuario(Empresa empresa, String correo, RolUsuario rolUsuario) {
        if (usuarioRepository.existsByEmail(correo)) {
            throw new NombreDuplicadoException("El correo " + correo + " ya está registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(correo);
        // Contraseña temporal aleatoria: el flujo real de invitación (token + link para
        // que el usuario la defina) queda fuera de esta HU puntual — avísame si lo
        // quieres modelar ahora o lo dejamos para cuando montemos el envío de correos.
        usuario.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setRolUsuario(rolUsuario);
        usuario.setEmpresa(empresa);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario cambiarRolAcceso(Long usuarioId, RolUsuario nuevoRol) {
        Usuario usuario = obtenerPorId(usuarioId);
        usuario.setRolUsuario(nuevoRol);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void desactivar(Long usuarioId) {
        Usuario usuario = obtenerPorId(usuarioId);
        usuario.setActivo(false);
        // No se tocan sus procesos: "si el usuario se desactiva, sus procesos siguen
        // disponibles" (HU-02) — Proceso no tiene FK a Usuario, así que esto ya se cumple solo.
        usuarioRepository.save(usuario);
    }

    public Usuario autenticar(String correo, String passwordPlano) {
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(CredencialesInvalidasException::new);
        if (!usuario.isActivo() || !passwordEncoder.matches(passwordPlano, usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        return usuario;
    }

    public List<Usuario> listarPorEmpresa(Long empresaId) {
        return usuarioRepository.findByEmpresaId(empresaId);
    }

    private Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }
}