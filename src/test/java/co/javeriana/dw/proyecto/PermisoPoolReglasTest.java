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

import co.javeriana.dw.proyecto.dto.permisopool.ConfigurarPermisoPoolRequest;
import co.javeriana.dw.proyecto.dto.permisopool.PermisoPoolResponse;
import co.javeriana.dw.proyecto.dto.pool.CrearPoolRequest;
import co.javeriana.dw.proyecto.dto.proceso.CrearProcesoRequest;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;
import co.javeriana.dw.proyecto.service.PermisoPoolService;
import co.javeriana.dw.proyecto.service.PoolService;
import co.javeriana.dw.proyecto.service.ProcesoService;

/** Criterios de aceptacion de HU-24. */
@SpringBootTest
@Transactional
class PermisoPoolReglasTest {

    @Autowired
    private PermisoPoolService permisoPoolService;
    @Autowired
    private PoolService poolService;
    @Autowired
    private ProcesoService procesoService;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Empresa empresa;
    private Usuario admin;
    private Usuario editor;
    private Usuario lector;
    private Long procesoId;

    @BeforeEach
    void prepararDatos() {
        empresa = empresaRepository.save(Empresa.builder()
                .nombre("Alpina").rut("900111").razonSocial("Alpina S.A.").email("alpina@x.com").build());
        admin = crearUsuario(empresa, "admin@x.com", RolUsuario.ADMIN);
        editor = crearUsuario(empresa, "editor@x.com", RolUsuario.EDITOR);
        lector = crearUsuario(empresa, "lector@x.com", RolUsuario.LECTURA);

        procesoId = procesoService.crear(
                new CrearProcesoRequest(empresa.getId(), "Vacaciones", "Solicitud", "RRHH"),
                admin.getId()).id();
    }

    @Test
    @DisplayName("HU-24: sin configurar rigen los valores por defecto de los tres roles")
    void sinConfigurarRigenLosValoresPorDefecto() {
        var permisos = permisoPoolService.consultar(empresa.getId());
        assertEquals(3, permisos.size(), "Deberian llegar los tres roles de acceso");
        assertTrue(permisos.stream().noneMatch(PermisoPoolResponse::configurado),
                "Nadie ha configurado nada todavia");

        var admin = buscarRol(RolUsuario.ADMIN);
        assertTrue(admin.puedeCrear() && admin.puedeEditar() && admin.puedeEliminar());

        var editor = buscarRol(RolUsuario.EDITOR);
        assertTrue(editor.puedeCrear() && editor.puedeEditar());
        assertFalse(editor.puedeEliminar(), "Por defecto solo el administrador elimina");

        var lectura = buscarRol(RolUsuario.LECTURA);
        assertFalse(lectura.puedeCrear() || lectura.puedeEditar() || lectura.puedeEliminar(),
                "El rol de solo lectura no deberia poder modificar nada");
    }

    @Test
    @DisplayName("HU-24: la configuracion de la empresa manda sobre los valores por defecto")
    void laConfiguracionMandaSobreLosValoresPorDefecto() {
        // Por defecto el editor no puede eliminar. La empresa decide que si.
        assertThrows(PermisoDenegadoException.class, () -> eliminarPoolNuevo(editor));

        permisoPoolService.configurar(
                new ConfigurarPermisoPoolRequest(RolUsuario.EDITOR, true, true, true), admin.getId());

        eliminarPoolNuevo(editor);
        assertTrue(buscarRol(RolUsuario.EDITOR).configurado(), "No quedo marcado como configurado");
    }

    @Test
    @DisplayName("HU-24: la empresa puede quitarle permisos a un rol que los tenia por defecto")
    void laEmpresaPuedeQuitarPermisos() {
        poolService.crear(new CrearPoolRequest(procesoId, "Cliente", false), editor.getId());

        permisoPoolService.configurar(
                new ConfigurarPermisoPoolRequest(RolUsuario.EDITOR, false, false, false), admin.getId());

        assertThrows(PermisoDenegadoException.class,
                () -> poolService.crear(new CrearPoolRequest(procesoId, "Proveedor", false), editor.getId()),
                "El editor creo un pool despues de que le quitaron el permiso");
    }

    @Test
    @DisplayName("HU-24: el rol de solo lectura visualiza pero no modifica")
    void elRolDeSoloLecturaVisualizaPeroNoModifica() {
        poolService.crear(new CrearPoolRequest(procesoId, "Cliente", false), editor.getId());

        assertEquals(2, poolService.listarPorProceso(procesoId).size(),
                "El lector deberia poder ver los pools");

        assertThrows(PermisoDenegadoException.class,
                () -> poolService.crear(new CrearPoolRequest(procesoId, "Proveedor", false), lector.getId()));
    }

    @Test
    @DisplayName("HU-24: solo el administrador configura los permisos, y solo de su empresa")
    void soloElAdministradorConfiguraYSoloSuEmpresa() {
        assertThrows(PermisoDenegadoException.class,
                () -> permisoPoolService.configurar(
                        new ConfigurarPermisoPoolRequest(RolUsuario.LECTURA, true, true, true), editor.getId()),
                "Un editor configuro los permisos");

        Empresa otra = empresaRepository.save(Empresa.builder()
                .nombre("Postobon").rut("900222").razonSocial("Postobon S.A.").email("p@x.com").build());
        Usuario adminOtra = crearUsuario(otra, "admin-otra@x.com", RolUsuario.ADMIN);

        permisoPoolService.configurar(
                new ConfigurarPermisoPoolRequest(RolUsuario.EDITOR, false, false, false), adminOtra.getId());

        assertFalse(buscarRol(RolUsuario.EDITOR).configurado(),
                "La configuracion de otra empresa afecto a esta");
    }

    @Test
    @DisplayName("Un usuario de otra empresa no puede tocar el diagrama de un proceso ajeno")
    void unUsuarioDeOtraEmpresaNoTocaElDiagramaAjeno() {
        Empresa otra = empresaRepository.save(Empresa.builder()
                .nombre("Postobon").rut("900222").razonSocial("Postobon S.A.").email("p@x.com").build());
        Usuario adminOtra = crearUsuario(otra, "admin-otra@x.com", RolUsuario.ADMIN);

        assertThrows(PermisoDenegadoException.class,
                () -> poolService.crear(new CrearPoolRequest(procesoId, "Intruso", false), adminOtra.getId()),
                "Un administrador de otra empresa agrego un pool a un proceso ajeno");
    }

    private void eliminarPoolNuevo(Usuario usuario) {
        Long poolId = poolService.crear(
                new CrearPoolRequest(procesoId, "Temporal " + System.nanoTime(), false), admin.getId()).id();
        poolService.eliminar(poolId, usuario.getId());
    }

    private PermisoPoolResponse buscarRol(RolUsuario rol) {
        return permisoPoolService.consultar(empresa.getId()).stream()
                .filter(p -> p.rolUsuario() == rol)
                .findFirst()
                .orElseThrow();
    }

    private Usuario crearUsuario(Empresa deLaEmpresa, String email, RolUsuario rol) {
        return usuarioRepository.save(Usuario.builder()
                .empresa(deLaEmpresa).nombre("Usuario " + rol).email(email)
                .rolUsuario(rol).passwordHash("x").build());
    }
}
