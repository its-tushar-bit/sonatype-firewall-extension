/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.policy.PolicyResource.ApplicablePolicies;
import com.sonatype.insight.brain.policy.PolicyResource.PoliciesByOwner;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PolicyResourceTest
    extends AbstractResourceTest
{
  private static final PolicyDAO policyDAO = new PolicyDAO();

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PolicyResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  private PolicyExportResult createImportBody() {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(new Policy());

    return policyExportResult;
  }

  @Test
  public void testImportPolicies_OrganizationDoesNotExist() throws Exception {
    String orgId = "OrgDoesNotExist";

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, orgId).path("import")
        .part("file", "file", createImportBody()).post();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText(), is("Cannot find organization with ID " + orgId + "."));
  }

  @Test
  public void testCRUD_ApplicationLevel() throws Exception {
    String applicationPublicId = "PolicyResourceTest_testCRUD";
    tempEntity.newApplicationWithParent(applicationPublicId);

    testCRUD(OwnerType.APPLICATION, applicationPublicId);
  }

  @Test
  public void testCRUD_OrganizationLevel() throws Exception {
    String orgId = tempEntity.newOrganization("test").getId();

    testCRUD(OwnerType.ORGANIZATION, orgId);
  }

  @Test
  public void testUpdatePolicy_DifferentOwnerId() throws Exception {
    Organization ownerOrg = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(ownerOrg);

    Organization otherOrg = tempEntity.newOrganization();
    policy.setOwnerId(otherOrg.getId());

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, otherOrg.getId()).body(policy).put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText(),
        is("Cannot find a policy with id " + policy.getId() + " for owner id " + otherOrg.getId()));
  }

  private void testCRUD(OwnerType ownerType, String ownerId) throws Exception {
    // Add a policy
    Policy policy = new Policy();
    policy.setName("PolicyResourceTest new policy");
    Constraint constraint = new Constraint();
    constraint.setName("PolicyResourceTest new constraint");
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    HttpResponse response = restRequest(ownerType, ownerId).body(policy).post();
    assertResponseStatus(200, response);
    final Policy policy1 = response.getBody(Policy.class);
    assertNotNull(policy1.getId());
    assertEquals("PolicyResourceTest new policy", policy1.getName());

    // Get all policies
    response = restRequest(ownerType, ownerId).get();
    assertResponseStatus(200, response);
    Policy[] policies = response.getBody(Policy[].class);
    assertNotNull(policies);
    assertEquals(1, policies.length);
    assertEquals(policy1.getId(), policies[0].getId());
    assertEquals(policy1.getName(), policies[0].getName());

    // Update a policy
    policy = policies[0];
    policy.setName("PolicyResourceTest updated policy");
    response = restRequest(ownerType, ownerId).body(policy).put();
    assertResponseStatus(200, response);
    final Policy policy2 = response.getBody(Policy.class);
    assertEquals("PolicyResourceTest updated policy", policy2.getName());

    // Get all policies
    response = restRequest(ownerType, ownerId).get();
    assertResponseStatus(200, response);
    policies = response.getBody(Policy[].class);
    assertNotNull(policies);
    assertEquals(1, policies.length);
    assertEquals(policy2.getId(), policies[0].getId());
    assertEquals(policy2.getName(), policies[0].getName());

    // Delete a policy
    policy = policies[0];
    response = restRequest(ownerType, ownerId).path(policy.getId()).delete();
    assertResponseStatus(204, response);

    // Get all policies
    response = restRequest(ownerType, ownerId).get();
    assertResponseStatus(200, response);
    policies = response.getBody(Policy[].class);
    assertNotNull(policies);
    assertEquals(0, policies.length);
  }

  @Test
  public void testAddPolicy_InvalidPolicy_AppLevel() throws Exception {
    String applicationPublicId = "PolicyResourceTest_testCreateInvalidPolicy";
    tempEntity.newApplicationWithParent(applicationPublicId);
    testAddPolicy_InvalidPolicy(OwnerType.APPLICATION, applicationPublicId);
  }

  @Test
  public void testAddPolicy_InvalidPolicy_OrgLevel() throws Exception {
    String orgId = tempEntity.newOrganization("test").getId();
    testAddPolicy_InvalidPolicy(OwnerType.ORGANIZATION, orgId);
  }

  private void testAddPolicy_InvalidPolicy(OwnerType ownerType, String ownerId) throws Exception {
    Policy policy = new Policy();
    policy.setName(null);
    Constraint constraint = new Constraint();
    constraint.setName("PolicyResourceTest new constraint");
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    HttpResponse response = restRequest(ownerType, ownerId).body(policy).post();
    assertResponseStatus(400, response);
    assertEquals("The policy name is required.", response.getBodyText());
  }

  @Test
  public void testUpdatePolicy_InvalidPolicy_AppLevel() throws Exception {
    String applicationPublicId = "PolicyResourceTest_testUpdateInvalidPolicy";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);
    testUpdatePolicy_InvalidPolicy(OwnerType.APPLICATION, app.getId(), app.getPublicId());
  }

  @Test
  public void testUpdatePolicy_InvalidPolicy_OrgLevel() throws Exception {
    String orgId = tempEntity.newOrganization("test").getId();
    testUpdatePolicy_InvalidPolicy(OwnerType.ORGANIZATION, orgId, orgId);
  }

  private void testUpdatePolicy_InvalidPolicy(OwnerType ownerType, String ownerId, String publicOwnerid)
      throws Exception
  {
    // Create a valid policy
    Policy policy = new Policy();
    policy.setOwnerId(ownerId);
    policy.setName("PolicyResourceTest-testUpdateInvalidPolicy");
    Constraint constraint = new Constraint();
    constraint.setName("PolicyResourceTest new constraint");
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    policyDAO.insert(policy);

    // Update invalid policy
    policy.setName(null);
    HttpResponse response = restRequest(ownerType, publicOwnerid).body(policy).put();
    assertResponseStatus(400, response);
    assertEquals("The policy name is required.", response.getBodyText());
  }

  private void assertPoliciesByOwner(String ownerId,
                                     String ownerName,
                                     OwnerType ownerType,
                                     int policyCount,
                                     PoliciesByOwner actual)
  {
    assertEquals(ownerId, actual.ownerId);
    assertEquals(ownerName, actual.ownerName);
    assertEquals(ownerType, actual.ownerType);
    assertEquals(policyCount, actual.policies.size());
  }

  @Test
  public void testGetApplicablePolicies() throws Exception {
    // Create an organization and an application
    String orgName = "testGetApplicablePoliciesOrg";
    Organization org = tempEntity.newOrganization(orgName);
    String orgId = org.getId();
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());
    String parentOrgId = parentOrg.getId();
    String parentOrgName = parentOrg.getName();
    String appName = "testGetApplicablePoliciesApp";
    String appPublicId = appName;
    Application app = tempEntity.newApplication(appName, appPublicId, orgId);
    String appId = app.getId();

    // Verify the applicable policies for the application
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    ApplicablePolicies applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    assertEquals(3, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(appId, appName, OwnerType.APPLICATION, 0, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 0, applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(2));

    // Verify the applicable policies for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    assertEquals(2, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 0, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(1));

    // Verify the applicable policies for the parent organization
    response = restRequest(OwnerType.ORGANIZATION, parentOrgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    assertEquals(1, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(0));

    // Create a policy for the application
    Policy appPolicy = tempEntity.newPolicy(app);

    // Verify the applicable policies for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    assertEquals(3, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(appId, appName, OwnerType.APPLICATION, 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 0, applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(2));
    assertEquals(appPolicy.getId(), applicablePolicies.policiesByOwner.get(0).policies.get(0).getId());

    // Verify the applicable policies for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    assertEquals(2, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 0, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(1));

    // Verify the applicable policies for the parent organization
    response = restRequest(OwnerType.ORGANIZATION, parentOrgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    assertEquals(1, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(0));

    // Create a policy for the organization
    Policy orgPolicy = tempEntity.newPolicy(org);

    // Verify the applicable policies for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    assertEquals(3, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(appId, appName, OwnerType.APPLICATION, 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 1, applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(2));
    assertEquals(appPolicy.getId(), applicablePolicies.policiesByOwner.get(0).policies.get(0).getId());
    assertEquals(orgPolicy.getId(), applicablePolicies.policiesByOwner.get(1).policies.get(0).getId());

    // Verify the applicable policies for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    assertEquals(2, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(1));
    assertEquals(orgPolicy.getId(), applicablePolicies.policiesByOwner.get(0).policies.get(0).getId());

    // Verify the applicable policies for the parent organization
    response = restRequest(OwnerType.ORGANIZATION, parentOrgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    assertEquals(1, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(0));

    // Create a policy for the parent organization
    Policy parentOrgPolicy = tempEntity.newPolicy(parentOrg);

    // Verify the applicable policies for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    assertEquals(3, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(appId, appName, OwnerType.APPLICATION, 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 1, applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 1,
        applicablePolicies.policiesByOwner.get(2));
    assertEquals(appPolicy.getId(), applicablePolicies.policiesByOwner.get(0).policies.get(0).getId());
    assertEquals(orgPolicy.getId(), applicablePolicies.policiesByOwner.get(1).policies.get(0).getId());
    assertEquals(parentOrgPolicy.getId(), applicablePolicies.policiesByOwner.get(2).policies.get(0).getId());

    // Verify the applicable policies for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    assertEquals(2, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 1,
        applicablePolicies.policiesByOwner.get(1));
    assertEquals(orgPolicy.getId(), applicablePolicies.policiesByOwner.get(0).policies.get(0).getId());
    assertEquals(parentOrgPolicy.getId(), applicablePolicies.policiesByOwner.get(1).policies.get(0).getId());

    // Verify the applicable policies for the parent organization
    response = restRequest(OwnerType.ORGANIZATION, parentOrgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    assertEquals(1, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 1,
        applicablePolicies.policiesByOwner.get(0));
    assertEquals(parentOrgPolicy.getId(), applicablePolicies.policiesByOwner.get(0).policies.get(0).getId());
  }

  @Test
  public void testGetApplicablePolicies_FilteredByTag() throws Exception {
    // Create an organization and an application
    Organization org = tempEntity.newOrganization();
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());
    Application app = tempEntity.newApplication(org.getId());

    Tag tag1 = tempEntity.newTag(org.getId());
    Tag tag2 = tempEntity.newTag(org.getId());

    // Create a tagged policy for the org that doesn't match an app tag. This policy should not appear in the result.
    Policy orgPolicy1 = tempEntity.newPolicy(org);
    tempEntity.newPolicyTag(orgPolicy1.getId(), tag1.getId());
    // Create another tagged policy for the org that matches an app tag. This policy should appear in the result.
    Policy orgPolicy2 = tempEntity.newPolicy(org);
    tempEntity.newPolicyTag(orgPolicy2.getId(), tag2.getId());

    // Create a tagged policy for the parent org that doesn't match an app tag. This policy should not appear in the
    // result.
    Policy parentOrgPolicy1 = tempEntity.newPolicy(parentOrg);
    tempEntity.newPolicyTag(parentOrgPolicy1.getId(), tag1.getId());
    // Create another tagged policy for the parentorg that matches an app tag. This policy should appear in the result.
    Policy parentOrgPolicy2 = tempEntity.newPolicy(parentOrg);
    tempEntity.newPolicyTag(parentOrgPolicy2.getId(), tag2.getId());

    tempEntity.newApplicationTag(app.getId(), tag2.getId());

    // Verify the applicable policies for the application
    HttpResponse response = restRequest(OwnerType.APPLICATION, app.getPublicId()).path("applicable").get();
    assertResponseStatus(200, response);
    ApplicablePolicies applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    assertEquals(3, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(app.getId(), app.getName(), OwnerType.APPLICATION, 0,
        applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(org.getId(), org.getName(), OwnerType.ORGANIZATION, 1,
        applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(parentOrg.getId(), parentOrg.getName(), OwnerType.ORGANIZATION, 1,
        applicablePolicies.policiesByOwner.get(2));
    assertEquals(orgPolicy2.getId(), applicablePolicies.policiesByOwner.get(1).policies.get(0).getId());
    assertEquals(parentOrgPolicy2.getId(), applicablePolicies.policiesByOwner.get(2).policies.get(0).getId());
  }

  @Test
  public void testImportPolicies_NonJsonPolicyFile() throws Exception {
    Organization org = tempEntity.newOrganization();
    HttpResponse response = restRequest(OwnerType.ORGANIZATION, org.getId()).path("import")
        .part("file", "garbage.png", "garbage").post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("The file you selected failed to upload correctly, are you certain "
        + "it is a properly formatted policy import json file?"));
  }

  @Test
  public void testImportPolicies_JsonFileIncorrectFormat() throws Exception {
    Organization org = tempEntity.newOrganization();
    HttpResponse response = restRequest(OwnerType.ORGANIZATION, org.getId()).path("import")
        .part("file", "badPolicy.json", "{\"badJson\":\"noClosingBraces\"").post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("The file you selected failed to upload correctly, are you certain "
        + "it is a properly formatted policy import json file?"));
  }

  @Test
  public void testDeletePolicy_OwnerIdMismatch() throws Exception {
    Organization org = tempEntity.newOrganization("testDeletePolicyOwnerIdMismatch");
    String appPublicId1 = "PolicyResourceTest_AppId1";
    Application app1 = tempEntity.newApplication(appPublicId1, org.getId());
    String appPublicId2 = "PolicyResourceTest_AppId2";
    tempEntity.newApplication(appPublicId2, org.getId());
    Policy policy = tempEntity.newPolicy(app1);

    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId2).path(policy.getId()).delete();
    assertResponseStatus(404, response);
    assertEquals("Cannot find a policy with ID " + policy.getId() + " for application ID " + appPublicId2,
        response.getBodyText());
    // Verify that the policy was not deleted
    assertThat(new PolicyDAO().getById(policy.getId()), notNullValue());
  }

  @Test
  public void testExportImport() throws Exception {
    // Export
    Organization fromOrg = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(fromOrg);

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, fromOrg.getId()).path("export").get();
    assertResponseStatus(200, response);
    PolicyExportResult policyExportResult = response.getBody(PolicyExportResult.class);
    assertThat(policyExportResult, is(notNullValue()));
    assertThat(policyExportResult.policies, hasSize(1));
    assertThat(policyExportResult.policies.get(0).getName(), is(policy.getName()));

    new OrganizationDAO().delete(fromOrg);

    // Import
    Organization toOrg = tempEntity.newOrganization();
    response = restRequest(OwnerType.ORGANIZATION, toOrg.getId()).path("import")
        .part("file", "policyExportResult.json", policyExportResult).post();
    assertResponseStatus(200, response);
    PolicyImportResult policyImportResult = response.getBody(PolicyImportResult.class);
    assertThat(policyImportResult, is(notNullValue()));
    assertThat(policyImportResult.ownerName, is(toOrg.getName()));

    List<Policy> policies = policyDAO.getByOwnerId(toOrg.getId());
    assertThat(policies, hasSize(1));
    assertThat(policies.get(0).getName(), is(policy.getName()));
  }

  @Test
  public void testImportPolicies_AppImportNotSupported() throws Exception {
    HttpResponse response = restRequest(OwnerType.APPLICATION, "foo").path("import")
        .part("file", "file", createImportBody()).post();

    // policy import to applications is no longer supported
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("Importing policies into an application is no longer supported."));
  }

  @Test
  public void testImportPolicies_NoPolicies() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, tempEntity.newOrganization().getId()).path("import")
        .part("file", "file", policyExportResult).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText(),
        is("The file you selected failed to upload correctly, the policy file needs to have at least one " +
            "policy defined."));
  }
}
