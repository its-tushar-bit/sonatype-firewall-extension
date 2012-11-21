/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

public abstract class AbstractServletClient<THIS extends AbstractServletClient<THIS>>
    extends AbstractClient
{
    private HttpServletRequest req;

    public AbstractServletClient( final Configuration config )
    {
        super( config );
    }

    @SuppressWarnings( "unchecked" )
    public final THIS take( final HttpServletRequest _req )
    {
        this.req = _req;
        return (THIS) this;
    }

    @Override
    protected HttpResponse execute( final HttpRequest request )
        throws IOException
    {
        if ( req != null )
        {
            // pass-through servlet metadata to request
            final Enumeration<?> n = req.getHeaderNames();
            while ( n != null && n.hasMoreElements() )
            {
                final String name = (String) n.nextElement();
                // avoid overwriting implicit content headers from entity
                if ( !HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase( name )
                    && !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase( name )
                    && !HttpHeaders.CONTENT_TYPE.equalsIgnoreCase( name ) )
                {
                    request.setHeader( name, req.getHeader( name ) );
                }
            }
        }
        return super.execute( request );
    }

    protected static Result consume( final HttpResponse response )
    {
        return new Result( response );
    }
}
