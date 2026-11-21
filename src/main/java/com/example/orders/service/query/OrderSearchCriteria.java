package com.example.orders.service.query;

import com.example.orders.domain.OrderStatus;

/** Optional filters for listing orders; null means "don't filter". */
public record OrderSearchCriteria(OrderStatus status, String customerId) {

    public static OrderSearchCriteria all() {
        return new OrderSearchCriteria(null, null);
    }
}
