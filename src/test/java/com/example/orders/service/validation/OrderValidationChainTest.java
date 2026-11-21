package com.example.orders.service.validation;

import com.example.orders.domain.OrderValidationException;
import com.example.orders.service.command.CreateOrderCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Chain of Responsibility. */
class OrderValidationChainTest {

    private final OrderValidationChain chain = new OrderValidationChain(List.of(
            new NonEmptyOrderValidator(),
            new DuplicateProductValidator(),
            new MaxOrderValueValidator(new BigDecimal("100.00"))));

    private static CreateOrderCommand.Line line(String product, int qty, String price) {
        return new CreateOrderCommand.Line(product, "name", qty, new BigDecimal(price));
    }

    @Test
    void validOrderPassesEveryLink() {
        assertThatCode(() -> chain.validate(new CreateOrderCommand("c", List.of(line("a", 1, "10"), line("b", 2, "5")))))
                .doesNotThrowAnyException();
    }

    @Test
    void emptyOrderStopsAtFirstLink() {
        assertThatThrownBy(() -> chain.validate(new CreateOrderCommand("c", List.of())))
                .isInstanceOf(OrderValidationException.class).hasMessageContaining("at least one item");
    }

    @Test
    void duplicateProductIsRejected() {
        assertThatThrownBy(() -> chain.validate(new CreateOrderCommand("c", List.of(line("a", 1, "1"), line("a", 2, "1")))))
                .hasMessageContaining("appears more than once");
    }

    @Test
    void orderAboveLimitIsRejected() {
        assertThatThrownBy(() -> chain.validate(new CreateOrderCommand("c", List.of(line("a", 11, "10")))))
                .hasMessageContaining("exceeds the maximum");
    }

    @Test
    void linksRunInOrderAndStopAtFirstFailure() {
        List<String> calls = new ArrayList<>();
        OrderValidator first = recording("first", calls, false);
        OrderValidator second = recording("second", calls, true);
        OrderValidator third = recording("third", calls, false);
        first.linkWith(second).linkWith(third);

        assertThatThrownBy(() -> first.validate(new CreateOrderCommand("c", List.of())));
        assertThat(calls).containsExactly("first", "second");
    }

    private static OrderValidator recording(String name, List<String> calls, boolean fail) {
        return new OrderValidator() {
            @Override
            protected void check(CreateOrderCommand command) {
                calls.add(name);
                if (fail) throw new OrderValidationException(name);
            }
        };
    }
}
