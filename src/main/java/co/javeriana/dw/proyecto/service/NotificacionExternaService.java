package co.javeriana.dw.proyecto.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.comun.CampoDatoDto;
import co.javeriana.dw.proyecto.dto.notificacionexterna.ActualizarNotificacionExternaRequest;
import co.javeriana.dw.proyecto.dto.notificacionexterna.CrearNotificacionExternaRequest;
import co.javeriana.dw.proyecto.dto.notificacionexterna.NotificacionExternaResponse;
import co.javeriana.dw.proyecto.entidad.AccionFalloNotificacion;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.Actividad;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.EventoNotificacionExterna;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.ActividadRepository;
import co.javeriana.dw.proyecto.repository.EventoNotificacionExternaRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;

/**
 * Notificaciones hacia sistemas externos (correo, servicio web o cola). Se documenta
 * el envio y su punto en el flujo: el sistema no se conecta a ningun servicio real.
 */
@Service
public class NotificacionExternaService {

    private final EventoNotificacionExternaRepository notificacionRepository;
    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final ActividadRepository actividadRepository;
    private final PermisoService permisoService;
    private final HistorialService historialService;

    public NotificacionExternaService(EventoNotificacionExternaRepository notificacionRepository,
                                      ProcesoRepository procesoRepository, PoolRepository poolRepository,
                                      ActividadRepository actividadRepository,
                                      PermisoService permisoService, HistorialService historialService) {
        this.notificacionRepository = notificacionRepository;
        this.procesoRepository = procesoRepository;
        this.poolRepository = poolRepository;
        this.actividadRepository = actividadRepository;
        this.permisoService = permisoService;
        this.historialService = historialService;
    }

    @Transactional
    public NotificacionExternaResponse crear(CrearNotificacionExternaRequest request, Long usuarioId) {
        Proceso proceso = obtenerProcesoActivo(request.procesoId());
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEsDeLaEmpresa(usuario, proceso);

        EventoNotificacionExterna notificacion = new EventoNotificacionExterna();
        notificacion.setProceso(proceso);
        notificacion.setPool(obtenerPoolContenedor(request.poolId(), proceso));
        notificacion.setActivo(true);
        aplicar(notificacion, proceso, request.nombre(), request.poolDestinoId(), request.tipoDestino(),
                request.momentoProceso(), request.accionSiFalla(), request.actividadManejoErrorId(),
                request.campos(), request.posicionX(), request.posicionY());
        notificacion = notificacionRepository.save(notificacion);

        historialService.registrar("NotificacionExterna", notificacion.getId(), AccionHistorial.CREACION,
                usuario, proceso, "Notificacion externa creada: " + notificacion.getNombre());
        return NotificacionExternaResponse.desde(notificacion);
    }

    @Transactional
    public NotificacionExternaResponse actualizar(Long notificacionId,
                                                  ActualizarNotificacionExternaRequest request,
                                                  Long usuarioId) {
        EventoNotificacionExterna notificacion = obtenerActiva(notificacionId);
        Proceso proceso = notificacion.getProceso();
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEsDeLaEmpresa(usuario, proceso);

        aplicar(notificacion, proceso, request.nombre(), request.poolDestinoId(), request.tipoDestino(),
                request.momentoProceso(), request.accionSiFalla(), request.actividadManejoErrorId(),
                request.campos(), request.posicionX(), request.posicionY());
        notificacion = notificacionRepository.save(notificacion);

        historialService.registrar("NotificacionExterna", notificacion.getId(), AccionHistorial.EDICION,
                usuario, proceso, "Notificacion externa actualizada: " + notificacion.getNombre());
        return NotificacionExternaResponse.desde(notificacion);
    }

    @Transactional(readOnly = true)
    public NotificacionExternaResponse obtener(Long notificacionId) {
        return NotificacionExternaResponse.desde(obtenerActiva(notificacionId));
    }

    @Transactional(readOnly = true)
    public List<NotificacionExternaResponse> listarPorProceso(Long procesoId) {
        obtenerProcesoActivo(procesoId);
        return notificacionRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(NotificacionExternaResponse::desde)
                .toList();
    }

    /** Eliminacion logica: queda inactiva y no se pierde el historico. */
    @Transactional
    public void eliminar(Long notificacionId, Long usuarioId) {
        EventoNotificacionExterna notificacion = obtenerActiva(notificacionId);
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        validarEsDeLaEmpresa(usuario, notificacion.getProceso());

        notificacion.setActivo(false);
        notificacionRepository.save(notificacion);

        historialService.registrar("NotificacionExterna", notificacion.getId(),
                AccionHistorial.ELIMINACION, usuario, notificacion.getProceso(),
                "Notificacion externa marcada como inactiva: " + notificacion.getNombre());
    }

    /** Campos comunes a crear y actualizar. */
    private void aplicar(EventoNotificacionExterna notificacion, Proceso proceso, String nombre,
                         Long poolDestinoId, co.javeriana.dw.proyecto.entidad.TipoDestinoExterno tipoDestino,
                         String momentoProceso, AccionFalloNotificacion accionSiFalla,
                         Long actividadManejoErrorId, List<CampoDatoDto> campos,
                         Double posicionX, Double posicionY) {

        notificacion.setNombre(nombre);
        notificacion.setPoolDestino(obtenerPoolDestino(poolDestinoId, proceso));
        notificacion.setTipoDestino(tipoDestino);
        notificacion.setMomentoProceso(momentoProceso);
        notificacion.setAccionSiFalla(accionSiFalla);
        notificacion.setActividadManejoError(obtenerActividadError(actividadManejoErrorId, accionSiFalla, proceso));
        notificacion.setPosicionX(posicionX);
        notificacion.setPosicionY(posicionY);

        notificacion.getCampos().clear();
        if (campos != null) {
            campos.stream().map(CampoDatoDto::aEntidad).forEach(notificacion.getCampos()::add);
        }
    }

    /** El pool donde se dibuja la notificacion: uno propio del proceso, nunca caja negra. */
    private Pool obtenerPoolContenedor(Long poolId, Proceso proceso) {
        Pool pool = obtenerPoolDelProceso(poolId, proceso);
        if (pool.isCajaNegra()) {
            throw new ReglaNegocioException(
                    "Un pool caja negra no puede contener elementos del diagrama");
        }
        return pool;
    }

    /** El sistema externo que recibe la notificacion se modela como pool caja negra. */
    private Pool obtenerPoolDestino(Long poolId, Proceso proceso) {
        Pool pool = obtenerPoolDelProceso(poolId, proceso);
        if (!pool.isCajaNegra()) {
            throw new ReglaNegocioException(
                    "El destino de la notificacion debe ser un pool caja negra");
        }
        return pool;
    }

    private Pool obtenerPoolDelProceso(Long poolId, Proceso proceso) {
        Pool pool = poolRepository.findByIdAndActivoTrue(poolId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + poolId));
        if (!pool.getProceso().getId().equals(proceso.getId())) {
            throw new ReglaNegocioException("El pool pertenece a otro proceso");
        }
        return pool;
    }

    /** La actividad de manejo de error solo tiene sentido si la accion es DERIVAR_ERROR. */
    private Actividad obtenerActividadError(Long actividadId, AccionFalloNotificacion accionSiFalla,
                                            Proceso proceso) {
        if (accionSiFalla != AccionFalloNotificacion.DERIVAR_ERROR) {
            return null;
        }
        if (actividadId == null) {
            throw new ReglaNegocioException(
                    "Con la accion DERIVAR_ERROR debe indicarse la actividad que maneja el error");
        }
        Actividad actividad = actividadRepository.findById(actividadId)
                .filter(Actividad::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada: " + actividadId));
        if (!actividad.getProceso().getId().equals(proceso.getId())) {
            throw new ReglaNegocioException("La actividad pertenece a otro proceso");
        }
        return actividad;
    }

    /** HU-03: el diagrama de un proceso solo lo modifica la empresa que lo creo. */
    private void validarEsDeLaEmpresa(Usuario usuario, Proceso proceso) {
        if (!usuario.getEmpresa().getId().equals(proceso.getEmpresa().getId())) {
            throw new PermisoDenegadoException(
                    "El proceso pertenece a otra empresa: el acceso compartido es de solo lectura");
        }
    }

    private EventoNotificacionExterna obtenerActiva(Long id) {
        return notificacionRepository.findById(id)
                .filter(EventoNotificacionExterna::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Notificacion externa no encontrada: " + id));
    }

    private Proceso obtenerProcesoActivo(Long procesoId) {
        return procesoRepository.findByIdAndEstadoNot(procesoId, EstadoProceso.INACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + procesoId));
    }
}