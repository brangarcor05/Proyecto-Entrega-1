package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.mensaje.ActualizarEventoMensajeRecepcionRequest;
import co.javeriana.dw.proyecto.dto.mensaje.CrearEventoMensajeRecepcionRequest;
import co.javeriana.dw.proyecto.dto.mensaje.EventoMensajeRecepcionResponse;
import co.javeriana.dw.proyecto.entidad.EventoMensajeRecepcion;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.EventoMensajeRecepcionRepository;
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
    private final ModelMapper modelMapper;

    public EventoMensajeRecepcionService(EventoMensajeRecepcionRepository eventoMensajeRecepcionRepository, ProcesoRepository procesoRepository, ModelMapper modelMapper) {
        this.eventoMensajeRecepcionRepository = eventoMensajeRecepcionRepository;
        this.procesoRepository = procesoRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public EventoMensajeRecepcionResponse crear(CrearEventoMensajeRecepcionRequest request) {
        Proceso proceso = procesoRepository.findById(request.getProcesoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + request.getProcesoId()));

        EventoMensajeRecepcion evento = modelMapper.map(request, EventoMensajeRecepcion.class);
        evento.setProceso(proceso);
        evento.setActivo(true);

        evento = eventoMensajeRecepcionRepository.save(evento);
        return mapearAResponse(evento);
    }

    public EventoMensajeRecepcionResponse obtenerPorId(Long id) {
        EventoMensajeRecepcion evento = eventoMensajeRecepcionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento de mensaje recepción no encontrado: " + id));
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
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento de mensaje recepción no encontrado: " + id));

        evento.setNombreMensaje(request.getNombreMensaje());
        evento.setVariante(request.getVariante());
        evento.setDatosEsperados(request.getDatosEsperados());
        evento.setOrigenExterno(request.isOrigenExterno());
        evento.setClaveCorrelacion(request.getClaveCorrelacion());

        if (request.getPosicionX() != null) evento.setPosicionX(request.getPosicionX());
        if (request.getPosicionY() != null) evento.setPosicionY(request.getPosicionY());

        evento = eventoMensajeRecepcionRepository.save(evento);
        return mapearAResponse(evento);
    }

    @Transactional
    public void eliminar(Long id) {
        EventoMensajeRecepcion evento = eventoMensajeRecepcionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento de mensaje recepción no encontrado: " + id));
        evento.setActivo(false);
        eventoMensajeRecepcionRepository.save(evento);
    }

    private EventoMensajeRecepcionResponse mapearAResponse(EventoMensajeRecepcion evento) {
        EventoMensajeRecepcionResponse response = modelMapper.map(evento, EventoMensajeRecepcionResponse.class);
        if (evento.getProceso() != null) {
            response.setProcesoId(evento.getProceso().getId());
        }
        return response;
    }
}