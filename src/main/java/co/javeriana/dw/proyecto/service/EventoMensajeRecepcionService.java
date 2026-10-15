package co.javeriana.dw.proyecto.service;

import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.javeriana.dw.proyecto.dto.mensaje.ActualizarEventoMensajeRecepcionRequest;
import co.javeriana.dw.proyecto.dto.mensaje.CrearEventoMensajeRecepcionRequest;
import co.javeriana.dw.proyecto.dto.mensaje.EventoMensajeRecepcionResponse;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.EventoMensajeRecepcion;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.EventoMensajeRecepcionRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;

@Service
public class EventoMensajeRecepcionService {
    private final EventoMensajeRecepcionRepository repository;
    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final PermisoService permisoService;
    private final HistorialService historialService;
    private final ModelMapper modelMapper;

    public EventoMensajeRecepcionService(EventoMensajeRecepcionRepository repository,
            ProcesoRepository procesoRepository, PoolRepository poolRepository,
            PermisoService permisoService, HistorialService historialService, ModelMapper modelMapper) {
        this.repository = repository;
        this.procesoRepository = procesoRepository;
        this.poolRepository = poolRepository;
        this.permisoService = permisoService;
        this.historialService = historialService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public EventoMensajeRecepcionResponse crear(CrearEventoMensajeRecepcionRequest request, Long usuarioId) {
        Proceso proceso = obtenerProcesoActivo(request.getProcesoId());
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEmpresa(usuario, proceso);
        Pool pool = obtenerPoolDelProceso(request.getPoolId(), proceso);
        EventoMensajeRecepcion evento = new EventoMensajeRecepcion();
        evento.setNombre(request.getNombreMensaje());
        evento.setNombreMensaje(request.getNombreMensaje());
        evento.setVariante(request.getVariante());
        evento.setDatosEsperados(request.getDatosEsperados());
        evento.setOrigenExterno(request.isOrigenExterno());
        evento.setClaveCorrelacion(request.getClaveCorrelacion());
        evento.setPosicionX(request.getPosicionX());
        evento.setPosicionY(request.getPosicionY());
        evento.setProceso(proceso);
        evento.setPool(pool);
        evento.setActivo(true);
        evento = repository.save(evento);
        historialService.registrar("EventoMensajeRecepcion", evento.getId(), AccionHistorial.CREACION,
                usuario, proceso, "Mensaje de recepcion creado: " + evento.getNombreMensaje());
        return mapear(evento);
    }

    @Transactional(readOnly = true)
    public EventoMensajeRecepcionResponse obtenerPorId(Long id) { return mapear(obtenerActivo(id)); }

    @Transactional(readOnly = true)
    public List<EventoMensajeRecepcionResponse> listarPorProceso(Long procesoId) {
        obtenerProcesoActivo(procesoId);
        return repository.findByProcesoIdAndActivoTrue(procesoId).stream().map(this::mapear).toList();
    }

    @Transactional
    public EventoMensajeRecepcionResponse actualizar(Long id,
            ActualizarEventoMensajeRecepcionRequest request, Long usuarioId) {
        EventoMensajeRecepcion evento = obtenerActivo(id);
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEmpresa(usuario, evento.getProceso());
        evento.setNombre(request.getNombreMensaje());
        evento.setNombreMensaje(request.getNombreMensaje());
        evento.setVariante(request.getVariante());
        evento.setDatosEsperados(request.getDatosEsperados());
        evento.setOrigenExterno(request.isOrigenExterno());
        evento.setClaveCorrelacion(request.getClaveCorrelacion());
        evento.setPosicionX(request.getPosicionX());
        evento.setPosicionY(request.getPosicionY());
        evento = repository.save(evento);
        historialService.registrar("EventoMensajeRecepcion", evento.getId(), AccionHistorial.EDICION,
                usuario, evento.getProceso(), "Mensaje de recepcion actualizado: " + evento.getNombreMensaje());
        return mapear(evento);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        EventoMensajeRecepcion evento = obtenerActivo(id);
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        validarEmpresa(usuario, evento.getProceso());
        evento.setActivo(false);
        repository.save(evento);
        historialService.registrar("EventoMensajeRecepcion", evento.getId(), AccionHistorial.ELIMINACION,
                usuario, evento.getProceso(), "Mensaje de recepcion marcado como inactivo: " + evento.getNombreMensaje());
    }

    private Pool obtenerPoolDelProceso(Long id, Proceso proceso) {
        Pool pool = poolRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + id));
        if (!pool.getProceso().getId().equals(proceso.getId())) {
            throw new ReglaNegocioException("El pool pertenece a otro proceso");
        }
        if (pool.isCajaNegra()) {
            throw new ReglaNegocioException("Un pool caja negra no puede contener eventos");
        }
        return pool;
    }

    private void validarEmpresa(Usuario usuario, Proceso proceso) {
        if (!usuario.getEmpresa().getId().equals(proceso.getEmpresa().getId())) {
            throw new PermisoDenegadoException("El proceso pertenece a otra empresa");
        }
    }

    private EventoMensajeRecepcion obtenerActivo(Long id) {
        return repository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento de mensaje recepcion no encontrado: " + id));
    }

    private Proceso obtenerProcesoActivo(Long id) {
        return procesoRepository.findByIdAndEstadoNot(id, EstadoProceso.INACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + id));
    }

    private EventoMensajeRecepcionResponse mapear(EventoMensajeRecepcion evento) {
        EventoMensajeRecepcionResponse response = modelMapper.map(evento, EventoMensajeRecepcionResponse.class);
        response.setProcesoId(evento.getProceso().getId());
        response.setPoolId(evento.getPool().getId());
        return response;
    }
}
