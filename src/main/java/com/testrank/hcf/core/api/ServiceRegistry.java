package com.testrank.hcf.core.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ServiceRegistry implements AutoCloseable {
    private final Map<Class<?>, Object> services = new LinkedHashMap<>();
    private final List<HCFService> lifecycle = new ArrayList<>();

    public <T> T register(Class<T> type, T service) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(service, "service");
        if (services.putIfAbsent(type, service) != null) {
            throw new IllegalStateException("Service already registered: " + type.getName());
        }
        if (service instanceof HCFService hcfService) {
            lifecycle.add(hcfService);
        }
        return service;
    }

    public <T> T require(Class<T> type) {
        Object service = services.get(type);
        if (service == null) {
            throw new IllegalStateException("Missing service: " + type.getName());
        }
        return type.cast(service);
    }

    public void startAll() {
        for (HCFService service : lifecycle) {
            service.start();
        }
    }

    @Override
    public void close() {
        List<HCFService> closing = new ArrayList<>(lifecycle);
        Collections.reverse(closing);
        for (HCFService service : closing) {
            try {
                service.close();
            } catch (Exception ignored) {
            }
        }
    }
}
