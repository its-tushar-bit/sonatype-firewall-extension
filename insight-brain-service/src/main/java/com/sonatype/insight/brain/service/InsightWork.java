/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

public class InsightWork
    extends AbstractInjectable<InsightWork>
{
    private static final Logger log = LoggerFactory.getLogger( InsightWork.class );

    private final InsightConfig insightConfig;

    public InsightWork( final InsightConfig insightConfig )
    {
        this.insightConfig = insightConfig;
    }

    public File getWorkDir()
    {
        return insightConfig.getSonatypeWork();
    }

    public File getScanDir( final String appId )
    {
        return new File( insightConfig.getSonatypeWork(), "scan/" + appId );
    }

    public File getAuditDir( final String appId )
    {
        return new File( insightConfig.getSonatypeWork(), "audit/" + appId );
    }

    public File getReportDir( final String appId, final String scanId )
    {
        return new File( insightConfig.getSonatypeWork(), "report/" + appId + '/' + scanId );
    }

    public File getReportFile( final String appId, final String scanId )
    {
        return new File( getReportDir( appId, scanId ), "report.zip" );
    }

    public String findOwningAppId( final String scanId )
    {
        final File rootDir = new File( insightConfig.getSonatypeWork(), "report" );
        if ( rootDir.isDirectory() )
        {
            try
            {
                final List<String> dirs = FileUtils.getDirectoryNames( rootDir, "*/" + scanId, null, false );
                if ( !dirs.isEmpty() )
                {
                    return FileUtils.dirname( dirs.get( 0 ) );
                }
            }
            catch ( final IOException e )
            {
                log.error( "Problem scanning directory: " + rootDir + " for scanId: " + scanId, e );
            }
        }
        return null;
    }

    public PolicyEvaluation getPolicyEvaluation( final String appId )
        throws IOException
    {
        final JsonStore auditStore = JsonUtils.fileStore( getAuditDir( appId ) );
        final ContainerNode<?> auditContainer = auditStore.history( null, "policyevaluations.json" );
        if ( auditContainer != null )
        {
            JsonNode latestAuditNode = auditContainer.get( "aaData" ).get( 0 );
            return JsonUtils.asPojo( latestAuditNode, PolicyEvaluation.class );
        }
        // Valid response as there have been no policy evaluations for this application
        return null;
    }
}
