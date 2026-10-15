package co.javeriana.dw.proyecto.service;

import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.javeriana.dw.proyecto.dto.mensaje.ActualizarEventoMensajeEnvioRequest;
import co.javeriana.dw.proyecto.dto.mensaje.CrearEventoMensajeEnvioRequest;
import co.javeriana.dw.proyecto.dto.mensaje.EventoMensajeEnvioResponse;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.EventoMensajeEnvio;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.EventoMensajeEnvioRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;

@Service
public class EventoMensajeEnvioService {
    private final EventoMensajeEnvioRepository repository;
    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final PermisoService permisoService;
    private final HistorialService historialService;
    private final ModelMapper modelMapper;

    public EventoMensajeEnvioService(EventoMensajeEnvioRepository repository,
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
    public EventoMensajeEnvioResponse crear(CrearEventoMensajeEnvioRequest request, Long usuarioId) {
        Proceso proceso = obtenerProcesoActivo(request.getProcesoId());
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEmpresa(usuario, proceso);
        Pool origen = obtenerPool(request.getPoolId(), proceso, true);
        Pool destino = obtenerPool(request.getPoolDestinoId(), proceso, false);
        validarPoolsDiferentes(origen, destino);
        EventoMensajeEnvio evento = new EventoMensajeEnvio();
        evento.setNombre(request.getNombreMensaje());
        evento.setNombreMensaje(request.getNombreMensaje());
        evento.setClaveCorrelacion(request.getClaveCorrelacion());
        evento.setPosicionX(request.getPosicionX());
        evento.setPosicionY(request.getPosicionY());
        evento.setProceso(proceso);
        evento.setPool(origen);
        evento.setPoolDestino(destino);
        evento.setActivo(true);
        evento = repository.save(evento);
        historialService.registrar("EventoMensajeEnvio", evento.getId(), AccionHistorial.CREACION,
                usuario, proceso, "Mensaje de envio creado: " + evento.getNombreMensaje());
        return mapear(evento);
    }

    @Transactional(readOnly = true)
    public EventoMensajeEnvioResponse obtenerPorId(Long id) { return mapear(obtenerActivo(id)); }

    @Transactional(readOnly = true)
    public List<EventoMensajeEnvioResponse> listarPorProceso(Long procesoId) {
        obtenerProcesoActivo(procesoId);
        return repository.findByProcesoIdAndActivoTrue(procesoId).stream().map(this::mapear).toList();
    }

    @Transactional
    public EventoMensajeEnvioResponse actualizar(Long id, ActualizarEventoMensajeEnvioRequest request,
            Long usuarioId) {
        EventoMensajeEnvio evento = obtenerActivo(id);
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEmpresa(usuario, evento.getProceso());
        Pool destino = obtenerPool(request.getPoolDestinoId(), evento.getProceso(), false);
        validarPoolsDiferentes(evento.getPool(), destino);
        evento.setNombre(request.getNombreMensaje());
        evento.setNombreMensaje(request.getNombreMensaje());
        evento.setClaveCorrelacion(request.getClaveCorrelacion());
        evento.setPoolDestino(destino);
        evento.setPosicionX(request.getPosicionX());
        evento.setPosicionY(request.getPosicionY());
        evento = repository.save(evento);
        historialService.registrar("EventoMensajeEnvio", evento.getId(), AccionHistorial.EDICION,
                usuario, evento.getProceso(), "Mensaje de envio actualizado: " + evento.getNombreMensaje());
        return mapear(evento);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        EventoMensajeEnvio evento = obtenerActivo(id);
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        validarEmpresa(usuario, evento.getProceso());
        evento.setActivo(false);
        repository.save(evento);
        historialService.registrar("EventoMensajeEnvio", evento.getId(), AccionHistorial.ELIMINACION,
                usuario, evento.getProceso(), "Mensaje de envio marcado como inactivo: " + evento.getNombreMensaje());
    }

    private Pool obtenerPool(Long id, Proceso proceso, boolean debePermitirContenido) {
        Pool pool = poolRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + id));
        if (!pool.getProceso().getId().equals(proceso.getId())) {
            throw new ReglaNegocioException("El pool pertenece a otro proceso");
        }
        if (debePermitirContenido && pool.isCajaNegra()) {
            throw new ReglaNegocioException("Un pool caja negra no puede contener eventos");
        }
        return pool;
    }

    private void validarPoolsDiferentes(Pool origen, Pool destino) {
        if (origen.getId().equals(destino.getId())) {
            throw new ReglaNegocioException("El mensaje debe conectar dos pools diferentes");
        }
    }

    private void validarEmpresa(Usuario usuario, Proceso proceso) {
        if (!usuario.getEmpresa().getId().equals(proceso.getEmpresa().getId())) {
            throw new PermisoDenegadoException("El proceso pertenece a otra empresa");
        }
    }

    private EventoMensajeEnvio obtenerActivo(Long id) {
        return repository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento de mensaje envio no encontrado: " + id));
    }

    private Proceso obtenerProcesoActivo(Long id) {
        return procesoRepository.findByIdAndEstadoNot(id, EstadoProceso.INACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + id));
    }

    private EventoMensajeEnvioResponse mapear(EventoMensajeEnvio evento) {
        EventoMensajeEnvioResponse response = modelMapper.map(evento, EventoMensajeEnvioResponse.class);
        response.setProcesoId(evento.getProceso().getId());
        response.setPoolId(evento.getPool().getId());
        response.setPoolDestinoId(evento.getPoolDestino().getId());
        response.setNombrePoolDestino(evento.getPoolDestino().getNombre());
        return response;
    }
}
