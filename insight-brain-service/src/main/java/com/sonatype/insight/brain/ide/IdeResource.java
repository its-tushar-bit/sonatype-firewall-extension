/*
 * Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
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
import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.clm.dto.model.ide.IdeMatchedComponent;
import com.sonatype.clm.dto.model.ide.MatchedComponent;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluator;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.saas.AugmentUtil;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

@Named
@Path( IdeResource.SERVICE_PATH )
@ProductLicenseEnforcementPoint( CLMEnforcementPoint.Develop )
public class IdeResource
{
    public static final String SERVICE_PATH = "rest/ide";

    @Context
    private SaasClient client;

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private PolicyEvaluator evaluator = new PolicyEvaluator();

    @Context
    private InsightWork work;

    @Context
    private BaseUrl baseUrl;

    /**
     * Requests an asset from the SaaS
     *
     * @return the response from the SaaS
     * @since 1.2
     */
    @GET
    @Path( "asset/{path:.*}" )
    public Response getAsset( @PathParam( "path" ) String path, @Context HttpServletRequest req )
        throws IOException
    {
        return client.doProxy( req, "ide/{path}", path );
    }

    /**
     * Get the result from a scan request, or a wait delta
     *
     * @param scanType simple or enhanced though we do not enforce that in the Brain
     * @param applicationPublicId the public application id
     * @return the result of the scan or a wait delta
     * @since 1.2
     */
    @GET
    @Path( "scan/{scanType}/{applicationPublicId}/{path:.*}" )
    @Produces( MediaType.APPLICATION_JSON )
    public IdeMatchedComponent doScan( @PathParam( "scanType" ) String scanType,
                                       @PathParam( "applicationPublicId" ) String applicationPublicId,
                                       @PathParam( "path" ) String path,
                                       @QueryParam( "proprietary" ) boolean proprietary, @Context HttpServletRequest req )
        throws IOException
    {
        Application app = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String applicationId = app.getId();

        MatchedComponent matchedComponent =
            client.get( req, MatchedComponent.class, "rest/ide/scan/{scanType}/{path}", scanType,
                        path );

        IdeMatchedComponent ideComponent = getComponent( matchedComponent );
        if ( ideComponent.getWaitDelta() == null
            && ( !"unknown".equals( ideComponent.getMatchState() ) || !ideComponent.isSimpleMatch() ) )
        {
            ObjectNode licenseData =
                AugmentUtil.getLicenseData( work, applicationId, matchedComponent.getGroupId(),
                                            matchedComponent.getArtifactId(), matchedComponent.getVersion() );
            ArrayNode svData =
                AugmentUtil.getSVData( work, applicationId, matchedComponent.getGroupId(),
                                       matchedComponent.getArtifactId(), matchedComponent.getVersion(),
                                       matchedComponent.getSecurityVulnerabilities() );

            ComponentDAO componentDAO = new ComponentDAO();
            Component component = componentDAO.getComponent( applicationId, matchedComponent, licenseData, svData );
            component.setProprietary( proprietary );
            List<PolicyAlert> policyAlerts =
                evaluator.evaluate( applicationId, new Stage( DevelopStageType.ID ), policyDAO(),
                                    Collections.singletonList( component ) );
            ideComponent.setAlerts( policyAlerts );
        }
        return ideComponent;
    }

    /**
     * Submit a scan request, may return the result or a wait delta.
     *
     * @param scanType simple or enhanced though we do not enforce that in the Brain
     * @param applicationPublicId the public applicationId
     * @return the result of the scan or a wait delta
     * @since 1.2
     */
    @POST
    @Path( "scan/{scanType}/{applicationPublicId}/{path:.*}" )
    @Produces( MediaType.APPLICATION_JSON )
    public IdeMatchedComponent postScan( @PathParam( "scanType" ) String scanType,
                                         @PathParam( "applicationPublicId" ) String applicationPublicId,
                                         @PathParam( "path" ) String path,
                                         @QueryParam( "proprietary" ) boolean proprietary,
                                         @Context HttpServletRequest req )
        throws IOException
    {
        return doScan( scanType, applicationPublicId, path, proprietary, req );
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

    /**
     * Gets the list of available versions for a given GA from the SaaS. (e.g. for use by migration wizard)
     *
     * @return the SaaS response
     * @since 1.3
     */
    @GET
    @Path( "component/versions" )
    public Response getVersions( @Context HttpServletRequest req )
        throws IOException
    {
        return client.doProxy( req, "rest/ide/artifact/versions" );
    }

    /**
     * Access a Brain resource
     *
     * @param path the path from the brain root
     * @since 1.3
     */
    @GET
    @Path( "brain/{path:.*}" )
    public Response brainGet( final @PathParam( "path" ) String path )
    {
        UriBuilder uriBuilder = baseUrl.redirect().path( path );

        return Response.temporaryRedirect( uriBuilder.build() ).build();
    }
}
