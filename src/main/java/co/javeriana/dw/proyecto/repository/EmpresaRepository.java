package co.javeriana.dw.proyecto.repository;

import co.javeriana.dw.proyecto.entidad.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    List<Enpresa> findAllByOrderByIdAsc();
}