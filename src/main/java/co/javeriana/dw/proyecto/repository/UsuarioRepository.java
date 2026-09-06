package co.javeriana.dw.proyecto.repository;

import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    List<Usuario> findAllByOrderByIdAsc();

    List<Usuario> findByEmpresa(Empresa empresa);

    List<Usuario> findByEmpresaAndActivo(Empresa empresa, boolean activo);

    Optional<Usuario> findByEmpresaAndEmail(Empresa empresa, String email);
}
