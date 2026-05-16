package com.example.rag.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class QaMetricsService {

    private final MeterRegistry registry;

    public QaMetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public <T> T recordOperation(String operation, String mode, String tenantId, Supplier<T> action) {
        String tenantScope = tenantScope(tenantId);
        Timer.Sample sample = Timer.start(registry);
        try {
            T result = action.get();
            sample.stop(Timer.builder("campus.qa.operation.duration")
                    .tag("operation", operation)
                    .tag("mode", mode)
                    .tag("status", "success")
                    .tag("tenant_scope", tenantScope)
                    .register(registry));
            Counter.builder("campus.qa.operation.requests")
                    .tag("operation", operation)
                    .tag("mode", mode)
                    .tag("status", "success")
                    .tag("tenant_scope", tenantScope)
                    .register(registry)
                    .increment();
            return result;
        } catch (RuntimeException ex) {
            sample.stop(Timer.builder("campus.qa.operation.duration")
                    .tag("operation", operation)
                    .tag("mode", mode)
                    .tag("status", "error")
                    .tag("tenant_scope", tenantScope)
                    .register(registry));
            Counter.builder("campus.qa.operation.requests")
                    .tag("operation", operation)
                    .tag("mode", mode)
                    .tag("status", "error")
                    .tag("tenant_scope", tenantScope)
                    .register(registry)
                    .increment();
            throw ex;
        }
    }

    public void recordSourceCount(String mode, String tenantId, int count) {
        DistributionSummary.builder("campus.qa.sources.count")
                .tag("mode", mode)
                .tag("tenant_scope", tenantScope(tenantId))
                .register(registry)
                .record(count);
    }

    private String tenantScope(String tenantId) {
        return (tenantId == null || tenantId.isBlank() || "default".equalsIgnoreCase(tenantId.trim()))
                ? "default"
                : "custom";
    }
}
