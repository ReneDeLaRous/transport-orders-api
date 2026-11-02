package com.transportes.api.order;

import com.transportes.api.common.exception.BusinessRuleException;
import com.transportes.api.common.exception.ResourceNotFoundException;
import com.transportes.api.order.dto.OrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba solo la capa web (validaciones y formato de errores). La seguridad se desactiva aquí
 * y se prueba completa en ApiFlowIntegrationTest.
 */
@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        when(orderService.create(any())).thenReturn(
                new OrderResponse(id, OrderStatus.CREATED, "Puebla", "CDMX", now, now));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\":\"Puebla\",\"destination\":\"CDMX\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/orders/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createWithBlankOriginReturns400WithDetails() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\":\"  \",\"destination\":\"CDMX\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value("origin: el origen es obligatorio"));
    }

    @Test
    void createWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getUnknownOrderReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getById(id)).thenThrow(new ResourceNotFoundException("No existe la orden con id " + id));

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No existe la orden con id " + id))
                .andExpect(jsonPath("$.path").value("/api/orders/" + id));
    }

    @Test
    void getWithInvalidUuidReturns400() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", "no-es-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void changeStatusWithUnknownValueReturns400() throws Exception {
        mockMvc.perform(patch("/api/orders/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FLYING\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatusWithInvalidTransitionReturns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.changeStatus(id, OrderStatus.DELIVERED))
                .thenThrow(new BusinessRuleException("No se puede cambiar una orden de CREATED a DELIVERED"));

        mockMvc.perform(patch("/api/orders/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void listWithInvalidDateReturns400() throws Exception {
        mockMvc.perform(get("/api/orders").param("createdFrom", "ayer"))
                .andExpect(status().isBadRequest());
    }
}
