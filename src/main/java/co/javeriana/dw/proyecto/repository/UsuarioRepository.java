package co.javeriana.dw.proyecto.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String correo);
    boolean existsByEmail(String correo);
    List<Usuario> findByEmpresaId(Long empresaId);
}
