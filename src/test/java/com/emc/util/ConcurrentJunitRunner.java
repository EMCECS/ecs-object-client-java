/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.util;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * NOTE: when threading a test class, remember that remote state must also be synchronized!  That means don't
 * use the same object keys in different tests!  And if you can help it, try not to use the same bucket either!
 */
public class ConcurrentJunitRunner extends BlockJUnit4ClassRunner {
    public ConcurrentJunitRunner(final Class<?> klass) throws InitializationError {
        super(klass);
        setScheduler(new RunnerScheduler() {
            ExecutorService executorService = Executors.newFixedThreadPool(
                    klass.isAnnotationPresent(Concurrent.class) ?
                            klass.getAnnotation(Concurrent.class).threads() :
                            (int) (Runtime.getRuntime().availableProcessors() * 1.5));
            Queue<Future<?>> tasks = new LinkedList<>();

            @Override
            public void schedule(Runnable childStatement) {
                tasks.add(executorService.submit(childStatement));
            }

            @Override
            public void finished() {
                try {
                    for (Future<?> task : tasks) task.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    e.printStackTrace(); // (JUnit *should* fail the test)
                } finally {
                    executorService.shutdownNow();
                }
            }
        });
    }
}