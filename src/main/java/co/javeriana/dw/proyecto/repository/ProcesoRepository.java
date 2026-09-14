package co.javeriana.dw.proyecto.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Proceso;



public interface ProcesoRepository extends JpaRepository<Proceso, Long> {
    
    boolean existsByNombreAndEmpresaId(String nombre, Long empresaId);
    Page<Proceso> findByEmpresaIdAndEstadoNot(Long empresaId, EstadoProceso estadoExcluido, Pageable pageable);
    Page<Proceso> findByEmpresaIdAndNombreContainingIgnoreCaseAndEstado(Long empresaId, String nombre, EstadoProceso estado, Pageable pageable);
    Page<Proceso> findByEmpresaIdAndCategoriaAndEstado(Long empresaId, String categoria, EstadoProceso estado, Pageable pageable);
    Page<Proceso> findByEmpresasCompartidasId(Long empresaId, Pageable pageable);
    boolean existsByIdAndEmpresasCompartidasId(Long procesoId, Long empresaId);
}
