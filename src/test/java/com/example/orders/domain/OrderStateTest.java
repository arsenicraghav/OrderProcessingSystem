package com.example.orders.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** State pattern, Builder pattern and the state factory. */
class OrderStateTest {

    private static Order newOrder() {
        return Order.builder()
                .customerId("c-1")
                .item("p-1", "Pen", 3, new BigDecimal("2.50"))
                .item("p-2", "Book", 1, new BigDecimal("10.00"))
                .build();
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING,PROCESSING,true",
            "PENDING,CANCELLED,true",
            "PENDING,SHIPPED,false",
            "PROCESSING,SHIPPED,true",
            "PROCESSING,CANCELLED,false",
            "PROCESSING,PENDING,false",
            "SHIPPED,DELIVERED,true",
            "SHIPPED,PROCESSING,false",
            "DELIVERED,PENDING,false",
            "CANCELLED,PENDING,false"
    })
    void lifecycleTransitions(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void factoryReturnsSingletonStateForEveryStatus(OrderStatus status) {
        OrderState state = OrderStateFactory.forStatus(status);
        assertThat(state.status()).isEqualTo(status);
        assertThat(OrderStateFactory.forStatus(status)).isSameAs(state);
    }

    @Test
    void onlyPendingIsCancellableAndTerminalStatesAreTerminal() {
        assertThat(OrderStatus.PENDING.state().isCancellable()).isTrue();
        assertThat(OrderStatus.PROCESSING.state().isCancellable()).isFalse();
        assertThat(OrderStatus.DELIVERED.state().isTerminal()).isTrue();
        assertThat(OrderStatus.CANCELLED.state().isTerminal()).isTrue();
        assertThat(OrderStatus.SHIPPED.state().isTerminal()).isFalse();
    }

    @Test
    void builderCreatesPendingOrderWithTotal() {
        Order order = newOrder();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("17.50");
        assertThat(order.getItems()).hasSize(2).allSatisfy(i -> assertThat(i.getOrder()).isSameAs(order));
    }

    @Test
    void builderRejectsIncompleteOrders() {
        assertThatThrownBy(() -> Order.builder().customerId("c").build()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> Order.builder().item("p", "x", 1, BigDecimal.ONE).build())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void walksFullLifecycleThroughStates() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.PROCESSING);
        order.transitionTo(OrderStatus.SHIPPED);
        order.transitionTo(OrderStatus.DELIVERED);
        assertThat(order.getState().isTerminal()).isTrue();
    }

    @Test
    void cancelWorksOnlyWhilePending() {
        Order pending = newOrder();
        pending.cancel();
        assertThat(pending.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        Order processing = newOrder();
        processing.transitionTo(OrderStatus.PROCESSING);
        assertThatThrownBy(processing::cancel)
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("only PENDING orders can be cancelled");
    }

    @Test
    void transitionToCancelledUsesTheCancelRule() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.PROCESSING);
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CANCELLED))
                .hasMessageContaining("only PENDING orders can be cancelled");
    }

    @Test
    void illegalTransitionThrows() {
        assertThatThrownBy(() -> newOrder().transitionTo(OrderStatus.DELIVERED))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("PENDING to DELIVERED");
    }
}
