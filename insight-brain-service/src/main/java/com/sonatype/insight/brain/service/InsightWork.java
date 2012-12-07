/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

public class InsightWork
    extends AbstractInjectable<InsightWork>
{
    private final InsightConfig insightConfig;

    public InsightWork( final InsightConfig insightConfig )
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

    public File getPolicyDir()
    {
        return new File( insightConfig.getSonatypeWork(), "policy" );
    }

    public File getReportFile( final String scanId )
    {
        return new File( getReportDir( scanId ), "report.zip" );
    }
}
