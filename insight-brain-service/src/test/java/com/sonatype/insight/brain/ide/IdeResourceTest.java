/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.clm.dto.model.ide.IdeMatchedComponent;
import com.sonatype.clm.dto.model.ide.ScannedComponent;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.license.LicenseStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.product.license.CLMEnforcementPoint;
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
        String saasUrl = convertToSaasUrl( serviceUrl, applicationPublicId );
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
        String saasUrl = convertToSaasUrl( serviceUrl, applicationPublicId );
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
        String saasUrl = convertToSaasUrl( serviceUrl, applicationPublicId );
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
        String saasUrl = convertToSaasUrl( serviceUrl, applicationPublicId );
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
        String saasUrl = convertToSaasUrl( serviceUrl, applicationPublicId );
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
        String saasUrl = convertToSaasUrl( serviceUrl, applicationPublicId );
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

        String serviceUrl = getScanSimpleUrl( applicationPublicId, hash );
        String saasUrl = convertToSaasUrl( serviceUrl, applicationPublicId );
        setSaasResponseForURI( saasUrl, 200, "/IdeResourceTest/SimpleMatch_000babababababababab.json" );
        Response response = RestAccess.get( serviceUrl );
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

        String serviceUrl = getScanSimpleUrl( applicationPublicId, hash );
        String saasUrl = convertToSaasUrl( serviceUrl, applicationPublicId );
        setSaasResponseForURI( saasUrl, 200, "/IdeResourceTest/SimpleMatch_000babababababababab_enhanced.json" );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        IdeMatchedComponent ideMatchedComponent =
            JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
    }
    
    @Test
    public void testDoScan_simple_Unlicensed()
        throws Exception
    {
        uninstallLicense();
        Response response = RestAccess.get( getScanSimpleUrl( "unlicensedappId", "ulh" ) );
        assertResponseStatus( 402, response );
    }
    
    @Test
    public void testDoScan_EnforcementPointUnlicensed()
        throws Exception
    {
        //note this enforcement point should not apply to this request
        getLicenseManager().setEnforcementPoints( CLMEnforcementPoint.StageRelease );

        Response response = RestAccess.get( getScanSimpleUrl( "unlicensedappId", "ulh" ) );
        assertResponseStatus( 402, response );
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

        String serviceUrl = getScanEnhancedUrl( applicationPublicId, hash );
        String saasUrl = convertToSaasUrl( serviceUrl, applicationPublicId );
        setSaasResponseForURI( saasUrl, 200, "/IdeResourceTest/SimpleMatch_000babababababababab_enhanced.json" );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        IdeMatchedComponent ideMatchedComponent =
            JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
    }
    
    @Test
    public void testDoScan_enhanced_Unlicensed()
        throws Exception
    {
        uninstallLicense();
        Response response = RestAccess.get( getScanEnhancedUrl( "unlicensedappId", "ulh" ) );
        assertResponseStatus( 402, response );
    }
    
    @Test
    public void testDoScan_enhanced_EnforcementPointUnlicensed()
        throws Exception
    {
        //note this enforcement point should not apply to this request
        getLicenseManager().setEnforcementPoints( CLMEnforcementPoint.StageRelease );

        Response response = RestAccess.get( getScanEnhancedUrl( "unlicensedappId", "ulh" ) );
        assertResponseStatus( 402, response );
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
    public void testGetComponentVersions_Unlicensed()
        throws Exception
    {
        uninstallLicense();
        Response response = RestAccess.get( getComponentVersionsUrl( "ulg", "ula" ) );
        assertResponseStatus( 402, response );
    }
    
    @Test
    public void testGetComponentVersions_EnforcementPointUnlicensed()
        throws Exception
    {
        //note this enforcement point should not apply to this request
        getLicenseManager().setEnforcementPoints( CLMEnforcementPoint.StageRelease );

        Response response = RestAccess.get( getComponentVersionsUrl( "ulg", "ula" ) );
        assertResponseStatus( 402, response );
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
    
    @Test
    public void testGetAsset_Unlicensed()
        throws Exception
    {
        uninstallLicense();
        Response response = RestAccess.get( getServiceURL() + "/asset/sub/dir/some%20space.html?x=y&a=b" );
        assertResponseStatus( 402, response );
    }
    
    @Test
    public void testGetAsset_EnforcementPointUnlicensed()
        throws Exception
    {
        //note this enforcement point should not apply to this request
        getLicenseManager().setEnforcementPoints( CLMEnforcementPoint.StageRelease );

        Response response = RestAccess.get( getServiceURL() + "/asset/sub/dir/some%20space.html?x=y&a=b" );
        assertResponseStatus( 402, response );
    }
    
    private String convertToSaasUrl( String brainUrl, String applicationId )
    {
        return brainUrl.substring( getRestBaseUrl().length() ).replace( "/" + applicationId, "" );
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
