package com.testrank.hcf.core.mongo;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class ListResultSubscriber<T> implements Subscriber<T> {
    private final CompletableFuture<List<T>> future = new CompletableFuture<>();
    private final List<T> values = new ArrayList<>();

    CompletableFuture<List<T>> future() {
        return future;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T item) {
        values.add(item);
    }

    @Override
    public void onError(Throwable throwable) {
        future.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        future.complete(List.copyOf(values));
    }
}
