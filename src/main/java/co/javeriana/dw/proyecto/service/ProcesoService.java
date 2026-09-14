package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcesoService {

    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final HistorialService historialService;

    public ProcesoService(ProcesoRepository procesoRepository, PoolRepository poolRepository,
                           HistorialService historialService) {
        this.procesoRepository = procesoRepository;
        this.poolRepository = poolRepository;
        this.historialService = historialService;
    }

    @Transactional
    public Proceso crear(Empresa empresa, String nombre, String descripcion, String categoria,
                          Usuario usuarioCreador) {
        if (procesoRepository.existsByNombreAndEmpresaId(nombre, empresa.getId())) {
            throw new NombreDuplicadoException("Ya existe un proceso llamado '" + nombre + "' en esta empresa");
        }

        Proceso proceso = new Proceso();
        proceso.setNombre(nombre);
        proceso.setDescripcion(descripcion);
        proceso.setCategoria(categoria);
        proceso.setEstado(EstadoProceso.BORRADOR);
        proceso.setEmpresa(empresa);
        proceso = procesoRepository.save(proceso);

        // HU-04: "al crearlo queda listo para agregarle elementos, con el pool de la
        // empresa ya definido" — se crea el pool propietario automáticamente.
        Pool poolPropietario = new Pool();
        poolPropietario.setNombre(empresa.getNombre());
        poolPropietario.setEsPropietario(true);
        poolPropietario.setCajaNegra(false);
        poolPropietario.setProceso(proceso);
        poolRepository.save(poolPropietario);

        historialService.registrar("Proceso", proceso.getId(), AccionHistorial.CREACION,
                usuarioCreador, proceso, "Proceso creado");
        return proceso;
    }

    @Transactional
    public Proceso editar(Long procesoId, String nombre, String descripcion, String categoria,
                           EstadoProceso estado, Usuario usuarioEditor) {
        validarPuedeEditar(usuarioEditor);
        Proceso proceso = obtenerPorId(procesoId);

        boolean cambioNombre = !proceso.getNombre().equals(nombre);
        if (cambioNombre && procesoRepository.existsByNombreAndEmpresaId(nombre, proceso.getEmpresa().getId())) {
            throw new NombreDuplicadoException("Ya existe un proceso llamado '" + nombre + "' en esta empresa");
        }

        proceso.setNombre(nombre);
        proceso.setDescripcion(descripcion);
        proceso.setCategoria(categoria);
        proceso.setEstado(estado);
        proceso = procesoRepository.save(proceso);

        historialService.registrar("Proceso", proceso.getId(), AccionHistorial.EDICION,
                usuarioEditor, proceso, "Datos del proceso actualizados");
        return proceso;
    }

    @Transactional
    public void eliminar(Long procesoId, Usuario usuarioAdministrador) {
        if (usuarioAdministrador.getRolUsuario() != RolUsuario.ADMIN) {
            throw new PermisoDenegadoException("Solo un administrador puede eliminar procesos");
        }
        Proceso proceso = obtenerPorId(procesoId);
        proceso.setEstado(EstadoProceso.INACTIVO); // eliminación lógica (HU-06)
        procesoRepository.save(proceso);

        historialService.registrar("Proceso", proceso.getId(), AccionHistorial.ELIMINACION,
                usuarioAdministrador, proceso, "Proceso marcado como inactivo");
    }

    public Page<Proceso> consultar(Long empresaId, String nombre, String categoria,
                                    EstadoProceso estado, Pageable pageable) {
        // HU-06: "un proceso inactivo deja de aparecer en el listado por defecto"
        if (estado == null) {
            return procesoRepository.findByEmpresaIdAndEstadoNot(empresaId, EstadoProceso.INACTIVO, pageable);
        }
        if (nombre != null && !nombre.isBlank()) {
            return procesoRepository.findByEmpresaIdAndNombreContainingIgnoreCaseAndEstado(
                    empresaId, nombre, estado, pageable);
        }
        return procesoRepository.findByEmpresaIdAndCategoriaAndEstado(empresaId, categoria, estado, pageable);
    }

    private void validarPuedeEditar(Usuario usuario) {
        if (usuario.getRolUsuario() == RolUsuario.LECTURA) {
            throw new PermisoDenegadoException("Los usuarios de solo lectura no pueden editar procesos");
        }
    }

    private Proceso obtenerPorId(Long id) {
        return procesoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + id));
    }
}