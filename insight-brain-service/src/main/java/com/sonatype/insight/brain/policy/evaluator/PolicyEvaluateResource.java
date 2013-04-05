/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.micromailer.Address;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluation;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.TemplateUtils;
import com.sonatype.insight.json.store.JsonUtils;

import freemarker.template.Template;

@Path( PolicyEvaluateResource.SERVICE_PATH )
public class PolicyEvaluateResource
{
    public static final String SERVICE_PATH = "rest/policy/{applicationPublicId}/evaluate";

    private static final Logger log = LoggerFactory.getLogger( PolicyEvaluateResource.class );

    private static Template policyThreatsTemplate;

    @Context
    private InsightWork work;

    @Context
    private InsightProxy proxy;

    @Context
    private InsightMail mail;

    @Context
    private BaseUrl baseUrl;

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public PolicyEvaluation evaluate( @PathParam( "applicationPublicId" ) final String applicationPublicId,
                                      @QueryParam( "scanId" ) final String scanId, final Stage stage,
                                      @HeaderParam( "user-agent" ) final String userAgent )
        throws IOException
    {
        log.debug( "Received request to evaluate policy for app id {}, scan id {}, stageTypeId {}",
                   applicationPublicId, scanId, stage.getStageTypeId() );

        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        final PolicyDAO policyDAO = new PolicyDAO( work.getWorkDir() );

        final File reportFile = ReportResource.fetchReport( work, proxy, applicationPublicId, appId, scanId, true );

        final ReportEntry licenseReportEntry = Report.getEntry( reportFile, "licenses.json" );
        final ReportEntry securityReportEntry = Report.getEntry( reportFile, "security.json" );
        final ReportEntry bomReportEntry = Report.getEntry( reportFile, "bom.json" );

        final List<Component> components =
            new ComponentDAO().getAll( appId, licenseReportEntry.buf, securityReportEntry.buf, bomReportEntry.buf );

        final List<PolicyAlert> alerts = new PolicyEvaluator().evaluate( appId, stage, policyDAO, components );

        Report.putEntry( reportFile, "policyalerts.json", JsonUtils.generate( JsonUtils.aaData( alerts ) ) );
        Report.putEntry( reportFile, "policythreats.json", JsonUtils.generate( analyzeThreats( alerts ) ) );

        ReportResource.flushReportChanges( appId, scanId ); // ensure policy count is recalculated on fetch

        final List<PolicyAlert> oldAlerts = findOldPolicyAlerts( applicationPublicId, appId, scanId, stage );

        @SuppressWarnings( "unchecked" )
        List<PolicyAlert>[] digest = new List[] { alerts, Collections.emptyList() };
        if ( !oldAlerts.isEmpty() )
        {
            digest = PolicyAlertDigester.digestPolicyAlerts( alerts, oldAlerts );
        }

        if ( digest != null )
        {
            sendNotifications( applicationPublicId, appId, scanId, stage, digest );
        }

        final PolicyEvaluation policyEvaluation = new PolicyEvaluation();
        policyEvaluation.setAlerts( alerts );
        calculateCounters( policyEvaluation );

        return policyEvaluation;
    }

    private void calculateCounters( PolicyEvaluation policyEvaluation )
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
            else if ( level >= 1 )
            {
                moderateCount++;
            }
        }

        policyEvaluation.setAffectedComponentCount( componentThreatLevels.size() );
        policyEvaluation.setCriticalComponentCount( criticalCount );
        policyEvaluation.setSevereComponentCount( severeCount );
        policyEvaluation.setModerateComponentCount( moderateCount );
    }

    protected List<PolicyAlert> findOldPolicyAlerts( final String applicationPublicId, String appId,
                                                     final String scanId, final Stage stage )
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
                final File reportFile =
                    ReportResource.fetchReport( work, proxy, applicationPublicId, appId, oldScanId, true );
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

    static Map<String, Object> createPolicyMailModel( final String serverUrl, final String cdnUrl,
                                                      final String applicationPublicId, final String scanId,
                                                      final Stage stage, final List<PolicyAlert> policyAlerts )
    {
        MailPolicyAlertCounts counts = new MailPolicyAlertCounts( policyAlerts );

        Collections.sort( policyAlerts, new Comparator<PolicyAlert>()
        {
            @Override
            public int compare( PolicyAlert o1, PolicyAlert o2 )
            {
                int t1 = o1.getTrigger().getThreatLevel();
                int t2 = o2.getTrigger().getThreatLevel();
                int r = t2 - t1;
                if ( r == 0 )
                {
                    r =
                        String.CASE_INSENSITIVE_ORDER.compare( o1.getTrigger().getPolicyName(),
                                                               o2.getTrigger().getPolicyName() );
                }
                return r;
            }
        } );

        final Map<String, Object> model = new HashMap<String, Object>();

        model.put( "cdnUrl", cdnUrl );
        model.put( "detailedReportUrl", serverUrl + ReportResource.getReportPath( applicationPublicId, scanId ) );
        model.put( "policyAlerts", policyAlerts );
        model.put( "policyThreatStage", StageTypes.getById( stage.getStageTypeId() ).getName() );
        model.put( "policyThreatApp", applicationPublicId );
        model.put( "policyThreatTime", new SimpleDateFormat( "MMMM dd, yyyy", Locale.ENGLISH ).format( new Date() ) );
        model.put( "policyThreatRedCount", counts.red );
        model.put( "policyThreatOrangeCount", counts.orange );
        model.put( "policyThreatYellowCount", counts.yellow );
        model.put( "policyThreatBlueCount", counts.blue );
        model.put( "actionTypes", ActionTypes.getAll() );

        return model;
    }

    static String createPolicyMailSubject( MailPolicyAlertCounts counts )
    {
        StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( "Policy Alert: " );
        int total = counts.red + counts.orange + counts.yellow + counts.blue;
        int highest = 0;
        if ( counts.red > 0 )
        {
            buffer.append( highest = counts.red ).append( " critical" );
        }
        else if ( counts.orange > 0 )
        {
            buffer.append( highest = counts.orange ).append( " severe" );
        }
        else if ( counts.yellow > 0 )
        {
            buffer.append( highest = counts.yellow ).append( " moderate" );
        }
        else if ( counts.blue > 0 )
        {
            buffer.append( highest = counts.blue ).append( " neutral" );
        }
        buffer.append( " violation" ).append( highest != 1 ? "s" : "" );
        buffer.append( " out of " ).append( total );
        return buffer.toString();
    }

    private String summarizeThreats( final String applicationPublicId, final String appId, final String scanId,
                                     final Stage stage, final List<PolicyAlert> policyAlerts )
        throws IOException
    {
        final Map<String, Object> model =
            createPolicyMailModel( baseUrl.get(), mail.getCdnUrl(), applicationPublicId, scanId, stage, policyAlerts );
        return TemplateUtils.render( getPolicyThreatsTemplate(), model );
    }

    private synchronized static Template getPolicyThreatsTemplate()
        throws IOException
    {
        if ( policyThreatsTemplate == null )
        {
            policyThreatsTemplate = TemplateUtils.createFreemarkerConfig().getTemplate( "policythreats.ftl" );
        }
        return policyThreatsTemplate;
    }

    private void sendNotifications( final String applicationPublicId, String appId, final String scanId,
                                    final Stage stage, final List<PolicyAlert>[] digest )
    {
        for ( final Entry<String, List<PolicyAlert>> details : byRecipients( digest[0] ).entrySet() )
        {
            try
            {
                final String mailId = "SONATYPE-CLM-" + applicationPublicId + '-' + scanId;
                final List<Address> addresses = Arrays.asList( new Address( details.getKey() ) );
                final String subject = createPolicyMailSubject( new MailPolicyAlertCounts( details.getValue() ) );
                final String body = summarizeThreats( applicationPublicId, appId, scanId, stage, details.getValue() );
                mail.sendHtml( mailId, addresses, subject, body );
            }
            catch ( final Exception e )
            {
                log.error( "Unable to send notification to: " + details.getKey(), e );
            }
        }

        // TODO: notify about cleared policy alerts...
    }

    private static Map<String, List<PolicyAlert>> byRecipients( final List<PolicyAlert> alerts )
    {
        final Map<String, List<PolicyAlert>> byRecipients = new HashMap<String, List<PolicyAlert>>();
        for ( final PolicyAlert alert : alerts )
        {
            for ( final Action action : alert.getActions() )
            {
                if ( NotifyActionType.ID.equals( action.getActionTypeId() ) )
                {
                    final String address = action.getTarget();
                    List<PolicyAlert> personalAlerts = byRecipients.get( address );
                    if ( personalAlerts == null )
                    {
                        byRecipients.put( address, personalAlerts = new ArrayList<PolicyAlert>() );
                    }
                    if ( !personalAlerts.contains( alert ) )
                    {
                        personalAlerts.add( alert );
                    }
                }
            }
        }
        return byRecipients;
    }

    static class MailPolicyAlertCounts
    {
        public int red, orange, yellow, blue;

        public MailPolicyAlertCounts( final int red, final int orange, final int yellow, final int blue )
        {
            this.red = red;
            this.orange = orange;
            this.yellow = yellow;
            this.blue = blue;
        }

        public MailPolicyAlertCounts( final List<PolicyAlert> alerts )
        {
            for ( PolicyAlert alert : alerts )
            {
                int level = alert.getTrigger().getThreatLevel();
                int components = alert.getTrigger().getComponentFacts().size();

                if ( level > 7 )
                {
                    red += components;
                }
                else if ( level > 3 )
                {
                    orange += components;
                }
                else if ( level > 0 )
                {
                    yellow += components;
                }
                else
                {
                    blue += components;
                }
            }
        }
    }

}
