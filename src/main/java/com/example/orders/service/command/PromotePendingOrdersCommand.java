package com.example.orders.service.command;

/** Issued by the background job. Result: number of orders moved to PROCESSING. */
public record PromotePendingOrdersCommand() implements Command<Integer> {}
