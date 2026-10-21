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

import co.javeriana.dw.proyecto.dto.pool.ActualizarPoolRequest;
import co.javeriana.dw.proyecto.dto.pool.CrearPoolRequest;
import co.javeriana.dw.proyecto.dto.pool.PoolResponse;
import co.javeriana.dw.proyecto.dto.proceso.CrearProcesoRequest;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.Lane;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.RolProceso;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import co.javeriana.dw.proyecto.repository.LaneRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.RolProcesoRepository;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;
import co.javeriana.dw.proyecto.service.PoolService;
import co.javeriana.dw.proyecto.service.ProcesoService;

/** Criterios de aceptacion de HU-21. */
@SpringBootTest
@Transactional
class PoolReglasTest {

    @Autowired
    private PoolService poolService;
    @Autowired
    private ProcesoService procesoService;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PoolRepository poolRepository;
    @Autowired
    private RolProcesoRepository rolProcesoRepository;
    @Autowired
    private LaneRepository laneRepository;

    private Empresa empresa;
    private Usuario admin;
    private Usuario editor;
    private Usuario lector;
    private Long procesoId;

    @BeforeEach
    void prepararDatos() {
        empresa = empresaRepository.save(Empresa.builder()
                .nombre("Alpina").rut("900111").razonSocial("Alpina S.A.").email("alpina@x.com").build());
        admin = crearUsuario("admin@x.com", RolUsuario.ADMIN);
        editor = crearUsuario("editor@x.com", RolUsuario.EDITOR);
        lector = crearUsuario("lector@x.com", RolUsuario.LECTURA);

        procesoId = procesoService.crear(
                new CrearProcesoRequest(empresa.getId(), "Vacaciones", "Solicitud", "RRHH"),
                admin.getId()).id();
    }

    @Test
    @DisplayName("HU-21: un diagrama puede tener varios pools ademas del propietario")
    void unDiagramaPuedeTenerVariosPools() {
        poolService.crear(new CrearPoolRequest(procesoId, "Cliente", false), editor.getId());
        poolService.crear(new CrearPoolRequest(procesoId, "Servicio de correo", true), editor.getId());

        var pools = poolService.listarPorProceso(procesoId);
        assertEquals(3, pools.size(), "Deberian estar el propietario y los dos participantes");
        assertEquals(1, pools.stream().filter(PoolResponse::esPropietario).count(),
                "Solo puede haber un pool propietario");
    }

    @Test
    @DisplayName("HU-21: el participante externo se modela como caja negra")
    void elParticipanteExternoSeModelaComoCajaNegra() {
        PoolResponse externo = poolService.crear(
                new CrearPoolRequest(procesoId, "Servicio de correo", true), editor.getId());

        assertTrue(externo.cajaNegra());
        assertFalse(externo.esPropietario(), "Un pool creado por la API no puede nacer como propietario");
    }

    @Test
    @DisplayName("HU-21: solo administrador o editor crean y modifican pools")
    void soloAdministradorOEditorModificanPools() {
        assertThrows(PermisoDenegadoException.class,
                () -> poolService.crear(new CrearPoolRequest(procesoId, "Cliente", false), lector.getId()),
                "Un usuario de solo lectura creo un pool");

        Long poolId = poolService.crear(new CrearPoolRequest(procesoId, "Cliente", false), editor.getId()).id();

        assertThrows(PermisoDenegadoException.class,
                () -> poolService.actualizar(poolId, new ActualizarPoolRequest("Otro", false), lector.getId()),
                "Un usuario de solo lectura modifico un pool");
    }

    @Test
    @DisplayName("HU-21: solo el administrador elimina, y la eliminacion es logica")
    void soloElAdministradorEliminaYEsLogica() {
        Long poolId = poolService.crear(new CrearPoolRequest(procesoId, "Cliente", false), editor.getId()).id();

        assertThrows(PermisoDenegadoException.class,
                () -> poolService.eliminar(poolId, editor.getId()),
                "Un editor pudo eliminar un pool");

        poolService.eliminar(poolId, admin.getId());

        assertTrue(poolRepository.findById(poolId).isPresent(), "El pool se borro fisicamente");
        assertFalse(poolRepository.findById(poolId).get().isActivo(), "No quedo marcado como inactivo");
        assertThrows(RecursoNoEncontradoException.class, () -> poolService.obtener(poolId));
    }

    @Test
    @DisplayName("HU-21: el pool de la empresa propietaria no se puede eliminar")
    void elPoolPropietarioNoSePuedeEliminar() {
        Long propietarioId = poolRepository.findByProcesoIdAndEsPropietarioTrue(procesoId).orElseThrow().getId();

        assertThrows(ReglaNegocioException.class,
                () -> poolService.eliminar(propietarioId, admin.getId()),
                "Se pudo eliminar el pool propietario y dejar el proceso sin dueno");
    }

    @Test
    @DisplayName("HU-21: un pool con lanes no se elimina ni se vuelve caja negra")
    void unPoolConContenidoNoSeEliminaNiSeVuelveCajaNegra() {
        Long poolId = poolService.crear(new CrearPoolRequest(procesoId, "Cliente", false), editor.getId()).id();
        agregarLane(poolId);

        assertThrows(ReglaNegocioException.class,
                () -> poolService.actualizar(poolId, new ActualizarPoolRequest("Cliente", true), editor.getId()),
                "Se volvio caja negra un pool que tiene lanes adentro");

        assertThrows(ReglaNegocioException.class,
                () -> poolService.eliminar(poolId, admin.getId()),
                "Se elimino un pool que tiene lanes adentro");
    }

    @Test
    @DisplayName("HU-21: los cambios sobre pools quedan en el historial del proceso")
    void losCambiosQuedanEnElHistorial() {
        int antes = procesoService.consultarHistorial(procesoId, empresa.getId()).size();
        poolService.crear(new CrearPoolRequest(procesoId, "Cliente", false), editor.getId());
        int despues = procesoService.consultarHistorial(procesoId, empresa.getId()).size();

        assertEquals(antes + 1, despues, "La creacion del pool no quedo registrada");
    }

    @Test
    @DisplayName("Un proceso eliminado no admite cambios en su diagrama")
    void unProcesoEliminadoNoAdmiteNuevosPools() {
        procesoService.eliminar(procesoId, admin.getId());

        assertThrows(RecursoNoEncontradoException.class,
                () -> poolService.crear(new CrearPoolRequest(procesoId, "Cliente", false), editor.getId()),
                "Se agrego un pool a un proceso eliminado");
    }

    private void agregarLane(Long poolId) {
        RolProceso rol = new RolProceso();
        rol.setNombre("Analista");
        rol.setActivo(true);
        rol.setEmpresa(empresa);
        rol = rolProcesoRepository.save(rol);

        Pool pool = poolRepository.findById(poolId).orElseThrow();
        Lane lane = new Lane();
        lane.setOrden(1);
        lane.setActivo(true);
        lane.setPool(pool);
        lane.setRolProceso(rol);
        laneRepository.save(lane);
    }

    private Usuario crearUsuario(String email, RolUsuario rol) {
        return usuarioRepository.save(Usuario.builder()
                .empresa(empresa).nombre("Usuario " + rol).email(email)
                .rolUsuario(rol).passwordHash("x").build());
    }
}
