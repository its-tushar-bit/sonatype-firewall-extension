/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;

import com.sonatype.insight.brain.service.AbstractInjectable;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

public class SaasClient
    extends AbstractInjectable<InsightProxy>
{
    private Configuration config;

    public SaasClient( final InsightProxy proxy )
    {
        config = proxy.contextualize( new Configuration() );
        // TODO Need to determine if there is additional information we should be sending to the SaaS
    }

    public Response doProxy( String path, HttpServletRequest request )
        throws IOException
    {
        HttpUriRequest cloudReq;
        if ( "GET".equals( request.getMethod() ) )
        {
            cloudReq = new HttpGet( buildUri( path ) );
        }
        else if ( "POST".equals( request.getMethod() ) )
        {
            cloudReq = new HttpPost( buildUri( path ) );
            ( (HttpPost) cloudReq ).setEntity( new InputStreamEntity( request.getInputStream(),
                                                                      request.getContentLength() ) );
        }
        else if ( "PUT".equals( request.getMethod() ) )
        {
            cloudReq = new HttpPut( buildUri( path ) );
            ( (HttpPut) cloudReq ).setEntity( new InputStreamEntity( request.getInputStream(),
                                                                     request.getContentLength() ) );
        }
        else if ( "DELETE".equals( request.getMethod() ) )
        {
            cloudReq = new HttpPut( buildUri( path ) );
        }
        else
        {
            throw new IllegalArgumentException( "Unknown request method" );
        }
        return execute( cloudReq );
    }

    private Response execute( HttpUriRequest request )
        throws IOException
    {
        // TODO should the client be shared?
        HttpClient client = HttpClientUtils.createConfig( config );
        final HttpResponse response = client.execute( request );

        ResponseBuilder builder = Response.status( response.getStatusLine().getStatusCode() );

        // pass-back response metadata+content to servlet
        for ( final Header h : response.getAllHeaders() )
        {
            final String name = h.getName();
            // ignore Transfer-Encoding since httpclient should have handled it
            if ( !HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase( name )
                && !HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase( name )
                && !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase( name )
                && !HttpHeaders.CONTENT_TYPE.equalsIgnoreCase( name ) )
            {
                builder.header( name, h.getValue() );
            }
        }

        final HttpEntity entity = response.getEntity();
        if ( entity != null )
        {
            if ( entity.getContentEncoding() != null )
            {
                builder.header( HttpHeaders.CONTENT_ENCODING, entity.getContentEncoding().getValue() );
            }
            builder.header( HttpHeaders.CONTENT_LENGTH, entity.getContentLength() );
            if ( entity.getContentType() != null )
            {
                builder.header( HttpHeaders.CONTENT_TYPE, entity.getContentType().getValue() );
            }
        }

        builder.entity( new StreamingOutput()
        {

            @Override
            public void write( OutputStream output )
                throws IOException, WebApplicationException
            {
                response.getEntity().writeTo( output );
            }
        } );
        return builder.build();
    }

    private String buildUri( String path )
    {
        String base = config.getServerUrl();

        boolean baseEnds = base.endsWith( "/" );
        boolean pathStarts = path.startsWith( "/" );
        if ( baseEnds && pathStarts )
        {
            return base + path.substring( 1 );
        }
        else if ( baseEnds || pathStarts )
        {
            return base + path;
        }
        else
        {
            return base + '/' + path;
        }
    }
}
