/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
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

    public File getIconDir()
    {
        return new File( insightConfig.getSonatypeWork(), "data/application" );
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

    public List<PolicyEvaluation> getMostRecentPolicyEvaluations( final String appId )
        throws IOException
    {
        final List<PolicyEvaluation> policyEvaluations = new ArrayList<PolicyEvaluation>();
        final JsonStore auditStore = JsonUtils.fileStore( getAuditDir( appId ) );
        final ContainerNode<?> auditContainer = auditStore.history( null, "policyevaluations.json" );
        if ( auditContainer != null )
        {
            final int stageTypeCount = StageTypes.getAll().size();
            final Map<String, PolicyEvaluation> perStage = new TreeMap<String, PolicyEvaluation>();
            for ( final JsonNode auditNode : auditContainer.get( "aaData" ) )
            {
                final PolicyEvaluation eval = JsonUtils.asPojo( auditNode, PolicyEvaluation.class );
                final String stage = eval.getStage().getStageTypeId();
                if ( !perStage.containsKey( stage ) )
                {
                    perStage.put( stage, eval );
                    if ( perStage.size() >= stageTypeCount )
                    {
                        break;
                    }
                }
            }
            policyEvaluations.addAll( perStage.values() );
        }
        return policyEvaluations;
    }
}
