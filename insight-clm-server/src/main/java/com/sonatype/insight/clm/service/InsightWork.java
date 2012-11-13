package com.sonatype.insight.clm.service;

import java.io.File;

public class InsightWork
    extends AbstractInjectable<InsightWork>
{
    private final InsightConfiguration insightConfig;

    public InsightWork( final InsightConfiguration insightConfig )
    {
        this.insightConfig = insightConfig;
    }

    public File getAuditDir( final String appId )
    {
        return new File( insightConfig.getSonatypeWork(), "audit" + File.separatorChar + appId );
    }
}
