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
import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
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
    public static final String SERVICE_PATH = "rest/policy/{appId}/evaluate";

    private static final Logger log = LoggerFactory.getLogger( PolicyEvaluateResource.class );

    private static Template policyThreatsTemplate;

    @Context
    private InsightWork work;

    @Context
    private InsightProxy proxy;

    @Context
    private UriInfo uriInfo;

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public List<PolicyAlert> evaluate( @PathParam( "appId" ) final String appId,
                                       @QueryParam( "scanId" ) final String scanId, final Stage stage )
        throws IOException
    {
        log.debug( "Received request to evaluate policy for app id {}, scan id {}, stageTypeId {}", appId, scanId,
                   stage.getStageTypeId() );

        final PolicyDAO policyDAO = new PolicyDAO( work.getWorkDir() );
        final List<Policy> policies = policyDAO.getByApplicationId( appId );

        final File reportFile = ReportResource.fetchReport( work, proxy, appId, scanId );

        final ReportEntry licenseReportEntry = Report.getEntry( reportFile, "licenses.json" );
        final ReportEntry securityReportEntry = Report.getEntry( reportFile, "security.json" );
        final ReportEntry bomReportEntry = Report.getEntry( reportFile, "bom.json" );
        final ReportEntry dependenciesReportEntry = Report.getEntry( reportFile, "dependencies.json" );

        final List<Component> components =
            new ComponentDAO().getAll( licenseReportEntry.buf, securityReportEntry.buf, bomReportEntry.buf,
                                       dependenciesReportEntry.buf );
        final List<PolicyAlert> result = new PolicyEvaluator().evaluate( stage, policies, components );

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

        model.put( "detailedReportUrl", uriInfo.getBaseUri()
            + ReportResource.SERVICE_PATH.replace( "{appId}", appId ).replace( "{scanId}", scanId ) + "/embedReport/" );

        model.put( "policyAlerts", policyAlerts );

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
