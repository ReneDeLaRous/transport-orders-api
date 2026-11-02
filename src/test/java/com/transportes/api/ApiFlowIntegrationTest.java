package com.transportes.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorre el flujo completo con la app levantada (H2 en memoria, seguridad real y JWT):
 * registro, conductor, orden, asignación, archivos, cambios de estado y filtros.
 */
@SpringBootTest(properties = "app.storage.dir=target/test-uploads")
@AutoConfigureMockMvc
class ApiFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectedRoutesNeedAToken() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer token.invalido.xyz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerAndLoginWork() throws Exception {
        String body = "{\"username\":\"maria\",\"password\":\"clave-segura-1\"}";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"maria\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullTransportFlow() throws Exception {
        String auth = "Bearer " + registerAndGetToken("flujo.completo");

        // Conductores: uno activo y uno inactivo
        String activeDriverId = createDriver(auth, "Ana Perez", "LIC-001", true);
        String inactiveDriverId = createDriver(auth, "Luis Gomez", "LIC-002", false);

        mockMvc.perform(post("/api/drivers").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Otra Persona\",\"licenseNumber\":\"lic-001\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/drivers/active").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Ana Perez"));

        // Órdenes
        String orderId = createOrder(auth, "Puebla", "Ciudad de Mexico");
        String otherOrderId = createOrder(auth, "Monterrey", "Guadalajara");

        mockMvc.perform(get("/api/orders/{id}", orderId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.origin").value("Puebla"));

        // Asignación: conductor inactivo no, activo sí, y no se puede asignar dos veces
        mockMvc.perform(post("/api/orders/{id}/assignment", orderId).header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"" + inactiveDriverId + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/orders/{id}/assignment", orderId).header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"" + activeDriverId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.driverName").value("Ana Perez"));

        mockMvc.perform(post("/api/orders/{id}/assignment", orderId).header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"" + activeDriverId + "\"}"))
                .andExpect(status().isConflict());

        // Archivos
        MockMultipartFile pdf = new MockMultipartFile("file", "guia.pdf", "application/pdf", "%PDF-1.4 demo".getBytes());
        mockMvc.perform(multipart("/api/orders/{id}/assignment/pdf", orderId).file(pdf)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdfFile").isNotEmpty());

        MockMultipartFile notAPdf = new MockMultipartFile("file", "notas.txt", "text/plain", "hola".getBytes());
        mockMvc.perform(multipart("/api/orders/{id}/assignment/pdf", orderId).file(notAPdf)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isBadRequest());

        MockMultipartFile png = new MockMultipartFile("file", "evidencia.png", "image/png", new byte[]{1, 2, 3, 4});
        mockMvc.perform(multipart("/api/orders/{id}/assignment/image", orderId).file(png)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageFile").isNotEmpty())
                .andExpect(jsonPath("$.pdfFile").isNotEmpty());

        MockMultipartFile gif = new MockMultipartFile("file", "movimiento.gif", "image/gif", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/orders/{id}/assignment/image", orderId).file(gif)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isBadRequest());

        // Archivos en una orden sin asignación
        mockMvc.perform(multipart("/api/orders/{id}/assignment/pdf", otherOrderId).file(pdf)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());

        // Cambios de estado
        mockMvc.perform(patch("/api/orders/{id}/status", orderId).header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/orders/{id}/status", orderId).header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_TRANSIT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));

        mockMvc.perform(patch("/api/orders/{id}/status", orderId).header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isOk());

        // Ya no se puede asignar conductor a una orden que no está en CREATED
        mockMvc.perform(post("/api/orders/{id}/assignment", orderId).header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"" + activeDriverId + "\"}"))
                .andExpect(status().isConflict());

        // Filtros del listado
        mockMvc.perform(get("/api/orders").param("status", "DELIVERED").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(orderId));

        mockMvc.perform(get("/api/orders").param("origin", "monter").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(otherOrderId));

        mockMvc.perform(get("/api/orders").param("destination", "GUADALAJARA").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/orders").param("createdFrom", "2020-01-01").param("createdTo", "2099-12-31")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/orders").param("createdTo", "2020-01-01").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String registerAndGetToken(String username) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"clave-segura-1\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return readField(result, "token");
    }

    private String createDriver(String auth, String name, String license, boolean active) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"licenseNumber\":\"" + license + "\",\"active\":" + active + "}";
        MvcResult result = mockMvc.perform(post("/api/drivers").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return readField(result, "id");
    }

    private String createOrder(String auth, String origin, String destination) throws Exception {
        String body = "{\"origin\":\"" + origin + "\",\"destination\":\"" + destination + "\"}";
        MvcResult result = mockMvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        assertEquals("CREATED", readField(result, "status"));
        return readField(result, "id");
    }

    private String readField(MvcResult result, String field) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode value = json.get(field);
        assertNotNull(value, "Falta el campo " + field + " en la respuesta");
        return value.asText();
    }
}
