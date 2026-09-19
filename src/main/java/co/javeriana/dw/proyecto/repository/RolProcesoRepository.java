package co.javeriana.dw.proyecto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.RolProceso;

public interface RolProcesoRepository extends JpaRepository<RolProceso, Long> {
    List<RolProceso> findByEmpresaId(Long empresaId);
}
