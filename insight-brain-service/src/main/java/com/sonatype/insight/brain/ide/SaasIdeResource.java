/*
 * Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.MatchedComponent;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.clm.dto.model.ide.IdeMatchedComponent;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

@Path( SaasIdeResource.SERVICE_PATH )
public class SaasIdeResource
{
    public static final String SERVICE_PATH = "rest/ide";

    private static final Logger log = LoggerFactory.getLogger( SaasIdeResource.class );

    @Context
    private SaasClient client;

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private LicenseDAO licenseDAO = new LicenseDAO();

    private PolicyEvaluator evaluator = new PolicyEvaluator();

    @Context
    private InsightWork work;

    @GET
    @Path( "cip/{path:.*}" )
    public Response getCipResource( @PathParam( "path" ) String path, @Context HttpServletRequest req )
        throws IOException
    {
        return client.doProxy( req, "ide", path );
    }

    @GET
    @Path( "details/{applicationPublicId}/{path:.*}" )
    public Response getDetailsResource( @PathParam( "path" ) String path,
                                        @PathParam( "applicationPublicId" ) String applicationPublicId,
                                        @Context HttpServletRequest req )
        throws IOException
    {
        return client.doProxy( req, "rest/ide/artifact/detail/", applicationPublicId, path );
    }

    @GET
    @Path( "artifact/{path:.*}" )
    public Response getArtifactInfo( @PathParam( "path" ) String path, @Context HttpServletRequest req )
        throws IOException
    {
        return client.doProxy( req, "rest/ide/artifact/", path );
    }

    @GET
    @Path( "component/details/versions/{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getComponentVersionDetails( @Context HttpServletRequest servletRequest,
                                                @PathParam( "applicationPublicId" ) String applicationPublicId,
                                                @QueryParam( "instanceId" ) String instanceId,
                                                @QueryParam( "groupId" ) String groupId,
                                                @QueryParam( "artifactId" ) String artifactId,
                                                @QueryParam( "version" ) String version )
        throws IOException
    {
        log.debug( "Getting component version details for application id {}, GAV {}:{}:{}.", applicationPublicId,
                   groupId, artifactId, version );
        return client.doProxy( servletRequest, "rest/ide/component/details/versions", applicationPublicId );
    }

    @GET
    @Path( "component/details/{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public ComponentDetails getComponentDetails( @Context HttpServletRequest servletRequest,
                                                 @PathParam( "applicationPublicId" ) String applicationPublicId,
                                                 @QueryParam( "instanceId" ) String instanceId,
                                                 @QueryParam( "groupId" ) String groupId,
                                                 @QueryParam( "artifactId" ) String artifactId,
                                                 @QueryParam( "version" ) String version )
        throws IOException
    {
        log.debug( "Getting component details for application id {}, GAV {}:{}:{}.", applicationPublicId, groupId,
                   artifactId, version );
        Application app = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String applicationId = app.getId();

        ComponentDetails componentDetails =
            client.get( servletRequest, ComponentDetails.class, "rest/ide/component/details", applicationPublicId );

        ObjectNode licenseData =
            getAugmentedLicenseData( applicationId, componentDetails.getGroupId(), componentDetails.getArtifactId(),
                                     componentDetails.getVersion() );
        ArrayNode svData =
            getAugmentedSVData( applicationId, componentDetails.getGroupId(), componentDetails.getArtifactId(),
                                componentDetails.getVersion(), componentDetails.getSecurityVulnerabilities() );

        ComponentDAO componentDAO = new ComponentDAO();
        Component component = componentDAO.getComponent( applicationId, componentDetails, licenseData, svData );
        for ( String overriddenLicenseId : component.getOverriddenLicenseIds() )
        {
            com.sonatype.insight.brain.model.license.License overriddenLicense =
                licenseDAO.getByIdNotNull( overriddenLicenseId );
            componentDetails.getOverriddenLicenses().add( new License( overriddenLicense.getId(),
                                                                       overriddenLicense.getShortDisplayName() ) );
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
        List<PolicyAlert> policyAlerts =
            evaluator.evaluate( applicationId, new Stage( DevelopStageType.ID ),
                                policyDAO().getByApplicationId( applicationId ), Collections.singletonList( component ) );
        componentDetails.setPolicyAlerts( policyAlerts );

        return componentDetails;
    }

    private ObjectNode getAugmentedLicenseData( String applicationId, String groupId, String artifactId, String version )
        throws IOException
    {
        ArrayNode licenseData = new ArrayNode( JsonNodeFactory.instance );
        ObjectNode gavNode = licenseData.objectNode();
        licenseData.add( gavNode );
        gavNode.put( "groupId", groupId );
        gavNode.put( "artifactId", artifactId );
        gavNode.put( "version", version );
        File auditDir = work.getAuditDir( applicationId );
        JsonUtils.fileStore( auditDir ).augment( licenseData, "licenses.json" );
        return (ObjectNode) licenseData.get( 0 );
    }

    private ArrayNode getAugmentedSVData( String applicationId, String groupId, String artifactId, String version,
                                          List<SecurityVulnerability> securityVulnerabilities )
        throws IOException
    {
        if ( securityVulnerabilities == null || securityVulnerabilities.isEmpty() )
        {
            return null;
        }
        ArrayNode svData = new ArrayNode( JsonNodeFactory.instance );
        for ( SecurityVulnerability securityVulnerability : securityVulnerabilities )
        {
            ObjectNode svNode = svData.objectNode();
            svData.add( svNode );
            svNode.put( "groupId", groupId );
            svNode.put( "artifactId", artifactId );
            svNode.put( "version", version );
            svNode.put( "reference", securityVulnerability.getRefId() );
            svNode.put( "source", securityVulnerability.getSource() );
        }
        File auditDir = work.getAuditDir( applicationId );
        JsonUtils.fileStore( auditDir ).augment( svData, "security.json" );
        return svData;
    }

    @GET
    @Path( "scan/{scanType}/{applicationPublicId}/{path:.*}" )
    @Produces( MediaType.APPLICATION_JSON )
    public IdeMatchedComponent doScan( @PathParam( "scanType" ) String scanType,
                                       @PathParam( "applicationPublicId" ) String applicationPublicId,
                                       @PathParam( "path" ) String path, @Context HttpServletRequest req )
        throws IOException
    {
        Application app = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String applicationId = app.getId();

        MatchedComponent matchedComponent =
            client.get( req, MatchedComponent.class, "rest/ide/scan", scanType, applicationPublicId, path );

        IdeMatchedComponent ideComponent = getComponent( matchedComponent );
        if ( ideComponent.getWaitDelta() == null && !"unknown".equals( ideComponent.getMatchState() ) )
        {
            ObjectNode licenseData =
                getAugmentedLicenseData( applicationId, matchedComponent.getGroupId(),
                                         matchedComponent.getArtifactId(), matchedComponent.getVersion() );
            ArrayNode svData =
                getAugmentedSVData( applicationId, matchedComponent.getGroupId(), matchedComponent.getArtifactId(),
                                    matchedComponent.getVersion(), matchedComponent.getSecurityThreats() );

            ComponentDAO componentDAO = new ComponentDAO();
            Component component =
                componentDAO.getComponent( applicationId, matchedComponent, licenseData, svData );
            List<PolicyAlert> policyAlerts =
                evaluator.evaluate( applicationId, new Stage( DevelopStageType.ID ),
                                    policyDAO().getByApplicationId( applicationId ),
                                    Collections.singletonList( component ) );
            ideComponent.setAlerts( policyAlerts );
        }
        return ideComponent;
    }

    @POST
    @Path( "scan/{scanType}/{applicationPublicId}/{path:.*}" )
    @Produces( MediaType.APPLICATION_JSON )
    public IdeMatchedComponent postScan( @PathParam( "scanType" ) String scanType,
                                         @PathParam( "applicationPublicId" ) String applicationPublicId,
                                         @PathParam( "path" ) String path, @Context HttpServletRequest req )
        throws IOException
    {
        return doScan( scanType, applicationPublicId, path, req );
    }

    private PolicyDAO policyDAO()
    {
        return new PolicyDAO( work.getWorkDir() );
    }

    private IdeMatchedComponent getComponent( MatchedComponent mComponent )
    {
        IdeMatchedComponent ide = new IdeMatchedComponent();
        ide.setArtifactId( mComponent.getArtifactId() );
        ide.setGroupId( mComponent.getGroupId() );
        ide.setVersion( mComponent.getVersion() );
        ide.setHash( mComponent.getHash() );
        ide.setMatchState( mComponent.getMatchState() );
        ide.setSimpleMatch( mComponent.isSimpleMatch() );
        ide.setWaitDelta( mComponent.getWaitDelta() );
        return ide;
    }
}
