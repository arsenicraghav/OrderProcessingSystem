package com.example.orders.api.mapper;

import com.example.orders.api.dto.CreateOrderRequest;
import com.example.orders.api.dto.OrderResponse;
import com.example.orders.api.dto.PageResponse;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderItem;
import com.example.orders.domain.OrderState;
import com.example.orders.service.command.CreateOrderCommand;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * <b>Mapper / Assembler</b> (an <b>Adapter</b> between the HTTP model and the domain model):
 * converts requests into commands and entities into response DTOs.
 */
@Component
public class OrderMapper {

    public CreateOrderCommand toCommand(CreateOrderRequest request) {
        return new CreateOrderCommand(request.customerId(), request.items().stream()
                .map(i -> new CreateOrderCommand.Line(i.productId(), i.productName(), i.quantity(), i.unitPrice()))
                .toList());
    }

    public OrderResponse toResponse(Order order) {
        OrderState state = order.getState();
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                state.isCancellable(),
                state.allowedTransitions().stream().sorted(Comparator.naturalOrder()).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(this::toItem).toList());
    }

    public PageResponse<OrderResponse> toPage(Page<Order> page) {
        return PageResponse.from(page, this::toResponse);
    }

    private OrderResponse.Item toItem(OrderItem i) {
        return new OrderResponse.Item(i.getId(), i.getProductId(), i.getProductName(),
                i.getQuantity(), i.getUnitPrice(), i.getLineTotal());
    }
}
