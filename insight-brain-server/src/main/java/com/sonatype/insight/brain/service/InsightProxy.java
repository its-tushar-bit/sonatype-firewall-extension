package com.sonatype.insight.brain.service;

import com.sonatype.insight.scan.upload.HttpClientUtils;

public class InsightProxy
    extends AbstractInjectable<InsightProxy>
{
    private final InsightConfiguration insightConfig;

    public InsightProxy( final InsightConfiguration insightConfig )
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
