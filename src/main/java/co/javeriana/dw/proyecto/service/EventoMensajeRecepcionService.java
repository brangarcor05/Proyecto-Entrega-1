package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.mensaje.ActualizarEventoMensajeRecepcionRequest;
import co.javeriana.dw.proyecto.dto.mensaje.CrearEventoMensajeRecepcionRequest;
import co.javeriana.dw.proyecto.dto.mensaje.EventoMensajeRecepcionResponse;
import co.javeriana.dw.proyecto.entidad.EventoMensajeRecepcion;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.EventoMensajeRecepcionRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventoMensajeRecepcionService {

    private final EventoMensajeRecepcionRepository eventoMensajeRecepcionRepository;
    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final ModelMapper modelMapper;

    public EventoMensajeRecepcionService(EventoMensajeRecepcionRepository eventoMensajeRecepcionRepository,
            ProcesoRepository procesoRepository, PoolRepository poolRepository, ModelMapper modelMapper) {
        this.eventoMensajeRecepcionRepository = eventoMensajeRecepcionRepository;
        this.procesoRepository = procesoRepository;
        this.poolRepository = poolRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public EventoMensajeRecepcionResponse crear(CrearEventoMensajeRecepcionRequest request) {
        Proceso proceso = procesoRepository.findById(request.getProcesoId())
                .orElseThrow(
                        () -> new RecursoNoEncontradoException("Proceso no encontrado: " + request.getProcesoId()));

        Pool pool = poolRepository.findById(request.getPoolId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + request.getPoolId()));

        EventoMensajeRecepcion evento = modelMapper.map(request, EventoMensajeRecepcion.class);
        evento.setProceso(proceso);
        evento.setPool(pool); // 👈 AGREGADO
        evento.setCanvasId(request.getCanvasId());
        evento.setActivo(true);

        // 👇 ESTO ARREGLA EL ERROR DEL NOMBRE NULL 👇
        evento.setNombre(request.getNombreMensaje());

        if (evento.getPosicionX() == null)
            evento.setPosicionX(0.0);
        if (evento.getPosicionY() == null)
            evento.setPosicionY(0.0);

        evento = eventoMensajeRecepcionRepository.save(evento);
        return mapearAResponse(evento);
    }

    public EventoMensajeRecepcionResponse obtenerPorId(Long id) {
        EventoMensajeRecepcion evento = eventoMensajeRecepcionRepository.findById(id)
                .orElseThrow(
                        () -> new RecursoNoEncontradoException("Evento de mensaje recepción no encontrado: " + id));
        return mapearAResponse(evento);
    }

    public List<EventoMensajeRecepcionResponse> listarPorProceso(Long procesoId) {
        return eventoMensajeRecepcionRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventoMensajeRecepcionResponse actualizar(Long id, ActualizarEventoMensajeRecepcionRequest request) {
        EventoMensajeRecepcion evento = eventoMensajeRecepcionRepository.findById(id)
                .orElseThrow(
                        () -> new RecursoNoEncontradoException("Evento de mensaje recepción no encontrado: " + id));

        if (request.getPoolId() != null) {
            Pool pool = poolRepository.findById(request.getPoolId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + request.getPoolId()));
            evento.setPool(pool);
        }

        evento.setNombre(request.getNombreMensaje());
        evento.setNombreMensaje(request.getNombreMensaje());
        evento.setVariante(request.getVariante());
        evento.setDatosEsperados(request.getDatosEsperados());
        evento.setOrigenExterno(request.isOrigenExterno());
        evento.setClaveCorrelacion(request.getClaveCorrelacion());
        evento.setCanvasId(request.getCanvasId());

        if (request.getPosicionX() != null)
            evento.setPosicionX(request.getPosicionX());
        if (request.getPosicionY() != null)
            evento.setPosicionY(request.getPosicionY());

        evento = eventoMensajeRecepcionRepository.save(evento);
        return mapearAResponse(evento);
    }

    @Transactional
    public void eliminar(Long id) {
        EventoMensajeRecepcion evento = eventoMensajeRecepcionRepository.findById(id)
                .orElseThrow(
                        () -> new RecursoNoEncontradoException("Evento de mensaje recepción no encontrado: " + id));
        evento.setActivo(false);
        eventoMensajeRecepcionRepository.save(evento);
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