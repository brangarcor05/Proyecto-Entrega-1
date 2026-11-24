package co.javeriana.dw.proyecto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Escenarios de seguridad entre organizaciones con la aplicacion completa (H2 en memoria)
 * y tokens reales: la empresa B no puede ver ni modificar lo de la empresa A, ni siquiera
 * mandando en la URL el usuarioId o el empresaId de A. Tambien cubre las reglas de
 * modelado y de historial de actividades, gateways y mensajes.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AislamientoEmpresasIntegracionTest {

    private static final String PASSWORD = "claveSegura123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode sesionA;
    private JsonNode sesionB;
    private long procesoA;
    private long poolPropietarioA;

    @BeforeEach
    void prepararDosEmpresas() throws Exception {
        sesionA = login(registrarEmpresa());
        sesionB = login(registrarEmpresa());

        procesoA = leer(enviar(post("/api/procesos"), sesionA,
                Map.of("nombre", "Compras", "descripcion", "Flujo de compras", "categoria", "Finanzas"),
                status().isCreated())).get("id").asLong();

        JsonNode pools = leer(mockMvc.perform(get("/api/pools").param("procesoId", String.valueOf(procesoA))
                        .header("Authorization", bearer(sesionA)))
                .andExpect(status().isOk()).andReturn());
        poolPropietarioA = pools.get(0).get("id").asLong();
    }

    /* ------------------------------------------------------------------
       Lectura: un proceso ajeno no compartido "no existe" para la otra empresa
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("B no ve el proceso de A, ni pidiendo el empresaId de A en la URL")
    void otraEmpresaNoConsultaElProceso() throws Exception {
        String empresaA = sesionA.get("empresaId").asText();

        mockMvc.perform(get("/api/procesos/" + procesoA).header("Authorization", bearer(sesionB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/procesos/" + procesoA).param("empresaId", empresaA)
                        .header("Authorization", bearer(sesionB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/procesos/" + procesoA + "/historial").header("Authorization", bearer(sesionB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/procesos/" + procesoA + "/diagrama").header("Authorization", bearer(sesionB)))
                .andExpect(status().isNotFound());

        JsonNode listado = leer(mockMvc.perform(get("/api/procesos").param("empresaId", empresaA)
                        .header("Authorization", bearer(sesionB)))
                .andExpect(status().isOk()).andReturn());
        assertThat(listado.get("totalElements").asLong()).isZero();
    }

    @Test
    @DisplayName("B no lista pools, actividades, gateways, mensajes, arcos ni eventos del proceso de A")
    void otraEmpresaNoConsultaElDiagrama() throws Exception {
        String id = String.valueOf(procesoA);
        String token = bearer(sesionB);

        mockMvc.perform(get("/api/pools").param("procesoId", id).header("Authorization", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/pools/" + poolPropietarioA).header("Authorization", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/lanes").param("poolId", String.valueOf(poolPropietarioA))
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/arcos").param("procesoId", id).header("Authorization", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/eventos").param("procesoId", id).header("Authorization", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/actividades/proceso/" + id).header("Authorization", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/gateways/proceso/" + id).header("Authorization", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/eventos-mensaje-envio/proceso/" + id).header("Authorization", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/eventos-mensaje-recepcion/proceso/" + id).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    /* ------------------------------------------------------------------
       Escritura: el usuarioId de la URL ya no permite suplantar a nadie
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("B no edita ni elimina el proceso de A aunque mande el usuarioId del admin de A")
    void otraEmpresaNoSuplantaAlAdministrador() throws Exception {
        String adminA = sesionA.get("usuarioId").asText();

        enviar(put("/api/procesos/" + procesoA).param("usuarioId", adminA), sesionB,
                Map.of("nombre", "Hackeado", "descripcion", "x", "categoria", "x", "estado", "PUBLICADO"),
                status().isForbidden());

        mockMvc.perform(delete("/api/procesos/" + procesoA).param("usuarioId", adminA)
                        .header("Authorization", bearer(sesionB)))
                .andExpect(status().isForbidden());

        // El proceso sigue intacto para A
        JsonNode proceso = leer(mockMvc.perform(get("/api/procesos/" + procesoA)
                        .header("Authorization", bearer(sesionA)))
                .andExpect(status().isOk()).andReturn());
        assertThat(proceso.get("nombre").asText()).isEqualTo("Compras");
    }

    @Test
    @DisplayName("B no crea gateways ni mensajes en el proceso de A")
    void otraEmpresaNoModificaElDiagrama() throws Exception {
        enviar(post("/api/gateways"), sesionB,
                Map.of("procesoId", procesoA, "poolId", poolPropietarioA, "tipo", "EXCLUSIVO"),
                status().isForbidden());

        long poolCliente = crearPool(sesionA, "Cliente");
        enviar(post("/api/eventos-mensaje-envio"), sesionB,
                Map.of("procesoId", procesoA, "poolId", poolPropietarioA,
                        "poolDestinoId", poolCliente, "nombreMensaje", "Pedido"),
                status().isForbidden());
    }

    @Test
    @DisplayName("Crear un proceso ignora el empresaId del cliente: queda en la empresa del token")
    void crearProcesoUsaLaEmpresaDelToken() throws Exception {
        Map<String, Object> cuerpo = new HashMap<>(Map.of(
                "nombre", "Intento", "descripcion", "d", "categoria", "c"));
        cuerpo.put("empresaId", sesionA.get("empresaId").asLong());

        JsonNode creado = leer(enviar(post("/api/procesos"), sesionB, cuerpo, status().isCreated()));

        // A no lo ve; B si
        mockMvc.perform(get("/api/procesos/" + creado.get("id").asLong())
                        .header("Authorization", bearer(sesionA)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/procesos/" + creado.get("id").asLong())
                        .header("Authorization", bearer(sesionB)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una empresa solo consulta sus propios datos y el directorio no la incluye")
    void consultaDeEmpresas() throws Exception {
        String empresaA = sesionA.get("empresaId").asText();
        String empresaB = sesionB.get("empresaId").asText();

        mockMvc.perform(get("/api/empresas/" + empresaA).header("Authorization", bearer(sesionB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/empresas/" + empresaB).header("Authorization", bearer(sesionB)))
                .andExpect(status().isOk());

        JsonNode directorio = leer(mockMvc.perform(get("/api/empresas").header("Authorization", bearer(sesionB)))
                .andExpect(status().isOk()).andReturn());
        for (JsonNode empresa : directorio) {
            assertThat(empresa.get("id").asText()).isNotEqualTo(empresaB);
            assertThat(empresa.path("email").isNull() || empresa.path("email").isMissingNode()).isTrue();
        }
    }

    /* ------------------------------------------------------------------
       Reglas de modelado e historial en la propia empresa
       ------------------------------------------------------------------ */

    @Test
    @DisplayName("HU-25: un mensaje dentro del mismo pool se rechaza; entre pools se acepta")
    void mensajeDebeCruzarDePoolAPool() throws Exception {
        enviar(post("/api/eventos-mensaje-envio"), sesionA,
                Map.of("procesoId", procesoA, "poolId", poolPropietarioA,
                        "poolDestinoId", poolPropietarioA, "nombreMensaje", "Pedido"),
                status().isConflict());

        long poolCliente = crearPool(sesionA, "Cliente");
        enviar(post("/api/eventos-mensaje-envio"), sesionA,
                Map.of("procesoId", procesoA, "poolId", poolPropietarioA,
                        "poolDestinoId", poolCliente, "nombreMensaje", "Pedido"),
                status().isCreated());
    }

    @Test
    @DisplayName("HU-08: nombre unico, lane del mismo pool e historial de la actividad")
    void reglasDeActividad() throws Exception {
        long rol = leer(enviar(post("/api/roles-proceso"), sesionA,
                Map.of("nombre", "Analista " + UUID.randomUUID().toString().substring(0, 6)),
                status().isCreated())).get("id").asLong();
        long lane = leer(enviar(post("/api/lanes"), sesionA,
                Map.of("poolId", poolPropietarioA, "rolProcesoId", rol),
                status().isCreated())).get("id").asLong();

        Map<String, Object> actividad = Map.of("procesoId", procesoA, "poolId", poolPropietarioA,
                "laneId", lane, "nombre", "Revisar solicitud", "tipo", "USUARIO");
        enviar(post("/api/actividades"), sesionA, actividad, status().isCreated());
        // Mismo nombre en el mismo proceso
        enviar(post("/api/actividades"), sesionA, actividad, status().isConflict());

        // Lane de un pool distinto al indicado
        long poolCliente = crearPool(sesionA, "Proveedor");
        enviar(post("/api/actividades"), sesionA, Map.of("procesoId", procesoA, "poolId", poolCliente,
                "laneId", lane, "nombre", "Otra", "tipo", "USUARIO"), status().isConflict());

        JsonNode historial = leer(mockMvc.perform(get("/api/procesos/" + procesoA + "/historial")
                        .header("Authorization", bearer(sesionA)))
                .andExpect(status().isOk()).andReturn());
        boolean registrada = false;
        for (JsonNode entrada : historial) {
            if ("Actividad".equals(entrada.get("entidadTipo").asText())
                    && "CREACION".equals(entrada.get("accion").asText())) {
                registrada = true;
            }
        }
        assertThat(registrada).as("La creacion de la actividad debe quedar en el historial").isTrue();
    }

    /* ------------------------------------------------------------------
       Utilidades
       ------------------------------------------------------------------ */

    private String registrarEmpresa() throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String correo = "admin-" + sufijo + "@empresa.com";
        Map<String, String> empresa = Map.of(
                "nombre", "Empresa " + sufijo,
                "rut", "RUT-" + sufijo,
                "razonSocial", "Empresa " + sufijo + " S.A.S.",
                "email", correo,
                "adminNombre", "Admin " + sufijo,
                "adminPassword", PASSWORD);

        mockMvc.perform(post("/api/empresas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(empresa)))
                .andExpect(status().isCreated());
        return correo;
    }

    private JsonNode login(String correo) throws Exception {
        return leer(mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", correo, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn());
    }

    private long crearPool(JsonNode sesion, String nombre) throws Exception {
        return leer(enviar(post("/api/pools"), sesion,
                Map.of("procesoId", procesoA, "nombre", nombre + " " + UUID.randomUUID().toString().substring(0, 4),
                        "cajaNegra", false),
                status().isCreated())).get("id").asLong();
    }

    private MvcResult enviar(MockHttpServletRequestBuilder peticion, JsonNode sesion, Map<String, ?> cuerpo,
                             ResultMatcher estadoEsperado) throws Exception {
        return mockMvc.perform(peticion
                        .header("Authorization", bearer(sesion))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(estadoEsperado)
                .andReturn();
    }

    private JsonNode leer(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    private String bearer(JsonNode sesion) {
        return "Bearer " + sesion.get("accessToken").asText();
    }
}
