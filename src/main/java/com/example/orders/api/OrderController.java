package com.example.orders.api;

import com.example.orders.api.dto.CreateOrderRequest;
import com.example.orders.api.dto.OrderResponse;
import com.example.orders.api.dto.PageResponse;
import com.example.orders.api.dto.UpdateStatusRequest;
import com.example.orders.domain.OrderStatus;
import com.example.orders.service.query.OrderSearchCriteria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Thin HTTP adapter: validation and status codes only; everything else is behind the facade. */
@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final OrderFacade orders;

    public OrderController(OrderFacade orders) {
        this.orders = orders;
    }

    /** Place a new order with one or more items. Starts in PENDING. */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orders.placeOrder(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** Fetch an order with its items. */
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orders.getOrder(id);
    }

    /** List orders, newest first, optionally filtered by status and/or customer. */
    @GetMapping
    public PageResponse<OrderResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String customerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return orders.listOrders(new OrderSearchCriteria(status, customerId), pageable);
    }

    /** Move an order to a new status, following the allowed lifecycle. */
    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return orders.changeStatus(id, request.status());
    }

    /** Cancel an order; only allowed while it is PENDING. */
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return orders.cancelOrder(id);
    }
}
