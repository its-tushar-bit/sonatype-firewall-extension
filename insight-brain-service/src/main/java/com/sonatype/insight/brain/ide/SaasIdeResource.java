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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.IdeMatchedComponent;
import com.sonatype.insight.brain.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.brain.MatchedComponent;

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
    @Path( "cip/{path:.*}" )
    public Response getCipResource( @PathParam( "path" ) String path, @Context HttpServletRequest req )
        throws IOException
    {
        return client.doProxy( req, "ide", path );
    }

    @GET
    @Path( "details/{appId}/{path:.*}" )
    public Response getDetailsResource( @PathParam( "path" ) String path,
                                        @PathParam( "appId" ) String applicationPublicId,
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
    @Path( "scan/{scanType}/{appId}/{path:.*}" )
    @Produces( MediaType.APPLICATION_JSON )
    public IdeMatchedComponent doScan( @PathParam( "scanType" ) String scanType,
                                       @PathParam( "appId" ) String applicationPublicId,
                                       @PathParam( "path" ) String path, @Context HttpServletRequest req )
        throws IOException
    {
        Application app = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String applicationId = app.getId();

        MatchedComponent matchedComponent =
            client.get( req, MatchedComponent.class, "rest/ide/scan", scanType, applicationPublicId, path );

        File auditDir = work.getAuditDir( applicationId );

        ArrayNode licenseData = null;
        if ( !"unknown".equals( matchedComponent.getMatchState() ) )
        {
            String json =
                "[{ \"groupId\" : \"" + matchedComponent.getGroupId() + "\", \"artifactId\" : \""
                    + matchedComponent.getArtifactId() + "\", \"version\" : \"" + matchedComponent.getVersion()
                    + "\" }]";
            licenseData = JsonUtils.parse( json );
            licenseData = (ArrayNode) JsonUtils.fileStore( auditDir ).augment( licenseData, "licenses.json" );
        }

        IdeMatchedComponent ideComponent = getComponent( matchedComponent );
        if ( ideComponent.getWaitDelta() == null
            && !MatchState.UNKNOWN.toString().equals( ideComponent.getMatchState() ) )
        {
            ComponentDAO componentDAO = new ComponentDAO();
            Component component = componentDAO.getComponent( applicationId, matchedComponent, licenseData.get( 0 ) );
            List<PolicyAlert> alerts =
                evaluator.evaluate( applicationId, new Stage( DevelopStageType.ID ),
                                    policyDAO().getByApplicationId( applicationId ),
                                    Collections.singletonList( component ) );
            ideComponent.setAlerts( alerts );
        }
        return ideComponent;
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