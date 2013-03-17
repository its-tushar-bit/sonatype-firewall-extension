/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.SimpleAuthentication;

public class InsightProxy
    extends AbstractInjectable<InsightProxy>
{
    private final InsightConfig insightConfig;

    public InsightProxy( final InsightConfig insightConfig )
    {
        this.insightConfig = insightConfig;
    }

    public <T extends HttpClientUtils.Configuration> T contextualize( final T httpConfig )
    {
        httpConfig.setServerUrl( insightConfig.getSaasAddress() );

        final ProxyConfig proxyConfig = insightConfig.getProxyConfig();
        if ( proxyConfig.getHost() != null )
        {
            httpConfig.setProxyHost( proxyConfig.getHost() );
            httpConfig.setProxyPort( proxyConfig.getPort() );
            if ( proxyConfig.getAuth() != null )
            {
                httpConfig.setProxyAuth( SimpleAuthentication.parse( proxyConfig.getAuth() ) );
            }
        }

        return httpConfig;
    }
}
