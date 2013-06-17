package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

@Named
public class PolicyEvaluationUtils
{
    private static final Logger log = LoggerFactory.getLogger( PolicyEvaluationUtils.class );

    private final InsightWork work;

    private final ReportDownloader reportDownloader;

    @Inject
    public PolicyEvaluationUtils( final InsightWork insightWork, final ReportDownloader reportDownloader )
    {
        this.work = insightWork;
        this.reportDownloader = reportDownloader;
    }

    public void calculateCounters( PolicyEvaluationResult policyEvaluation )
    {
        final Map<String, Integer> componentThreatLevels = new HashMap<String, Integer>();
        for ( final PolicyAlert alert : policyEvaluation.getAlerts() )
        {
            final PolicyFact trigger = alert.getTrigger();
            final int policyThreatLevel = trigger.getThreatLevel();
            for ( final ComponentFact component : trigger.getComponentFacts() )
            {
                final String id = component.getComponentId();
                final Integer level = componentThreatLevels.get( id );
                if ( level == null || level < policyThreatLevel )
                {
                    componentThreatLevels.put( id, policyThreatLevel );
                }
            }
        }
        int criticalCount = 0, severeCount = 0, moderateCount = 0;
        for ( final int level : componentThreatLevels.values() )
        {
            if ( level >= 8 )
            {
                criticalCount++;
            }
            else if ( level >= 4 )
            {
                severeCount++;
            }
            else if ( level >= 2 )
            {
                moderateCount++;
            }
        }

        policyEvaluation.setAffectedComponentCount( componentThreatLevels.size() );
        policyEvaluation.setCriticalComponentCount( criticalCount );
        policyEvaluation.setSevereComponentCount( severeCount );
        policyEvaluation.setModerateComponentCount( moderateCount );
    }

    public List<PolicyAlert> findOldPolicyAlerts( final String applicationPublicId, String appId, final String scanId,
                                                  final Stage stage )
        throws IOException
    {
        PolicyEvaluationLog evalLog = new PolicyEvaluationLog( work.getAuditDir( appId ) );

        // retrieve last known scanId for stage
        com.sonatype.insight.brain.model.policy.PolicyEvaluation last = evalLog.last( stage.getStageTypeId() );
        final String oldScanId = ( last != null ) ? last.getScanId() : null;

        // add new entry in the rolling log (TODO: populate invoker's details)
        evalLog.add( stage, scanId, "anonymous", "127.0.0.1" );

        if ( !StringUtils.isBlank( oldScanId ) )
        {
            try
            {
                final File reportFile = ReportResource.fetchReport( reportDownloader, work, appId, oldScanId, true );
                final ReportEntry reportEntry = Report.getEntry( reportFile, "policyalerts.json" );
                if ( reportEntry != null )
                {
                    return Arrays.asList( JsonUtils.parse( reportEntry.buf, PolicyAlert[].class ) );
                }
            }
            catch ( final Exception e )
            {
                // don't abort sending notifications if old results are corrupt, just means full digest will be sent
                log.warn( "Cannot load previous results for app id {}, scan id {}", applicationPublicId, scanId, e );
            }
        }
        return Collections.emptyList();
    }
}
