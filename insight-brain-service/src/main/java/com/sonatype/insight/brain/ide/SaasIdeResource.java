/*
 * Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.clm.dto.model.MatchedComponent;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.ide.IdeMatchedComponent;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.facts.PolicyFact;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

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

    private ArrayNode getAugmentedLicenseData( String applicationId, MatchedComponent matchedComponent )
        throws IOException
    {
        ArrayNode licenseData = new ArrayNode( JsonNodeFactory.instance );
        ObjectNode gavNode = licenseData.objectNode();
        licenseData.add( gavNode );
        gavNode.put( "groupId", matchedComponent.getGroupId() );
        gavNode.put( "artifactId", matchedComponent.getArtifactId() );
        gavNode.put( "version", matchedComponent.getVersion() );
        File auditDir = work.getAuditDir( applicationId );
        licenseData = (ArrayNode) JsonUtils.fileStore( auditDir ).augment( licenseData, "licenses.json" );
        return licenseData;
    }

    private ArrayNode getAugmentedSVData( String applicationId, MatchedComponent matchedComponent )
        throws IOException
    {
        List<SecurityVulnerability> securityVulnerabilities = matchedComponent.getSecurityThreats();
        if ( securityVulnerabilities == null || securityVulnerabilities.isEmpty() )
        {
            return null;
        }
        ArrayNode svData = new ArrayNode( JsonNodeFactory.instance );
        for ( SecurityVulnerability securityVulnerability : securityVulnerabilities )
        {
            ObjectNode svNode = svData.objectNode();
            svData.add( svNode );
            svNode.put( "groupId", matchedComponent.getGroupId() );
            svNode.put( "artifactId", matchedComponent.getArtifactId() );
            svNode.put( "version", matchedComponent.getVersion() );
            svNode.put( "reference", securityVulnerability.getRefId() );
            svNode.put( "source", securityVulnerability.getSource() );
        }
        File auditDir = work.getAuditDir( applicationId );
        svData = (ArrayNode) JsonUtils.fileStore( auditDir ).augment( svData, "security.json" );
        return svData;
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

        IdeMatchedComponent ideComponent = getComponent( matchedComponent );
        if ( ideComponent.getWaitDelta() == null && !"unknown".equals( ideComponent.getMatchState() ) )
        {
            ArrayNode licenseData = getAugmentedLicenseData( applicationId, matchedComponent );
            ArrayNode svData = getAugmentedSVData( applicationId, matchedComponent );

            ComponentDAO componentDAO = new ComponentDAO();
            Component component =
                componentDAO.getComponent( applicationId, matchedComponent, licenseData.get( 0 ), svData );
            List<PolicyAlert> alerts =
                evaluator.evaluate( applicationId, new Stage( DevelopStageType.ID ),
                                    policyDAO().getByApplicationId( applicationId ),
                                    Collections.singletonList( component ) );
            ideComponent.setAlerts( toPolicyAlertsDTO( alerts ) );
        }
        return ideComponent;
    }

    private PolicyDAO policyDAO()
    {
        return new PolicyDAO( work.getWorkDir() );
    }

    private static List<com.sonatype.clm.dto.model.policy.PolicyAlert> toPolicyAlertsDTO( List<PolicyAlert> alerts )
    {
        List<com.sonatype.clm.dto.model.policy.PolicyAlert> dtoAlerts =
            new ArrayList<com.sonatype.clm.dto.model.policy.PolicyAlert>( alerts.size() );
        for ( PolicyAlert alert : alerts )
        {
            dtoAlerts.add( new com.sonatype.clm.dto.model.policy.PolicyAlert( toDTO( alert.getTrigger() ),
                                                                              toActionsDTO( alert.getActions() ) ) );
        }
        return dtoAlerts;
    }

    private static com.sonatype.clm.dto.model.policy.PolicyFact toDTO( PolicyFact fact )
    {
        return new com.sonatype.clm.dto.model.policy.PolicyFact( fact.getPolicyId(), fact.getPolicyName(),
                                                                 fact.getThreatLevel() );
    }

    private static List<com.sonatype.clm.dto.model.policy.Action> toActionsDTO( List<Action> actions )
    {
        List<com.sonatype.clm.dto.model.policy.Action> dtoActions =
            new ArrayList<com.sonatype.clm.dto.model.policy.Action>( actions.size() );
        for ( Action action : actions )
        {
            dtoActions.add( new com.sonatype.clm.dto.model.policy.Action( action.getActionTypeId(), action.getTarget() ) );
        }
        return dtoActions;
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