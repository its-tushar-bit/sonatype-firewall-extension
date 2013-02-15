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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.micromailer.Address;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.facts.ComponentFact;
import com.sonatype.insight.brain.model.policy.facts.ConstraintFact;
import com.sonatype.insight.brain.model.policy.facts.PolicyFact;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
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

    private static final Pattern CI_PLUGIN_PRE_2_6 = Pattern.compile( "Insight_CI_[A-Za-z]*/2\\.[45].*" );

    private static final Logger log = LoggerFactory.getLogger( PolicyEvaluateResource.class );

    private static Template policyThreatsTemplate;

    @Context
    private InsightWork work;

    @Context
    private InsightProxy proxy;

    @Context
    private InsightMail mail;

    @Context
    private UriInfo uriInfo;

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public List<PolicyAlert> evaluate( @PathParam( "applicationPublicId" ) final String applicationPublicId,
                                       @QueryParam( "scanId" ) final String scanId, final Stage stage,
                                       @HeaderParam( "user-agent" ) final String userAgent )
        throws IOException
    {
        log.debug( "Received request to evaluate policy for app id {}, scan id {}, stageTypeId {}",
                   applicationPublicId, scanId, stage.getStageTypeId() );

        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        final PolicyDAO policyDAO = new PolicyDAO( work.getWorkDir() );
        final List<Policy> policies = policyDAO.getByApplicationId( appId );

        final File reportFile = ReportResource.fetchReport( work, proxy, applicationPublicId, appId, scanId, true );

        final ReportEntry licenseReportEntry = Report.getEntry( reportFile, "licenses.json" );
        final ReportEntry securityReportEntry = Report.getEntry( reportFile, "security.json" );
        final ReportEntry bomReportEntry = Report.getEntry( reportFile, "bom.json" );
        final ReportEntry dependenciesReportEntry = Report.getEntry( reportFile, "dependencies.json" );

        final List<Component> components =
            new ComponentDAO().getAll( appId, licenseReportEntry.buf, securityReportEntry.buf, bomReportEntry.buf,
                                       dependenciesReportEntry.buf );

        final List<PolicyAlert> result = new PolicyEvaluator().evaluate( appId, stage, policies, components );
        Report.putEntry( reportFile, "policythreats.json", JsonUtils.generate( analyzeThreats( result ) ) );

        final String policyThreatsHtml = summarizeThreats( appId, scanId, stage, result );
        Report.putEntry( reportFile, "policythreats.html", policyThreatsHtml );

        sendNotifications( "SONATYPE-CLM-" + applicationPublicId + "-" + scanId, policyThreatsHtml, result );

        if ( CI_PLUGIN_PRE_2_6.matcher( userAgent ).matches() )
        {
            /*
             * Hide componentFacts list from older clients who can't deserialize it
             */
            for ( final PolicyAlert alert : result )
            {
                alert.getTrigger().getComponentFacts().clear();
            }
        }

        return result;
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
                final String gav = component.getGAV();
                ObjectNode threat = (ObjectNode) componentThreats.get( gav );
                if ( threat == null )
                {
                    threat = JsonUtils.asTree( component );
                    threat.remove( "constraintFacts" );
                    componentThreats.put( gav, threat );
                }
                if ( threatLevel > threat.path( "policyThreatLevel" ).asInt( -1 ) )
                {
                    // log first constraint (must be at least one for component to be listed)
                    final ConstraintFact constraint = component.getConstraintFacts().get( 0 );
                    threat.put( "constraintId", constraint.getConstraintId() );
                    threat.put( "constraintName", constraint.getConstraintName() );
                    threat.put( "policyId", trigger.getPolicyId() );
                    threat.put( "policyName", trigger.getPolicyName() );
                    threat.put( "policyThreatLevel", threatLevel );
                }
            }
        }

        final ObjectNode threats = JsonUtils.objectNode( null );
        threats.withArray( "aaData" ).addAll( componentThreats.values() );
        return threats;
    }

    private String summarizeThreats( final String appId, final String scanId, final Stage stage, final List<PolicyAlert> policyAlerts )
        throws IOException
    {
        int red = 0;
        int orange = 0;
        int yellow = 0;
        int blue = 0;
        for ( PolicyAlert alert : policyAlerts )
        {
            if ( alert.getTrigger().getThreatLevel() > 7 )
            {
                red++;
            }
            else if ( alert.getTrigger().getThreatLevel() > 3 )
            {
                orange++;
            }
            else if ( alert.getTrigger().getThreatLevel() > 0 )
            {
                yellow++;
            }
            else
            {
                blue++;
            }
        }
        
        final Map<String, Object> model = new HashMap<String, Object>();

        model.put( "detailedReportUrl",
                   uriInfo.getBaseUri()
                       + ReportResource.SERVICE_PATH.replace( "{applicationPublicId}", appId ).replace( "{scanId}",
                                                                                                        scanId )
                       + "/embedReport/" );

        model.put( "policyAlerts", policyAlerts );
        model.put( "policyThreatStage", stage.getStageTypeId() );
        model.put( "policyThreatApp", appId );
        model.put( "policyThreatTime", new SimpleDateFormat( "MMMM dd, yyyy" ).format( new Date() ) );
        model.put( "policyThreatLocation", "Hudson-10" );
        model.put( "policyThreatRedCount", red );
        model.put( "policyThreatOrangeCount", orange );
        model.put( "policyThreatYellowCount", yellow );
        model.put( "policyThreatBlueCount", blue );
        model.put( "actionTypes", ActionTypes.getAll() );
        
        //TODO: policyThreatLocation
        //TODO: need to get proper condition failure detail text into the DTO (i.e. 'Component has GPL License' for a !license condition)
        
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

    private void sendNotifications( final String mailId, final String body, final List<PolicyAlert> policyAlerts )
    {
        try
        {
            final List<Address> recipients = new ArrayList<Address>();
            for ( final PolicyAlert policyAlert : policyAlerts )
            {
                for ( final Action action : policyAlert.getActions() )
                {
                    if ( NotifyActionType.ID.equals( action.getActionTypeId() ) )
                    {
                        recipients.add( new Address( action.getTarget() ) );
                    }
                }
            }
            if ( !recipients.isEmpty() )
            {
                mail.sendHtml( mailId, recipients, "Sonatype-CLM Policy Alert", body );
            }
        }
        catch ( final Exception e )
        {
            log.warn( "Unable to send notifications", e );
        }
    }
}
