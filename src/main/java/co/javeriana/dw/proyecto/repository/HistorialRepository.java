package co.javeriana.dw.proyecto.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.Historial;

public interface HistorialRepository extends JpaRepository<Historial, Long> {
    List<Historial> findByProcesoIdOrderByFechaDesc(Long procesoId);
    List<Historial> findByEntidadTipoAndEntidadIdOrderByFechaDesc(String entidadTipo, Long entidadId);
}