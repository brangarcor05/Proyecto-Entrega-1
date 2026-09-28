package co.javeriana.dw.proyecto.service;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.arco.ActualizarArcoRequest;
import co.javeriana.dw.proyecto.dto.arco.ArcoEliminadoResponse;
import co.javeriana.dw.proyecto.dto.arco.ArcoResponse;
import co.javeriana.dw.proyecto.dto.arco.CrearArcoRequest;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.Arco;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Gateway;
import co.javeriana.dw.proyecto.entidad.NodoProceso;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.ArcoRepository;
import co.javeriana.dw.proyecto.repository.NodoProcesoRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;

/**
 * Arcos del diagrama (HU-11 a HU-13). Un arco es el flujo de secuencia de BPMN:
 * la linea continua que fija el orden en que ocurren las cosas dentro de un pool.
 * La comunicacion entre pools no se modela asi, sino con mensajes (HU-25).
 */
@Service
public class ArcoService {

    private final ArcoRepository arcoRepository;
    private final NodoProcesoRepository nodoProcesoRepository;
    private final ProcesoRepository procesoRepository;
    private final PermisoService permisoService;
    private final HistorialService historialService;
    private final ModelMapper modelMapper;

    public ArcoService(ArcoRepository arcoRepository, NodoProcesoRepository nodoProcesoRepository,
                       ProcesoRepository procesoRepository, PermisoService permisoService,
                       HistorialService historialService,
                          ModelMapper modelMapper) {
        this.arcoRepository = arcoRepository;
        this.nodoProcesoRepository = nodoProcesoRepository;
        this.procesoRepository = procesoRepository;
        this.permisoService = permisoService;
        this.historialService = historialService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public ArcoResponse crear(CrearArcoRequest request, Long usuarioId) {
        Proceso proceso = obtenerProcesoActivo(request.procesoId());
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEsDeLaEmpresa(usuario, proceso);

        NodoProceso origen = obtenerNodo(request.origenId(), proceso);
        NodoProceso destino = obtenerNodo(request.destinoId(), proceso);
        validarConexion(origen, destino);
        validarCondicion(origen, request.condicion());

        if (arcoRepository.existsByOrigenIdAndDestinoIdAndActivoTrue(origen.getId(), destino.getId())) {
            throw new ReglaNegocioException(
                    "Ya existe un arco entre esos dos elementos: no se permiten arcos duplicados");
        }

        // La restriccion unica de la tabla no distingue arcos inactivos, asi que un
        // arco borrado que se vuelve a crear se reactiva en vez de insertarse de nuevo.
        Arco arco = arcoRepository.findByOrigenIdAndDestinoId(origen.getId(), destino.getId())
                .orElseGet(Arco::new);
        arco.setProceso(proceso);
        arco.setOrigen(origen);
        arco.setDestino(destino);
        arco.setEtiqueta(request.etiqueta());
        arco.setCondicion(request.condicion());
        arco.setActivo(true);
        arco = arcoRepository.save(arco);

        historialService.registrar("Arco", arco.getId(), AccionHistorial.CREACION, usuario, proceso,
                "Arco creado: " + origen.getNombre() + " -> " + destino.getNombre());
        return modelMapper.map(arco, ArcoResponse.class);
    }

    /** HU-12: se puede reconectar el arco, y se le aplican las validaciones de la creacion. */
    @Transactional
    public ArcoResponse actualizar(Long arcoId, ActualizarArcoRequest request, Long usuarioId) {
        Arco arco = obtenerActivo(arcoId);
        Proceso proceso = arco.getProceso();
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEsDeLaEmpresa(usuario, proceso);

        NodoProceso origen = obtenerNodo(request.origenId(), proceso);
        NodoProceso destino = obtenerNodo(request.destinoId(), proceso);
        validarConexion(origen, destino);
        validarCondicion(origen, request.condicion());

        boolean cambianLosExtremos = !origen.getId().equals(arco.getOrigen().getId())
                || !destino.getId().equals(arco.getDestino().getId());
        if (cambianLosExtremos
                && arcoRepository.existsByOrigenIdAndDestinoIdAndActivoTrue(origen.getId(), destino.getId())) {
            throw new ReglaNegocioException(
                    "Ya existe un arco entre esos dos elementos: no se permiten arcos duplicados");
        }

        arco.setOrigen(origen);
        arco.setDestino(destino);
        arco.setEtiqueta(request.etiqueta());
        arco.setCondicion(request.condicion());
        arco = arcoRepository.save(arco);

        historialService.registrar("Arco", arco.getId(), AccionHistorial.EDICION, usuario, proceso,
                "Arco actualizado: " + origen.getNombre() + " -> " + destino.getNombre());
        return modelMapper.map(arco, ArcoResponse.class);
    }

    @Transactional(readOnly = true)
    public ArcoResponse obtener(Long arcoId) {
        return modelMapper.map(obtenerActivo(arcoId), ArcoResponse.class);
    }

    @Transactional(readOnly = true)
    public List<ArcoResponse> listarPorProceso(Long procesoId) {
        obtenerProcesoActivo(procesoId);
        return arcoRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(arco -> modelMapper.map(arco, ArcoResponse.class))
                .toList();
    }

    /**
     * HU-13. La eliminacion es logica y solo la hace el administrador. No se bloquea
     * cuando deja elementos sueltos, porque un diagrama en borrador puede estar
     * incompleto, pero se devuelven las advertencias para que el editor las muestre.
     */
    @Transactional
    public ArcoEliminadoResponse eliminar(Long arcoId, Long usuarioId) {
        Arco arco = obtenerActivo(arcoId);
        Proceso proceso = arco.getProceso();
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        validarEsDeLaEmpresa(usuario, proceso);

        NodoProceso origen = arco.getOrigen();
        NodoProceso destino = arco.getDestino();

        arco.setActivo(false);
        arcoRepository.save(arco);
        arcoRepository.flush();

        List<String> advertencias = new ArrayList<>();
        if (!arcoRepository.existsByOrigenIdAndActivoTrue(origen.getId())) {
            advertencias.add("'" + origen.getNombre() + "' queda sin camino de salida");
        }
        if (!arcoRepository.existsByDestinoIdAndActivoTrue(destino.getId())) {
            advertencias.add("'" + destino.getNombre() + "' queda sin camino de entrada: "
                    + "deja de ser alcanzable en el proceso");
        }

        historialService.registrar("Arco", arco.getId(), AccionHistorial.ELIMINACION, usuario, proceso,
                "Arco eliminado: " + origen.getNombre() + " -> " + destino.getNombre());
        return new ArcoEliminadoResponse(arco.getId(), advertencias);
    }

    /** Las tres reglas de HU-11 sobre que conexiones son validas. */
    private void validarConexion(NodoProceso origen, NodoProceso destino) {
        if (origen.getId().equals(destino.getId())) {
            throw new ReglaNegocioException(
                    "Un arco no puede tener el mismo elemento como origen y destino");
        }
        if (!origen.getPool().getId().equals(destino.getPool().getId())) {
            throw new ReglaNegocioException(
                    "Un arco no puede cruzar de un pool a otro: esa comunicacion se modela como mensaje");
        }
    }

    /**
     * La condicion es el criterio por el que el flujo toma un camino y no otro, asi
     * que solo tiene sentido en un arco que sale de un gateway (HU-12, HU-14).
     */
    private void validarCondicion(NodoProceso origen, String condicion) {
        boolean tieneCondicion = condicion != null && !condicion.isBlank();
        if (tieneCondicion && !(origen instanceof Gateway)) {
            throw new ReglaNegocioException(
                    "Solo los arcos que salen de un gateway llevan condicion");
        }
    }

    /** HU-03: el diagrama de un proceso solo lo modifica la empresa que lo creo. */
    private void validarEsDeLaEmpresa(Usuario usuario, Proceso proceso) {
        if (!usuario.getEmpresa().getId().equals(proceso.getEmpresa().getId())) {
            throw new PermisoDenegadoException(
                    "El proceso pertenece a otra empresa: el acceso compartido es de solo lectura");
        }
    }

    private NodoProceso obtenerNodo(Long nodoId, Proceso proceso) {
        NodoProceso nodo = nodoProcesoRepository.findByIdAndActivoTrue(nodoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Elemento no encontrado: " + nodoId));
        if (!nodo.getProceso().getId().equals(proceso.getId())) {
            throw new ReglaNegocioException(
                    "El elemento " + nodo.getNombre() + " pertenece a otro proceso");
        }
        return nodo;
    }

    private Arco obtenerActivo(Long id) {
        return arcoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Arco no encontrado: " + id));
    }

    private Proceso obtenerProcesoActivo(Long procesoId) {
        return procesoRepository.findByIdAndEstadoNot(procesoId, EstadoProceso.INACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + procesoId));
    }
}
