/*
 * Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
import com.sonatype.insight.brain.saas.AugmentUtil;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.service.InsightWork;

@Path( SaasIdeResource.SERVICE_PATH )
public class SaasIdeResource
{
    public static final String SERVICE_PATH = "rest/ide";

    @Context
    private SaasClient client;

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private PolicyEvaluator evaluator = new PolicyEvaluator();

    @Context
    private InsightWork work;

    @GET
    @Path( "asset/{path:.*}" )
    public Response getAsset( @PathParam( "path" ) String path, @Context HttpServletRequest req )
        throws IOException
    {
        return client.doProxy( req, "ide", path );
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
        if ( ideComponent.getWaitDelta() == null
            && ( !"unknown".equals( ideComponent.getMatchState() ) || !ideComponent.isSimpleMatch() ) )
        {
            ObjectNode licenseData =
                AugmentUtil.getLicenseData( work, applicationId, matchedComponent.getGroupId(),
                                            matchedComponent.getArtifactId(), matchedComponent.getVersion() );
            ArrayNode svData =
                AugmentUtil.getSVData( work, applicationId, matchedComponent.getGroupId(),
                                       matchedComponent.getArtifactId(), matchedComponent.getVersion(),
                                       matchedComponent.getSecurityThreats() );

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

    @GET
    @Path( "component/versions" )
    public Response getVersions( @Context HttpServletRequest req )
        throws IOException
    {
        return client.doProxy( req, "rest/ide/artifact/versions" );
    }
}
