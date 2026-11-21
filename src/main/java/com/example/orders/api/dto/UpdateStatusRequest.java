package com.example.orders.api.dto;

import com.example.orders.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull OrderStatus status) {}
