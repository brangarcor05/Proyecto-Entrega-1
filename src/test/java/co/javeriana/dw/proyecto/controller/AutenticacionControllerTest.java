package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.autenticacion.LoginRequest;
import co.javeriana.dw.proyecto.dto.autenticacion.SesionResponse;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.exception.CredencialesInvalidasException;
import co.javeriana.dw.proyecto.exception.ManejadorGlobalExcepciones;
import co.javeriana.dw.proyecto.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de HU-03 a nivel HTTP: login exitoso, validacion de entrada, y el
 * mensaje generico cuando las credenciales son invalidas (sin revelar si el
 * correo existe, tal como pide la HU).
 */
@WebMvcTest(AutenticacionController.class)
@Import(ManejadorGlobalExcepciones.class)
class AutenticacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService usuarioService;

    private LoginRequest requestValido() {
        LoginRequest request = new LoginRequest();
        request.setEmail("juan@acme.com");
        request.setPassword("claveSegura123");
        return request;
    }

    @Test
    void login_devuelve200_cuandoCredencialesValidas() throws Exception {
        SesionResponse sesion = new SesionResponse();
        sesion.setUsuarioId(3L);
        sesion.setEmail("juan@acme.com");
        sesion.setRolUsuario(RolUsuario.ADMIN);
        sesion.setEmpresaId(1L);
        sesion.setEmpresaNombre("Acme S.A.S.");
        when(usuarioService.autenticar(any())).thenReturn(sesion);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(3))
                .andExpect(jsonPath("$.empresaNombre").value("Acme S.A.S."));
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
        when(usuarioService.autenticar(any())).thenThrow(new CredencialesInvalidasException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isUnauthorized());
    }
}