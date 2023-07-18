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
package com.emc.object.s3.jersey;

import com.emc.object.s3.S3Exception;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;
import java.util.Random;

@Provider
@Priority(FilterPriorities.PRIORITY_FAULTINJECTION)
public class FaultInjectionFilter implements ClientRequestFilter {
    public static final String FAULT_INJECTION_ERROR_CODE = "FaultInjection";
    public static final String FAULT_INJECTION_ERROR_MESSAGE = "Fault Injection";

    public static final float DEFAULT_FAILURE_RATE = 0.25f;

    private Random random = new Random();
    private float failureRate;

    public FaultInjectionFilter() {
        this(DEFAULT_FAILURE_RATE);
    }

    public FaultInjectionFilter(float failureRate) {
        this.failureRate = failureRate;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws WebApplicationException {
        if (random.nextFloat() < failureRate) {
            throw new S3Exception(FAULT_INJECTION_ERROR_MESSAGE, 500, FAULT_INJECTION_ERROR_CODE, null);
        }
    }

    public float getFailureRate() {
        return failureRate;
    }
}
