package com.example.orders.repository;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * <b>Repository pattern</b>: collection-like access to orders, hiding SQL/JPA details.
 * {@link JpaSpecificationExecutor} adds <b>Specification</b>-based queries.
 */
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    /** Loads an order and its items in a single query. */
    @EntityGraph(attributePaths = "items")
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<Order> findWithItemsById(@Param("id") Long id);

    /**
     * Atomic, set-based status change. A single UPDATE statement is far cheaper than
     * loading every order into memory, and is safe to run concurrently on several
     * application instances: each row is only changed if it is still in {@code from}.
     * The version is bumped so concurrent optimistic-locked writers detect the change.
     */
    @Transactional   // joins the caller's transaction; also makes direct calls (e.g. tests) valid
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CustomerOrder o
               set o.status = :to, o.updatedAt = :now, o.version = o.version + 1
             where o.status = :from
            """)
    int bulkTransition(@Param("from") OrderStatus from,
                       @Param("to") OrderStatus to,
                       @Param("now") Instant now);
}
