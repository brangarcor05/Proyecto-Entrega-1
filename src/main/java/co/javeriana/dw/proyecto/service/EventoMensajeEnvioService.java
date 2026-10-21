package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.mensaje.ActualizarEventoMensajeEnvioRequest;
import co.javeriana.dw.proyecto.dto.mensaje.CrearEventoMensajeEnvioRequest;
import co.javeriana.dw.proyecto.dto.mensaje.EventoMensajeEnvioResponse;
import co.javeriana.dw.proyecto.entidad.EventoMensajeEnvio;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.EventoMensajeEnvioRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventoMensajeEnvioService {

    private final EventoMensajeEnvioRepository eventoMensajeEnvioRepository;
    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final ModelMapper modelMapper;

    public EventoMensajeEnvioService(EventoMensajeEnvioRepository eventoMensajeEnvioRepository, ProcesoRepository procesoRepository, PoolRepository poolRepository, ModelMapper modelMapper) {
        this.eventoMensajeEnvioRepository = eventoMensajeEnvioRepository;
        this.procesoRepository = procesoRepository;
        this.poolRepository = poolRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public EventoMensajeEnvioResponse crear(CrearEventoMensajeEnvioRequest request) {
        Proceso proceso = procesoRepository.findById(request.getProcesoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + request.getProcesoId()));

        Pool poolDestino = poolRepository.findById(request.getPoolDestinoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool de destino no encontrado: " + request.getPoolDestinoId()));

        EventoMensajeEnvio evento = modelMapper.map(request, EventoMensajeEnvio.class);
        evento.setProceso(proceso);
        evento.setPoolDestino(poolDestino);
        evento.setActivo(true);

        evento = eventoMensajeEnvioRepository.save(evento);
        return mapearAResponse(evento);
    }

    public EventoMensajeEnvioResponse obtenerPorId(Long id) {
        EventoMensajeEnvio evento = eventoMensajeEnvioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento de mensaje envío no encontrado: " + id));
        return mapearAResponse(evento);
    }

    public List<EventoMensajeEnvioResponse> listarPorProceso(Long procesoId) {
        return eventoMensajeEnvioRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventoMensajeEnvioResponse actualizar(Long id, ActualizarEventoMensajeEnvioRequest request) {
        EventoMensajeEnvio evento = eventoMensajeEnvioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento de mensaje envío no encontrado: " + id));

        Pool poolDestino = poolRepository.findById(request.getPoolDestinoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool de destino no encontrado: " + request.getPoolDestinoId()));

        evento.setNombreMensaje(request.getNombreMensaje());
        evento.setClaveCorrelacion(request.getClaveCorrelacion());
        evento.setPoolDestino(poolDestino);

        if (request.getPosicionX() != null) evento.setPosicionX(request.getPosicionX());
        if (request.getPosicionY() != null) evento.setPosicionY(request.getPosicionY());

        evento = eventoMensajeEnvioRepository.save(evento);
        return mapearAResponse(evento);
    }

    @Transactional
    public void eliminar(Long id) {
        EventoMensajeEnvio evento = eventoMensajeEnvioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento de mensaje envío no encontrado: " + id));
        evento.setActivo(false);
        eventoMensajeEnvioRepository.save(evento);
    }

    private EventoMensajeEnvioResponse mapearAResponse(EventoMensajeEnvio evento) {
        EventoMensajeEnvioResponse response = modelMapper.map(evento, EventoMensajeEnvioResponse.class);
        if (evento.getProceso() != null) {
            response.setProcesoId(evento.getProceso().getId());
        }
        if (evento.getPoolDestino() != null) {
            response.setPoolDestinoId(evento.getPoolDestino().getId());
            response.setNombrePoolDestino(evento.getPoolDestino().getNombre());
        }
        return response;
    }
}