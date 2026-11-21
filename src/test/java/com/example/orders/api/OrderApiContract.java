package com.example.orders.api;

import com.example.orders.domain.OrderStatus;
import com.example.orders.domain.event.OrderCreatedEvent;
import com.example.orders.domain.event.OrderStatusChangedEvent;
import com.example.orders.repository.OrderRepository;
import com.example.orders.service.command.CancelOrderCommand;
import com.example.orders.service.command.ChangeOrderStatusCommand;
import com.example.orders.service.command.CommandBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Test contract: runs on H2 (*H2Test, mvn test) and PostgreSQL (*PostgresIT, mvn verify).
/** End-to-end: HTTP → Facade → CommandBus → Handlers → State → DB, plus Observer events. */
@SpringBootTest(properties = "orders.processing.enabled=false")
@AutoConfigureMockMvc
@RecordApplicationEvents
public abstract class OrderApiContract {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired OrderRepository repository;
    @Autowired CommandBus commandBus;
    @Autowired ApplicationEvents events;

    private static final String VALID_ORDER = """
            {
              "customerId": "cust-42",
              "items": [
                {"productId": "sku-1", "productName": "Keyboard", "quantity": 2, "unitPrice": 49.99},
                {"productId": "sku-2", "productName": "Mouse",    "quantity": 1, "unitPrice": 20.02}
              ]
            }
            """;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private long createOrder() throws Exception {
        return createOrder(VALID_ORDER);
    }

    private long createOrder(String body) throws Exception {
        String response = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    // ---------- Create ----------

    @Test
    void createOrderWithMultipleItems() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/orders/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.customerId").value("cust-42"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.cancellable").value(true))
                .andExpect(jsonPath("$.allowedNextStatuses", contains("PROCESSING", "CANCELLED")))
                .andExpect(jsonPath("$.totalAmount").value(120.00))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].lineTotal").value(99.98));
    }

    @Test
    void createPublishesOrderCreatedEvent() throws Exception {
        long id = createOrder();
        assertThat(events.stream(OrderCreatedEvent.class))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.orderId()).isEqualTo(id);
                    assertThat(e.customerId()).isEqualTo("cust-42");
                });
    }

    @Test
    void createOrderRejectsEmptyItems() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.items").exists());
    }

    @Test
    void createOrderRejectsInvalidItem() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId":"c","items":[{"productId":"p","productName":"x","quantity":0,"unitPrice":-1}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists())
                .andExpect(jsonPath("$.errors['items[0].unitPrice']").exists());
    }

    @Test
    void validationChainRejectsDuplicateProducts() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId":"c","items":[
                          {"productId":"p","productName":"x","quantity":1,"unitPrice":1},
                          {"productId":"p","productName":"x","quantity":2,"unitPrice":1}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Order rejected"))
                .andExpect(jsonPath("$.detail", containsString("appears more than once")));
        assertThat(repository.count()).isZero();
    }

    @Test
    void createOrderRejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Retrieve ----------

    @Test
    void getOrderById() throws Exception {
        long id = createOrder();
        mvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].productName", containsInAnyOrder("Keyboard", "Mouse")));
    }

    @Test
    void getUnknownOrderReturns404() throws Exception {
        mvc.perform(get("/api/orders/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order not found"));
    }

    // ---------- List (Specification) ----------

    @Test
    void listAllAndFilterByStatus() throws Exception {
        createOrder();
        createOrder();
        long c = createOrder();
        commandBus.dispatch(new CancelOrderCommand(c));

        mvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].items", hasSize(2)));

        mvc.perform(get("/api/orders").param("status", "PENDING"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].status", everyItem(is("PENDING"))));

        mvc.perform(get("/api/orders").param("status", "CANCELLED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(c))
                .andExpect(jsonPath("$.content[0].cancellable").value(false));

        mvc.perform(get("/api/orders").param("status", "SHIPPED"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void filtersCombineStatusAndCustomer() throws Exception {
        createOrder();
        createOrder(VALID_ORDER.replace("cust-42", "cust-7"));
        long other = createOrder(VALID_ORDER.replace("cust-42", "cust-7"));
        commandBus.dispatch(new CancelOrderCommand(other));

        mvc.perform(get("/api/orders").param("customerId", "cust-7"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/orders").param("customerId", "cust-7").param("status", "PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customerId").value("cust-7"));
    }

    @Test
    void listSupportsPaging() throws Exception {
        for (int i = 0; i < 5; i++) createOrder();
        mvc.perform(get("/api/orders").param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void listRejectsUnknownStatusAndBadPageSize() throws Exception {
        mvc.perform(get("/api/orders").param("status", "LOST")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/orders").param("size", "1000")).andExpect(status().isBadRequest());
    }

    // ---------- Update status (State) ----------

    @Test
    void walkFullLifecycleAndPublishEvents() throws Exception {
        long id = createOrder();
        for (String s : new String[]{"PROCESSING", "SHIPPED", "DELIVERED"}) {
            mvc.perform(patch("/api/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + s + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(s));
        }
        mvc.perform(get("/api/orders/{id}", id))
                .andExpect(jsonPath("$.allowedNextStatuses", empty()));
        assertThat(events.stream(OrderStatusChangedEvent.class).map(OrderStatusChangedEvent::to))
                .containsExactly(OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
    }

    @Test
    void illegalTransitionReturns409AndPublishesNothing() throws Exception {
        long id = createOrder();
        mvc.perform(patch("/api/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("PENDING to DELIVERED")));
        assertThat(events.stream(OrderStatusChangedEvent.class)).isEmpty();
    }

    @Test
    void updateStatusRequiresStatus() throws Exception {
        long id = createOrder();
        mvc.perform(patch("/api/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Cancel ----------

    @Test
    void cancelPendingOrder() throws Exception {
        long id = createOrder();
        mvc.perform(post("/api/orders/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cannotCancelProcessingOrder() throws Exception {
        long id = createOrder();
        commandBus.dispatch(new ChangeOrderStatusCommand(id, OrderStatus.PROCESSING));
        mvc.perform(post("/api/orders/{id}/cancel", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("only PENDING orders can be cancelled")));
    }

    @Test
    void cannotCancelTwice() throws Exception {
        long id = createOrder();
        mvc.perform(post("/api/orders/{id}/cancel", id)).andExpect(status().isOk());
        mvc.perform(post("/api/orders/{id}/cancel", id)).andExpect(status().isConflict());
    }

    @Test
    void cancelUnknownOrderReturns404() throws Exception {
        mvc.perform(post("/api/orders/{id}/cancel", 999_999)).andExpect(status().isNotFound());
    }

    @Test
    void cancelViaStatusEndpointAppliesSameRule() throws Exception {
        long id = createOrder();
        commandBus.dispatch(new ChangeOrderStatusCommand(id, OrderStatus.PROCESSING));
        mvc.perform(patch("/api/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict());
    }
}
