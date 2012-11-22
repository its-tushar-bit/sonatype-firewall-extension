/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.RuleDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.PolicyFact;
import com.sonatype.insight.brain.model.rule.Rule;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sun.jersey.api.NotFoundException;

@Path( PolicyEvaluatorResource.SERVICE_PATH )
public class PolicyEvaluatorResource
{
    public static final String SERVICE_PATH = "rest/policy/evaluator/{appId}";

    private static final Logger log = LoggerFactory.getLogger( PolicyEvaluatorResource.class );

    @Context
    private InsightWork work;

    @Context
    private InsightProxy proxy;

    @Path( "{scanId}" )
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public List<PolicyFact> evaluate( @PathParam( "appId" ) String appId, @PathParam( "scanId" ) String scanId )
        throws IOException
    {
        log.debug( "Received request to evaluate policy for app id {}, scan id {}", appId, scanId );

        File ruleDir = work.getRuleDir();
        RuleDAO ruleDAO = new RuleDAO( ruleDir );
        List<Rule> rules = ruleDAO.getByApplicationId( appId );

        File reportFile = work.getReportFile( scanId );
        if ( !reportFile.exists() )
        {
            if ( !ReportResource.downloadReport( proxy, appId, scanId, reportFile ) )
            {
                throw new NotFoundException( "Could not download the report for scan id " + scanId );
            }
        }

        ReportEntry licenseReportEntry = Report.getEntry( reportFile, "licenses.json" );
        ReportEntry securityReportEntry = Report.getEntry( reportFile, "security.json" );
        ComponentDAO componentDAO = new ComponentDAO();
        List<Component> components = componentDAO.getAll( licenseReportEntry.buf, securityReportEntry.buf );
        PolicyEvaluator policyEvaluator = new PolicyEvaluator();
        List<PolicyFact> result = policyEvaluator.evaluate( rules, components );
        return result;
    }
}
