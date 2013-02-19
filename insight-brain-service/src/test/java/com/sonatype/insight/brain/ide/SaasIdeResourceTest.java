/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.IdeMatchedComponent;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class SaasIdeResourceTest
    extends AbstractResourceTest
{
    private Response addPolicy( String applicationPublicId, Policy policy )
        throws Exception
    {
        Response response =
            RestAccess.post( getRestBaseUrl()
                                 + PolicyResource.SERVICE_PATH.replace( "{applicationPublicId}", applicationPublicId ),
                             JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        return response;
    }

    @Test
    public void testDoScan()
        throws Exception
    {
        String applicationPublicId = "PolicyEvaluateResourceTest_AppId";
        createApplication( applicationPublicId );

        Constraint constraint1 = new Constraint( "C1", "PolicyEvaluateResourceTest constraint 1", LogicalOperator.AND );
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
        URL testResponseFileUrl = getClass().getResource( "/SaasIdeResourceTest/SimpleMatch_abababababababababab.json" );
        String responseBody = FileUtils.fileRead( testResponseFileUrl.getFile() );
        setSaasResponseForURI( saasUrl, responseBody, 200 );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        IdeMatchedComponent ideMatchedComponent =
            JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertEquals( "g1", ideMatchedComponent.getGroupId() );
        Assert.assertEquals( "a1", ideMatchedComponent.getArtifactId() );
        Assert.assertEquals( "v1", ideMatchedComponent.getVersion() );
        // TODO check why the hash is not set
        // Assert.assertEquals( "abababababababababab", ideMatchedComponent.getHash() );
        Assert.assertEquals( "exact", ideMatchedComponent.getMatchState() );
        Assert.assertTrue( ideMatchedComponent.isSimpleMatch() );
        List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
    }

    @Test
    public void testDoScan_OverriddenLicense()
        throws Exception
    {
        String applicationPublicId = "PolicyEvaluateResourceTest_AppId";
        Application application = createApplication( applicationPublicId );

        Constraint constraint1 = new Constraint( "C1", "PolicyEvaluateResourceTest constraint 1", LogicalOperator.AND );
        Condition condition1 = new Condition( LicenseConditionType.ID, "is", "AAL" );
        constraint1.addCondition( condition1 );
        Policy policy1 = new Policy( "PolicyId1", "Policy Name 1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        Action failAction = new Action( FailActionType.ID );
        policy1.addAction( BuildStageType.ID, failAction );
        addPolicy( applicationPublicId, policy1 );

        String serviceUrl = getScanSimpleUrl( applicationPublicId, "abababababababababab" );
        String saasUrl = serviceUrl.substring( getRestBaseUrl().length() );
        URL testResponseFileUrl = getClass().getResource( "/SaasIdeResourceTest/SimpleMatch_abababababababababab.json" );
        String responseBody = FileUtils.fileRead( testResponseFileUrl.getFile() );
        setSaasResponseForURI( saasUrl, responseBody, 200 );
        Response response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        IdeMatchedComponent ideMatchedComponent =
            JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertEquals( "g1", ideMatchedComponent.getGroupId() );
        Assert.assertEquals( "a1", ideMatchedComponent.getArtifactId() );
        Assert.assertEquals( "v1", ideMatchedComponent.getVersion() );
        // TODO check why the hash is not set
        // Assert.assertEquals( "abababababababababab", ideMatchedComponent.getHash() );
        Assert.assertEquals( "exact", ideMatchedComponent.getMatchState() );
        Assert.assertTrue( ideMatchedComponent.isSimpleMatch() );
        List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 0, policyAlerts.size() );

        // Override the license and evaluate the policy again
        URL testOverriddenLicenseFileUrl =
            getClass().getResource( "/SaasIdeResourceTest/LicenseOverride_abababababababababab.json" );
        FileUtils.copyFile( new File( testOverriddenLicenseFileUrl.getFile() ),
                            new File( brain.getAuditDir( application.getId() ), "licenses.json" ) );
        response = RestAccess.get( serviceUrl );
        assertResponseStatus( 200, response );
        ideMatchedComponent = JsonHelpers.fromJson( response.getResponseBody(), IdeMatchedComponent.class );
        Assert.assertEquals( "g1", ideMatchedComponent.getGroupId() );
        Assert.assertEquals( "a1", ideMatchedComponent.getArtifactId() );
        Assert.assertEquals( "v1", ideMatchedComponent.getVersion() );
        // TODO check why the hash is not set
        // Assert.assertEquals( "abababababababababab", ideMatchedComponent.getHash() );
        Assert.assertEquals( "exact", ideMatchedComponent.getMatchState() );
        Assert.assertTrue( ideMatchedComponent.isSimpleMatch() );
        policyAlerts = ideMatchedComponent.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + SaasIdeResource.SERVICE_PATH;
    }

    private String getScanSimpleUrl( String applicationPublicId, String hash )
    {
        return getScanUrl( "simple", applicationPublicId, hash, null, null, null, null );
    }

    @SuppressWarnings( "unused" )
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
