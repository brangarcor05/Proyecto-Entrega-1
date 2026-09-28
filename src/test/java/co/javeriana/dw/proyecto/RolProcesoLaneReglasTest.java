package co.javeriana.dw.proyecto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.lane.ActualizarLaneRequest;
import co.javeriana.dw.proyecto.dto.lane.CrearLaneRequest;
import co.javeriana.dw.proyecto.dto.lane.LaneResponse;
import co.javeriana.dw.proyecto.dto.lane.ReordenarLanesRequest;
import co.javeriana.dw.proyecto.dto.pool.CrearPoolRequest;
import co.javeriana.dw.proyecto.dto.proceso.CrearProcesoRequest;
import co.javeriana.dw.proyecto.dto.rolproceso.ActualizarRolProcesoRequest;
import co.javeriana.dw.proyecto.dto.rolproceso.CrearRolProcesoRequest;
import co.javeriana.dw.proyecto.dto.rolproceso.RolProcesoResponse;
import co.javeriana.dw.proyecto.entidad.Actividad;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.Historial;
import co.javeriana.dw.proyecto.entidad.Lane;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.ActividadRepository;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import co.javeriana.dw.proyecto.repository.HistorialRepository;
import co.javeriana.dw.proyecto.repository.LaneRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;
import co.javeriana.dw.proyecto.service.LaneService;
import co.javeriana.dw.proyecto.service.PoolService;
import co.javeriana.dw.proyecto.service.ProcesoService;
import co.javeriana.dw.proyecto.service.RolProcesoService;

/** Criterios de aceptacion de HU-17 a HU-20 (rol de proceso) y HU-22 (lane). */
@SpringBootTest
@Transactional
class RolProcesoLaneReglasTest {

    @Autowired
    private RolProcesoService rolProcesoService;
    @Autowired
    private LaneService laneService;
    @Autowired
    private PoolService poolService;
    @Autowired
    private ProcesoService procesoService;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ProcesoRepository procesoRepository;
    @Autowired
    private PoolRepository poolRepository;
    @Autowired
    private LaneRepository laneRepository;
    @Autowired
    private ActividadRepository actividadRepository;
    @Autowired
    private HistorialRepository historialRepository;

    private Empresa empresa;
    private Usuario admin;
    private Usuario editor;
    private Usuario lector;
    private Proceso proceso;
    private Pool poolPropio;

    @BeforeEach
    void prepararDatos() {
        empresa = empresaRepository.save(Empresa.builder()
                .nombre("Alpina").rut("900111").razonSocial("Alpina S.A.").email("alpina@x.com").build());
        admin = crearUsuario("admin@x.com", RolUsuario.ADMIN);
        editor = crearUsuario("editor@x.com", RolUsuario.EDITOR);
        lector = crearUsuario("lector@x.com", RolUsuario.LECTURA);

        Long procesoId = procesoService.crear(
                new CrearProcesoRequest(empresa.getId(), "Vacaciones", "Solicitud", "RRHH"),
                admin.getId()).id();
        proceso = procesoRepository.findById(procesoId).orElseThrow();
        poolPropio = poolRepository.findByProcesoIdAndEsPropietarioTrue(procesoId).orElseThrow();
    }

    /* ------------------------------------------------------------------
       HU-17: crear rol de proceso
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("HU-17: el administrador crea un rol de proceso con nombre y descripcion")
    void elAdministradorCreaUnRolDeProceso() {
        RolProcesoResponse rol = crearRol("Analista", "Revisa las solicitudes entrantes");

        assertEquals("Analista", rol.nombre());
        assertEquals("Revisa las solicitudes entrantes", rol.descripcion());
        assertTrue(rol.activo());
        assertFalse(rol.enUso(), "Un rol recien creado no deberia estar en uso todavia");
    }

    @Test
    @DisplayName("HU-17: el nombre del rol es unico dentro de la empresa")
    void elNombreDelRolEsUnicoEnLaEmpresa() {
        crearRol("Analista", null);

        assertThrows(NombreDuplicadoException.class, () -> crearRol("Analista", "Otra descripcion"),
                "Se creo un segundo rol con el mismo nombre en la misma empresa");
    }

    @Test
    @DisplayName("HU-17: solo el administrador puede crear roles, ni siquiera el editor")
    void soloElAdministradorCreaRoles() {
        assertThrows(PermisoDenegadoException.class,
                () -> rolProcesoService.crear(
                        new CrearRolProcesoRequest(empresa.getId(), "Analista", null), editor.getId()),
                "El editor no deberia poder crear roles de proceso");
    }

    /* ------------------------------------------------------------------
       HU-18: editar rol de proceso
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("HU-18: editar el rol actualiza nombre y descripcion, y la lane que lo usa")
    void editarElRolActualizaSuEtiquetaEnLasLanes() {
        RolProcesoResponse rol = crearRol("Analista", "Version inicial");
        LaneResponse lane = crearLane(poolPropio.getId(), rol.id());
        assertEquals("Analista", lane.rolProcesoNombre());

        RolProcesoResponse actualizado = rolProcesoService.actualizar(rol.id(),
                new ActualizarRolProcesoRequest("Analista Senior", "Version revisada"), admin.getId());

        assertEquals("Analista Senior", actualizado.nombre());
        LaneResponse laneTrasRenombrar = laneService.listarPorPool(poolPropio.getId()).stream()
                .filter(l -> l.id().equals(lane.id())).findFirst().orElseThrow();
        assertEquals("Analista Senior", laneTrasRenombrar.rolProcesoNombre(),
                "La lane no guarda el nombre, solo la referencia: debe reflejar el cambio de inmediato");
    }

    @Test
    @DisplayName("HU-18: el nuevo nombre tambien debe ser unico dentro de la empresa")
    void alEditarElNuevoNombreSigueSiendoUnico() {
        crearRol("Analista", null);
        RolProcesoResponse supervisor = crearRol("Supervisor", null);

        assertThrows(NombreDuplicadoException.class,
                () -> rolProcesoService.actualizar(supervisor.id(),
                        new ActualizarRolProcesoRequest("Analista", null), admin.getId()));
    }

    /* ------------------------------------------------------------------
       HU-19: eliminar rol de proceso
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("HU-19: no se puede eliminar un rol en uso, y se indica en que proceso")
    void noSePuedeEliminarUnRolEnUso() {
        RolProcesoResponse rol = crearRol("Analista", null);
        crearLane(poolPropio.getId(), rol.id());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> rolProcesoService.eliminar(rol.id(), admin.getId()));
        assertTrue(ex.getMessage().contains("Vacaciones"),
                "El mensaje deberia decir en que proceso esta siendo usado el rol");
    }

    @Test
    @DisplayName("HU-19: un rol que no esta en uso si se puede eliminar (logicamente)")
    void unRolSinUsoSiSePuedeEliminar() {
        RolProcesoResponse rol = crearRol("Auditor", null);

        rolProcesoService.eliminar(rol.id(), admin.getId());

        assertThrows(RecursoNoEncontradoException.class,
                () -> rolProcesoService.obtener(rol.id(), empresa.getId()),
                "El rol eliminado no deberia poder consultarse como activo");
    }

    @Test
    @DisplayName("HU-19: solo el administrador puede eliminar roles")
    void soloElAdministradorEliminaRoles() {
        RolProcesoResponse rol = crearRol("Auditor", null);

        assertThrows(PermisoDenegadoException.class,
                () -> rolProcesoService.eliminar(rol.id(), editor.getId()));
    }

    /* ------------------------------------------------------------------
       HU-20: consultar roles de proceso
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("HU-20: el listado es paginado, busca por nombre y marca si el rol esta en uso")
    void elListadoIndicaSiElRolEstaEnUso() {
        RolProcesoResponse enUso = crearRol("Analista", null);
        crearRol("Supervisor", null);
        crearLane(poolPropio.getId(), enUso.id());

        var pagina = rolProcesoService.consultar(empresa.getId(), "Ana", PageRequest.of(0, 10));

        assertEquals(1, pagina.getTotalElements(), "El filtro por nombre deberia traer solo el Analista");
        RolProcesoResponse resultado = pagina.getContent().get(0);
        assertTrue(resultado.enUso());
        assertTrue(resultado.procesosDondeSeUsa().contains("Vacaciones"));
    }

    /* ------------------------------------------------------------------
       HU-22: lanes
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("HU-22: crear una lane la asocia a un rol y le asigna el siguiente orden disponible")
    void crearUnaLaneLeAsignaElSiguienteOrden() {
        RolProcesoResponse analista = crearRol("Analista", null);
        RolProcesoResponse supervisor = crearRol("Supervisor", null);

        LaneResponse primera = crearLane(poolPropio.getId(), analista.id());
        LaneResponse segunda = crearLane(poolPropio.getId(), supervisor.id());

        assertEquals(0, primera.orden());
        assertEquals(1, segunda.orden());
    }

    @Test
    @DisplayName("HU-22: no se permiten dos lanes con el mismo rol en el mismo pool")
    void noSePermitenDosLanesConElMismoRolEnElMismoPool() {
        RolProcesoResponse analista = crearRol("Analista", null);
        crearLane(poolPropio.getId(), analista.id());

        assertThrows(ReglaNegocioException.class, () -> crearLane(poolPropio.getId(), analista.id()));
    }

    @Test
    @DisplayName("HU-21/HU-22: un pool caja negra no puede tener lanes")
    void unPoolCajaNegraNoPuedeTenerLanes() {
        RolProcesoResponse analista = crearRol("Analista", null);
        Long poolExternoId = poolService.crear(
                new CrearPoolRequest(proceso.getId(), "Proveedor", true), editor.getId()).id();

        assertThrows(ReglaNegocioException.class, () -> crearLane(poolExternoId, analista.id()));
    }

    @Test
    @DisplayName("HU-22: \"renombrar\" una lane es reasignarle otro rol de proceso")
    void renombrarUnaLaneEsReasignarleOtroRol() {
        RolProcesoResponse analista = crearRol("Analista", null);
        RolProcesoResponse supervisor = crearRol("Supervisor", null);
        LaneResponse lane = crearLane(poolPropio.getId(), analista.id());

        LaneResponse actualizada = laneService.actualizar(lane.id(),
                new ActualizarLaneRequest(supervisor.id()), admin.getId());

        assertEquals(supervisor.id(), actualizada.rolProcesoId());
        assertEquals("Supervisor", actualizada.rolProcesoNombre());
    }

    @Test
    @DisplayName("HU-22: reordenar exige incluir exactamente las lanes activas del pool")
    void reordenarExigeLaListaCompletaDeLanes() {
        LaneResponse l1 = crearLane(poolPropio.getId(), crearRol("Analista", null).id());
        LaneResponse l2 = crearLane(poolPropio.getId(), crearRol("Supervisor", null).id());

        assertThrows(ReglaNegocioException.class,
                () -> laneService.reordenar(poolPropio.getId(),
                        new ReordenarLanesRequest(List.of(l1.id())), admin.getId()),
                "Falta l2 en la lista: no deberia aceptarse una lista incompleta");

        var reordenadas = laneService.reordenar(poolPropio.getId(),
                new ReordenarLanesRequest(List.of(l2.id(), l1.id())), admin.getId());

        assertEquals(l2.id(), reordenadas.get(0).id());
        assertEquals(0, reordenadas.get(0).orden());
        assertEquals(1, reordenadas.get(1).orden());
    }

    @Test
    @DisplayName("HU-22: no se puede eliminar una lane que tiene actividades asignadas")
    void noSePuedeEliminarUnaLaneConActividades() {
        LaneResponse lane = crearLane(poolPropio.getId(), crearRol("Analista", null).id());
        crearActividadEnLane(lane.id(), "Radicar solicitud");

        assertThrows(ReglaNegocioException.class, () -> laneService.eliminar(lane.id(), admin.getId()));
    }

    @Test
    @DisplayName("HU-22: una lane sin actividades si se puede eliminar")
    void unaLaneSinActividadesSiSePuedeEliminar() {
        LaneResponse lane = crearLane(poolPropio.getId(), crearRol("Auditor", null).id());

        laneService.eliminar(lane.id(), admin.getId());

        assertTrue(laneService.listarPorPool(poolPropio.getId()).isEmpty());
    }

    @Test
    @DisplayName("HU-18: cada cambio sobre un rol de proceso queda registrado en el historial")
    void editarUnRolQuedaRegistradoEnElHistorial() {
        RolProcesoResponse rol = crearRol("Analista", "Version inicial");

        rolProcesoService.actualizar(rol.id(),
                new ActualizarRolProcesoRequest("Analista Senior", "Version revisada"), admin.getId());

        List<Historial> historial = historialRepository
                .findByEntidadTipoAndEntidadIdOrderByFechaDesc("RolProceso", rol.id());
        assertEquals(2, historial.size(), "Deberian quedar dos entradas: creacion y edicion");
        assertEquals(AccionHistorial.EDICION, historial.get(0).getAccion(),
                "La mas reciente (indice 0) debe ser la edicion");
        assertEquals(AccionHistorial.CREACION, historial.get(1).getAccion());
    }

    @Test
    @DisplayName("HU-19: eliminar un rol de proceso tambien queda registrado en el historial")
    void eliminarUnRolQuedaRegistradoEnElHistorial() {
        RolProcesoResponse rol = crearRol("Auditor", null);

        rolProcesoService.eliminar(rol.id(), admin.getId());

        List<Historial> historial = historialRepository
                .findByEntidadTipoAndEntidadIdOrderByFechaDesc("RolProceso", rol.id());
        assertEquals(AccionHistorial.ELIMINACION, historial.get(0).getAccion());
    }

    @Test
    @DisplayName("HU-22: crear, renombrar y eliminar una lane queda registrado en el historial")
    void losCambiosDeUnaLaneQuedanEnElHistorial() {
        LaneResponse lane = crearLane(poolPropio.getId(), crearRol("Analista", null).id());
        RolProcesoResponse supervisor = crearRol("Supervisor", null);

        laneService.actualizar(lane.id(), new ActualizarLaneRequest(supervisor.id()), admin.getId());
        laneService.eliminar(lane.id(), admin.getId());

        List<Historial> historial = historialRepository
                .findByEntidadTipoAndEntidadIdOrderByFechaDesc("Lane", lane.id());
        assertEquals(3, historial.size(), "Deberian quedar tres entradas: creacion, edicion y eliminacion");
        assertEquals(AccionHistorial.ELIMINACION, historial.get(0).getAccion());
        assertEquals(AccionHistorial.EDICION, historial.get(1).getAccion());
        assertEquals(AccionHistorial.CREACION, historial.get(2).getAccion());
    }

    /* ------------------------------------------------------------------
       Helpers
       ------------------------------------------------------------------ */

    private RolProcesoResponse crearRol(String nombre, String descripcion) {
        return rolProcesoService.crear(
                new CrearRolProcesoRequest(empresa.getId(), nombre, descripcion), admin.getId());
    }

    private LaneResponse crearLane(Long poolId, Long rolProcesoId) {
        return laneService.crear(new CrearLaneRequest(poolId, rolProcesoId), admin.getId());
    }

    /** ActividadService todavia no existe (le corresponde a Eilen): se crea directo por repositorio. */
    private Actividad crearActividadEnLane(Long laneId, String nombre) {
        Lane lane = laneRepository.findById(laneId).orElseThrow();
        Actividad actividad = new Actividad();
        actividad.setTipo("TAREA");
        actividad.setLane(lane);
        actividad.setNombre(nombre);
        actividad.setActivo(true);
        actividad.setProceso(proceso);
        actividad.setPool(lane.getPool());
        return actividadRepository.save(actividad);
    }

    private Usuario crearUsuario(String email, RolUsuario rol) {
        return usuarioRepository.save(Usuario.builder()
                .empresa(empresa).nombre("Usuario " + rol).email(email)
                .rolUsuario(rol).passwordHash("x").build());
    }
}
