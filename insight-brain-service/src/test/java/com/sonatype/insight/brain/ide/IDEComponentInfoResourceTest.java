/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.product.license.CLMEnforcementPoint;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class IDEComponentInfoResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testGetComponentDetails_PolicyAlerts()
        throws Exception
    {
        String applicationPublicId = "IdeResourceTest_AppId";
        Application application = createApplication( applicationPublicId );
        Label label = new Label( application.getId(), "white", null );
        new LabelDAO().insert( label );
        new ComponentLabelDAO().insert( new ComponentLabel( application.getId(), label.getId(), "01234567890123456789" ) );

        Constraint constraint1 = new Constraint( "C1", "Constraint 1", LogicalOperator.AND );
        Condition condition1 = new Condition( SecurityVulnerabilityConditionType.ID, "present" );
        constraint1.addCondition( condition1 );
        Policy policy1 = new Policy( "PolicyId1", "Policy1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        Action failAction = new Action( FailActionType.ID );
        policy1.addAction( BuildStageType.ID, failAction );
        addPolicy( applicationPublicId, policy1 );

        Constraint constraint2 = new Constraint( "C2", "Constraint 2", LogicalOperator.AND );
        constraint2.addCondition( new Condition( MatchStateConditionType.ID, "is not", "similar" ) );
        Constraint constraint3 = new Constraint( "C3", "Constraint 3", LogicalOperator.AND );
        constraint3.addCondition( new Condition( LabelConditionType.ID, "is not", label.getId() ) );
        Policy policy2 = new Policy( "PolicyId2", "Policy2" );
        policy2.setThreatLevel( 8 );
        policy2.addConstraint( constraint2 );
        policy2.addConstraint( constraint3 );
        policy2.addAction( BuildStageType.ID, failAction );
        addPolicy( applicationPublicId, policy2 );

        String groupId = "g1";
        String artifactId = "a1";
        String version = "v1";
        String serviceUrl =
            getComponentDetailsUrl( applicationPublicId, groupId, artifactId, version, "01234567890123456789",
                                    "similar" );
        String saasUrl = serviceUrl.substring( getRestBaseUrl().length() );
        ComponentDetails saasComponentDetails = new ComponentDetails( groupId, artifactId, version );
        saasComponentDetails.addSecurityVulnerability( new SecurityVulnerability( "Test Ref Id", "Test Source", 7.5F ) );
        setSaasResponseForURI( saasUrl, JsonHelpers.asJson( saasComponentDetails ), 200 );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );

        ComponentDetails componentDetails = JsonHelpers.fromJson( response.getResponseBody(), ComponentDetails.class );
        Assert.assertNotNull( componentDetails );
        Assert.assertEquals( groupId, componentDetails.getGroupId() );
        Assert.assertEquals( artifactId, componentDetails.getArtifactId() );
        Assert.assertEquals( version, componentDetails.getVersion() );
        List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( "Policy1", policyAlerts.get( 0 ).getTrigger().getPolicyName() );
    }

    @Test
    public void testGetComponentDetails_OverriddenLicense()
        throws Exception
    {
        String applicationPublicId = "IdeResourceTest_AppId";
        Application application = createApplication( applicationPublicId );

        setLicenseAuditLog( application.getId(), "/IdeResourceTest/LicenseOverride_abababababababababab.json" );

        String groupId = "g1";
        String artifactId = "a1";
        String version = "v1";
        String serviceUrl = getComponentDetailsUrl( applicationPublicId, groupId, artifactId, version, null, null );
        String saasUrl = serviceUrl.substring( getRestBaseUrl().length() );
        ComponentDetails saasComponentDetails = new ComponentDetails( groupId, artifactId, version );
        setSaasResponseForURI( saasUrl, JsonHelpers.asJson( saasComponentDetails ), 200 );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );

        ComponentDetails componentDetails = JsonHelpers.fromJson( response.getResponseBody(), ComponentDetails.class );
        Assert.assertNotNull( componentDetails );
        Assert.assertEquals( groupId, componentDetails.getGroupId() );
        Assert.assertEquals( artifactId, componentDetails.getArtifactId() );
        Assert.assertEquals( version, componentDetails.getVersion() );
        Assert.assertEquals( 1, componentDetails.getOverriddenLicenses().size() );
        License overriddenLicense = componentDetails.getOverriddenLicenses().iterator().next();
        Assert.assertNotNull( overriddenLicense );
        Assert.assertEquals( "GPL-2.0", overriddenLicense.getLicenseId() );
        Assert.assertEquals( "GPL-2.0", overriddenLicense.getLicenseName() );
        Assert.assertEquals( new Integer( 9 ), componentDetails.getLicenseThreatLevel() );
    }

    @Test
    public void testGetComponentDetails_OverriddenSecurityVulnerabilityStatus()
        throws Exception
    {
        String applicationPublicId = "IdeResourceTest_AppId";
        Application application = createApplication( applicationPublicId );

        setSecurityAuditLog( application.getId(), "/IdeResourceTest/SecurityOverride_abababababababababab.json" );

        String groupId = "g1";
        String artifactId = "a1";
        String version = "v1";
        String serviceUrl = getComponentDetailsUrl( applicationPublicId, groupId, artifactId, version, null, null );
        String saasUrl = serviceUrl.substring( getRestBaseUrl().length() );
        ComponentDetails saasComponentDetails = new ComponentDetails( groupId, artifactId, version );
        saasComponentDetails.addSecurityVulnerability( new SecurityVulnerability( "36079", "osvdb", 7.5F, "Summary" ) );
        setSaasResponseForURI( saasUrl, JsonHelpers.asJson( saasComponentDetails ), 200 );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );

        ComponentDetails componentDetails = JsonHelpers.fromJson( response.getResponseBody(), ComponentDetails.class );
        Assert.assertNotNull( componentDetails );
        Assert.assertEquals( groupId, componentDetails.getGroupId() );
        Assert.assertEquals( artifactId, componentDetails.getArtifactId() );
        Assert.assertEquals( version, componentDetails.getVersion() );
        Assert.assertEquals( 1, componentDetails.getSecurityVulnerabilities().size() );
        Assert.assertEquals( "36079", componentDetails.getSecurityVulnerabilities().get( 0 ).getRefId() );
        Assert.assertEquals( "osvdb", componentDetails.getSecurityVulnerabilities().get( 0 ).getSource() );
        Assert.assertEquals( 7.5F, componentDetails.getSecurityVulnerabilities().get( 0 ).getSeverity(), 0.1 );
        Assert.assertEquals( "Summary", componentDetails.getSecurityVulnerabilities().get( 0 ).getSummary() );
        Assert.assertEquals( "Acknowledged", componentDetails.getSecurityVulnerabilities().get( 0 ).getStatus() );
    }

    @Test
    public void testGetComponentDetails_UnknownComponent()
        throws Exception
    {
        String applicationPublicId = "IdeResourceTest_AppId";
        createApplication( applicationPublicId );

        Constraint constraint1 = new Constraint( "C1", "Constraint 1", LogicalOperator.AND );
        constraint1.addCondition( new Condition( MatchStateConditionType.ID, "is", "unknown" ) );
        Policy policy1 = new Policy( "PolicyId1", "Policy1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        Action failAction = new Action( FailActionType.ID );
        policy1.addAction( BuildStageType.ID, failAction );
        addPolicy( applicationPublicId, policy1 );

        String groupId = "ug1";
        String artifactId = "ua1";
        String version = "uv1";
        String serviceUrl =
            getComponentDetailsUrl( applicationPublicId, groupId, artifactId, version, "01234567890123456789",
                                    "unknown" );
        setSaasResponseForURI( serviceUrl.substring( getRestBaseUrl().length() ), "unknown GAV", 404 );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );

        ComponentDetails componentDetails = JsonHelpers.fromJson( response.getResponseBody(), ComponentDetails.class );
        Assert.assertNotNull( componentDetails );
        Assert.assertEquals( groupId, componentDetails.getGroupId() );
        Assert.assertEquals( artifactId, componentDetails.getArtifactId() );
        Assert.assertEquals( version, componentDetails.getVersion() );
        List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( "Policy1", policyAlerts.get( 0 ).getTrigger().getPolicyName() );

        serviceUrl = getComponentDetailsUrl( applicationPublicId, "", "", "", "01234567890123456789", "unknown" );
        setSaasResponseForURI( serviceUrl.substring( getRestBaseUrl().length() ), "unknown GAV", 404 );
        response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );

        componentDetails = JsonHelpers.fromJson( response.getResponseBody(), ComponentDetails.class );
        Assert.assertNotNull( componentDetails );
        Assert.assertEquals( "", componentDetails.getGroupId() );
        Assert.assertEquals( "", componentDetails.getArtifactId() );
        Assert.assertEquals( "", componentDetails.getVersion() );
        policyAlerts = componentDetails.getPolicyAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( "Policy1", policyAlerts.get( 0 ).getTrigger().getPolicyName() );
    }

    @Test
    public void testGetComponentDetails_AppIdWithUnsafeCharacters()
        throws Exception
    {
        String applicationPublicId = "bom 1&2%20?";
        createApplication( applicationPublicId );

        String groupId = "ug1";
        String artifactId = "ua1";
        String version = "uv1";
        String serviceUrl =
            getComponentDetailsUrl( applicationPublicId, groupId, artifactId, version, "01234567890123456789",
                                    "unknown" );
        ComponentDetails saasComponentDetails = new ComponentDetails( groupId, artifactId, version );
        setSaasResponseForURI( serviceUrl.substring( getRestBaseUrl().length() ),
                               JsonHelpers.asJson( saasComponentDetails ), 200 );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );

        ComponentDetails componentDetails = JsonHelpers.fromJson( response.getResponseBody(), ComponentDetails.class );
        Assert.assertNotNull( componentDetails );
        Assert.assertEquals( groupId, componentDetails.getGroupId() );
        Assert.assertEquals( artifactId, componentDetails.getArtifactId() );
        Assert.assertEquals( version, componentDetails.getVersion() );
    }

    @Test
    public void testGetComponentDetails_Unlicensed()
        throws Exception
    {
        uninstallLicense();
        Response response =
            RestAccess.get( getComponentDetailsUrl( "unlicensedappId", "ulg", "ula", "ulv", "ulh", "unknown" ) );
        assertResponseStatus( 402, response );
    }

    @Test
    public void testGetComponentDetails_EnforcementPointUnlicensed()
        throws Exception
    {
        // note this enforcement point should not apply to this request
        getLicenseManager().setEnforcementPoints( CLMEnforcementPoint.StageRelease );

        Response response =
            RestAccess.get( getComponentDetailsUrl( "unlicensedappId", "ulg", "ula", "ulv", "ulh", "unknown" ) );
        assertResponseStatus( 402, response );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + IDEComponentInfoResource.SERVICE_PATH;
    }

    private String getComponentDetailsUrl( String applicationPublicId, String groupId, String artifactId,
                                           String version, String hash, String matchState )
    {
        UriBuilder builder = UriBuilder.fromUri( getServiceURL() );
        builder.path( "{appId}" );
        builder.queryParam( "groupId", groupId );
        builder.queryParam( "artifactId", artifactId );
        builder.queryParam( "version", version );
        if ( hash != null )
        {
            builder.queryParam( "hash", hash );
        }
        if ( matchState != null )
        {
            builder.queryParam( "matchState", matchState );
        }
        return builder.build( applicationPublicId ).toString();
    }

    private void addPolicy( String applicationPublicId, Policy policy )
        throws Exception
    {
        String appId = new ApplicationDAO().getByPublicIdNotNull( applicationPublicId ).getId();
        PolicyDAO policyDAO = new PolicyDAO( brain.getWorkDir() );
        policyDAO.insert( appId, policy );
    }
}
