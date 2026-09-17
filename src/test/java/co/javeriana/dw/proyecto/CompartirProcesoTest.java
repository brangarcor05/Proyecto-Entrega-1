package co.javeriana.dw.proyecto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.proceso.ActualizarProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.CompartirProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.CrearProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.ProcesoResponse;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;
import co.javeriana.dw.proyecto.service.ProcesoService;

@SpringBootTest
@Transactional
class CompartirProcesoTest {

    @Autowired
    private ProcesoService procesoService;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Empresa duena;
    private Empresa invitada;
    private Usuario adminDuena;
    private Usuario adminInvitada;
    private Long procesoId;

    @BeforeEach
    void prepararDatos() {
        duena = crearEmpresa("Alpina", "900111", "alpina@x.com");
        invitada = crearEmpresa("Postobon", "900222", "postobon@x.com");
        adminDuena = crearAdmin(duena, "admin-alpina@x.com");
        adminInvitada = crearAdmin(invitada, "admin-postobon@x.com");

        procesoId = procesoService.crear(
                new CrearProcesoRequest(duena.getId(), "Vacaciones", "Solicitud", "RRHH"),
                adminDuena.getId()).id();
    }

    @Test
    void laEmpresaInvitadaNoVeElProcesoAntesDeCompartirlo() {
        var pagina = procesoService.consultar(invitada.getId(), null, null, null, false, PageRequest.of(0, 10));
        assertEquals(0, pagina.getTotalElements(), "Vio un proceso que no le compartieron");

        assertThrows(RecursoNoEncontradoException.class,
                () -> procesoService.obtener(procesoId, invitada.getId()),
                "Pudo abrir un proceso ajeno");
    }

    @Test
    void alCompartirloLaInvitadaLoVeEnSoloLectura() {
        procesoService.compartir(procesoId,
                new CompartirProcesoRequest(true, Set.of(invitada.getId())), adminDuena.getId());

        var pagina = procesoService.consultar(invitada.getId(), null, null, null, false, PageRequest.of(0, 10));
        assertEquals(1, pagina.getTotalElements(), "No le llego el proceso compartido");

        ProcesoResponse compartido = pagina.getContent().get(0);
        assertTrue(compartido.soloLectura(), "El proceso compartido no quedo marcado como solo lectura");

        ProcesoResponse propio = procesoService.obtener(procesoId, duena.getId());
        assertFalse(propio.soloLectura(), "La duena no deberia verlo como solo lectura");
    }

    @Test
    void laDuenaSiguePudiendoVerYEditarSuProceso() {
        procesoService.compartir(procesoId,
                new CompartirProcesoRequest(true, Set.of(invitada.getId())), adminDuena.getId());

        var pagina = procesoService.consultar(duena.getId(), null, null, null, false, PageRequest.of(0, 10));
        assertEquals(1, pagina.getTotalElements(), "El join con las empresas invitadas duplico o perdio filas");

        ProcesoResponse editado = procesoService.actualizar(procesoId,
                new ActualizarProcesoRequest("Vacaciones v2", "Solicitud", "RRHH", EstadoProceso.PUBLICADO),
                adminDuena.getId());
        assertEquals("Vacaciones v2", editado.nombre());
    }

    @Test
    void laInvitadaNoPuedeEditarNiEliminarElProcesoCompartido() {
        procesoService.compartir(procesoId,
                new CompartirProcesoRequest(true, Set.of(invitada.getId())), adminDuena.getId());

        assertThrows(PermisoDenegadoException.class,
                () -> procesoService.actualizar(procesoId,
                        new ActualizarProcesoRequest("Robado", "x", "y", EstadoProceso.BORRADOR),
                        adminInvitada.getId()),
                "La empresa invitada pudo editar un proceso ajeno");

        assertThrows(PermisoDenegadoException.class,
                () -> procesoService.eliminar(procesoId, adminInvitada.getId()),
                "La empresa invitada pudo eliminar un proceso ajeno");
    }

    @Test
    void laInvitadaNoPuedeCambiarLaComparticion() {
        assertThrows(PermisoDenegadoException.class,
                () -> procesoService.compartir(procesoId,
                        new CompartirProcesoRequest(true, Set.of(invitada.getId())), adminInvitada.getId()),
                "Un admin de otra empresa cambio la comparticion");
    }

    @Test
    void alRetirarLaComparticionLaInvitadaDejaDeVerlo() {
        procesoService.compartir(procesoId,
                new CompartirProcesoRequest(true, Set.of(invitada.getId())), adminDuena.getId());
        procesoService.compartir(procesoId,
                new CompartirProcesoRequest(false, null), adminDuena.getId());

        var pagina = procesoService.consultar(invitada.getId(), null, null, null, false, PageRequest.of(0, 10));
        assertEquals(0, pagina.getTotalElements(), "Sigue viendo un proceso que ya no le comparten");
    }

    @Test
    void elHistorialSoloLoVeLaEmpresaDuena() {
        procesoService.compartir(procesoId,
                new CompartirProcesoRequest(true, Set.of(invitada.getId())), adminDuena.getId());

        assertFalse(procesoService.consultarHistorial(procesoId, duena.getId()).isEmpty(),
                "La duena no ve su propio historial");

        assertThrows(RecursoNoEncontradoException.class,
                () -> procesoService.consultarHistorial(procesoId, invitada.getId()),
                "La empresa invitada vio el historial, que expone los usuarios de la duena");
    }

    @Test
    void noSePuedeCompartirConLaPropiaEmpresaNiSinIndicarEmpresas() {
        assertThrows(ReglaNegocioException.class,
                () -> procesoService.compartir(procesoId,
                        new CompartirProcesoRequest(true, Set.of(duena.getId())), adminDuena.getId()));

        assertThrows(ReglaNegocioException.class,
                () -> procesoService.compartir(procesoId,
                        new CompartirProcesoRequest(true, Set.of()), adminDuena.getId()));
    }

    private Empresa crearEmpresa(String nombre, String ruc, String email) {
        return empresaRepository.save(Empresa.builder()
                .nombre(nombre)
                .ruc(ruc)
                .razonSocial(nombre + " S.A.")
                .email(email)
                .build());
    }

    private Usuario crearAdmin(Empresa empresa, String email) {
        return usuarioRepository.save(Usuario.builder()
                .empresa(empresa)
                .nombre("Admin de " + empresa.getNombre())
                .email(email)
                .rolUsuario(RolUsuario.ADMIN)
                .rol(RolUsuario.ADMIN.name())
                .passwordHash("no-importa-para-esta-prueba")
                .build());
    }
}
