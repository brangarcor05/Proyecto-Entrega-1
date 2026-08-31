package co.javeriana.dw.proyecto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.Empresa;
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    List<Empresa> findAllByOrderByIdAsc();
}