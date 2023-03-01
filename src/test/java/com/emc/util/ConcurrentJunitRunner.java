/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.emc.util;

import org.junit.jupiter.api.extension.*;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * NOTE: when threading a test class, remember that remote state must also be synchronized!  That means don't
 * use the same object keys in different tests!  And if you can help it, try not to use the same bucket either!
 */
public class ConcurrentJunitRunner implements ExecutionCondition, BeforeAllCallback, AfterAllCallback {

    ExecutorService executorService;
    private static int THREAD_COUNT;
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(ConcurrentJunitRunner.class);

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        ExecutorService executorService = context.getStore(ExtensionContext.Namespace.GLOBAL)
                .get("ConcurrentJunitRunner", ExecutorService.class);
        return ConditionEvaluationResult.enabled("Using custom thread pool with " + THREAD_COUNT + " threads");
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Optional<AnnotatedElement> element = context.getElement();
        Concurrent CONCURRENT = element.map(annotatedElement -> annotatedElement.getAnnotation(Concurrent.class)).orElse(null);
        THREAD_COUNT = CONCURRENT != null ? CONCURRENT.threads() : (int) (Runtime.getRuntime().availableProcessors() * 1.5);
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        // store the executor service in the extension context
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        store.put("ConcurrentJunitRunner", executorService);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        ExecutorService executorService = store.remove("ConcurrentJunitRunner", ExecutorService.class);
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}