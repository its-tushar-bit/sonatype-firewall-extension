/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.model.policy.PolicyEvent;
import com.sonatype.insight.client.utils.AbstractServletClient;
import com.sonatype.insight.client.utils.ClientException;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.ServletResult;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;

public class PolicyClient
    extends AbstractServletClient<PolicyClient>
{
    private static final Logger log = LoggerFactory.getLogger( PolicyClient.class );

    private final String appId;

    public PolicyClient( final Configuration config, final String appId )
    {
        super( config );

        this.appId = UrlUtils.encodeUrlComponent( appId );
    }

    @Override
    public ServletResult handle( final String path, final String query )
        throws IOException
    {
        if ( path == null || path.length() == 0 )
        {
            // implicit redirect from initial top-level request to the actual management asset
            final HttpResponse redirect = new BasicHttpResponse( HttpVersion.HTTP_1_1, 302, null );
            redirect.setHeader( HttpHeaders.LOCATION, "policy-assets/index.html?appId=" + appId );
            return result( redirect );
        }

        // workaround for DropWizard directory->index redirect bug
        if ( path.contains( "-assets/" ) && path.endsWith( "/" ) )
        {
            return path( path, "index.html" ).get();
        }

        return super.handle( path, query );
    }

    public List<PolicyEvent> evaluate( final String scanId, final String contextTypeId /* FIXME: pass in full Context */)
        throws IOException
    {
        final Result httpResult =
            path( "rest/policy", appId, "evaluate" ).query( "scanId", scanId, "contextTypeId", contextTypeId ).get();
        if ( httpResult.status() >= 400 )
        {
            throw new ClientException( httpResult );
        }

        final String jsonResult = httpResult.text();
        try
        {
            return Arrays.asList( JsonUtils.parse( jsonResult, PolicyEvent[].class ) );
        }
        catch ( final IOException e )
        {
            log.error( "Cannot parse json:" + jsonResult );
            throw new ClientException( httpResult, e );
        }
    }
}
