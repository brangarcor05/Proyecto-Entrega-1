package co.javeriana.dw.proyecto.repository;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.Evento;

public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findByProcesoId(Long procesoId);
}