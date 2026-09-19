package co.javeriana.dw.proyecto.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.historial.HistorialResponse;
import co.javeriana.dw.proyecto.dto.proceso.ActualizarProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.CompartirProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.CrearProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.ProcesoResponse;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Service
public class ProcesoService {

    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final EmpresaRepository empresaRepository;
    private final PermisoService permisoService;
    private final HistorialService historialService;

    public ProcesoService(ProcesoRepository procesoRepository, PoolRepository poolRepository,
                          EmpresaRepository empresaRepository, PermisoService permisoService,
                          HistorialService historialService) {
        this.procesoRepository = procesoRepository;
        this.poolRepository = poolRepository;
        this.empresaRepository = empresaRepository;
        this.permisoService = permisoService;
        this.historialService = historialService;
    }

    @Transactional
    public ProcesoResponse crear(CrearProcesoRequest request, Long usuarioId) {
        Usuario usuarioCreador = permisoService.validarPuedeEditar(usuarioId);

        Empresa empresa = empresaRepository.findById(request.empresaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la empresa " + request.empresaId()));

        if (procesoRepository.existsByNombreAndEmpresaId(request.nombre(), empresa.getId())) {
            throw new NombreDuplicadoException(
                    "Ya existe un proceso con ese nombre en esta empresa");
        }

        Proceso proceso = new Proceso();
        proceso.setNombre(request.nombre());
        proceso.setDescripcion(request.descripcion());
        proceso.setCategoria(request.categoria());
        proceso.setEstado(EstadoProceso.BORRADOR);
        proceso.setEmpresa(empresa);
        proceso = procesoRepository.save(proceso);

        // HU-04: "al crearlo queda listo para agregarle elementos, con el pool de la
        // empresa ya definido" - se crea el pool propietario automaticamente.
        Pool poolPropietario = new Pool();
        poolPropietario.setNombre(empresa.getNombre());
        poolPropietario.setEsPropietario(true);
        poolPropietario.setCajaNegra(false);
        poolPropietario.setProceso(proceso);
        poolRepository.save(poolPropietario);

        historialService.registrar("Proceso", proceso.getId(), AccionHistorial.CREACION,
                usuarioCreador, proceso, "Proceso creado");
        return ProcesoResponse.desde(proceso, empresa.getId());
    }

    @Transactional
    public ProcesoResponse actualizar(Long procesoId, ActualizarProcesoRequest request, Long usuarioId) {
        Usuario usuarioEditor = permisoService.validarPuedeEditar(usuarioId);
        Proceso proceso = obtenerActivo(procesoId);
        validarEsPropietario(usuarioEditor, proceso);

        boolean cambioNombre = !proceso.getNombre().equals(request.nombre());
        if (cambioNombre
                && procesoRepository.existsByNombreAndEmpresaId(request.nombre(), proceso.getEmpresa().getId())) {
            throw new NombreDuplicadoException(
                    "Ya existe un proceso con ese nombre en esta empresa");
        }

        proceso.setNombre(request.nombre());
        proceso.setDescripcion(request.descripcion());
        proceso.setCategoria(request.categoria());
        proceso.setEstado(request.estado());
        proceso = procesoRepository.save(proceso);

        historialService.registrar("Proceso", proceso.getId(), AccionHistorial.EDICION,
                usuarioEditor, proceso, "Datos del proceso actualizados");
        return ProcesoResponse.desde(proceso, usuarioEditor.getEmpresa().getId());
    }

    /**
     * HU-23. Define que otras empresas pueden consultar el proceso. Solo el
     * administrador de la empresa propietaria puede cambiarlo, y el acceso que
     * concede es siempre de solo lectura.
     */
    @Transactional
    public ProcesoResponse compartir(Long procesoId, CompartirProcesoRequest request, Long usuarioId) {
        Usuario administrador = permisoService.validarEsAdministrador(usuarioId);
        Proceso proceso = obtenerActivo(procesoId);
        validarEsPropietario(administrador, proceso);

        proceso.getEmpresasCompartidas().clear();

        if (request.compartido()) {
            if (request.empresasIds() == null || request.empresasIds().isEmpty()) {
                throw new ReglaNegocioException(
                        "Para compartir el proceso hay que indicar al menos una empresa");
            }
            proceso.getEmpresasCompartidas().addAll(resolverInvitadas(request.empresasIds(), proceso));
        }
        proceso.setCompartido(request.compartido());
        proceso = procesoRepository.save(proceso);

        String detalle = request.compartido()
                ? "Proceso compartido con " + proceso.getEmpresasCompartidas().size() + " empresa(s)"
                : "Se retiro la comparticion del proceso";
        historialService.registrar("Proceso", proceso.getId(), AccionHistorial.EDICION,
                administrador, proceso, detalle);

        return ProcesoResponse.desde(proceso, administrador.getEmpresa().getId());
    }

    /**
     * HU-23: la empresa invitada puede abrir el proceso, pero lo recibe marcado como
     * solo lectura. Un proceso ajeno que no le fue compartido se reporta como
     * inexistente, para no revelar que existe.
     */
    @Transactional(readOnly = true)
    public ProcesoResponse obtener(Long procesoId, Long empresaId) {
        Proceso proceso = obtenerActivo(procesoId);
        if (!puedeConsultar(proceso, empresaId)) {
            throw new RecursoNoEncontradoException("Proceso no encontrado: " + procesoId);
        }
        return ProcesoResponse.desde(proceso, empresaId);
    }

    /**
     * HU-07: historial de cambios del proceso. Queda reservado a la empresa
     * propietaria: HU-23 aclara que compartir un proceso comparte su modelo, nunca
     * los usuarios de la organizacion, y el historial dice quien hizo cada cambio.
     */
    @Transactional(readOnly = true)
    public List<HistorialResponse> consultarHistorial(Long procesoId, Long empresaId) {
        Proceso proceso = obtenerActivo(procesoId);
        if (!proceso.getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException("Proceso no encontrado: " + procesoId);
        }
        return historialService.listarPorProceso(procesoId);
    }

    /**
     * Listado de HU-07: los procesos de la empresa con busqueda por nombre y filtros
     * combinables de estado y categoria. Incluye ademas los que otras empresas le
     * compartieron (HU-23), que llegan marcados como solo lectura.
     */
    @Transactional(readOnly = true)
    public Page<ProcesoResponse> consultar(Long empresaId, String nombre, EstadoProceso estado,
                                           String categoria, boolean incluirInactivos, Pageable pageable) {
        if (!empresaRepository.existsById(empresaId)) {
            throw new RecursoNoEncontradoException("No existe la empresa " + empresaId);
        }

        Specification<Proceso> filtro = (root, query, cb) -> {
            // El join con las empresas invitadas puede repetir filas del mismo proceso.
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> condiciones = new ArrayList<>();

            var invitadas = root.join("empresasCompartidas", JoinType.LEFT);
            condiciones.add(cb.or(
                    cb.equal(root.get("empresa").get("id"), empresaId),
                    cb.and(cb.isTrue(root.get("compartido")), cb.equal(invitadas.get("id"), empresaId))));

            if (!incluirInactivos) {
                condiciones.add(cb.notEqual(root.get("estado"), EstadoProceso.INACTIVO));
            }
            if (nombre != null && !nombre.isBlank()) {
                condiciones.add(cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
            }
            if (estado != null) {
                condiciones.add(cb.equal(root.get("estado"), estado));
            }
            if (categoria != null && !categoria.isBlank()) {
                condiciones.add(cb.equal(cb.lower(root.get("categoria")), categoria.toLowerCase()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };

        return procesoRepository.findAll(filtro, pageable)
                .map(proceso -> ProcesoResponse.desde(proceso, empresaId));
    }

    /** Eliminacion logica de HU-06: el proceso se conserva y deja de listarse. */
    @Transactional
    public void eliminar(Long procesoId, Long usuarioId) {
        Usuario usuarioAdministrador = permisoService.validarEsAdministrador(usuarioId);
        Proceso proceso = obtenerActivo(procesoId);
        validarEsPropietario(usuarioAdministrador, proceso);

        // Se marcan los dos campos para que no queden contradictorios: el estado es el
        // que consultan los filtros, y activo ya venia en la entidad desde antes.
        proceso.setEstado(EstadoProceso.INACTIVO);
        proceso.setActivo(false);
        procesoRepository.save(proceso);

        historialService.registrar("Proceso", proceso.getId(), AccionHistorial.ELIMINACION,
                usuarioAdministrador, proceso, "Proceso marcado como inactivo");
    }

    private Set<Empresa> resolverInvitadas(Set<Long> empresasIds, Proceso proceso) {
        Set<Empresa> invitadas = new HashSet<>();
        for (Long empresaId : empresasIds) {
            if (proceso.getEmpresa().getId().equals(empresaId)) {
                throw new ReglaNegocioException(
                        "El proceso no se comparte con su propia empresa: ya es la propietaria");
            }
            invitadas.add(empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe la empresa " + empresaId)));
        }
        return invitadas;
    }

    private boolean puedeConsultar(Proceso proceso, Long empresaId) {
        return proceso.getEmpresa().getId().equals(empresaId)
                || (proceso.isCompartido()
                        && procesoRepository.existsByIdAndEmpresasCompartidasId(proceso.getId(), empresaId));
    }

    /**
     * HU-03 y HU-23: el proceso pertenece a la empresa que lo creo. Una empresa
     * invitada lo ve, pero no lo modifica.
     */
    private void validarEsPropietario(Usuario usuario, Proceso proceso) {
        if (!usuario.getEmpresa().getId().equals(proceso.getEmpresa().getId())) {
            throw new PermisoDenegadoException(
                    "El proceso pertenece a otra empresa: el acceso compartido es de solo lectura");
        }
    }

    private Proceso obtenerActivo(Long id) {
        return procesoRepository.findByIdAndEstadoNot(id, EstadoProceso.INACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + id));
    }
}
