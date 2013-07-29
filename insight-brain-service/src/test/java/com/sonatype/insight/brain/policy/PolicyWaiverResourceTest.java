/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class PolicyWaiverResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testCRUD_Application()
        throws Exception
    {
        String appPublicId = "PolicyWaiverResourceTest_AppId";
        Application application = createApplication( appPublicId );

        testCRUD( IdUtils.TYPE_APPLICATION, appPublicId, application.getId() );
    }

    @Test
    public void testCRUD_Organization()
        throws Exception
    {
        Organization organization = createOrganization( "PolicyWaiverResourceTest" );

        testCRUD( IdUtils.TYPE_ORGANIZATION, organization.getId(), organization.getId() );
    }

    private void testCRUD( String ownerType, String ownerPublicId, String ownerId )
        throws Exception
    {
        // Create
        PolicyWaiver policyWaiver =
            new PolicyWaiver( "12345678901234567890", "MyPolicyId", null /* ownerId */, "My comment" );
        Response response =
            RestAccess.post( getServiceURL( ownerType, ownerPublicId ), JsonHelpers.asJson( policyWaiver ) );
        assertResponseStatus( 200, response );
        policyWaiver = JsonHelpers.fromJson( response.getResponseBody(), PolicyWaiver.class );
        assertPolicyWaiver( "MyPolicyId", ownerId, "My comment", policyWaiver );

        // Get
        response = RestAccess.get( getServiceURL( ownerType, ownerPublicId ) + "/component/12345678901234567890" );
        assertResponseStatus( 200, response );
        PolicyWaiver[] policyWaivers = JsonHelpers.fromJson( response.getResponseBody(), PolicyWaiver[].class );
        assertEquals( 1, policyWaivers.length );
        assertPolicyWaiver( "MyPolicyId", ownerId, "My comment", policyWaivers[0] );

        // Delete
        response = RestAccess.delete( getServiceURL( ownerType, ownerPublicId ) + "/" + policyWaiver.getId() );
        assertResponseStatus( 204, response );

        // Get
        response = RestAccess.get( getServiceURL( ownerType, ownerPublicId ) + "/component/12345678901234567890" );
        assertResponseStatus( 200, response );
        policyWaivers = JsonHelpers.fromJson( response.getResponseBody(), PolicyWaiver[].class );
        assertEquals( 0, policyWaivers.length );
    }

    @Test
    public void testDelete_OwnerIdMismatch_Application()
        throws Exception
    {
        String appPublicId1 = "PolicyWaiverResourceTest_AppId1";
        Application application1 = createApplication( appPublicId1 );
        String appPublicId2 = "PolicyWaiverResourceTest_AppId2";
        createApplication( appPublicId2 );

        testDelete_OwnerIdMismatch( IdUtils.TYPE_APPLICATION, appPublicId1, application1.getId(), appPublicId2 );
    }

    @Test
    public void testDelete_OwnerIdMismatch_Organization()
        throws Exception
    {
        Organization organization1 = createOrganization( "PolicyWaiverResourceTest1" );
        Organization organization2 = createOrganization( "PolicyWaiverResourceTest2" );

        testDelete_OwnerIdMismatch( IdUtils.TYPE_ORGANIZATION, organization1.getId(), organization1.getId(),
                                    organization2.getId() );
    }

    @Test
    public void testGetPolicyWaiversByHash()
        throws Exception
    {
        Organization organization1 = createOrganization( "PolicyWaiverResourceTest1" );
        String appPublicId1 = "PolicyWaiverResourceTest_AppId1";
        Application application1 = createApplication( appPublicId1, "PolicyWaiverResourceTest AppId1", organization1 );

        PolicyWaiver waiver1 =
            new PolicyWaiver( "12345678901234567890", "MyPolicyId", application1.getId(), "My comment" );

        PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
        policyWaiverDAO.insert( waiver1 );

        Response response =
            RestAccess.get( getServiceURL( "application", application1.getPublicId() )
                + "/component/12345678901234567890" );
        assertResponseStatus( 200, response );
        PolicyWaiver[] waivers = JsonHelpers.fromJson( response.getResponseBody(), PolicyWaiver[].class );
        assertEquals( 1, waivers.length );
        assertPolicyWaiver( "MyPolicyId", application1.getId(), "My comment", waivers[0] );

        PolicyWaiver waiver2 =
            new PolicyWaiver( "12345678901234567890", "MyPolicyId", organization1.getId(), "My comment" );
        policyWaiverDAO.insert( waiver2 );

        response =
            RestAccess.get( getServiceURL( "application", application1.getPublicId() )
                + "/component/12345678901234567890" );
        assertResponseStatus( 200, response );
        waivers = JsonHelpers.fromJson( response.getResponseBody(), PolicyWaiver[].class );
        assertEquals( 2, waivers.length );
        assertPolicyWaiver( "MyPolicyId", organization1.getId(), "My comment", waivers[0] );
        assertPolicyWaiver( "MyPolicyId", application1.getId(), "My comment", waivers[1] );

        response =
            RestAccess.get( getServiceURL( "organization", organization1.getId() ) + "/component/12345678901234567890" );
        assertResponseStatus( 200, response );
        waivers = JsonHelpers.fromJson( response.getResponseBody(), PolicyWaiver[].class );
        assertEquals( 1, waivers.length );
        assertPolicyWaiver( "MyPolicyId", organization1.getId(), "My comment", waivers[0] );
    }

    private void testDelete_OwnerIdMismatch( String ownerType, String ownerPublicId1, String ownerId1,
                                             String ownerPublicId2 )
        throws Exception
    {
        PolicyWaiver policyWaiver =
            new PolicyWaiver( "12345678901234567890", "MyPolicyId", null /* ownerId */, "My comment" );
        Response response =
            RestAccess.post( getServiceURL( ownerType, ownerPublicId1 ), JsonHelpers.asJson( policyWaiver ) );
        assertResponseStatus( 200, response );
        policyWaiver = JsonHelpers.fromJson( response.getResponseBody(), PolicyWaiver.class );

        response = RestAccess.delete( getServiceURL( ownerType, ownerPublicId2 ) + "/" + policyWaiver.getId() );
        assertResponseStatus( 404, response );
        Assert.assertEquals( "Cannot find a policy waiver with id " + policyWaiver.getId() + " for " + ownerType
            + " id " + ownerPublicId2, response.getResponseBody() );
        // Verify that the policy waiver was not deleted
        PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
        List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId( ownerId1 );
        assertEquals( 1, policyWaivers.size() );
        assertPolicyWaiver( "MyPolicyId", ownerId1, "My comment", policyWaivers.get( 0 ) );
    }

    @Test
    public void testDelete_Nonexistant_Application()
        throws Exception
    {
        String appPublicId = "PolicyWaiverResourceTest_AppId";
        createApplication( appPublicId );

        Response response = RestAccess.delete( getServiceURL( IdUtils.TYPE_APPLICATION, appPublicId ) + "/YettiId" );
        assertResponseStatus( 404, response );
        Assert.assertEquals( "Cannot find a policy waiver with id YettiId", response.getResponseBody() );
    }

    @Test
    public void testDelete_Nonexistant_Organization()
        throws Exception
    {
        Organization organization = createOrganization( "PolicyWaiverResourceTest" );

        Response response =
            RestAccess.delete( getServiceURL( IdUtils.TYPE_ORGANIZATION, organization.getId() ) + "/YettiId" );
        assertResponseStatus( 404, response );
        Assert.assertEquals( "Cannot find a policy waiver with id YettiId", response.getResponseBody() );
    }

    private void assertPolicyWaiver( String policyId, String ownerId, String comment, PolicyWaiver actual )
    {
        assertEquals( policyId, actual.getPolicyId() );
        assertEquals( ownerId, actual.getOwnerId() );
        assertEquals( comment, actual.getComment() );
    }

    private String getServiceURL( final String ownerType, final String ownerId )
    {
        return getRestBaseUrl() + PolicyWaiverResource.SERVICE_BASEPATH + ownerType + "/" + ownerId;
    }
}
