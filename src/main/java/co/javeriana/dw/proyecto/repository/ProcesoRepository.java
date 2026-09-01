package co.javeriana.dw.proyecto.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;


public interface ProcesoRepository extends JpaRepository<Proceso,Long>{
    List<Proceso> findAllByOrderByIdAsc();
    List<Proceso> findByEmpresa(Empresa empresa);
    List<Proceso> findByEmpresaAndActivo(Empresa empresa, boolean activo);
    Optional<Proceso> findByEmpresaAndNombre(Empresa empresa, String nombre);
    List<Proceso> findByEmpresaAndEstado(Empresa empresa, EstadoProceso estado);
    List<Proceso> findByEmpresaAndCategoria(Empresa empresa, String categoria);
    List<Proceso> findByEmpresaAndActivoAndEstado( Empresa empresa, boolean activo, EstadoProceso estado);
    List<Proceso> findByEmpresaAndActivoAndCategoria( Empresa empresa, boolean activo, String categoria);
}
