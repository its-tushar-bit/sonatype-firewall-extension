/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.facts.ComponentFact;
import com.sonatype.insight.brain.model.policy.facts.ConstraintFact;
import com.sonatype.insight.brain.model.policy.facts.PolicyFact;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
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
    private UriInfo uriInfo;

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public List<PolicyAlert> evaluate( @PathParam( "applicationPublicId" ) final String applicationPublicId,
                                       @QueryParam( "scanId" ) final String scanId, final Stage stage )
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
        Report.putEntry( reportFile, "policythreats.html", summarizeThreats( appId, scanId, result ) );

        return result;
    }

    private static ObjectNode analyzeThreats( final List<PolicyAlert> policyAlerts )
    {
        final Map<String, JsonNode> componentThreats = new HashMap<String, JsonNode>();
        for ( final PolicyAlert alert : policyAlerts )
        {
            final PolicyFact trigger = alert.getTrigger();
            final int threatLevel = trigger.getThreatLevel();
            for ( final ConstraintFact constraint : trigger.getConstraintFacts() )
            {
                for ( final ComponentFact component : constraint.getComponentFacts() )
                {
                    final String gav = component.getGAV();
                    ObjectNode threat = (ObjectNode) componentThreats.get( gav );
                    if ( threat == null )
                    {
                        threat = JsonUtils.asTree( component );
                        componentThreats.put( gav, threat );
                    }
                    if ( threatLevel > threat.path( "policyThreatLevel" ).asInt( -1 ) )
                    {
                        threat.put( "constraintId", constraint.getConstraintId() );
                        threat.put( "constraintName", constraint.getConstraintName() );
                        threat.put( "policyId", trigger.getPolicyId() );
                        threat.put( "policyName", trigger.getPolicyName() );
                        threat.put( "policyThreatLevel", threatLevel );
                    }
                }
            }
        }

        final ObjectNode threats = JsonUtils.objectNode( null );
        threats.withArray( "aaData" ).addAll( componentThreats.values() );
        return threats;
    }

    private String summarizeThreats( final String appId, final String scanId, final List<PolicyAlert> policyAlerts )
        throws IOException
    {
        final Map<String, Object> model = new HashMap<String, Object>();

        model.put( "detailedReportUrl",
                   uriInfo.getBaseUri()
                       + ReportResource.SERVICE_PATH.replace( "{applicationPublicId}", appId ).replace( "{scanId}",
                                                                                                        scanId )
                       + "/embedReport/" );

        model.put( "policyAlerts", policyAlerts );
        model.put( "policyThreatStage", "Build" );
        model.put( "policyThreatApp", "Foo" );
        model.put( "policyThreatTime", "January 10, 2013" );
        model.put( "policyThreatLocation", "Hudson-10" );
        model.put( "policyThreatRedCount", 4 );
        model.put( "policyThreatOrangeCount", 3 );
        model.put( "policyThreatYellowCount", 2 );
        model.put( "policyThreatBlueCount", 5 );
        
        //TODO: policyThreatStage
        //TODO: policyThreatApp
        //TODO: policyThreatTime
        //TODO: policyThreatLocation
        //TODO: policyThreatRedCount
        //TODO: policyThreatOrangeCount
        //TODO: policyThreatYellowCount
        //TODO: policyThreatBlueCount
        //TODO: need to get proper action text into the DTO for display (i.e. Build failed, Notification Sent, etc.)
        //TODO: need to get proper condition text and condition failure text into the DTO
        //TODO: no need to have a list of ComponentFact objects in the ConstraintFact, as we are displaying 1 GAV per item
        //seems should be rearranged to be like so policy -> constraints -> condition
        //rather than how it is now, which doesn't seem to match what we need in the email at all

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
}
