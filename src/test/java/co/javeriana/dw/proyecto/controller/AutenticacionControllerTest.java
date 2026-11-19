package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.config.SeguridadConfig;
import co.javeriana.dw.proyecto.dto.autenticacion.LoginRequest;
import co.javeriana.dw.proyecto.dto.autenticacion.SesionResponse;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.exception.CredencialesInvalidasException;
import co.javeriana.dw.proyecto.exception.ManejadorGlobalExcepciones;
import co.javeriana.dw.proyecto.exception.SesionInvalidaException;
import co.javeriana.dw.proyecto.security.JwtService;
import co.javeriana.dw.proyecto.service.AutenticacionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-03 a nivel HTTP: login, renovación y logout. Importa la configuración de seguridad
 * real para comprobar que los tres endpoints son públicos (no exigen access token).
 */
@WebMvcTest(AutenticacionController.class)
@Import({ManejadorGlobalExcepciones.class, SeguridadConfig.class})
class AutenticacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AutenticacionService autenticacionService;

    // SeguridadConfig lo necesita para construir el filtro JWT
    @MockBean
    private JwtService jwtService;

    private LoginRequest requestValido() {
        LoginRequest request = new LoginRequest();
        request.setEmail("juan@acme.com");
        request.setPassword("claveSegura123");
        return request;
    }

    private SesionResponse sesion(String access, String refresh) {
        SesionResponse sesion = new SesionResponse();
        sesion.setUsuarioId(3L);
        sesion.setEmail("juan@acme.com");
        sesion.setRolUsuario(RolUsuario.ADMIN);
        sesion.setEmpresaId(1L);
        sesion.setEmpresaNombre("Acme S.A.S.");
        sesion.setAccessToken(access);
        sesion.setExpiresIn(900);
        sesion.setRefreshToken(refresh);
        sesion.setRefreshExpiresIn(604800);
        return sesion;
    }

    @Test
    void login_devuelveAmbosTokens_cuandoCredencialesValidas() throws Exception {
        when(autenticacionService.iniciarSesion(any())).thenReturn(sesion("access-1", "refresh-1"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(3))
                .andExpect(jsonPath("$.empresaNombre").value("Acme S.A.S."))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("access-1"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").value("refresh-1"))
                .andExpect(jsonPath("$.refreshExpiresIn").value(604800));
    }

    @Test
    void login_devuelve400_cuandoFaltaPassword() throws Exception {
        LoginRequest request = requestValido();
        request.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_devuelve400_cuandoEmailInvalido() throws Exception {
        LoginRequest request = requestValido();
        request.setEmail("no-es-correo");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_devuelve401_cuandoCredencialesInvalidas() throws Exception {
        when(autenticacionService.iniciarSesion(any())).thenThrow(new CredencialesInvalidasException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_devuelveParNuevo_sinNecesitarAccessToken() throws Exception {
        when(autenticacionService.renovar("refresh-1")).thenReturn(sesion("access-2", "refresh-2"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"refresh-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-2"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-2"));
    }

    @Test
    void refresh_devuelve401_cuandoElRefreshTokenNoSirve() throws Exception {
        when(autenticacionService.renovar("vencido")).thenThrow(new SesionInvalidaException());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"vencido\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void refresh_devuelve400_cuandoNoSeEnviaElToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_devuelve204_yRevocaElRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"refresh-1\"}"))
                .andExpect(status().isNoContent());

        verify(autenticacionService).cerrarSesion("refresh-1");
    }
}
