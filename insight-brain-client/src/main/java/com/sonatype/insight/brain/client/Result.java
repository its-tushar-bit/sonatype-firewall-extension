/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

public final class Result
{
    private final HttpResponse response;

    Result( final HttpResponse response )
    {
        this.response = response;
    }

    public int status()
    {
        return response.getStatusLine().getStatusCode();
    }

    public String header( final String name )
    {
        final Header header = response.getFirstHeader( name );
        return header != null ? header.getValue() : null;
    }

    public String text()
        throws IOException
    {
        return EntityUtils.toString( response.getEntity() );
    }

    public byte[] data()
        throws IOException
    {
        return EntityUtils.toByteArray( response.getEntity() );
    }

    public void serve( final HttpServletResponse rsp )
        throws IOException
    {
        // pass-back response metadata+content to servlet
        for ( final Header h : response.getAllHeaders() )
        {
            rsp.setHeader( h.getName(), h.getValue() );
        }
        final HttpEntity entity = response.getEntity();
        if ( entity != null )
        {
            rsp.setContentLength( (int) entity.getContentLength() );
            rsp.setContentType( EntityUtils.getContentMimeType( entity ) );
            entity.writeTo( rsp.getOutputStream() );
        }
        rsp.setStatus( response.getStatusLine().getStatusCode() );
    }
}
