/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.PolicyWaiverResource.ApplicableContext;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
        Organization organization = createOrganization( "PolicyWaiverResourceTest1" );
        String appPublicId = "PolicyWaiverResourceTest_AppId1";
        Application application = createApplication( appPublicId, "PolicyWaiverResourceTest AppId1", organization );

        PolicyWaiver waiver1 =
            new PolicyWaiver( "12345678901234567890", "MyPolicyId", application.getId(), "My comment" );

        PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
        policyWaiverDAO.insert( waiver1 );

        Response response =
            RestAccess.get( getServiceURL( IdUtils.TYPE_APPLICATION, application.getPublicId() )
                + "/component/12345678901234567890" );
        assertResponseStatus( 200, response );
        PolicyWaiver[] waivers = JsonHelpers.fromJson( response.getResponseBody(), PolicyWaiver[].class );
        assertEquals( 1, waivers.length );
        assertPolicyWaiver( "MyPolicyId", application.getId(), "My comment", waivers[0] );

        PolicyWaiver waiver2 =
            new PolicyWaiver( "12345678901234567890", "MyPolicyId", organization.getId(), "My comment" );
        policyWaiverDAO.insert( waiver2 );

        response =
            RestAccess.get( getServiceURL( IdUtils.TYPE_APPLICATION, application.getPublicId() )
                + "/component/12345678901234567890" );
        assertResponseStatus( 200, response );
        waivers = JsonHelpers.fromJson( response.getResponseBody(), PolicyWaiver[].class );
        assertEquals( 2, waivers.length );
        assertPolicyWaiver( "MyPolicyId", organization.getId(), "My comment", waivers[0] );
        assertPolicyWaiver( "MyPolicyId", application.getId(), "My comment", waivers[1] );

        response =
            RestAccess.get( getServiceURL( IdUtils.TYPE_ORGANIZATION, organization.getId() )
                + "/component/12345678901234567890" );
        assertResponseStatus( 200, response );
        waivers = JsonHelpers.fromJson( response.getResponseBody(), PolicyWaiver[].class );
        assertEquals( 1, waivers.length );
        assertPolicyWaiver( "MyPolicyId", organization.getId(), "My comment", waivers[0] );
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

  @Test
  public void testGetApplicableContexts_Application() throws Exception {
    String appPublicId = "testGetApplicableContexts_Application";
    Application application = createApplication(appPublicId);

    // Create a policy for the application
    Condition condition = new Condition(SecurityVulnerabilityConditionType.ID, "present");
    Constraint constraint = new Constraint(null, "Constraint name 1", LogicalOperator.AND);
    constraint.addCondition(condition);
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);
    Policy policy = new Policy(null, "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    Response response = RestAccess.post(
        getRestBaseUrl() + expandRestUrl(PolicyResource.SERVICE_PATH, "application", appPublicId),
        JsonHelpers.asJson(policy));
    assertResponseStatus(200, response);
    policy = JsonHelpers.fromJson(response.getResponseBody(), Policy.class);

    response = RestAccess.get(getServiceURL("application", appPublicId) + "/applicable/context/" + policy.getId());
    assertResponseStatus(200, response);
    ApplicableContext result = JsonHelpers.fromJson(response.getResponseBody(), ApplicableContext.class);
    assertApplicableContext(application.getId(), application.getName(), "application", result);
  }

  @Test
  public void testGetApplicableContexts_Organization() throws Exception {
    String appPublicId = "testGetApplicableContexts_Organization";
    Application application = createApplication(appPublicId);
    Organization organization = new OrganizationDAO().getByIdNotNull(application.getOrganizationId());

    // Create a policy for the organization
    Condition condition = new Condition(SecurityVulnerabilityConditionType.ID, "present");
    Constraint constraint = new Constraint(null, "Constraint name 1", LogicalOperator.AND);
    constraint.addCondition(condition);
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);
    Policy policy = new Policy(null, "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    Response response = RestAccess.post(
        getRestBaseUrl() + expandRestUrl(PolicyResource.SERVICE_PATH, "organization", application.getOrganizationId()),
        JsonHelpers.asJson(policy));
    assertResponseStatus(200, response);
    policy = JsonHelpers.fromJson(response.getResponseBody(), Policy.class);

    response = RestAccess.get(getServiceURL("application", appPublicId) + "/applicable/context/" + policy.getId());
    assertResponseStatus(200, response);
    ApplicableContext result = JsonHelpers.fromJson(response.getResponseBody(), ApplicableContext.class);
    assertApplicableContext(organization.getId(), organization.getName(), "organization", result);
    assertNotNull(result.children);
    assertEquals(1, result.children.size());
    ApplicableContext childContext = result.children.get(0);
    assertApplicableContext(application.getId(), application.getName(), "application", childContext);
    assertNull(childContext.children);
  }

  private void assertApplicableContext(String id, String name, String type, ApplicableContext actual) {
    assertNotNull(actual);
    assertEquals(id, actual.id);
    assertEquals(name, actual.name);
    assertEquals(type, actual.type);
  }
}
