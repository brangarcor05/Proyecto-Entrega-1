package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.config.SeguridadConfig;
import co.javeriana.dw.proyecto.dto.usuario.CambiarRolRequest;
import co.javeriana.dw.proyecto.dto.usuario.CrearUsuarioRequest;
import co.javeriana.dw.proyecto.dto.usuario.UsuarioResponse;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.exception.ManejadorGlobalExcepciones;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.security.JwtService;
import co.javeriana.dw.proyecto.security.UsuarioAutenticado;
import co.javeriana.dw.proyecto.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de HU-02 a nivel HTTP: invitar, cambiar rol, desactivar y listar usuarios.
 *
 * El controlador toma la empresa del usuario autenticado (@AuthenticationPrincipal
 * UsuarioAutenticado), así que las peticiones se simulan con ese principal, igual que
 * lo deja el filtro JWT. @WithMockUser no sirve aquí: crea un usuario genérico de
 * Spring y el controlador recibiría null.
 */
@WebMvcTest(UsuarioController.class)
@Import({ManejadorGlobalExcepciones.class, SeguridadConfig.class})
class UsuarioControllerTest {

    private static final long EMPRESA_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService usuarioService;

    // SeguridadConfig lo necesita para construir el filtro JWT
    @MockBean
    private JwtService jwtService;

    /** Simula la petición de un usuario ya autenticado con su access token. */
    private RequestPostProcessor como(RolUsuario rol) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(3L, EMPRESA_ID, "u@acme.com", rol);
        return authentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))));
    }

    private CrearUsuarioRequest requestValido() {
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombre("Ana Ruiz");
        request.setEmail("ana@acme.com");
        request.setRolUsuario(RolUsuario.EDITOR);
        request.setPassword("claveSegura123");
        return request;
    }

    @Test
    void sinToken_responde401() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invitar_devuelve201_cuandoRequestValido() throws Exception {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(20L);
        response.setNombre("Ana Ruiz");
        response.setRolUsuario(RolUsuario.EDITOR);
        when(usuarioService.invitarUsuario(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/usuarios")
                        .with(como(RolUsuario.ADMIN))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.rolUsuario").value("EDITOR"));
    }

    @Test
    void invitar_usaLaEmpresaDelToken_eIgnoraLaQueMandeElCliente() throws Exception {
        when(usuarioService.invitarUsuario(any(), any())).thenReturn(new UsuarioResponse());

        // El cliente intenta crear el usuario en la empresa 2; se crea en la 1 (la del token)
        mockMvc.perform(post("/api/usuarios")
                        .with(como(RolUsuario.ADMIN))
                        .param("empresaId", "2")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated());

        verify(usuarioService).invitarUsuario(argThat(empresa -> empresa.getId() == EMPRESA_ID), any());
    }

    @Test
    void invitar_editor_responde403_yNoLlegaAlServicio() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .with(como(RolUsuario.EDITOR))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).invitarUsuario(any(), any());
    }

    @Test
    void invitar_devuelve400_cuandoEmailInvalido() throws Exception {
        CrearUsuarioRequest request = requestValido();
        request.setEmail("no-es-correo");

        mockMvc.perform(post("/api/usuarios")
                        .with(como(RolUsuario.ADMIN))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitar_devuelve400_cuandoFaltaRolAcceso() throws Exception {
        CrearUsuarioRequest request = requestValido();
        request.setRolUsuario(null);

        mockMvc.perform(post("/api/usuarios")
                        .with(como(RolUsuario.ADMIN))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitar_devuelve409_cuandoCorreoDuplicado() throws Exception {
        when(usuarioService.invitarUsuario(any(), any()))
                .thenThrow(new NombreDuplicadoException("El correo ya esta registrado"));

        mockMvc.perform(post("/api/usuarios")
                        .with(como(RolUsuario.ADMIN))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict());
    }

    @Test
    void cambiarRol_devuelve200_cuandoValido() throws Exception {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(5L);
        response.setRolUsuario(RolUsuario.ADMIN);
        when(usuarioService.cambiarRolAcceso(eq(5L), eq(EMPRESA_ID), any())).thenReturn(response);

        CambiarRolRequest request = new CambiarRolRequest();
        request.setRolUsuario(RolUsuario.ADMIN);

        mockMvc.perform(patch("/api/usuarios/5/rol")
                        .with(como(RolUsuario.ADMIN))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolUsuario").value("ADMIN"));
    }

    @Test
    void cambiarRol_devuelve404_cuandoUsuarioNoExiste() throws Exception {
        when(usuarioService.cambiarRolAcceso(eq(99L), eq(EMPRESA_ID), any()))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado"));

        CambiarRolRequest request = new CambiarRolRequest();
        request.setRolUsuario(RolUsuario.ADMIN);

        mockMvc.perform(patch("/api/usuarios/99/rol")
                        .with(como(RolUsuario.ADMIN))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambiarRol_devuelve400_cuandoFaltaRol() throws Exception {
        mockMvc.perform(patch("/api/usuarios/5/rol")
                        .with(como(RolUsuario.ADMIN))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CambiarRolRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarRol_lectura_responde403() throws Exception {
        CambiarRolRequest request = new CambiarRolRequest();
        request.setRolUsuario(RolUsuario.ADMIN);

        mockMvc.perform(patch("/api/usuarios/5/rol")
                        .with(como(RolUsuario.LECTURA))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).cambiarRolAcceso(anyLong(), anyLong(), any());
    }

    @Test
    void desactivar_devuelve204_conLaEmpresaDelToken() throws Exception {
        mockMvc.perform(delete("/api/usuarios/7").with(como(RolUsuario.ADMIN)))
                .andExpect(status().isNoContent());

        verify(usuarioService).desactivar(7L, EMPRESA_ID);
    }

    @Test
    void listar_cualquierRol_veSoloLosUsuariosDeSuEmpresa() throws Exception {
        UsuarioResponse u1 = new UsuarioResponse();
        u1.setId(1L);
        when(usuarioService.listarPorEmpresa(EMPRESA_ID)).thenReturn(List.of(u1));

        mockMvc.perform(get("/api/usuarios")
                        .param("empresaId", "2")
                        .with(como(RolUsuario.LECTURA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(usuarioService).listarPorEmpresa(EMPRESA_ID);
    }
}
