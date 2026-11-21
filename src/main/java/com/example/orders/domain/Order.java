package com.example.orders.domain;

import com.example.orders.domain.pricing.PricingStrategy;
import com.example.orders.domain.pricing.StandardPricingStrategy;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Order aggregate root. Status changes are delegated to the current {@link OrderState}
 * (State pattern); new orders are assembled with {@link Builder} (Builder pattern).
 */
// Entity name avoids the JPQL keyword ORDER
@Entity(name = "CustomerOrder")
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)   // plain VARCHAR column, not a native ENUM type
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic locking: prevents lost updates between API calls and the background job. */
    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
        // for JPA
    }

    private Order(String customerId) {
        this.customerId = customerId;
        this.status = OrderStatus.PENDING;
    }

    public static Builder builder() {
        return new Builder();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ---------- State pattern: behaviour is delegated to the current state ----------

    public OrderState getState() {
        return OrderStateFactory.forStatus(status);
    }

    public void transitionTo(OrderStatus target) {
        getState().moveTo(this, target);
    }

    public void cancel() {
        getState().cancel(this);
    }

    /** Only states (same package) may change the status. */
    void changeStatus(OrderStatus newStatus) {
        this.status = newStatus;
    }

    // ---------- getters ----------

    public Long getId() { return id; }
    public String getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }

    /**
     * <b>Builder pattern</b>: assembles a valid PENDING order step by step and prices it
     * with the chosen {@link PricingStrategy}.
     */
    public static final class Builder {
        private String customerId;
        private final List<OrderItem> items = new ArrayList<>();
        private PricingStrategy pricing = new StandardPricingStrategy();

        private Builder() {}

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder item(String productId, String productName, int quantity, BigDecimal unitPrice) {
            items.add(new OrderItem(productId, productName, quantity, unitPrice));
            return this;
        }

        public Builder pricedWith(PricingStrategy pricing) {
            this.pricing = Objects.requireNonNull(pricing, "pricing");
            return this;
        }

        public Order build() {
            if (customerId == null || customerId.isBlank()) {
                throw new IllegalStateException("customerId is required");
            }
            if (items.isEmpty()) {
                throw new IllegalStateException("an order needs at least one item");
            }
            Order order = new Order(customerId.trim());
            for (OrderItem item : items) {
                item.setOrder(order);
                order.items.add(item);
            }
            order.totalAmount = pricing.calculateTotal(order.items);
            return order;
        }
    }
}
