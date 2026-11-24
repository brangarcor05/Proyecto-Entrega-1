package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.mensaje.ActualizarEventoMensajeEnvioRequest;
import co.javeriana.dw.proyecto.dto.mensaje.CrearEventoMensajeEnvioRequest;
import co.javeriana.dw.proyecto.dto.mensaje.EventoMensajeEnvioResponse;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.EventoMensajeEnvio;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.EventoMensajeEnvioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Message Throw (HU-25) con su clave de correlacion (HU-28).
 *
 * - Crear y editar: administrador o editor. Eliminar: solo administrador.
 * - Solo la empresa propietaria modifica el proceso; las invitadas solo consultan.
 * - El mensaje es comunicacion ENTRE pools: el pool destino debe ser otro pool del
 *   mismo proceso (HU-25, "rechaza los que quedan dentro del mismo pool").
 * - Cada cambio queda en el historial.
 */
@Service
public class EventoMensajeEnvioService {

    private static final String ENTIDAD = "MensajeEnvio";

    private final EventoMensajeEnvioRepository eventoMensajeEnvioRepository;
    private final PermisoService permisoService;
    private final AccesoProcesoService accesoProcesoService;
    private final HistorialService historialService;
    private final ModelMapper modelMapper;

    public EventoMensajeEnvioService(EventoMensajeEnvioRepository eventoMensajeEnvioRepository,
            PermisoService permisoService, AccesoProcesoService accesoProcesoService,
            HistorialService historialService, ModelMapper modelMapper) {
        this.eventoMensajeEnvioRepository = eventoMensajeEnvioRepository;
        this.permisoService = permisoService;
        this.accesoProcesoService = accesoProcesoService;
        this.historialService = historialService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public EventoMensajeEnvioResponse crear(CrearEventoMensajeEnvioRequest request, Long usuarioId) {
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        Proceso proceso = accesoProcesoService.obtenerProcesoActivo(request.getProcesoId());
        accesoProcesoService.validarEscritura(usuario, proceso);

        Pool pool = obtenerPoolOrigen(request.getPoolId(), proceso);
        Pool poolDestino = obtenerPoolDestino(request.getPoolDestinoId(), pool, proceso);

        EventoMensajeEnvio evento = modelMapper.map(request, EventoMensajeEnvio.class);
        evento.setId(null);
        evento.setProceso(proceso);
        evento.setPool(pool);
        evento.setPoolDestino(poolDestino);
        evento.setCanvasId(request.getCanvasId());
        evento.setActivo(true);
        evento.setNombreMensaje(request.getNombreMensaje().trim());
        evento.setNombre(evento.getNombreMensaje());

        if (evento.getPosicionX() == null)
            evento.setPosicionX(0.0);
        if (evento.getPosicionY() == null)
            evento.setPosicionY(0.0);

        evento = eventoMensajeEnvioRepository.save(evento);

        historialService.registrar(ENTIDAD, evento.getId(), AccionHistorial.CREACION, usuario, proceso,
                "Mensaje '" + evento.getNombreMensaje() + "' de " + pool.getNombre() + " a " + poolDestino.getNombre());
        return mapearAResponse(evento);
    }

    @Transactional(readOnly = true)
    public EventoMensajeEnvioResponse obtenerPorId(Long id, Long empresaId) {
        EventoMensajeEnvio evento = obtenerActivo(id);
        accesoProcesoService.validarLectura(evento.getProceso().getId(), empresaId);
        return mapearAResponse(evento);
    }

    @Transactional(readOnly = true)
    public List<EventoMensajeEnvioResponse> listarPorProceso(Long procesoId, Long empresaId) {
        accesoProcesoService.validarLectura(procesoId, empresaId);
        return eventoMensajeEnvioRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional
    public EventoMensajeEnvioResponse actualizar(Long id, ActualizarEventoMensajeEnvioRequest request,
            Long usuarioId) {
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        EventoMensajeEnvio evento = obtenerActivo(id);
        Proceso proceso = evento.getProceso();
        accesoProcesoService.validarEscritura(usuario, proceso);

        Pool pool = request.getPoolId() != null
                ? obtenerPoolOrigen(request.getPoolId(), proceso)
                : evento.getPool();
        Pool poolDestino = obtenerPoolDestino(request.getPoolDestinoId(), pool, proceso);

        evento.setPool(pool);
        evento.setPoolDestino(poolDestino);
        evento.setNombreMensaje(request.getNombreMensaje().trim());
        evento.setNombre(evento.getNombreMensaje());
        evento.setClaveCorrelacion(request.getClaveCorrelacion());
        if (request.getCanvasId() != null)
            evento.setCanvasId(request.getCanvasId());
        if (request.getPosicionX() != null)
            evento.setPosicionX(request.getPosicionX());
        if (request.getPosicionY() != null)
            evento.setPosicionY(request.getPosicionY());

        evento = eventoMensajeEnvioRepository.save(evento);

        historialService.registrar(ENTIDAD, evento.getId(), AccionHistorial.EDICION, usuario, proceso,
                "Mensaje actualizado: " + evento.getNombreMensaje());
        return mapearAResponse(evento);
    }

    /** HU-25: se elimina como cualquier otro nodo (logicamente, solo el administrador). */
    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        EventoMensajeEnvio evento = obtenerActivo(id);
        accesoProcesoService.validarEscritura(usuario, evento.getProceso());

        evento.setActivo(false);
        eventoMensajeEnvioRepository.save(evento);

        historialService.registrar(ENTIDAD, evento.getId(), AccionHistorial.ELIMINACION, usuario,
                evento.getProceso(), "Mensaje marcado como inactivo: " + evento.getNombreMensaje());
    }

    private EventoMensajeEnvio obtenerActivo(Long id) {
        return eventoMensajeEnvioRepository.findById(id)
                .filter(EventoMensajeEnvio::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento de mensaje envío no encontrado: " + id));
    }

    /** El throw vive dentro de un pool modelado: un pool caja negra no tiene elementos. */
    private Pool obtenerPoolOrigen(Long poolId, Proceso proceso) {
        Pool pool = accesoProcesoService.obtenerPoolDelProceso(poolId, proceso);
        if (pool.isCajaNegra()) {
            throw new ReglaNegocioException("Un pool caja negra no puede enviar mensajes desde adentro");
        }
        return pool;
    }

    /** HU-25: el destino es otro pool del mismo proceso (puede ser caja negra). */
    private Pool obtenerPoolDestino(Long poolDestinoId, Pool origen, Proceso proceso) {
        Pool destino = accesoProcesoService.obtenerPoolDelProceso(poolDestinoId, proceso);
        if (destino.getId().equals(origen.getId())) {
            throw new ReglaNegocioException(
                    "Un mensaje debe cruzar de un pool a otro: dentro del mismo pool se usa un arco");
        }
        return destino;
    }

    private EventoMensajeEnvioResponse mapearAResponse(EventoMensajeEnvio evento) {
        EventoMensajeEnvioResponse response = modelMapper.map(evento, EventoMensajeEnvioResponse.class);
        if (evento.getProceso() != null) {
            response.setProcesoId(evento.getProceso().getId());
        }
        if (evento.getPool() != null) {
            response.setPoolId(evento.getPool().getId());
        }
        if (evento.getPoolDestino() != null) {
            response.setPoolDestinoId(evento.getPoolDestino().getId());
            response.setNombrePoolDestino(evento.getPoolDestino().getNombre());
        }
        return response;
    }
}
