/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.model.component.PolicyFact;
import com.sonatype.insight.client.utils.AbstractServletClient;
import com.sonatype.insight.client.utils.ClientException;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.ServletResult;
import com.sonatype.insight.client.utils.UrlUtils;

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

    public ServletResult manage( final String path )
        throws IOException
    {
        return path( "policy-assets", path.length() == 0 || path.endsWith( "/" ) ? path + "index.html" : path ).get();
    }

    public List<PolicyFact> evaluate( final String scanId )
        throws IOException
    {
        final Result httpResult = path( "rest/policy/evaluator", appId, UrlUtils.encodeUrlComponent( scanId ) ).get();
        if ( httpResult.status() >= 400 )
        {
            throw new ClientException( httpResult );
        }

        final String jsonResult = httpResult.text();
        try
        {
            final ObjectMapper mapper = new ObjectMapper();
            final PolicyFact[] policyFacts = mapper.readValue( jsonResult, PolicyFact[].class );
            final List<PolicyFact> result = new ArrayList<PolicyFact>();
            result.addAll( Arrays.asList( policyFacts ) );
            return result;
        }
        catch ( final IOException e )
        {
            log.error( "Cannot parse json:" + jsonResult );
            throw new ClientException( httpResult, e );
        }
    }
}
