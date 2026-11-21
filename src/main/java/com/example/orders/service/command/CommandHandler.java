package com.example.orders.service.command;

/** Executes one kind of {@link Command}. Each handler is a Spring bean, discovered automatically. */
public interface CommandHandler<C extends Command<R>, R> {

    Class<C> commandType();

    R handle(C command);
}
