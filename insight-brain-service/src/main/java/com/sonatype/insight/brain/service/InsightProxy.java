/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.client.utils.HttpClientUtils;

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
        // TODO: proxy settings
        return httpConfig;
    }
}
