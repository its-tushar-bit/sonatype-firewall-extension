/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.policy.PolicyResource.ApplicablePolicies;
import com.sonatype.insight.brain.policy.PolicyResource.PoliciesByOwner;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.multipart.ByteArrayPartSource;
import com.ning.http.multipart.FilePart;
import org.junit.Assert;
import org.junit.Test;

import static com.sonatype.insight.brain.utils.IdUtils.TYPE_APPLICATION;
import static com.sonatype.insight.brain.utils.IdUtils.TYPE_ORGANIZATION;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;

public class PolicyResourceTest
    extends AbstractResourceTest
{
  private static final String APP = TYPE_APPLICATION;
  private static final String ORG = TYPE_ORGANIZATION;
  private static final PolicyDAO policyDAO = new PolicyDAO();

  @Test
  public void testAppImport_InsertFailure() throws Exception {
    String applicationPublicId = "PolicyResourceTest-testAppImport_Insert";
    Response response = AuthedRestAccess
        .post(getServiceURL(APP, applicationPublicId), toJson(new PolicyExportResult()));
    // ensure that we cannot import to an App that does not exist
    assertResponseStatus(404, response);
    assertThat(response.getResponseBody(), is("Could not find an application with public ID " + applicationPublicId
        + "."));
  }

  @Test
  public void testOrgImport_InsertFailure() throws Exception {
    String orgId = "PolicyResourceTest-testOrgImport_Insert";
    Response response = AuthedRestAccess.post(getServiceURL(ORG, orgId), toJson(new PolicyExportResult()));
    // ensure that we cannot import to an Org that does not exist
    assertResponseStatus(404, response);
    assertThat(response.getResponseBody(), is("Cannot find organization with ID " + orgId + "."));
  }

  @Test
  public void testCRUD_ApplicationLevel() throws Exception {
    String applicationPublicId = "PolicyResourceTest_testCRUD";
    tempEntity.newApplicationWithParent(applicationPublicId);

    testCRUD(APP, applicationPublicId);
  }

  @Test
  public void testCRUD_OrganizationLevel() throws Exception {
    String orgId = tempEntity.newOrganization("test").getId();

    testCRUD(ORG, orgId);
  }

  private void testCRUD(String ownerType, String ownerId) throws Exception {
    // Add a policy
    Policy policy = new Policy();
    policy.setName("PolicyResourceTest new policy");
    Constraint constraint = new Constraint();
    constraint.setName("PolicyResourceTest new constraint");
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    Response response = AuthedRestAccess.post(getServiceURL(ownerType, ownerId), toJson(policy));
    assertResponseStatus(200, response);
    final Policy policy1 = fromJson(response, Policy.class);
    assertNotNull(policy1.getId());
    Assert.assertEquals("PolicyResourceTest new policy", policy1.getName());

    // Get all policies
    response = AuthedRestAccess.get(getServiceURL(ownerType, ownerId));
    assertResponseStatus(200, response);
    Policy[] policies = fromJson(response, Policy[].class);
    assertNotNull(policies);
    Assert.assertEquals(1, policies.length);
    Assert.assertEquals(policy1.getId(), policies[0].getId());
    Assert.assertEquals(policy1.getName(), policies[0].getName());

    // Update a policy
    policy = policies[0];
    policy.setName("PolicyResourceTest updated policy");
    response = AuthedRestAccess.put(getServiceURL(ownerType, ownerId), toJson(policy));
    assertResponseStatus(200, response);
    final Policy policy2 = fromJson(response, Policy.class);
    Assert.assertEquals("PolicyResourceTest updated policy", policy2.getName());

    // Get all policies
    response = AuthedRestAccess.get(getServiceURL(ownerType, ownerId));
    assertResponseStatus(200, response);
    policies = fromJson(response, Policy[].class);
    assertNotNull(policies);
    Assert.assertEquals(1, policies.length);
    Assert.assertEquals(policy2.getId(), policies[0].getId());
    Assert.assertEquals(policy2.getName(), policies[0].getName());

    // Delete a policy
    policy = policies[0];
    response = AuthedRestAccess.delete(getServiceURL(ownerType, ownerId, policy.getId()));
    assertResponseStatus(204, response);

    // Get all policies
    response = AuthedRestAccess.get(getServiceURL(ownerType, ownerId));
    assertResponseStatus(200, response);
    policies = fromJson(response, Policy[].class);
    assertNotNull(policies);
    Assert.assertEquals(0, policies.length);
  }

  @Test
  public void testCreateInvalidPolicy_AppLevel() throws Exception {
    String applicationPublicId = "PolicyResourceTest_testCreateInvalidPolicy";
    tempEntity.newApplicationWithParent(applicationPublicId);
    testCreateInvalidPolicy(APP, applicationPublicId);
  }

  @Test
  public void testCreateInvalidPolicy_OrgLevel() throws Exception {
    String orgId = tempEntity.newOrganization("test").getId();
    testCreateInvalidPolicy(ORG, orgId);
  }

  private void testCreateInvalidPolicy(String ownerType, String ownerId) throws Exception {
    Policy policy = new Policy();
    policy.setName(null);
    Constraint constraint = new Constraint();
    constraint.setName("PolicyResourceTest new constraint");
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    Response response = AuthedRestAccess.post(getServiceURL(ownerType, ownerId), toJson(policy));
    assertResponseStatus(400, response);
    Assert.assertEquals("The policy name is required.", response.getResponseBody());
  }

  @Test
  public void testUpdateInvalidPolicy_AppLevel() throws Exception {
    String applicationPublicId = "PolicyResourceTest_testUpdateInvalidPolicy";
    tempEntity.newApplicationWithParent(applicationPublicId);
    testUpdateInvalidPolicy(APP, applicationPublicId);
  }

  @Test
  public void testUpdateInvalidPolicy_OrgLevel() throws Exception {
    String orgId = tempEntity.newOrganization("test").getId();
    testUpdateInvalidPolicy(ORG, orgId);
  }

  private void testUpdateInvalidPolicy(String ownerType, String ownerId) throws Exception {
    // Create a valid policy
    Policy policy = new Policy();
    policy.setOwnerId(ownerId);
    policy.setName("PolicyResourceTest-testUpdateInvalidPolicy");
    Constraint constraint = new Constraint();
    constraint.setName("PolicyResourceTest new constraint");
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    policyDAO.insert(policy);

    // Update invalid policy
    policy.setName(null);
    Response response = AuthedRestAccess.put(getServiceURL(ownerType, ownerId), toJson(policy));
    assertResponseStatus(400, response);
    Assert.assertEquals("The policy name is required.", response.getResponseBody());
  }

  private void assertPoliciesByOwner(String ownerId, String ownerName, String ownerType, int policyCount,
      PoliciesByOwner actual)
  {
    Assert.assertEquals(ownerId, actual.ownerId);
    Assert.assertEquals(ownerName, actual.ownerName);
    Assert.assertEquals(ownerType, actual.ownerType);
    Assert.assertEquals(policyCount, actual.policies.size());
  }

  @Test
  public void testGetApplicablePolicies() throws Exception {
    // Create an organization and an application
    String orgName = "testGetApplicablePoliciesOrg";
    String orgId = tempEntity.newOrganization(orgName).getId();
    String appName = "testGetApplicablePoliciesApp";
    String appPublicId = appName;
    Application app = tempEntity.newApplication(appName, appPublicId, orgId);
    String appId = app.getId();

    // Verify the applicable policies for the application
    Response response = AuthedRestAccess.get(getServiceURL(APP, appPublicId) + "/applicable");
    assertResponseStatus(200, response);
    ApplicablePolicies applicablePolicies = fromJson(response, ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    Assert.assertEquals(2, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(appId, appName, "application", 0, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get(1));

    // Verify the applicable policies for the organization
    response = AuthedRestAccess.get(getServiceURL(ORG, orgId) + "/applicable");
    assertResponseStatus(200, response);
    applicablePolicies = fromJson(response, ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    Assert.assertEquals(1, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get(0));

    // Create a policy for the application
    Policy appPolicy = tempEntity.newPolicy(appId, "testGetApplicablePolicies App Policy");

    // Verify the applicable policies for the application
    response = AuthedRestAccess.get(getServiceURL(APP, appPublicId) + "/applicable");
    assertResponseStatus(200, response);
    applicablePolicies = fromJson(response, ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    Assert.assertEquals(2, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(appId, appName, "application", 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get(1));
    Assert.assertEquals(appPolicy.getId(), applicablePolicies.policiesByOwner.get(0).policies.get(0).getId());

    // Verify the applicable policies for the organization
    response = AuthedRestAccess.get(getServiceURL(ORG, orgId) + "/applicable");
    assertResponseStatus(200, response);
    applicablePolicies = fromJson(response, ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    Assert.assertEquals(1, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get(0));

    // Create a policy for the organization
    Policy orgPolicy = tempEntity.newPolicy(orgId, "testGetApplicablePolicies Org Policy");

    // Verify the applicable policies for the application
    response = AuthedRestAccess.get(getServiceURL(APP, appPublicId) + "/applicable");
    assertResponseStatus(200, response);
    applicablePolicies = fromJson(response, ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    Assert.assertEquals(2, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(appId, appName, "application", 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, "organization", 1, applicablePolicies.policiesByOwner.get(1));
    Assert.assertEquals(appPolicy.getId(), applicablePolicies.policiesByOwner.get(0).policies.get(0).getId());
    Assert.assertEquals(orgPolicy.getId(), applicablePolicies.policiesByOwner.get(1).policies.get(0).getId());

    // Verify the applicable policies for the organization
    response = AuthedRestAccess.get(getServiceURL(ORG, orgId) + "/applicable");
    assertResponseStatus(200, response);
    applicablePolicies = fromJson(response, ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    Assert.assertEquals(1, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(orgId, orgName, "organization", 1, applicablePolicies.policiesByOwner.get(0));
    Assert.assertEquals(orgPolicy.getId(), applicablePolicies.policiesByOwner.get(0).policies.get(0).getId());
  }

  @Test
  public void testGetApplicablePolicies_FilteredByTag() throws Exception {
    // Create an organization and an application
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    // Create a tagged policy that doesn't match an app tag. This policy should not appear in the result.
    Policy orgPolicy1 = tempEntity.newPolicy(org.getId(), "testGetApplicablePolicies Org Policy 1");
    Tag tag1 = tempEntity.newTag(org.getId());
    tempEntity.newPolicyTag(orgPolicy1.getId(), tag1.getId());

    // Create another tagged policy that matches an app tag. This policy should appear in the result.
    Policy orgPolicy2 = tempEntity.newPolicy(org.getId(), "testGetApplicablePolicies Org Policy 2");
    Tag tag2 = tempEntity.newTag(org.getId());
    tempEntity.newPolicyTag(orgPolicy2.getId(), tag2.getId());
    tempEntity.newApplicationTag(app.getId(), tag2.getId());

    // Verify the applicable policies for the application
    Response response = AuthedRestAccess.get(getServiceURL(APP, app.getPublicId()) + "/applicable");
    assertResponseStatus(200, response);
    ApplicablePolicies applicablePolicies = fromJson(response, ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    Assert.assertEquals(2, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(app.getId(), app.getName(), "application", 0, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(org.getId(), org.getName(), "organization", 1, applicablePolicies.policiesByOwner.get(1));
    Assert.assertEquals(orgPolicy2.getId(), applicablePolicies.policiesByOwner.get(1).policies.get(0).getId());
  }

  /**
   * Expected to return a validation error text response and HTTP 200 in the case of an error uploading to IE.
   */
  @Test
  public void testImportPoliciesForIEReturnsErrorMessage() throws Exception {
    Application app = tempEntity.newApplicationWithParent("testAppPublicId");
    PolicyExportResult export = new PolicyExportResult();

    String url = getRestUrl(PolicyResource.SERVICE_PATH + "/import/ie", IdUtils.TYPE_APPLICATION, app.getPublicId());
    AsyncHttpClient.BoundRequestBuilder builder =  AuthedRestAccess.getClient().preparePost(url);
    builder.addBodyPart(new FilePart("file", new ByteArrayPartSource("file", toJson(export).getBytes())));
    RestAccess.addAuthorization(builder, User.ADMIN_USERNAME, "admin123");

    Response response = builder.execute().get();
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(),
        is("The file you selected failed to upload correctly, are you certain it is a properly formatted policy import json file?"));
  }

  @Test
  public void testImportOfNonJsonPolicyFile() throws Exception{
    Organization org = tempEntity.newOrganization();
    Response response = AuthedRestAccess.put(getServiceURL(ORG, org.getId()) + "/import",
        "garbage");
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("The file you selected failed to upload correctly, are you certain " +
        "it is a properly formatted policy import json file?"));
  }

  @Test
  public void testImportOfJsonFileIncorrectFormat() throws Exception {
    Organization org = tempEntity.newOrganization();
    Response response = AuthedRestAccess.put(getServiceURL(ORG, org.getId()) + "/import",
        "{\"notPolicy\":\"anything\"}");
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("The file you selected failed to upload correctly, are you certain "
        + "it is a properly formatted policy import json file?"));
  }

  private String getServiceURL(final String ownerType, final String ownerId) {
    return getRestUrl(PolicyResource.SERVICE_PATH, ownerType, ownerId);
  }

  private String getServiceURL(final String ownerType, final String ownerId, final String policyId) {
    return getServiceURL(ownerType, ownerId) + "/" + policyId;
  }

  @Test
  public void testDeletePolicy_OwnerIdMismatch() throws Exception {
    Organization org = tempEntity.newOrganization("testDeletePolicyOwnerIdMismatch");
    String appPublicId1 = "PolicyResourceTest_AppId1";
    Application app1 = tempEntity.newApplication(appPublicId1, org.getId());
    String appPublicId2 = "PolicyResourceTest_AppId2";
    tempEntity.newApplication(appPublicId2, org.getId());
    Policy policy = tempEntity.newPolicy(app1.getId(), "testDeletePolicyOwnerIdMismatch");

    Response response = AuthedRestAccess.delete(getServiceURL(IdUtils.TYPE_APPLICATION, appPublicId2) + "/"
        + policy.getId());
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a policy with ID " + policy.getId() + " for application ID " + appPublicId2,
        response.getResponseBody());
    // Verify that the policy was not deleted
    assertThat(new PolicyDAO().getById(policy.getId()), notNullValue());
  }

  @Test
  public void testExportImport_Organization() throws Exception {
    // Export
    Organization fromOrg = tempEntity.newOrganization();
    tempEntity.newPolicy(fromOrg.getId(), "Test Policy");

    Response response = AuthedRestAccess.get(getServiceURL(ORG, fromOrg.getId()) + "/export");
    assertResponseStatus(200, response);
    PolicyExportResult policyExportResult = JsonUtils.parse(response.getResponseBody(), PolicyExportResult.class);
    assertThat(policyExportResult, is(notNullValue()));
    assertThat(policyExportResult.policies, hasSize(1));
    assertThat(policyExportResult.policies.get(0).getName(), is("Test Policy"));

    new OrganizationDAO().delete(fromOrg);

    // Import
    Organization toOrg = tempEntity.newOrganization();
    response = AuthedRestAccess
        .put(getServiceURL(ORG, toOrg.getId()) + "/import", JsonUtils.format(policyExportResult));
    assertResponseStatus(200, response);
    PolicyImportResult policyImportResult = JsonUtils.parse(response.getResponseBody(), PolicyImportResult.class);
    assertThat(policyImportResult, is(notNullValue()));
    assertThat(policyImportResult.ownerName, is(toOrg.getName()));
    assertThat(policyImportResult.url, endsWith("index.html#/management/organization/" + toOrg.getId()));

    List<Policy> policies = policyDAO.getByOwnerId(toOrg.getId());
    assertThat(policies, hasSize(1));
    assertThat(policies.get(0).getName(), is("Test Policy"));
  }

  @Test
  public void testExportImport_Application() throws Exception {
    // Export
    Application fromApp = tempEntity.newApplicationWithParent("FromAppPublicId");
    tempEntity.newPolicy(fromApp.getId(), "Test Policy");

    Response response = AuthedRestAccess.get(getServiceURL(APP, fromApp.getPublicId()) + "/export");
    assertResponseStatus(200, response);
    PolicyExportResult policyExportResult = JsonUtils.parse(response.getResponseBody(), PolicyExportResult.class);
    assertThat(policyExportResult, is(notNullValue()));
    assertThat(policyExportResult.policies, hasSize(1));
    assertThat(policyExportResult.policies.get(0).getName(), is("Test Policy"));

    new ApplicationDAO().delete(fromApp);

    // Import
    Application toApp = tempEntity.newApplicationWithParent("ToAppPublicId");
    response = AuthedRestAccess.put(getServiceURL(APP, toApp.getPublicId()) + "/import",
        JsonUtils.format(policyExportResult));
    assertResponseStatus(200, response);
    PolicyImportResult policyImportResult = JsonUtils.parse(response.getResponseBody(), PolicyImportResult.class);
    assertThat(policyImportResult, is(notNullValue()));
    assertThat(policyImportResult.ownerName, is(toApp.getName()));
    assertThat(policyImportResult.url, endsWith("index.html#/management/application/" + toApp.getPublicId()));

    List<Policy> policies = policyDAO.getByOwnerId(toApp.getId());
    assertThat(policies, hasSize(1));
    assertThat(policies.get(0).getName(), is("Test Policy"));
  }
}
