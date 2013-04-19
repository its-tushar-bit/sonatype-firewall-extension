/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;

import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;

public final class ValidationClient
    extends AbstractClient
{
    public ValidationClient( final Configuration config )
    {
        super( config );
    }

    @SuppressWarnings( "unchecked" )
    public Map<String, String> getApplicationIdNameMap()
        throws IOException
    {
        Result result = path( "rest/application/services/names" ).get();
        return (Map<String, String>) JsonUtils.parse( result.data(), Map.class );
    }

    public void validateConfiguration()
        throws IOException
    {
        final Result result;
        try
        {
            result = path( "rest/version" ).get();
        }
        catch ( UnknownHostException e )
        {
            // improve error msg
            throw (IOException) new UnknownHostException( "Unknown host: " + e.getMessage() ).initCause( e );
        }
        catch ( NumberFormatException e )
        {
            // improve error msg (thrown from httpclient in response to non-numeric port specs)
            throw new IllegalArgumentException( "Invalid port", e );
        }
        final int status = result.status();
        final String text = result.text();
        // at this point, the network connection appears fine, now let's just check we actually talked to a CLM server
        if ( status >= 300 )
        {
            throw new IOException( "Error code " + status + ": " + text );
        }
        try
        {
            final Map<?, ?> versionInfo = JsonUtils.parse( text, Map.class );
            if ( versionInfo.get( "version" ) == null && versionInfo.get( "name" ) == null )
            {
                throw new Exception( "No CLM version information present" );
            }
        }
        catch ( Exception e )
        {
            throw new IOException( "Server is not compatible with this Sonatype CLM integration", e );
        }
    }

    public void validateApplicationId( final String appId )
        throws IOException
    {
        final Result result;
        try
        {
            result = path( "rest/application/validate", UrlUtils.encodeUrlComponent( appId ) ).get();
        }
        catch ( UnknownHostException e )
        {
            // improve error msg
            throw (IOException) new UnknownHostException( "Unknown host: " + e.getMessage() ).initCause( e );
        }
        final int status = result.status();
        final String text = result.text();
        if ( status >= 300 )
        {
            throw new IOException( "Error code " + status + ": " + text );
        }
        if ( !"OK".equals( text ) )
        {
            throw new IOException( text );
        }
    }
}
