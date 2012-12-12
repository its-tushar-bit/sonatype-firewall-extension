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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.PolicyFact;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sun.jersey.api.NotFoundException;

@Path( PolicyEvaluateResource.SERVICE_PATH )
public class PolicyEvaluateResource
{
    public static final String SERVICE_PATH = "rest/policy/{appId}/evaluate";

    private static final Logger log = LoggerFactory.getLogger( PolicyEvaluateResource.class );

    @Context
    private InsightWork work;

    @Context
    private InsightProxy proxy;

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public List<PolicyFact> evaluate( @PathParam( "appId" ) final String appId,
                                      @QueryParam( "scanId" ) final String scanId )
        throws IOException
    {
        log.debug( "Received request to evaluate policy for app id {}, scan id {}", appId, scanId );

        final File policyDir = work.getPolicyDir();
        final PolicyDAO policyDAO = new PolicyDAO( policyDir );
        final List<Policy> policies = policyDAO.getByApplicationId( appId );

        final File reportFile = work.getReportFile( scanId );
        if ( !reportFile.exists() )
        {
            if ( !ReportResource.downloadReport( proxy, appId, scanId, reportFile ) )
            {
                throw new NotFoundException( "Could not download the report for scan id " + scanId );
            }
        }

        final ReportEntry licenseReportEntry = Report.getEntry( reportFile, "licenses.json" );
        final ReportEntry securityReportEntry = Report.getEntry( reportFile, "security.json" );
        final ComponentDAO componentDAO = new ComponentDAO();
        final List<Component> components = componentDAO.getAll( licenseReportEntry.buf, securityReportEntry.buf );
        final PolicyEvaluator policyEvaluator = new PolicyEvaluator();
        final List<PolicyFact> result = policyEvaluator.evaluate( policies, components );
        return result;
    }
}
