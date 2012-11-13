package com.sonatype.insight.brain.service;

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

    public File getReportDir( final String scanId )
    {
        return new File( insightConfig.getSonatypeWork(), "report" + File.separatorChar + scanId );
    }

    public File getReportFile( final String scanId )
    {
        return new File( getReportDir( scanId ), "report.zip" );
    }
}
