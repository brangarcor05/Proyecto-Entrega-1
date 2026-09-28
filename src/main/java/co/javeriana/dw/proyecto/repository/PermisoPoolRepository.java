package co.javeriana.dw.proyecto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.PermisoPool;
import co.javeriana.dw.proyecto.entidad.RolUsuario;

public interface PermisoPoolRepository extends JpaRepository<PermisoPool, Long> {
    List<PermisoPool> findByEmpresaId(Long empresaId);
    Optional<PermisoPool> findByEmpresaIdAndRolUsuario(Long empresaId, RolUsuario rolAcceso);
}
