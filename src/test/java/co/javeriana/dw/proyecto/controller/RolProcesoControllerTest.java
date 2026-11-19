package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.config.SeguridadConfig;
import co.javeriana.dw.proyecto.dto.rolproceso.RolProcesoResponse;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.exception.ManejadorGlobalExcepciones;
import co.javeriana.dw.proyecto.security.JwtService;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import co.javeriana.dw.proyecto.service.RolProcesoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Roles de proceso (HU-17 a HU-20) a nivel HTTP: la empresa y el usuario salen del token,
 * consultar está abierto a cualquier rol de acceso y modificar es solo del ADMIN.
 */
@WebMvcTest(RolProcesoController.class)
@Import({ManejadorGlobalExcepciones.class, SeguridadConfig.class})
class RolProcesoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RolProcesoService rolProcesoService;

    @MockBean
    private JwtService jwtService;

    /** Simula la petición de un usuario ya autenticado con su access token. */
    private RequestPostProcessor como(RolUsuario rol, long usuarioId, long empresaId) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(usuarioId, empresaId, "u@acme.com", rol);
        return authentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))));
    }

    private RolProcesoResponse rol() {
        return new RolProcesoResponse(9L, 1L, "Analista", "Revisa", true, false, List.of());
    }

    @Test
    void sinToken_responde401() throws Exception {
        mockMvc.perform(get("/api/roles-proceso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultar_usaLaEmpresaDelToken_eIgnoraLaQueMandeElCliente() throws Exception {
        when(rolProcesoService.consultar(eq(1L), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(rol())));

        // Aunque el cliente intente pedir los roles de la empresa 2, se consultan los de la 1
        mockMvc.perform(get("/api/roles-proceso").param("empresaId", "2")
                        .with(como(RolUsuario.LECTURA, 5L, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Analista"));

        verify(rolProcesoService).consultar(eq(1L), isNull(), any());
        verify(rolProcesoService, never()).consultar(eq(2L), any(), any());
    }

    @Test
    void buscarPorId_usaLaEmpresaDelToken() throws Exception {
        when(rolProcesoService.obtener(9L, 1L)).thenReturn(rol());

        mockMvc.perform(get("/api/roles-proceso/9").with(como(RolUsuario.EDITOR, 5L, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void crear_admin_responde201_conElUsuarioDelToken() throws Exception {
        when(rolProcesoService.crear(any(), eq(4L))).thenReturn(rol());

        mockMvc.perform(post("/api/roles-proceso")
                        .with(como(RolUsuario.ADMIN, 4L, 1L))
                        .contentType("application/json")
                        .content("{\"nombre\":\"Analista\",\"descripcion\":\"Revisa\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Analista"));

        verify(rolProcesoService).crear(any(), eq(4L));
    }

    @Test
    void crear_editor_responde403_yNoLlegaAlServicio() throws Exception {
        mockMvc.perform(post("/api/roles-proceso")
                        .with(como(RolUsuario.EDITOR, 5L, 1L))
                        .contentType("application/json")
                        .content("{\"nombre\":\"Analista\"}"))
                .andExpect(status().isForbidden());

        verify(rolProcesoService, never()).crear(any(), anyLong());
    }

    @Test
    void crear_sinNombre_responde400() throws Exception {
        mockMvc.perform(post("/api/roles-proceso")
                        .with(como(RolUsuario.ADMIN, 4L, 1L))
                        .contentType("application/json")
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizar_lectura_responde403() throws Exception {
        mockMvc.perform(put("/api/roles-proceso/9")
                        .with(como(RolUsuario.LECTURA, 6L, 1L))
                        .contentType("application/json")
                        .content("{\"nombre\":\"Analista senior\"}"))
                .andExpect(status().isForbidden());

        verify(rolProcesoService, never()).actualizar(anyLong(), any(), anyLong());
    }

    @Test
    void eliminar_admin_responde204_conElUsuarioDelToken() throws Exception {
        mockMvc.perform(delete("/api/roles-proceso/9").with(como(RolUsuario.ADMIN, 4L, 1L)))
                .andExpect(status().isNoContent());

        verify(rolProcesoService).eliminar(9L, 4L);
    }

    @Test
    void eliminar_editor_responde403() throws Exception {
        mockMvc.perform(delete("/api/roles-proceso/9").with(como(RolUsuario.EDITOR, 5L, 1L)))
                .andExpect(status().isForbidden());

        verify(rolProcesoService, never()).eliminar(anyLong(), anyLong());
    }
}
