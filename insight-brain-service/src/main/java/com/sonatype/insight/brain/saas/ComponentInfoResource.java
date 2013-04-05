/*
 * Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;

@Path( ComponentInfoResource.SERVICE_PATH )
public class ComponentInfoResource
{
    public static final String SERVICE_PATH = "rest/{tool : ide|ci}/component/details";

    private static final Logger log = LoggerFactory.getLogger( ComponentInfoResource.class );

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private LicenseDAO licenseDAO = new LicenseDAO();

    private PolicyEvaluator evaluator = new PolicyEvaluator();

    @Context
    private SaasClient client;

    @Context
    private InsightWork work;

    @GET
    @Path( "versions/{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getComponentVersionDetails( @Context HttpServletRequest servletRequest,
                                                @PathParam( "tool" ) String tool,
                                                @PathParam( "applicationPublicId" ) String applicationPublicId,
                                                @QueryParam( "instanceId" ) String instanceId,
                                                @QueryParam( "groupId" ) String groupId,
                                                @QueryParam( "artifactId" ) String artifactId,
                                                @QueryParam( "version" ) String version )
        throws IOException
    {
        log.debug( "Getting {} component version details for application id {}, GAV {}:{}:{}.", tool,
                   applicationPublicId, groupId, artifactId, version );
        return client.doProxy( servletRequest, "rest/ide/component/details/versions/{appId}", applicationPublicId );
    }

    @GET
    @Path( "{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public ComponentDetails getComponentDetails( @Context HttpServletRequest servletRequest,
                                                 @PathParam( "tool" ) String tool,
                                                 @PathParam( "applicationPublicId" ) String applicationPublicId,
                                                 @QueryParam( "instanceId" ) String instanceId,
                                                 @QueryParam( "groupId" ) String groupId,
                                                 @QueryParam( "artifactId" ) String artifactId,
                                                 @QueryParam( "version" ) String version,
                                                 @QueryParam( "hash" ) String hash,
                                                 @QueryParam( "matchState" ) String matchState )
        throws IOException
    {
        log.debug( "Getting {} component details for application id {}, GAV {}:{}:{}, hash {}.", tool, applicationPublicId,
                   groupId, artifactId, version, hash );
        Application app = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String applicationId = app.getId();

        // Get component details from the SAAS server
        ComponentDetails componentDetails;
        try
        {
            componentDetails =
                client.get( servletRequest, ComponentDetails.class, "rest/ide/component/details/{appId}",
                            applicationPublicId );
        }
        catch ( NotFoundException e )
        {
            // GAV is unknown to SaaS, still want to provide minimal data for details view
            componentDetails = new ComponentDetails( groupId, artifactId, version );
        }

        if ( hash != null && !hash.isEmpty() )
        {
            componentDetails.setHash( hash );
        }
        if ( matchState != null && !matchState.isEmpty() )
        {
            componentDetails.setMatchState( matchState );
        }
        else
        {
            componentDetails.setMatchState( MatchState.EXACT.getId() );
        }

        // Load the augmented data for licenses and security vulnerabilities
        ObjectNode licenseData =
            AugmentUtil.getLicenseData( work, applicationId, componentDetails.getGroupId(),
                                        componentDetails.getArtifactId(), componentDetails.getVersion() );
        ArrayNode svData =
            AugmentUtil.getSVData( work, applicationId, componentDetails.getGroupId(),
                                   componentDetails.getArtifactId(), componentDetails.getVersion(),
                                   componentDetails.getSecurityVulnerabilities() );

        // Load all data into a Component instance for policy evaluation
        ComponentDAO componentDAO = new ComponentDAO();
        Component component = componentDAO.getComponent( applicationId, componentDetails, licenseData, svData );

        // Use CLM data to populate the component details
        for ( String overriddenLicenseId : component.getOverriddenLicenseIds() )
        {
            com.sonatype.insight.brain.model.license.License overriddenLicense =
                licenseDAO.getByIdNotNull( overriddenLicenseId );
            componentDetails.getOverriddenLicenses().add( new License( overriddenLicense.getId(),
                                                                       overriddenLicense.getShortDisplayName() ) );
        }
        if ( !component.getLicenseThreatGroups().isEmpty() )
        {
            int licenseThreatLevel = 0;
            for ( LicenseThreatGroup licenseThreatGroup : component.getLicenseThreatGroups() )
            {
                licenseThreatLevel = Math.max( licenseThreatLevel, licenseThreatGroup.getThreatLevel() );
            }
            componentDetails.setLicenseThreatLevel( licenseThreatLevel );
        }
        if ( componentDetails.getSecurityVulnerabilities() != null )
        {
            for ( SecurityVulnerability issue : componentDetails.getSecurityVulnerabilities() )
            {
                issue.setStatus( SecurityVulnerabilityStatus.OPEN.getName() );
                for ( com.sonatype.insight.brain.model.component.SecurityVulnerability sv : component.getSecurityVulnerabilities() )
                {
                    if ( issue.getRefId().equals( sv.getRefId() ) && issue.getSource().equals( sv.getSource() ) )
                    {
                        issue.setStatus( sv.getStatus().getName() );
                        break;
                    }
                }
            }
        }

        // Evaluate the policies
        List<PolicyAlert> policyAlerts =
            evaluator.evaluate( applicationId, new Stage( DevelopStageType.ID ), policyDAO(),
                                Collections.singletonList( component ) );
        componentDetails.setPolicyAlerts( policyAlerts );

        return componentDetails;
    }

    private PolicyDAO policyDAO()
    {
        return new PolicyDAO( work.getWorkDir() );
    }

}
