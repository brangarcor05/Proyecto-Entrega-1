package co.javeriana.dw.proyecto.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;

/**
 * Reglas de acceso comunes a todos los servicios del modelado. Las historias repiten
 * dos: crear y modificar exige administrador o editor (HU-05, HU-09, HU-21), y
 * eliminar es solo del administrador (HU-06, HU-10, HU-13, HU-16, HU-19).
 *
 * Cada metodo devuelve el usuario ya cargado, que es el que necesita el historial.
 */
@Service
public class PermisoService {

    private final UsuarioRepository usuarioRepository;

    public PermisoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + usuarioId));
    }

    @Transactional(readOnly = true)
    public Usuario validarPuedeEditar(Long usuarioId) {
        Usuario usuario = obtener(usuarioId);
        boolean puedeEditar = usuario.getRolUsuario() == RolUsuario.ADMIN
                || usuario.getRolUsuario() == RolUsuario.EDITOR;
        if (!puedeEditar) {
            throw new PermisoDenegadoException(
                    "Los usuarios de solo lectura no pueden crear ni modificar elementos");
        }
        return usuario;
    }

    @Transactional(readOnly = true)
    public Usuario validarEsAdministrador(Long usuarioId) {
        Usuario usuario = obtener(usuarioId);
        if (usuario.getRolUsuario() != RolUsuario.ADMIN) {
            throw new PermisoDenegadoException("Solo un administrador puede eliminar");
        }
        return usuario;
    }
}
