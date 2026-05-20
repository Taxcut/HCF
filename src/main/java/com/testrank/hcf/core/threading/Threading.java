package com.testrank.hcf.core.threading;

import com.testrank.hcf.core.api.HCFService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class Threading implements HCFService {
    private final Plugin plugin;
    private final ExecutorService persistence;
    private final ExecutorService compute;

    public Threading(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.persistence = Executors.newFixedThreadPool(4, named("hcf-persistence"));
        this.compute = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2), named("hcf-compute"));
    }

    public Executor persistence() {
        return persistence;
    }

    public Executor compute() {
        return compute;
    }

    public <T> CompletableFuture<T> supplyPersistence(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, persistence);
    }

    public CompletableFuture<Void> runSync(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (Bukkit.isPrimaryThread()) {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
            return future;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    @Override
    public void close() {
        shutdown(persistence);
        shutdown(compute);
    }

    private static ThreadFactory named(String prefix) {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(prefix + "-" + thread.threadId());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static void shutdown(ExecutorService service) {
        service.shutdown();
        try {
            if (!service.awaitTermination(3, TimeUnit.SECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            service.shutdownNow();
        }
    }
}
