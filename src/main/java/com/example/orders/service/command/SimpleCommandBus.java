package com.example.orders.service.command;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Routes each command to the single handler registered for its type. */
@Component
public class SimpleCommandBus implements CommandBus {

    private final Map<Class<?>, CommandHandler<?, ?>> handlers = new HashMap<>();

    public SimpleCommandBus(List<CommandHandler<?, ?>> handlerBeans) {
        for (CommandHandler<?, ?> handler : handlerBeans) {
            CommandHandler<?, ?> previous = handlers.put(handler.commandType(), handler);
            if (previous != null) {
                throw new IllegalStateException("Two handlers registered for " + handler.commandType().getName());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R dispatch(Command<R> command) {
        CommandHandler<Command<R>, R> handler = (CommandHandler<Command<R>, R>) handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalStateException("No handler registered for " + command.getClass().getName());
        }
        return handler.handle(command);
    }
}
