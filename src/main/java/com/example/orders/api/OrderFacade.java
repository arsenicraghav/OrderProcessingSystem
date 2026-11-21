package com.example.orders.api;

import com.example.orders.api.dto.CreateOrderRequest;
import com.example.orders.api.dto.OrderResponse;
import com.example.orders.api.dto.PageResponse;
import com.example.orders.api.mapper.OrderMapper;
import com.example.orders.domain.OrderStatus;
import com.example.orders.service.command.CancelOrderCommand;
import com.example.orders.service.command.ChangeOrderStatusCommand;
import com.example.orders.service.command.CommandBus;
import com.example.orders.service.query.OrderQueryService;
import com.example.orders.service.query.OrderSearchCriteria;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * <b>Facade pattern</b>: one simple entry point for the controller that hides the command
 * bus, the query service and the mapper behind five plain methods.
 */
@Component
public class OrderFacade {

    private final CommandBus commandBus;
    private final OrderQueryService queries;
    private final OrderMapper mapper;

    public OrderFacade(CommandBus commandBus, OrderQueryService queries, OrderMapper mapper) {
        this.commandBus = commandBus;
        this.queries = queries;
        this.mapper = mapper;
    }

    public OrderResponse placeOrder(CreateOrderRequest request) {
        return mapper.toResponse(commandBus.dispatch(mapper.toCommand(request)));
    }

    public OrderResponse getOrder(Long id) {
        return mapper.toResponse(queries.getOrder(id));
    }

    public PageResponse<OrderResponse> listOrders(OrderSearchCriteria criteria, Pageable pageable) {
        return mapper.toPage(queries.search(criteria, pageable));
    }

    public OrderResponse changeStatus(Long id, OrderStatus target) {
        return mapper.toResponse(commandBus.dispatch(new ChangeOrderStatusCommand(id, target)));
    }

    public OrderResponse cancelOrder(Long id) {
        return mapper.toResponse(commandBus.dispatch(new CancelOrderCommand(id)));
    }
}
