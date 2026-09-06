package co.javeriana.dw.proyecto.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario guardar(Usuario usuario) {
        Optional<Usuario> usuarioExistente = usuarioRepository
                .findByEmpresaAndEmail(usuario.getEmpresa(), usuario.getEmail());
        if (usuarioExistente.isPresent() && !usuarioExistente.get().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email en la empresa.");
        }
        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAllByOrderByIdAsc();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public List<Usuario> listarPorEmpresa(Empresa empresa) {
        return usuarioRepository.findByEmpresaAndActivo(empresa, true);
    }

    public Optional<Usuario> buscarPorEmpresaYEmail(Empresa empresa, String email) {
        return usuarioRepository.findByEmpresaAndEmail(empresa, email);
    }

    public Usuario actualizar(Usuario usuario) {
        if (usuario.getId() == null) {
            throw new IllegalArgumentException("El usuario debe tener un ID para ser actualizado.");
        }
        if (!usuarioRepository.existsById(usuario.getId())) {
            throw new IllegalArgumentException("El usuario no existe.");
        }
        return guardar(usuario);
    }

    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }
}
