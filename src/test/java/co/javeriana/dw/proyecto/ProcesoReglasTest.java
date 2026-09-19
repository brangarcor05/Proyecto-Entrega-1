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
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.proceso.ActualizarProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.CrearProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.ProcesoResponse;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;
import co.javeriana.dw.proyecto.service.ProcesoService;

/** Criterios de aceptacion de HU-04, HU-05, HU-06 y HU-07. */
@SpringBootTest
@Transactional
class ProcesoReglasTest {

    @Autowired
    private ProcesoService procesoService;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PoolRepository poolRepository;

    private Empresa empresa;
    private Usuario admin;
    private Usuario editor;
    private Usuario lector;

    @BeforeEach
    void prepararDatos() {
        empresa = empresaRepository.save(Empresa.builder()
                .nombre("Alpina").rut("900111").razonSocial("Alpina S.A.").email("alpina@x.com").build());
        admin = crearUsuario("admin@x.com", RolUsuario.ADMIN);
        editor = crearUsuario("editor@x.com", RolUsuario.EDITOR);
        lector = crearUsuario("lector@x.com", RolUsuario.LECTURA);
    }

    @Test
    @DisplayName("HU-04: el proceso nace en BORRADOR y con el pool de la empresa ya creado")
    void elProcesoNaceEnBorradorConSuPool() {
        ProcesoResponse proceso = crearProceso("Vacaciones");

        assertEquals(EstadoProceso.BORRADOR, proceso.estado());
        assertTrue(proceso.activo());
        assertFalse(proceso.compartido(), "Por defecto los procesos no son compartidos");

        var pools = poolRepository.findByProcesoIdAndActivoTrue(proceso.id());
        assertEquals(1, pools.size(), "No se creo el pool propietario");
        assertTrue(pools.get(0).isEsPropietario());
        assertEquals(empresa.getNombre(), pools.get(0).getNombre());
    }

    @Test
    @DisplayName("HU-04: el nombre del proceso es unico dentro de la empresa")
    void noSePuedeRepetirElNombreEnLaMismaEmpresa() {
        crearProceso("Vacaciones");
        assertThrows(NombreDuplicadoException.class, () -> crearProceso("Vacaciones"));
    }

    @Test
    @DisplayName("HU-05: solo administrador o editor pueden modificar")
    void soloAdministradorOEditorModifican() {
        Long procesoId = crearProceso("Vacaciones").id();
        var cambio = new ActualizarProcesoRequest("Vacaciones v2", "d", "RRHH", EstadoProceso.PUBLICADO);

        assertEquals("Vacaciones v2", procesoService.actualizar(procesoId, cambio, editor.getId()).nombre());

        assertThrows(PermisoDenegadoException.class,
                () -> procesoService.actualizar(procesoId, cambio, lector.getId()),
                "Un usuario de solo lectura pudo editar");
    }

    @Test
    @DisplayName("HU-05: cada cambio queda en el historial con su usuario y su fecha")
    void cadaCambioQuedaEnElHistorial() {
        Long procesoId = crearProceso("Vacaciones").id();
        procesoService.actualizar(procesoId,
                new ActualizarProcesoRequest("Vacaciones v2", "d", "RRHH", EstadoProceso.PUBLICADO),
                admin.getId());

        var historial = procesoService.consultarHistorial(procesoId, empresa.getId());
        assertEquals(2, historial.size(), "Falta la creacion o la edicion en el historial");
        assertEquals(admin.getId(), historial.get(0).usuarioId());
        assertTrue(historial.get(0).fecha() != null, "El historial no guardo la fecha");
    }

    @Test
    @DisplayName("HU-06: la eliminacion es logica y solo la hace el administrador")
    void laEliminacionEsLogicaYSoloDelAdministrador() {
        Long procesoId = crearProceso("Vacaciones").id();

        assertThrows(PermisoDenegadoException.class,
                () -> procesoService.eliminar(procesoId, editor.getId()),
                "Un editor pudo eliminar");

        procesoService.eliminar(procesoId, admin.getId());

        assertThrows(RecursoNoEncontradoException.class,
                () -> procesoService.obtener(procesoId, empresa.getId()),
                "El proceso eliminado sigue siendo accesible");
    }

    @Test
    @DisplayName("HU-06: el inactivo sale del listado por defecto pero se consulta con filtro")
    void elInactivoSaleDelListadoPeroSeConsultaConFiltro() {
        Long procesoId = crearProceso("Vacaciones").id();
        procesoService.eliminar(procesoId, admin.getId());

        assertEquals(0, listar(false).getTotalElements(), "El eliminado sigue en el listado por defecto");
        assertEquals(1, listar(true).getTotalElements(), "No se puede consultar con el filtro de inactivos");
    }

    @Test
    @DisplayName("HU-07: la busqueda por nombre y los filtros de estado y categoria se combinan")
    void losFiltrosSeCombinan() {
        crearProceso("Solicitud de vacaciones", "RRHH");
        crearProceso("Solicitud de compras", "COMPRAS");
        Long publicado = crearProceso("Cierre contable", "FINANZAS").id();
        procesoService.actualizar(publicado,
                new ActualizarProcesoRequest("Cierre contable", "d", "FINANZAS", EstadoProceso.PUBLICADO),
                admin.getId());

        assertEquals(2, buscar("solicitud", null, null).getTotalElements(), "Fallo la busqueda por nombre");
        assertEquals(1, buscar(null, null, "RRHH").getTotalElements(), "Fallo el filtro por categoria");
        assertEquals(1, buscar(null, EstadoProceso.PUBLICADO, null).getTotalElements(), "Fallo el filtro por estado");

        // Los tres a la vez: este es el caso que no funcionaba antes
        assertEquals(1, buscar("solicitud", EstadoProceso.BORRADOR, "RRHH").getTotalElements(),
                "Los filtros combinados no se aplican juntos");
        assertEquals(0, buscar("solicitud", EstadoProceso.BORRADOR, "FINANZAS").getTotalElements(),
                "Devolvio resultados que no cumplen los tres filtros");
    }

    @Test
    @DisplayName("HU-07: el listado es paginado")
    void elListadoEsPaginado() {
        for (int i = 1; i <= 5; i++) {
            crearProceso("Proceso " + i);
        }
        var primeraPagina = procesoService.consultar(empresa.getId(), null, null, null, false, PageRequest.of(0, 2));

        assertEquals(2, primeraPagina.getContent().size());
        assertEquals(5, primeraPagina.getTotalElements());
        assertEquals(3, primeraPagina.getTotalPages());
    }

    private ProcesoResponse crearProceso(String nombre) {
        return crearProceso(nombre, "RRHH");
    }

    private ProcesoResponse crearProceso(String nombre, String categoria) {
        return procesoService.crear(
                new CrearProcesoRequest(empresa.getId(), nombre, "Descripcion", categoria), admin.getId());
    }

    private org.springframework.data.domain.Page<ProcesoResponse> listar(boolean incluirInactivos) {
        return procesoService.consultar(empresa.getId(), null, null, null, incluirInactivos, PageRequest.of(0, 10));
    }

    private org.springframework.data.domain.Page<ProcesoResponse> buscar(
            String nombre, EstadoProceso estado, String categoria) {
        return procesoService.consultar(empresa.getId(), nombre, estado, categoria, false, PageRequest.of(0, 10));
    }

    private Usuario crearUsuario(String email, RolUsuario rol) {
        return usuarioRepository.save(Usuario.builder()
                .empresa(empresa).nombre("Usuario " + rol).email(email)
                .rolUsuario(rol).rol(rol.name()).passwordHash("x").build());
    }
}
