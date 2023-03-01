/*
 * Copyright (c) 2015-2016, EMC Corporation.
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
package com.emc.object.s3;

import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.ListBucketsRequest;
import com.emc.object.util.RestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClockSkewTest extends AbstractS3ClientTest {
    @Override
    protected S3Client createS3Client() throws Exception {
        return new S3JerseyClient(createS3Config());
    }

    @Test
    public void testClockSkew() throws Exception {
        try {
            ListBucketsRequest request = new ListBucketsRequest() {
                @Override
                public Map<String, List<Object>> getHeaders() {
                    Map<String, List<Object>> headers = super.getHeaders();
                    // set x-amz-date, subtracting 30 minutes from current time
                    Date oldDate = new Date(System.currentTimeMillis() - (30 * 60 * 1000));
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
                    try {
                        Date d = sdf.parse(RestUtil.headerFormat(oldDate));
                        sdf.applyPattern("yyyyMMdd'T'HHmmss'Z'");
                        RestUtil.putSingle(headers, S3Constants.AMZ_DATE, sdf.format(d));
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    return headers;
                }
            };
            client.listBuckets(request);
        } catch (S3Exception e) {
            Assertions.assertEquals(403, e.getHttpCode());
        }
    }
}
