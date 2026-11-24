package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.mensaje.ActualizarEventoMensajeRecepcionRequest;
import co.javeriana.dw.proyecto.dto.mensaje.CrearEventoMensajeRecepcionRequest;
import co.javeriana.dw.proyecto.dto.mensaje.EventoMensajeRecepcionResponse;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.EventoMensajeRecepcion;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.EventoMensajeRecepcionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Message Catch (HU-27) con su clave de correlacion (HU-28).
 *
 * - Crear y editar: administrador o editor. Eliminar: solo administrador.
 * - Solo la empresa propietaria modifica el proceso; las invitadas solo consultan.
 * - Cada cambio queda en el historial.
 */
@Service
public class EventoMensajeRecepcionService {

    private static final String ENTIDAD = "MensajeRecepcion";

    private final EventoMensajeRecepcionRepository eventoMensajeRecepcionRepository;
    private final PermisoService permisoService;
    private final AccesoProcesoService accesoProcesoService;
    private final HistorialService historialService;
    private final ModelMapper modelMapper;

    public EventoMensajeRecepcionService(EventoMensajeRecepcionRepository eventoMensajeRecepcionRepository,
            PermisoService permisoService, AccesoProcesoService accesoProcesoService,
            HistorialService historialService, ModelMapper modelMapper) {
        this.eventoMensajeRecepcionRepository = eventoMensajeRecepcionRepository;
        this.permisoService = permisoService;
        this.accesoProcesoService = accesoProcesoService;
        this.historialService = historialService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public EventoMensajeRecepcionResponse crear(CrearEventoMensajeRecepcionRequest request, Long usuarioId) {
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        Proceso proceso = accesoProcesoService.obtenerProcesoActivo(request.getProcesoId());
        accesoProcesoService.validarEscritura(usuario, proceso);
        Pool pool = obtenerPoolReceptor(request.getPoolId(), proceso);

        EventoMensajeRecepcion evento = modelMapper.map(request, EventoMensajeRecepcion.class);
        evento.setId(null);
        evento.setProceso(proceso);
        evento.setPool(pool);
        evento.setCanvasId(request.getCanvasId());
        evento.setActivo(true);
        evento.setNombreMensaje(request.getNombreMensaje().trim());
        evento.setNombre(evento.getNombreMensaje());

        if (evento.getPosicionX() == null)
            evento.setPosicionX(0.0);
        if (evento.getPosicionY() == null)
            evento.setPosicionY(0.0);

        evento = eventoMensajeRecepcionRepository.save(evento);

        historialService.registrar(ENTIDAD, evento.getId(), AccionHistorial.CREACION, usuario, proceso,
                "Recepcion de mensaje creada: '" + evento.getNombreMensaje() + "' en " + pool.getNombre());
        return mapearAResponse(evento);
    }

    @Transactional(readOnly = true)
    public EventoMensajeRecepcionResponse obtenerPorId(Long id, Long empresaId) {
        EventoMensajeRecepcion evento = obtenerActivo(id);
        accesoProcesoService.validarLectura(evento.getProceso().getId(), empresaId);
        return mapearAResponse(evento);
    }

    @Transactional(readOnly = true)
    public List<EventoMensajeRecepcionResponse> listarPorProceso(Long procesoId, Long empresaId) {
        accesoProcesoService.validarLectura(procesoId, empresaId);
        return eventoMensajeRecepcionRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional
    public EventoMensajeRecepcionResponse actualizar(Long id, ActualizarEventoMensajeRecepcionRequest request,
            Long usuarioId) {
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        EventoMensajeRecepcion evento = obtenerActivo(id);
        Proceso proceso = evento.getProceso();
        accesoProcesoService.validarEscritura(usuario, proceso);

        if (request.getPoolId() != null) {
            evento.setPool(obtenerPoolReceptor(request.getPoolId(), proceso));
        }

        evento.setNombreMensaje(request.getNombreMensaje().trim());
        evento.setNombre(evento.getNombreMensaje());
        evento.setVariante(request.getVariante());
        evento.setDatosEsperados(request.getDatosEsperados());
        evento.setOrigenExterno(request.isOrigenExterno());
        evento.setClaveCorrelacion(request.getClaveCorrelacion());
        if (request.getCanvasId() != null)
            evento.setCanvasId(request.getCanvasId());
        if (request.getPosicionX() != null)
            evento.setPosicionX(request.getPosicionX());
        if (request.getPosicionY() != null)
            evento.setPosicionY(request.getPosicionY());

        evento = eventoMensajeRecepcionRepository.save(evento);

        historialService.registrar(ENTIDAD, evento.getId(), AccionHistorial.EDICION, usuario, proceso,
                "Recepcion de mensaje actualizada: " + evento.getNombreMensaje());
        return mapearAResponse(evento);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        EventoMensajeRecepcion evento = obtenerActivo(id);
        accesoProcesoService.validarEscritura(usuario, evento.getProceso());

        evento.setActivo(false);
        eventoMensajeRecepcionRepository.save(evento);

        historialService.registrar(ENTIDAD, evento.getId(), AccionHistorial.ELIMINACION, usuario,
                evento.getProceso(), "Recepcion de mensaje marcada como inactiva: " + evento.getNombreMensaje());
    }

    private EventoMensajeRecepcion obtenerActivo(Long id) {
        return eventoMensajeRecepcionRepository.findById(id)
                .filter(EventoMensajeRecepcion::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Evento de mensaje recepción no encontrado: " + id));
    }

    /** El catch vive dentro de un pool modelado: un pool caja negra no tiene elementos. */
    private Pool obtenerPoolReceptor(Long poolId, Proceso proceso) {
        Pool pool = accesoProcesoService.obtenerPoolDelProceso(poolId, proceso);
        if (pool.isCajaNegra()) {
            throw new ReglaNegocioException("Un pool caja negra no puede contener eventos de recepción");
        }
        return pool;
    }

    private EventoMensajeRecepcionResponse mapearAResponse(EventoMensajeRecepcion evento) {
        EventoMensajeRecepcionResponse response = modelMapper.map(evento, EventoMensajeRecepcionResponse.class);
        if (evento.getProceso() != null) {
            response.setProcesoId(evento.getProceso().getId());
        }
        if (evento.getPool() != null) {
            response.setPoolId(evento.getPool().getId());
        }
        return response;
    }
}
