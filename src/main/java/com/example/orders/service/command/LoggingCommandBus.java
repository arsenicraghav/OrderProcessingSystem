package com.example.orders.service.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * <b>Decorator pattern</b>: wraps the real bus to add timing and logging without changing
 * it. Marked {@code @Primary}, so everyone who asks for a {@link CommandBus} gets the
 * decorated one. More decorators (metrics, retries) could be stacked the same way.
 */
@Primary
@Component
public class LoggingCommandBus implements CommandBus {

    private static final Logger log = LoggerFactory.getLogger(LoggingCommandBus.class);

    private final CommandBus delegate;

    public LoggingCommandBus(SimpleCommandBus delegate) {
        this.delegate = delegate;
    }

    @Override
    public <R> R dispatch(Command<R> command) {
        String name = command.getClass().getSimpleName();
        long start = System.nanoTime();
        try {
            R result = delegate.dispatch(command);
            log.debug("{} handled in {} ms", name, (System.nanoTime() - start) / 1_000_000);
            return result;
        } catch (RuntimeException e) {
            log.warn("{} failed after {} ms: {}", name, (System.nanoTime() - start) / 1_000_000, e.getMessage());
            throw e;
        }
    }
}
