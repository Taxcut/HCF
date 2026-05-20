package com.testrank.hcf.core.api;

public interface HCFService extends AutoCloseable {
    default void start() {}

    @Override
    default void close() {}
}
