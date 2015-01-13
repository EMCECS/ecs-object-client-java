/*
 * Copyright 2013 EMC Corporation. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
