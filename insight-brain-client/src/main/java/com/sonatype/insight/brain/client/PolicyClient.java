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

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.model.component.PolicyFact;
import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.ClientException;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.UrlUtils;

public class PolicyClient
    extends AbstractClient
{
    private static final Logger log = LoggerFactory.getLogger( PolicyClient.class );

    private final String appId;

    private final String scanId;

    public PolicyClient( final Configuration config, final String appId, final String scanId )
    {
        super( config );

        this.appId = UrlUtils.encodeUrlComponent( appId );
        this.scanId = UrlUtils.encodeUrlComponent( scanId );
    }

    public List<PolicyFact> evaluate()
        throws IOException
    {
        Result httpResult = path( "rest/policy/evaluator", appId, scanId ).get();
        if ( httpResult.status() >= 400 )
        {
            throw new ClientException( httpResult );
        }

        String jsonResult = httpResult.text();
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            PolicyFact[] policyFacts = mapper.readValue( jsonResult, PolicyFact[].class );
            List<PolicyFact> result = new ArrayList<PolicyFact>();
            result.addAll( Arrays.asList( policyFacts ) );
            return result;
        }
        catch ( IOException e )
        {
            log.error( "Cannot parse json:" + jsonResult );
            throw new ClientException( httpResult, e );
        }
    }
}
