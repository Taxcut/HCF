package com.testrank.hcf.core.mongo;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;

final class SingleResultSubscriber<T> implements Subscriber<T> {
    private final CompletableFuture<T> future = new CompletableFuture<>();
    private T value;

    CompletableFuture<T> future() {
        return future;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T item) {
        value = item;
    }

    @Override
    public void onError(Throwable throwable) {
        future.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        future.complete(value);
    }
}
