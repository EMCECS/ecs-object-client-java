/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
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
package com.emc.object.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Iso8601Adapter extends XmlAdapter<String, Date> {
    private static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    // DateFormat is *not* thread-safe!
    private static final ThreadLocal<DateFormat> iso8601Format = new ThreadLocal<DateFormat>();

    /**
     * Until Java 7, SimpleDateFormat doesn't support ISO 8601 time zones ('Z', '+0000', '-03', etc.)  This extra
     * parsing ensures that we can *read* them.
     */
    @Override
    public Date unmarshal( String s ) throws Exception {
        int hourOffset = 0, minuteOffset = 0;

        String tzPattern = "([-+])(\\d{2}):?(\\d{2})?$";

        Matcher matcher = Pattern.compile( tzPattern ).matcher( s );
        if ( matcher.find() ) {

            hourOffset = Integer.parseInt( matcher.group( 2 ) );

            if ( matcher.group( 3 ) != null ) minuteOffset = Integer.parseInt( matcher.group( 3 ) );

            // formatter reads as GMT, so reverse the offset to get the real GMT time
            if ( "+".equals( matcher.group( 1 ) ) ) {
                hourOffset *= -1;
                minuteOffset *= -1;
            }

            s = s.replaceAll( tzPattern, "" ) + "Z";
        }

        Calendar cal = Calendar.getInstance();

        cal.setTime( getFormat().parse( s ) );

        cal.add( Calendar.HOUR_OF_DAY, hourOffset );
        cal.add( Calendar.MINUTE, minuteOffset );

        return cal.getTime();
    }

    /**
     * We will always write in UTC with no offset, so no need for extra logic here.
     */
    @Override
    public String marshal( Date date ) throws Exception {
        return getFormat().format( date );
    }

    public static DateFormat getFormat() {
        DateFormat format = iso8601Format.get();
        if ( format == null ) {
            format = new SimpleDateFormat( ISO_8601_FORMAT );
            format.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
            iso8601Format.set( format );
        }
        return format;
    }
}
