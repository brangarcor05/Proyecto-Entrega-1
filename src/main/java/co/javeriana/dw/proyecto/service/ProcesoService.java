package co.javeriana.dw.proyecto.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;

@Service
public class ProcesoService {
    private final ProcesoRepository procesoRepository;

    public ProcesoService(ProcesoRepository procesoRepository) {
        this.procesoRepository = procesoRepository;
    }

    public Proceso guardar(Proceso proceso) {
        Optional<Proceso> procesoExistente = procesoRepository.findByEmpresaAndNombre(proceso.getEmpresa(), proceso.getNombre());
        if (procesoExistente.isPresent() && !procesoExistente.get().getId().equals(proceso.getId())) {
            throw new IllegalArgumentException("Ya existe un proceso con ese nombre en la empresa.");
        }
        return procesoRepository.save(proceso);
    }

    public List<Proceso> listarTodos() {
        return procesoRepository.findAllByOrderByIdAsc();
    }

    public Optional<Proceso> buscarPorId(Long id) {
        return procesoRepository.findById(id);
    }

    public List<Proceso> listarPorEmpresa(Empresa empresa) {
        return procesoRepository.findByEmpresaAndActivo(empresa,true);
    }

    public List<Proceso> listarPorEmpresaYEstado(Empresa empresa, EstadoProceso estado) {
        return procesoRepository.findByEmpresaAndActivoAndEstado(empresa,true, estado);
    }

    public List<Proceso> listarPorEmpresaYCategoria(Empresa empresa, String categoria) {
        return procesoRepository.findByEmpresaAndActivoAndCategoria(empresa,true, categoria);
    }

    public Proceso actualizar(Proceso proceso) {
        if (proceso.getId() == null) {
            throw new IllegalArgumentException("El proceso debe tener un ID para ser actualizado.");
        }
        if (!procesoRepository.existsById(proceso.getId())) {
            throw new IllegalArgumentException("El proceso no existe.");
        }
        return guardar(proceso);
    }

    public void eliminar(Long id) {
        Optional<Proceso> proceso = procesoRepository.findById(id);
        if (proceso.isEmpty()) {
            throw new IllegalArgumentException("El proceso no existe.");
        }

        Proceso procesoExistente = proceso.get();
        procesoExistente.setActivo(false);
        procesoRepository.save(procesoExistente);
    }

    public List<Proceso> listarPorEstado(Empresa empresa, EstadoProceso estado) {
        return procesoRepository.findByEmpresaAndEstado(empresa, estado);
    }

    public List<Proceso> listarPorCategoria(Empresa empresa, String categoria) {
        return procesoRepository.findByEmpresaAndCategoria(empresa,categoria);
    }
}