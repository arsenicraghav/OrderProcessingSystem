package com.example.orders.service.query;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * <b>Specification pattern</b>: small, reusable query predicates combined with {@code and}.
 * Adding a filter means adding one method, not a new repository method per combination.
 */
public final class OrderSpecifications {

    private OrderSpecifications() {}

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Order> belongsTo(String customerId) {
        return (root, query, cb) -> cb.equal(root.get("customerId"), customerId);
    }

    public static Specification<Order> matching(OrderSearchCriteria criteria) {
        Specification<Order> spec = (root, query, cb) -> cb.conjunction();
        if (criteria.status() != null) {
            spec = spec.and(hasStatus(criteria.status()));
        }
        if (criteria.customerId() != null && !criteria.customerId().isBlank()) {
            spec = spec.and(belongsTo(criteria.customerId().trim()));
        }
        return spec;
    }
}
