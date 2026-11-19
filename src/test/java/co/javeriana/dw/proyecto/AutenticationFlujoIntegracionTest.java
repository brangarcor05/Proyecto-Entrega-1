package co.javeriana.dw.proyecto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración de HU-03 con la aplicación completa (H2 en memoria): registro de
 * empresa, login, uso del access token, renovación con el refresh token, rotación y logout.
 * También comprueba que los roles de proceso se crean en la empresa del token.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutenticacionFlujoIntegracionTest {

    private static final String PASSWORD = "claveSegura123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Registra una empresa nueva (HU-01) y devuelve el correo de su administrador. */
    private String registrarEmpresa() throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String correo = "admin-" + sufijo + "@acme.com";
        Map<String, String> empresa = Map.of(
                "nombre", "Acme " + sufijo,
                "rut", "RUT-" + sufijo,
                "razonSocial", "Acme " + sufijo + " S.A.S.",
                "email", correo,
                "adminNombre", "Ana Admin",
                "adminPassword", PASSWORD);

        mockMvc.perform(post("/api/empresas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(empresa)))
                .andExpect(status().isCreated());
        return correo;
    }

    private JsonNode login(String correo) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", correo, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    private MvcResult refresh(String refreshToken, int estadoEsperado) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().is(estadoEsperado))
                .andReturn();
    }

    private String bearer(JsonNode sesion) {
        return "Bearer " + sesion.get("accessToken").asText();
    }

    @Test
    @DisplayName("Login entrega ambos tokens y el access token abre la API")
    void loginYUsoDelAccessToken() throws Exception {
        JsonNode sesion = login(registrarEmpresa());

        assertThat(sesion.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(sesion.get("accessToken").asText()).isNotBlank();
        assertThat(sesion.get("refreshToken").asText()).isNotBlank();
        assertThat(sesion.get("expiresIn").asLong()).isEqualTo(15 * 60);
        assertThat(sesion.get("refreshExpiresIn").asLong()).isEqualTo(7 * 24 * 60 * 60);

        mockMvc.perform(get("/api/roles-proceso").header("Authorization", bearer(sesion)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sin token, con un token inventado o con el refresh token en la cabecera: 401")
    void accesoSinTokenValido() throws Exception {
        JsonNode sesion = login(registrarEmpresa());

        mockMvc.perform(get("/api/roles-proceso"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/roles-proceso").header("Authorization", "Bearer inventado"))
                .andExpect(status().isUnauthorized());
        // El refresh token no sirve como access token
        mockMvc.perform(get("/api/roles-proceso")
                        .header("Authorization", "Bearer " + sesion.get("refreshToken").asText()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El refresh token se cambia por un par nuevo y el anterior deja de servir")
    void renovacionConRotacion() throws Exception {
        JsonNode sesion = login(registrarEmpresa());
        String refreshOriginal = sesion.get("refreshToken").asText();

        JsonNode renovada = objectMapper.readTree(
                refresh(refreshOriginal, 200).getResponse().getContentAsString());

        assertThat(renovada.get("refreshToken").asText()).isNotEqualTo(refreshOriginal);
        mockMvc.perform(get("/api/roles-proceso").header("Authorization", bearer(renovada)))
                .andExpect(status().isOk());

        // El refresh anterior ya fue usado: se rechaza
        refresh(refreshOriginal, 401);
        // Al ser una repetición inmediata (posible carrera entre pestañas) no se cierran
        // las demás sesiones: el refresh nuevo sigue sirviendo
        refresh(renovada.get("refreshToken").asText(), 200);
    }

    @Test
    @DisplayName("Logout revoca el refresh token")
    void logoutRevocaElRefreshToken() throws Exception {
        JsonNode sesion = login(registrarEmpresa());
        String refreshToken = sesion.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isNoContent());

        refresh(refreshToken, 401);
    }

    @Test
    @DisplayName("Los roles de proceso se crean en la empresa del token y solo se ven en ella")
    void rolesDeProcesoAisladosPorEmpresa() throws Exception {
        JsonNode empresaA = login(registrarEmpresa());
        JsonNode empresaB = login(registrarEmpresa());

        mockMvc.perform(post("/api/roles-proceso")
                        .header("Authorization", bearer(empresaA))
                        .contentType("application/json")
                        .content("{\"nombre\":\"Analista\",\"descripcion\":\"Revisa solicitudes\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.empresaId").value(empresaA.get("empresaId").asLong()));

        // La empresa B no ve el rol de A, aunque pida explícitamente la empresa de A
        mockMvc.perform(get("/api/roles-proceso")
                        .param("empresaId", empresaA.get("empresaId").asText())
                        .header("Authorization", bearer(empresaB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/roles-proceso").header("Authorization", bearer(empresaA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Analista"));
    }
}
