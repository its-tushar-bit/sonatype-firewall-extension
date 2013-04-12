/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.clm.dto.model.ide.IdeMatchedComponent;
import com.sonatype.clm.dto.model.ide.ScannedComponent;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class IdeResourceTest
    extends AbstractResourceTest
{
    private void addPolicy( String applicationPublicId, Policy policy )
        throws Exception
    {
        String appId = new ApplicationDAO().getByPublicIdNotNull( applicationPublicId ).getId();
        PolicyDAO policyDAO = new PolicyDAO( brain.getWorkDir() );
        policyDAO.insert( appId, policy );
    }

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
    public void testDoScan_Simple()
        throws Exception
    {
        String applicationPublicId = "IdeResourceTest_AppId";
        createApplication( applicationPublicId );

        Constraint constraint1 = new Constraint( "C1", "Constraint 1", LogicalOperator.AND );
        Condition condition1 = new Condition( SecurityVulnerabilityConditionType.ID, "present" );
        constraint1.addCondition( condition1 );
        Policy policy1 = new Policy( "PolicyId1", "Policy Name 1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        Action failAction = new Action( FailActionType.ID );
        policy1.addAction( BuildStageType.ID, failAction );
        addPolicy( applicationPublicId, policy1 );

        String serviceUrl = getScanSimpleUrl( applicationPublicId, "abababababababababab" );
        String saasUrl = serviceUrl.substring( getRestBaseUrl().length() );
        setSaasResponseForURI( saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json" );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        IdeMatchedComponent ideMatchedComponent =
            JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertEquals( "g1", ideMatchedComponent.getGroupId() );
        Assert.assertEquals( "a1", ideMatchedComponent.getArtifactId() );
        Assert.assertEquals( "v1", ideMatchedComponent.getVersion() );
        Assert.assertEquals( "abababababababababab", ideMatchedComponent.getHash() );
        Assert.assertEquals( "exact", ideMatchedComponent.getMatchState() );
        Assert.assertTrue( ideMatchedComponent.isSimpleMatch() );
        List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
    }

    @Test
    public void testDoScan_Enhanced()
        throws Exception
    {
        String applicationPublicId = "IdeResourceTest_AppId";
        createApplication( applicationPublicId );

        Constraint constraint1 = new Constraint( "C1", "Constraint 1", LogicalOperator.AND );
        constraint1.addCondition( new Condition( MatchStateConditionType.ID, "is", "exact" ) );
        Policy policy1 = new Policy( "PolicyId1", "Policy Name 1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        Action failAction = new Action( FailActionType.ID );
        policy1.addAction( BuildStageType.ID, failAction );
        addPolicy( applicationPublicId, policy1 );

        String serviceUrl = getScanEnhancedUrl( applicationPublicId, "abababababababababab" );
        String saasUrl = serviceUrl.substring( getRestBaseUrl().length() );
        setSaasResponseForURI( saasUrl, 202, "/IdeResourceTest/EnhancedMatch_wait.json" );
        Response response = RestAccess.post( serviceUrl, JsonHelpers.asJson( new ScannedComponent() ) );
        assertResponseStatus( 200, response );
        IdeMatchedComponent ideMatchedComponent =
            JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertNotNull( ideMatchedComponent.getWaitDelta() );
        Assert.assertTrue( ideMatchedComponent.getWaitDelta() > 0 );

        setSaasResponseForURI( saasUrl, 200, "/IdeResourceTest/EnhancedMatch_abababababababababab.json" );
        response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        ideMatchedComponent = JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertEquals( "g1", ideMatchedComponent.getGroupId() );
        Assert.assertEquals( "a1", ideMatchedComponent.getArtifactId() );
        Assert.assertEquals( "v1", ideMatchedComponent.getVersion() );
        Assert.assertEquals( "abababababababababab", ideMatchedComponent.getHash() );
        Assert.assertEquals( "exact", ideMatchedComponent.getMatchState() );
        Assert.assertFalse( ideMatchedComponent.isSimpleMatch() );
        List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
    }

    @Test
    public void testDoScan_OverriddenLicense()
        throws Exception
    {
        String applicationPublicId = "IdeResourceTest_AppId";
        Application application = createApplication( applicationPublicId );

        Constraint constraint1 = new Constraint( "C1", "Constraint 1", LogicalOperator.AND );
        Condition condition1 = new Condition( LicenseConditionType.ID, "is", "GPL-2.0" );
        constraint1.addCondition( condition1 );
        Policy policy1 = new Policy( "PolicyId1", "Policy Name 1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        Action failAction = new Action( FailActionType.ID );
        policy1.addAction( BuildStageType.ID, failAction );
        addPolicy( applicationPublicId, policy1 );

        String serviceUrl = getScanSimpleUrl( applicationPublicId, "abababababababababab" );
        String saasUrl = serviceUrl.substring( getRestBaseUrl().length() );
        setSaasResponseForURI( saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json" );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        IdeMatchedComponent ideMatchedComponent =
            JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertEquals( "g1", ideMatchedComponent.getGroupId() );
        Assert.assertEquals( "a1", ideMatchedComponent.getArtifactId() );
        Assert.assertEquals( "v1", ideMatchedComponent.getVersion() );
        Assert.assertEquals( "abababababababababab", ideMatchedComponent.getHash() );
        Assert.assertEquals( "exact", ideMatchedComponent.getMatchState() );
        Assert.assertTrue( ideMatchedComponent.isSimpleMatch() );
        List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 0, policyAlerts.size() );

        // Override the license and evaluate the policy again
        setLicenseAuditLog( application.getId(), "/IdeResourceTest/LicenseOverride_abababababababababab.json" );
        response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        ideMatchedComponent = JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertEquals( "g1", ideMatchedComponent.getGroupId() );
        Assert.assertEquals( "a1", ideMatchedComponent.getArtifactId() );
        Assert.assertEquals( "v1", ideMatchedComponent.getVersion() );
        Assert.assertEquals( "abababababababababab", ideMatchedComponent.getHash() );
        Assert.assertEquals( "exact", ideMatchedComponent.getMatchState() );
        Assert.assertTrue( ideMatchedComponent.isSimpleMatch() );
        policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
    }

    @Test
    public void testDoScan_LicenseStatus()
        throws Exception
    {
        String applicationPublicId = "IdeResourceTest_AppId";
        Application application = createApplication( applicationPublicId );

        Constraint constraint1 = new Constraint( "C1", "Constraint 1", LogicalOperator.AND );
        Condition condition1 = new Condition( LicenseStatusConditionType.ID, "is", LicenseStatus.OVERRIDDEN.getId() );
        constraint1.addCondition( condition1 );
        Policy policy1 = new Policy( "PolicyId1", "Policy Name 1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        Action failAction = new Action( FailActionType.ID );
        policy1.addAction( BuildStageType.ID, failAction );
        addPolicy( applicationPublicId, policy1 );

        String serviceUrl = getScanSimpleUrl( applicationPublicId, "abababababababababab" );
        String saasUrl = serviceUrl.substring( getRestBaseUrl().length() );
        setSaasResponseForURI( saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json" );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        IdeMatchedComponent ideMatchedComponent =
            JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertEquals( "g1", ideMatchedComponent.getGroupId() );
        Assert.assertEquals( "a1", ideMatchedComponent.getArtifactId() );
        Assert.assertEquals( "v1", ideMatchedComponent.getVersion() );
        Assert.assertEquals( "abababababababababab", ideMatchedComponent.getHash() );
        Assert.assertEquals( "exact", ideMatchedComponent.getMatchState() );
        Assert.assertTrue( ideMatchedComponent.isSimpleMatch() );
        List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 0, policyAlerts.size() );

        // Override the license and evaluate the policy again
        setLicenseAuditLog( application.getId(), "/IdeResourceTest/LicenseOverride_abababababababababab.json" );
        response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        ideMatchedComponent = JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertEquals( "g1", ideMatchedComponent.getGroupId() );
        Assert.assertEquals( "a1", ideMatchedComponent.getArtifactId() );
        Assert.assertEquals( "v1", ideMatchedComponent.getVersion() );
        Assert.assertEquals( "abababababababababab", ideMatchedComponent.getHash() );
        Assert.assertEquals( "exact", ideMatchedComponent.getMatchState() );
        Assert.assertTrue( ideMatchedComponent.isSimpleMatch() );
        policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
    }

    @Test
    public void testDoScan_SecurityStatus()
        throws Exception
    {
        String applicationPublicId = "IdeResourceTest_AppId";
        Application application = createApplication( applicationPublicId );

        Constraint constraint1 = new Constraint( "C1", "Constraint 1", LogicalOperator.AND );
        Condition condition1 =
            new Condition( SecurityVulnerabilityStatusConditionType.ID, "is",
                           SecurityVulnerabilityStatus.ACKNOWLEDGED.getId() );
        constraint1.addCondition( condition1 );
        Policy policy1 = new Policy( "PolicyId1", "Policy Name 1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        Action failAction = new Action( FailActionType.ID );
        policy1.addAction( BuildStageType.ID, failAction );
        addPolicy( applicationPublicId, policy1 );

        // There should be no policy alerts when none of the security vulnerabilities was overridden
        String serviceUrl = getScanSimpleUrl( applicationPublicId, "abababababababababab" );
        String saasUrl = serviceUrl.substring( getRestBaseUrl().length() );
        setSaasResponseForURI( saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json" );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        IdeMatchedComponent ideMatchedComponent =
            JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertEquals( "g1", ideMatchedComponent.getGroupId() );
        Assert.assertEquals( "a1", ideMatchedComponent.getArtifactId() );
        Assert.assertEquals( "v1", ideMatchedComponent.getVersion() );
        Assert.assertEquals( "abababababababababab", ideMatchedComponent.getHash() );
        Assert.assertEquals( "exact", ideMatchedComponent.getMatchState() );
        Assert.assertTrue( ideMatchedComponent.isSimpleMatch() );
        List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 0, policyAlerts.size() );

        // Override the security vulnerabilities status for a security vulnerability that does not match and evaluate
        // the policy again. There should be no policy alerts.
        setSecurityAuditLog( application.getId(),
                             "/IdeResourceTest/SecurityOverride_abababababababababab_NotMatch.json" );
        response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        ideMatchedComponent = JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertEquals( "g1", ideMatchedComponent.getGroupId() );
        Assert.assertEquals( "a1", ideMatchedComponent.getArtifactId() );
        Assert.assertEquals( "v1", ideMatchedComponent.getVersion() );
        Assert.assertEquals( "abababababababababab", ideMatchedComponent.getHash() );
        Assert.assertEquals( "exact", ideMatchedComponent.getMatchState() );
        Assert.assertTrue( ideMatchedComponent.isSimpleMatch() );
        policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 0, policyAlerts.size() );

        // Override the security vulnerabilities status and evaluate the policy again. There should be one policy alert.
        setSecurityAuditLog( application.getId(), "/IdeResourceTest/SecurityOverride_abababababababababab.json" );
        response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        ideMatchedComponent = JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertEquals( "g1", ideMatchedComponent.getGroupId() );
        Assert.assertEquals( "a1", ideMatchedComponent.getArtifactId() );
        Assert.assertEquals( "v1", ideMatchedComponent.getVersion() );
        Assert.assertEquals( "abababababababababab", ideMatchedComponent.getHash() );
        Assert.assertEquals( "exact", ideMatchedComponent.getMatchState() );
        Assert.assertTrue( ideMatchedComponent.isSimpleMatch() );
        policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
    }

    @Test
    public void testDoScan_Age()
        throws Exception
    {
        String applicationPublicId = "IdeResourceTest_AppId";
        createApplication( applicationPublicId );

        Constraint constraint1 = new Constraint( "C1", "Constraint 1", LogicalOperator.AND );
        Condition condition1 = new Condition( AgeInDaysConditionType.ID, "older than", "365" );
        constraint1.addCondition( condition1 );
        Policy policy1 = new Policy( "PolicyId1", "Policy Name 1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        Action failAction = new Action( FailActionType.ID );
        policy1.addAction( BuildStageType.ID, failAction );
        addPolicy( applicationPublicId, policy1 );

        String serviceUrl = getScanSimpleUrl( applicationPublicId, "abababababababababab" );
        String saasUrl = serviceUrl.substring( getRestBaseUrl().length() );
        setSaasResponseForURI( saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json" );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        IdeMatchedComponent ideMatchedComponent =
            JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertEquals( "g1", ideMatchedComponent.getGroupId() );
        Assert.assertEquals( "a1", ideMatchedComponent.getArtifactId() );
        Assert.assertEquals( "v1", ideMatchedComponent.getVersion() );
        Assert.assertEquals( "abababababababababab", ideMatchedComponent.getHash() );
        Assert.assertEquals( "exact", ideMatchedComponent.getMatchState() );
        Assert.assertTrue( ideMatchedComponent.isSimpleMatch() );
        List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
    }

    @Test
    public void testDoScan_unknown_simple()
        throws Exception
    {
        String hash = "000babababababababab";
        String applicationPublicId = "IdeResourceTest_AppId";
        createApplication( applicationPublicId );

        Constraint constraint1 = new Constraint( "C1", "Constraint 1", LogicalOperator.AND );
        Condition condition1 = new Condition( MatchStateConditionType.ID, "is", "unknown" );
        constraint1.addCondition( condition1 );
        Policy policy1 = new Policy( "PolicyId1", "Policy Name 1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        Action failAction = new Action( FailActionType.ID );
        policy1.addAction( BuildStageType.ID, failAction );
        addPolicy( applicationPublicId, policy1 );

        String url = getScanSimpleUrl( applicationPublicId, hash );
        String saasUrl = url.substring( getRestBaseUrl().length() );
        setSaasResponseForURI( saasUrl, 200, "/IdeResourceTest/SimpleMatch_000babababababababab.json" );
        Response response = RestAccess.get( url );
        assertResponseStatus( 200, response );
        IdeMatchedComponent ideMatchedComponent =
            JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertNull( ideMatchedComponent.getAlerts() );
    }

    @Test
    public void testDoScan_unknown_simple_enhancedResponse()
        throws Exception
    {
        String hash = "000babababababababab";
        String applicationPublicId = "IdeResourceTest_AppId";
        createApplication( applicationPublicId );

        Constraint constraint1 = new Constraint( "C1", "Constraint 1", LogicalOperator.AND );
        Condition condition1 = new Condition( MatchStateConditionType.ID, "is", "unknown" );
        constraint1.addCondition( condition1 );
        Policy policy1 = new Policy( "PolicyId1", "Policy Name 1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        Action failAction = new Action( FailActionType.ID );
        policy1.addAction( BuildStageType.ID, failAction );
        addPolicy( applicationPublicId, policy1 );

        String url = getScanSimpleUrl( applicationPublicId, hash );
        String saasUrl = url.substring( getRestBaseUrl().length() );
        setSaasResponseForURI( saasUrl, 200, "/IdeResourceTest/SimpleMatch_000babababababababab_enhanced.json" );
        Response response = RestAccess.get( url );
        assertResponseStatus( 200, response );
        IdeMatchedComponent ideMatchedComponent =
            JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
    }

    @Test
    public void testDoScan_unknown_enhanced()
        throws Exception
    {
        String hash = "000babababababababab";
        String applicationPublicId = "IdeResourceTest_AppId";
        createApplication( applicationPublicId );

        Constraint constraint1 = new Constraint( "C1", "Constraint 1", LogicalOperator.AND );
        Condition condition1 = new Condition( MatchStateConditionType.ID, "is", "unknown" );
        constraint1.addCondition( condition1 );
        Policy policy1 = new Policy( "PolicyId1", "Policy Name 1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        Action failAction = new Action( FailActionType.ID );
        policy1.addAction( BuildStageType.ID, failAction );
        addPolicy( applicationPublicId, policy1 );

        String url = getScanEnhancedUrl( applicationPublicId, hash );
        String saasUrl = url.substring( getRestBaseUrl().length() );
        setSaasResponseForURI( saasUrl, 200, "/IdeResourceTest/SimpleMatch_000babababababababab_enhanced.json" );
        Response response = RestAccess.get( url );
        assertResponseStatus( 200, response );
        IdeMatchedComponent ideMatchedComponent =
            JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
    }

    @Test
    public void testGetComponentVersions()
        throws Exception
    {
        setSaasResponseForURI( "rest/ide/artifact/versions?groupId=gid&artifactId=aid", "[\"1.1\", \"2.0\"]", 200 );
        Response response = RestAccess.get( getComponentVersionsUrl( "gid", "aid" ) );
        assertResponseStatus( 200, response );
        String[] versions = JsonHelpers.fromJson( response.getResponseBody(), String[].class );
        Assert.assertEquals( Arrays.asList( "1.1", "2.0" ), Arrays.asList( versions ) );
    }

    @Test
    public void testGetAsset()
        throws Exception
    {
        setSaasResponseForURI( "ide/sub/dir/some%20space.html?x=y&a=b", "OK", 200 );
        Response response = RestAccess.get( getServiceURL() + "/asset/sub/dir/some%20space.html?x=y&a=b" );
        assertResponseStatus( 200, response );
        Assert.assertEquals( "OK", response.getResponseBody() );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + IdeResource.SERVICE_PATH;
    }

    private String getScanSimpleUrl( String applicationPublicId, String hash )
    {
        return getScanUrl( "simple", applicationPublicId, hash, null, null, null, null );
    }

    private String getScanEnhancedUrl( String applicationPublicId, String hash )
    {
        return getScanUrl( "enhanced", applicationPublicId, hash, null, null, null, null );
    }

    private String getScanUrl( String mode, String applicationPublicId, String hash, String filename, String groupId,
                               String artifactId, String version )
    {
        return getServiceURL() + "/scan/" + mode + "/" + applicationPublicId + "/" + hash
            + getQueryParams( "filename", filename, "groupId", groupId, "artifactId", artifactId, "version", version );
    }

    private String getComponentDetailsUrl( String applicationPublicId, String groupId, String artifactId,
                                           String version, String hash, String matchState )
    {
        UriBuilder builder = UriBuilder.fromUri( getServiceURL() );
        builder.path( "component/details/{appId}" );
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

    private String getComponentVersionsUrl( String groupId, String artifactId )
    {
        return getServiceURL() + "/component/versions/?groupId=" + groupId + "&artifactId=" + artifactId;
    }

    private String getQueryParams( String... params )
    {
        if ( params.length % 2 != 0 )
        {
            throw new IllegalArgumentException( "query parameter mismatch" );
        }

        StringBuilder buffer = new StringBuilder( 256 );
        for ( int i = 0; i < params.length - 1; i += 2 )
        {
            String param = params[i];
            String value = params[i + 1];
            if ( value != null && !value.isEmpty() )
            {
                buffer.append( ( buffer.length() > 0 ) ? '&' : '?' );
                buffer.append( param ).append( '=' ).append( value );
            }
        }
        return buffer.toString();
    }
}
