package co.javeriana.dw.proyecto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.arco.ActualizarArcoRequest;
import co.javeriana.dw.proyecto.dto.arco.ArcoResponse;
import co.javeriana.dw.proyecto.dto.arco.CrearArcoRequest;
import co.javeriana.dw.proyecto.dto.pool.CrearPoolRequest;
import co.javeriana.dw.proyecto.dto.proceso.CrearProcesoRequest;
import co.javeriana.dw.proyecto.entidad.Actividad;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.Evento;
import co.javeriana.dw.proyecto.entidad.Gateway;
import co.javeriana.dw.proyecto.entidad.Lane;
import co.javeriana.dw.proyecto.entidad.NodoProceso;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.RolProceso;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.TipoGateway;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.ArcoRepository;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import co.javeriana.dw.proyecto.repository.LaneRepository;
import co.javeriana.dw.proyecto.repository.NodoProcesoRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;
import co.javeriana.dw.proyecto.repository.RolProcesoRepository;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;
import co.javeriana.dw.proyecto.service.ArcoService;
import co.javeriana.dw.proyecto.service.PoolService;
import co.javeriana.dw.proyecto.service.ProcesoService;

/** Criterios de aceptacion de HU-11, HU-12 y HU-13. */
@SpringBootTest
@Transactional
class ArcoReglasTest {

    @Autowired
    private ArcoService arcoService;
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
    private RolProcesoRepository rolProcesoRepository;
    @Autowired
    private LaneRepository laneRepository;
    @Autowired
    private NodoProcesoRepository nodoProcesoRepository;
    @Autowired
    private ArcoRepository arcoRepository;

    private Empresa empresa;
    private Usuario admin;
    private Usuario editor;
    private Usuario lector;
    private Proceso proceso;
    private Pool poolPropio;

    private Evento inicio;
    private Actividad radicar;
    private Gateway decision;

    @BeforeEach
    void prepararDiagrama() {
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

        inicio = crearEvento("Inicio", poolPropio);
        radicar = crearActividad("Radicar solicitud", poolPropio);
        decision = crearGateway("Aprobada?", poolPropio);
    }

    @Test
    @DisplayName("HU-11: el arco conecta actividades, gateways y eventos indistintamente")
    void elArcoConectaCualquierTipoDeNodo() {
        ArcoResponse desdeEvento = crearArco(inicio, radicar, null);
        assertEquals(inicio.getId(), desdeEvento.origenId());
        assertEquals("Radicar solicitud", desdeEvento.destinoNombre());

        ArcoResponse haciaGateway = crearArco(radicar, decision, null);
        assertEquals(decision.getId(), haciaGateway.destinoId());

        assertEquals(2, arcoService.listarPorProceso(proceso.getId()).size());
    }

    @Test
    @DisplayName("HU-11: no se permite un arco cuyo origen y destino sean el mismo elemento")
    void noSePermiteElArcoReflexivo() {
        assertThrows(ReglaNegocioException.class, () -> crearArco(radicar, radicar, null),
                "Se creo un arco de un elemento hacia si mismo");
    }

    @Test
    @DisplayName("HU-11: un arco no puede cruzar de un pool a otro")
    void elArcoNoCruzaDePoolAPool() {
        Long otroPoolId = poolService.crear(
                new CrearPoolRequest(proceso.getId(), "Cliente", false), editor.getId()).id();
        Pool otroPool = poolRepository.findById(otroPoolId).orElseThrow();
        Evento enOtroPool = crearEvento("Recibe", otroPool);

        assertThrows(ReglaNegocioException.class, () -> crearArco(radicar, enOtroPool, null),
                "Se creo un arco que cruza de un pool a otro: eso se modela como mensaje");
    }

    @Test
    @DisplayName("HU-11: no se permiten dos arcos identicos entre el mismo par de nodos")
    void noSePermitenArcosDuplicados() {
        crearArco(inicio, radicar, null);
        assertThrows(ReglaNegocioException.class, () -> crearArco(inicio, radicar, null),
                "Se creo un arco duplicado");
    }

    @Test
    @DisplayName("HU-12: la condicion solo tiene sentido en un arco que sale de un gateway")
    void laCondicionSoloAplicaSaliendoDeUnGateway() {
        ArcoResponse desdeGateway = crearArco(decision, radicar, "monto > 100");
        assertEquals("monto > 100", desdeGateway.condicion());

        assertThrows(ReglaNegocioException.class, () -> crearArco(inicio, radicar, "monto > 100"),
                "Se acepto una condicion en un arco que no sale de un gateway");
    }

    @Test
    @DisplayName("HU-12: al reconectar el arco se aplican las validaciones de la creacion")
    void alReconectarSeAplicanLasMismasValidaciones() {
        Long arcoId = crearArco(inicio, radicar, null).id();

        ArcoResponse reconectado = arcoService.actualizar(arcoId,
                new ActualizarArcoRequest(inicio.getId(), decision.getId(), "sigue", null), editor.getId());
        assertEquals(decision.getId(), reconectado.destinoId());
        assertEquals("sigue", reconectado.etiqueta());

        assertThrows(ReglaNegocioException.class,
                () -> arcoService.actualizar(arcoId,
                        new ActualizarArcoRequest(radicar.getId(), radicar.getId(), null, null),
                        editor.getId()),
                "Se pudo reconectar un arco sobre si mismo");
    }

    @Test
    @DisplayName("HU-13: la eliminacion es logica y solo la hace el administrador")
    void laEliminacionEsLogicaYSoloDelAdministrador() {
        Long arcoId = crearArco(inicio, radicar, null).id();

        assertThrows(PermisoDenegadoException.class, () -> arcoService.eliminar(arcoId, editor.getId()),
                "Un editor pudo eliminar un arco");

        arcoService.eliminar(arcoId, admin.getId());

        assertTrue(arcoRepository.findById(arcoId).isPresent(), "El arco se borro fisicamente");
        assertFalse(arcoRepository.findById(arcoId).get().isActivo());
        assertThrows(RecursoNoEncontradoException.class, () -> arcoService.obtener(arcoId));
    }

    @Test
    @DisplayName("HU-13: advierte si la eliminacion deja un elemento sin entrada o sin salida")
    void advierteSiDejaElementosSueltos() {
        Long arcoId = crearArco(inicio, radicar, null).id();
        crearArco(radicar, decision, null);

        var resultado = arcoService.eliminar(arcoId, admin.getId());

        assertEquals(2, resultado.advertencias().size(),
                "Deberia advertir por el origen sin salida y por el destino sin entrada");
        assertTrue(resultado.advertencias().stream().anyMatch(a -> a.contains("Inicio")),
                "No advirtio que el evento de inicio queda sin salida");
        assertTrue(resultado.advertencias().stream().anyMatch(a -> a.contains("Radicar solicitud")),
                "No advirtio que la actividad queda sin entrada");
    }

    @Test
    @DisplayName("HU-13: no advierte cuando los elementos conservan otras conexiones")
    void noAdvierteCuandoQuedanOtrasConexiones() {
        Long arcoId = crearArco(inicio, radicar, null).id();
        crearArco(inicio, decision, null);
        crearArco(decision, radicar, null);

        var resultado = arcoService.eliminar(arcoId, admin.getId());

        assertTrue(resultado.advertencias().isEmpty(),
                "Advirtio aunque los dos extremos conservan otras conexiones");
    }

    @Test
    @DisplayName("Un arco eliminado se puede volver a crear, pese a la restriccion unica de la tabla")
    void unArcoEliminadoSePuedeVolverACrear() {
        Long arcoId = crearArco(inicio, radicar, null).id();
        arcoService.eliminar(arcoId, admin.getId());

        ArcoResponse recreado = crearArco(inicio, radicar, null);
        assertTrue(recreado.activo());
        assertEquals(1, arcoService.listarPorProceso(proceso.getId()).size());
    }

    @Test
    @DisplayName("El usuario de solo lectura no puede crear arcos")
    void elLectorNoCreaArcos() {
        assertThrows(PermisoDenegadoException.class,
                () -> arcoService.crear(new CrearArcoRequest(
                        proceso.getId(), inicio.getId(), radicar.getId(), null, null), lector.getId()));
    }

    private ArcoResponse crearArco(NodoProceso origen, NodoProceso destino, String condicion) {
        return arcoService.crear(new CrearArcoRequest(
                proceso.getId(), origen.getId(), destino.getId(), null, condicion), editor.getId());
    }

    private Evento crearEvento(String nombre, Pool pool) {
        Evento evento = new Evento();
        evento.setTipo("INICIO");
        return nodoProcesoRepository.save(prepararNodo(evento, nombre, pool));
    }

    private Gateway crearGateway(String nombre, Pool pool) {
        Gateway gateway = new Gateway();
        gateway.setTipo(TipoGateway.EXCLUSIVO);
        return nodoProcesoRepository.save(prepararNodo(gateway, nombre, pool));
    }

    private Actividad crearActividad(String nombre, Pool pool) {
        RolProceso rol = new RolProceso();
        rol.setNombre("Analista " + System.nanoTime());
        rol.setActivo(true);
        rol.setEmpresa(empresa);
        rol = rolProcesoRepository.save(rol);

        Lane lane = new Lane();
        lane.setOrden(1);
        lane.setActivo(true);
        lane.setPool(pool);
        lane.setRolProceso(rol);
        lane = laneRepository.save(lane);

        Actividad actividad = new Actividad();
        actividad.setTipo("TAREA");
        actividad.setLane(lane);
        return nodoProcesoRepository.save(prepararNodo(actividad, nombre, pool));
    }

    private <T extends NodoProceso> T prepararNodo(T nodo, String nombre, Pool pool) {
        nodo.setNombre(nombre);
        nodo.setActivo(true);
        nodo.setProceso(proceso);
        nodo.setPool(pool);
        return nodo;
    }

    private Usuario crearUsuario(String email, RolUsuario rol) {
        return usuarioRepository.save(Usuario.builder()
                .empresa(empresa).nombre("Usuario " + rol).email(email)
                .rolUsuario(rol).passwordHash("x").build());
    }
}
