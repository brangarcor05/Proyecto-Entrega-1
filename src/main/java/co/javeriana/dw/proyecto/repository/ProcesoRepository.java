package co.javeriana.dw.proyecto.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Proceso;

/**
 * JpaSpecificationExecutor permite combinar los filtros de HU-07 (nombre, estado y
 * categoria) en una sola consulta, sin multiplicar metodos derivados por cada mezcla.
 */
public interface ProcesoRepository extends JpaRepository<Proceso, Long>, JpaSpecificationExecutor<Proceso> {

    boolean existsByNombreAndEmpresaId(String nombre, Long empresaId);

    Optional<Proceso> findByIdAndEstadoNot(Long id, EstadoProceso estadoExcluido);

    Page<Proceso> findByEmpresaIdAndEstadoNot(Long empresaId, EstadoProceso estadoExcluido, Pageable pageable);

    Page<Proceso> findByEmpresaIdAndNombreContainingIgnoreCaseAndEstado(Long empresaId, String nombre, EstadoProceso estado, Pageable pageable);

    Page<Proceso> findByEmpresaIdAndCategoriaAndEstado(Long empresaId, String categoria, EstadoProceso estado, Pageable pageable);

    Page<Proceso> findByEmpresasCompartidasId(Long empresaId, Pageable pageable);

    boolean existsByIdAndEmpresasCompartidasId(Long procesoId, Long empresaId);
}
