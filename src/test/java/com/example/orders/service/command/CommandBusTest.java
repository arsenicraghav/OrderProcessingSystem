package com.example.orders.service.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Command, Mediator and Decorator patterns, without Spring. */
class CommandBusTest {

    record Ping(String text) implements Command<String> {}
    record Unhandled() implements Command<Void> {}

    static class PingHandler implements CommandHandler<Ping, String> {
        @Override public Class<Ping> commandType() { return Ping.class; }
        @Override public String handle(Ping command) {
            if (command.text().isEmpty()) throw new IllegalArgumentException("empty");
            return "pong:" + command.text();
        }
    }

    private final SimpleCommandBus bus = new SimpleCommandBus(List.of(new PingHandler()));

    @Test
    void routesCommandToItsHandler() {
        assertThat(bus.dispatch(new Ping("hi"))).isEqualTo("pong:hi");
    }

    @Test
    void unknownCommandFailsClearly() {
        assertThatThrownBy(() -> bus.dispatch(new Unhandled()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("No handler");
    }

    @Test
    void duplicateHandlersAreRejectedAtStartup() {
        assertThatThrownBy(() -> new SimpleCommandBus(List.of(new PingHandler(), new PingHandler())))
                .hasMessageContaining("Two handlers");
    }

    @Test
    void loggingDecoratorDelegatesAndRethrows() {
        LoggingCommandBus decorated = new LoggingCommandBus(bus);
        assertThat(decorated.dispatch(new Ping("x"))).isEqualTo("pong:x");
        assertThatThrownBy(() -> decorated.dispatch(new Ping(""))).isInstanceOf(IllegalArgumentException.class);
    }
}
