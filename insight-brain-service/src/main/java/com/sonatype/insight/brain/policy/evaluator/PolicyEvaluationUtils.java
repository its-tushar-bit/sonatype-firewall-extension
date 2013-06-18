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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

@Named
public class PolicyEvaluationUtils
{
    private static final Logger log = LoggerFactory.getLogger( PolicyEvaluationUtils.class );

    private final InsightWork work;

    private final ReportDownloader reportDownloader;

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    @Inject
    public PolicyEvaluationUtils( final InsightWork insightWork, final ReportDownloader reportDownloader )
    {
        this.work = insightWork;
        this.reportDownloader = reportDownloader;
    }

    public PolicyEvaluationResult evaluate( final String applicationPublicId, final String scanId, final Stage stage )
        throws IOException
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        final PolicyDAO policyDAO = new PolicyDAO( work.getWorkDir() );

        final File reportFile = ReportResource.fetchReport( reportDownloader, work, appId, scanId, true );

        final ReportEntry licenseReportEntry = Report.getEntry( reportFile, "licenses.json" );
        final ReportEntry securityReportEntry = Report.getEntry( reportFile, "security.json" );
        final ReportEntry bomReportEntry = Report.getEntry( reportFile, "bom.json" );

        if ( bomReportEntry == null || securityReportEntry == null || licenseReportEntry == null )
        {
            throw new BadRequestException( "Unable to evaluate policy, the scan " + scanId + " could not be processed" );
        }

        final List<Component> components =
            new ComponentDAO().getAll( appId, licenseReportEntry.buf, securityReportEntry.buf, bomReportEntry.buf );

        final List<PolicyAlert> alerts = new PolicyEvaluator().evaluate( appId, stage, policyDAO, components );

        Report.putEntry( reportFile, "policyalerts.json", JsonUtils.generate( JsonUtils.aaData( alerts ) ) );
        Report.putEntry( reportFile, "policythreats.json", JsonUtils.generate( analyzeThreats( alerts ) ) );

        ReportResource.flushReportChanges( appId, scanId ); // ensure policy count is recalculated on fetch

        final PolicyEvaluationResult policyEvaluation = new PolicyEvaluationResult();
        policyEvaluation.setAlerts( alerts );
        calculateCounters( policyEvaluation );

        return policyEvaluation;
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

    private static ObjectNode analyzeThreats( final List<PolicyAlert> policyAlerts )
    {
        final Map<String, JsonNode> componentThreats = new HashMap<String, JsonNode>();
        for ( final PolicyAlert alert : policyAlerts )
        {
            final PolicyFact trigger = alert.getTrigger();
            final int threatLevel = trigger.getThreatLevel();
            for ( final ComponentFact component : trigger.getComponentFacts() )
            {
                final String id = component.getComponentId();
                ObjectNode threat = (ObjectNode) componentThreats.get( id );
                if ( threat == null )
                {
                    threat = JsonUtils.asTree( component );
                    threat.remove( "constraintFacts" );
                    componentThreats.put( id, threat );
                }
                if ( threatLevel > threat.path( "policyThreatLevel" ).asInt( -1 ) )
                {
                    threat.put( "policyId", trigger.getPolicyId() );
                    threat.put( "policyName", trigger.getPolicyName() );
                    threat.put( "policyThreatLevel", threatLevel );
                }
            }
        }
        return JsonUtils.aaDataNode( componentThreats.values() );
    }
}
