package co.javeriana.dw.proyecto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.RolProceso;

public interface RolProcesoRepository extends JpaRepository<RolProceso, Long> {

    List<RolProceso> findByEmpresaId(Long empresaId);

    Optional<RolProceso> findByIdAndActivoTrue(Long id);

    // HU-17: el nombre del rol es unico dentro de la empresa.
    boolean existsByNombreIgnoreCaseAndEmpresaId(String nombre, Long empresaId);

    // HU-18: al editar, la unicidad se valida excluyendo el propio rol (si no cambio
    // el nombre, no deberia chocar consigo mismo).
    boolean existsByNombreIgnoreCaseAndEmpresaIdAndIdNot(String nombre, Long empresaId, Long id);

    // HU-20: listado paginado, con busqueda opcional por nombre.
    Page<RolProceso> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);

    Page<RolProceso> findByEmpresaIdAndActivoTrueAndNombreContainingIgnoreCase(
            Long empresaId, String nombre, Pageable pageable);
}
