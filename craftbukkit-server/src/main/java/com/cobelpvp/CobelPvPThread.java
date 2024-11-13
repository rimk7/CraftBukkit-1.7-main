package com.cobelpvp;

import com.cobelpvp.threads.AbstractThread;
import com.cobelpvp.threads.TickThread;
import org.bukkit.Bukkit;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CobelPvPThread {

    private final ExecutorService service;
    private AbstractThread tickThread;

    public CobelPvPThread(final int threads) {
        this.service = Executors.newFixedThreadPool(threads);
    }

    public void requestRunnable(Runnable runnable) {
        this.service.submit(runnable);
    }

    public Future<?> requestTask(final Callable<?> callable) {
        return this.service.submit(callable);
    }

    public void loadAsyncThreads() {
        try {
            this.tickThread = new TickThread();
        }
        catch (Exception ex) {
            Bukkit.getLogger().warning("Could not load async threads!");
        }
    }

    public AbstractThread getTickThread() {
        return tickThread;
    }
}

