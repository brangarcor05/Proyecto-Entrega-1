package co.javeriana.dw.proyecto.config;

import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import co.javeriana.dw.proyecto.dto.arco.CrearArcoRequest;
import co.javeriana.dw.proyecto.dto.empresa.CrearEmpresaRequest;
import co.javeriana.dw.proyecto.dto.empresa.EmpresaResponse;
import co.javeriana.dw.proyecto.dto.evento.CrearEventoRequest;
import co.javeriana.dw.proyecto.dto.evento.EventoResponse;
import co.javeriana.dw.proyecto.dto.lane.CrearLaneRequest;
import co.javeriana.dw.proyecto.dto.pool.CrearPoolRequest;
import co.javeriana.dw.proyecto.dto.pool.PoolResponse;
import co.javeriana.dw.proyecto.dto.proceso.CrearProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.ProcesoResponse;
import co.javeriana.dw.proyecto.dto.rolproceso.CrearRolProcesoRequest;
import co.javeriana.dw.proyecto.dto.rolproceso.RolProcesoResponse;
import co.javeriana.dw.proyecto.dto.usuario.CrearUsuarioRequest;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import co.javeriana.dw.proyecto.service.ArcoService;
import co.javeriana.dw.proyecto.service.EmpresaService;
import co.javeriana.dw.proyecto.service.EventoService;
import co.javeriana.dw.proyecto.service.LaneService;
import co.javeriana.dw.proyecto.service.PoolService;
import co.javeriana.dw.proyecto.service.ProcesoService;
import co.javeriana.dw.proyecto.service.RolProcesoService;
import co.javeriana.dw.proyecto.service.UsuarioService;

/**
 * Datos reproducibles para probar la API con JMeter sin utilizar Supabase.
 * Esta configuracion solo existe cuando se activa el perfil "jmeter".
 */
@Configuration
@Profile("jmeter")
public class DatosPruebaJMeterConfig {

    @Bean
    ApplicationRunner cargarDatosJMeter(
            EmpresaService empresaService,
            EmpresaRepository empresaRepository,
            UsuarioService usuarioService,
            ProcesoService procesoService,
            PoolService poolService,
            RolProcesoService rolProcesoService,
            LaneService laneService,
            EventoService eventoService,
            ArcoService arcoService) {

        return argumentos -> {
            EmpresaResponse empresa = empresaService.crear(new CrearEmpresaRequest(
                    "Procesos Javeriana",
                    "900123456-1",
                    "Procesos Javeriana S.A.S.",
                    "admin@jmeter.local",
                    "6015550101",
                    "Carrera 7 # 40-62",
                    "Educacion",
                    "Administrador JMeter",
                    "Admin123!"));

            EmpresaResponse empresaInvitada = empresaService.crear(new CrearEmpresaRequest(
                    "Proveedor JMeter",
                    "901987654-2",
                    "Proveedor JMeter S.A.S.",
                    "admin.proveedor@jmeter.local",
                    "6015550202",
                    "Calle 100 # 10-20",
                    "Servicios",
                    "Administrador Proveedor",
                    "Admin123!"));

            Empresa empresaEntidad = empresaRepository.findById(empresa.getId()).orElseThrow();

            usuarioService.invitarUsuario(
                    empresaEntidad,
                    new CrearUsuarioRequest("Editor de procesos", "editor@jmeter.local", RolUsuario.EDITOR));
            usuarioService.invitarUsuario(
                    empresaEntidad,
                    new CrearUsuarioRequest("Usuario de consulta", "lector@jmeter.local", RolUsuario.LECTURA));

            Long administradorId = empresa.getAdminUsuarioId();

            ProcesoResponse proceso = procesoService.crear(new CrearProcesoRequest(
                    empresa.getId(),
                    "Solicitud de compra",
                    "Proceso completo para validar consultas y rendimiento con JMeter",
                    "Compras"), administradorId);

            procesoService.crear(new CrearProcesoRequest(
                    empresa.getId(),
                    "Ingreso de colaboradores",
                    "Proceso alternativo para probar listados y paginacion",
                    "Talento humano"), administradorId);

            procesoService.crear(new CrearProcesoRequest(
                    empresaInvitada.getId(),
                    "Atencion de pedidos",
                    "Proceso perteneciente a una segunda empresa",
                    "Operaciones"), empresaInvitada.getAdminUsuarioId());

            List<PoolResponse> poolsIniciales = poolService.listarPorProceso(proceso.id());
            PoolResponse poolPropietario = poolsIniciales.stream()
                    .filter(PoolResponse::esPropietario)
                    .findFirst()
                    .orElseThrow();

            PoolResponse poolProveedor = poolService.crear(
                    new CrearPoolRequest(proceso.id(), "Proveedor externo", true),
                    administradorId);

            RolProcesoResponse solicitante = rolProcesoService.crear(
                    new CrearRolProcesoRequest(empresa.getId(), "Solicitante", "Crea la solicitud de compra"),
                    administradorId);
            RolProcesoResponse aprobador = rolProcesoService.crear(
                    new CrearRolProcesoRequest(empresa.getId(), "Aprobador", "Revisa y aprueba la solicitud"),
                    administradorId);

            laneService.crear(new CrearLaneRequest(poolPropietario.id(), solicitante.id()), administradorId);
            laneService.crear(new CrearLaneRequest(poolPropietario.id(), aprobador.id()), administradorId);

            EventoResponse inicio = eventoService.crear(new CrearEventoRequest(
                    proceso.id(), poolPropietario.id(), "Solicitud recibida", "INICIO", 100.0, 150.0),
                    administradorId);
            EventoResponse fin = eventoService.crear(new CrearEventoRequest(
                    proceso.id(), poolPropietario.id(), "Compra finalizada", "FIN", 700.0, 150.0),
                    administradorId);

            arcoService.crear(new CrearArcoRequest(
                    proceso.id(), inicio.id(), fin.id(), "Flujo principal", null),
                    administradorId);

            System.out.println("Datos JMeter cargados: empresaId=" + empresa.getId()
                    + ", usuarioAdminId=" + administradorId
                    + ", procesoId=" + proceso.id()
                    + ", poolPropietarioId=" + poolPropietario.id()
                    + ", poolExternoId=" + poolProveedor.id());
        };
    }

}
