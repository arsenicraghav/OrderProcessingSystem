package com.example.orders.service.query;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderNotFoundException;
import com.example.orders.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side. Writes go through commands, reads come here (a light <b>CQRS</b> split),
 * so read paths can be tuned (read-only transactions, batch fetching) independently.
 */
@Service
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository repository;

    public OrderQueryService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order getOrder(Long id) {
        return repository.findWithItemsById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public Page<Order> search(OrderSearchCriteria criteria, Pageable pageable) {
        Page<Order> page = repository.findAll(OrderSpecifications.matching(criteria), pageable);
        // Initialise items inside the transaction; batch fetching loads them in one extra query.
        page.forEach(o -> o.getItems().size());
        return page;
    }
}
