package com.example.orders.service.factory;

import com.example.orders.domain.pricing.PricingStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** <b>Factory</b>: picks the {@link PricingStrategy} named by {@code orders.pricing.strategy}. */
@Component
public class PricingStrategyFactory {

    private final Map<String, PricingStrategy> strategies;
    private final PricingStrategy active;

    public PricingStrategyFactory(List<PricingStrategy> all,
                                  @Value("${orders.pricing.strategy:standard}") String activeName) {
        this.strategies = all.stream().collect(Collectors.toMap(PricingStrategy::name, Function.identity()));
        this.active = get(activeName);
    }

    public PricingStrategy get(String name) {
        PricingStrategy strategy = strategies.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown pricing strategy '" + name + "'; known: " + strategies.keySet());
        }
        return strategy;
    }

    public PricingStrategy active() {
        return active;
    }
}
