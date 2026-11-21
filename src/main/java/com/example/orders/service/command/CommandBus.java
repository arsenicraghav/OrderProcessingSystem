package com.example.orders.service.command;

/**
 * <b>Mediator pattern</b>: callers send commands to the bus without knowing which handler
 * executes them, which keeps controllers, jobs and handlers decoupled.
 */
public interface CommandBus {

    <R> R dispatch(Command<R> command);
}
