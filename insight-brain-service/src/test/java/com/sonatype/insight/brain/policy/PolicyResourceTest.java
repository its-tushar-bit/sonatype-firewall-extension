/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ValidationResult;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.policy.PolicyResource.ApplicablePolicies;
import com.sonatype.insight.brain.policy.PolicyResource.PoliciesByOwner;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.google.common.collect.Lists;
import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Test;

import static com.sonatype.insight.brain.utils.IdUtils.TYPE_APPLICATION;
import static com.sonatype.insight.brain.utils.IdUtils.TYPE_ORGANIZATION;
import static com.yammer.dropwizard.testing.JsonHelpers.asJson;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PolicyResourceTest
    extends AbstractResourceTest
{
  private static final String APP = TYPE_APPLICATION;
  private static final String ORG = TYPE_ORGANIZATION;
  private static final LabelDAO labelDAO = new LabelDAO();
  private static final ComponentLabelDAO componentLabelDao = new ComponentLabelDAO();
  private static final LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
  private static final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
  private static final TagDAO tagDAO = new TagDAO();
  private static final PolicyTagDAO policyTagDAO = new PolicyTagDAO();

  @Test
  public void testAppImport_InsertFailure() throws Exception {
    String applicationPublicId = "PolicyResourceTest-testAppImport_Insert";
    Response response = AuthedRestAccess.post(getServiceURL(APP, applicationPublicId),
        asJson(createPolicyExportResult()));
    //ensure that we cannot import to an App that does not exist
    assertResponseStatus(404, response);
    assertThat(response.getResponseBody(),
        is("Could not find an application with public id " + applicationPublicId + "."));
  }

  @Test
  public void testOrgImport_InsertFailure() throws Exception {
    String orgId = "PolicyResourceTest-testOrgImport_Insert";
    Response response = AuthedRestAccess.post(getServiceURL(ORG, orgId), asJson(createPolicyExportResult()));
    //ensure that we cannot import to an Org that does not exist
    assertResponseStatus(404, response);
    assertThat(response.getResponseBody(), is("Cannot find organization with id " + orgId + "."));
  }

  @Test
  public void testExportImport_Update() throws Exception {
    String applicationPublicId = "PolicyResourceTest-testExportImport-Update";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);
    String appId = application.getId();

    Label label1 = tempEntity.newLabel(appId, "label1", Color.blue);
    Label label2 = tempEntity.newLabel(appId,"label2", Color.red);
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(appId);
    LicenseThreatGroupLicense licenseThreatGroupLicense = tempEntity
        .newLicenseThreatGroupLicense(appId, licenseThreatGroup.getId());

    Policy policy = new Policy();
    policy.setName("Policy1");
    Constraint constraint1 = new Constraint();
    constraint1.setName("Constraint1");
    constraint1.addCondition(new Condition(LabelConditionType.ID, "is", label1.getId()));
    policy.addConstraint(constraint1);
    Constraint constraint2 = new Constraint();
    constraint2.setName("Constraint2");
    constraint2.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is", licenseThreatGroup.getId()));
    policy.addConstraint(constraint2);
    policy.addAction(BuildStageType.ID, new Action(Action.ID_FAIL));
    Response response = AuthedRestAccess.post(getServiceURL(APP, applicationPublicId), asJson(policy));
    assertResponseStatus(200, response);

    // Export
    response = AuthedRestAccess.get(getServiceURL(APP, applicationPublicId) + "/export");
    assertResponseStatus(200, response);
    PolicyExportResult policyExportResult = JsonHelpers.fromJson(response.getResponseBody(), PolicyExportResult.class);
    assertNotNull(policyExportResult);
    assertTrue(!policyExportResult.policies.isEmpty());
    assertTrue(!policyExportResult.labels.isEmpty());
    assertTrue(!policyExportResult.licenseThreatGroups.isEmpty());
    assertTrue(!policyExportResult.licenseThreatGroupLicenses.isEmpty());

    // Delete and re-create one label - it should be reset by import (matched by label case insensitive)
    labelDAO.delete(label1);
    label1 = tempEntity.newLabel(appId, label1.getLabel().toUpperCase(Locale.ENGLISH), Color.black);
    // Delete one label - it should be re-created by the import.
    labelDAO.delete(label2);
    // Add a new label - it should be deleted by the import.
    tempEntity.newLabel(appId, "label3", Color.red);

    // Import
    response = AuthedRestAccess.put(getServiceURL(APP, applicationPublicId) + "/import",
        asJson(policyExportResult));
    assertResponseStatus(200, response);
    PolicyImportResult policyImportResult = JsonHelpers.fromJson(response.getResponseBody(), PolicyImportResult.class);
    assertNotNull(policyImportResult);
    Assert.assertEquals(application.getName(), policyImportResult.ownerName);
    assertThat(policyImportResult.url, endsWith("index.html#/management/application/" + applicationPublicId));
    application = new ApplicationDAO().getByName(policyImportResult.ownerName);
    tempEntity.register(application);
    assertNotNull(application);
    List<Label> labels = labelDAO.getByOwnerId(application.getId());
    Assert.assertEquals(2, labels.size());
    Assert.assertEquals(label1.getId(), labels.get(0).getId());
    Assert.assertEquals("label1", labels.get(0).getLabel());
    Assert.assertEquals(Color.blue, labels.get(0).getColor());
    Assert.assertNotEquals(label2.getId(), labels.get(1).getId());
    Assert.assertEquals(label2.getLabel(), labels.get(1).getLabel());
    Assert.assertEquals(label2.getColor(), labels.get(1).getColor());
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(application.getId());
    Assert.assertEquals(1, licenseThreatGroups.size());
    Assert.assertEquals(licenseThreatGroup.getName(), licenseThreatGroups.get(0).getName());
    Assert.assertNotEquals(licenseThreatGroup.getId(), licenseThreatGroups.get(0).getId());
    List<LicenseThreatGroupLicense> licenseThreatGroupLicenses = licenseThreatGroupLicenseDAO.getByOwnerId(application
        .getId());
    Assert.assertEquals(1, licenseThreatGroupLicenses.size());
    Assert.assertEquals(licenseThreatGroupLicense.getLicenseId(), licenseThreatGroupLicenses.get(0).getLicenseId());
    Assert.assertNotEquals(licenseThreatGroupLicense.getId(), licenseThreatGroupLicenses.get(0).getId());
    response = AuthedRestAccess.get(getServiceURL(APP, applicationPublicId));
    assertResponseStatus(200, response);
    Policy[] policies = JsonHelpers.fromJson(response.getResponseBody(), Policy[].class);
    Assert.assertEquals(1, policies.length);
    Assert.assertEquals(policy.getName(), policies[0].getName());
    Assert.assertNotEquals(policy.getId(), policies[0].getId());
    ValidationResult policyValidationResult = policies[0].validate(application.getId());
    assertTrue(policyValidationResult.toMessageString(), policyValidationResult.isValid());
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
    Response response = AuthedRestAccess.post(getServiceURL(ownerType, ownerId), asJson(policy));
    assertResponseStatus(200, response);
    final Policy policy1 = JsonHelpers.fromJson(response.getResponseBody(), Policy.class);
    assertNotNull(policy1.getId());
    Assert.assertEquals("PolicyResourceTest new policy", policy1.getName());

    // Get all policies
    response = AuthedRestAccess.get(getServiceURL(ownerType, ownerId));
    assertResponseStatus(200, response);
    Policy[] policies = JsonHelpers.fromJson(response.getResponseBody(), Policy[].class);
    assertNotNull(policies);
    Assert.assertEquals(1, policies.length);
    Assert.assertEquals(policy1.getId(), policies[0].getId());
    Assert.assertEquals(policy1.getName(), policies[0].getName());

    // Update a policy
    policy = policies[0];
    policy.setName("PolicyResourceTest updated policy");
    response = AuthedRestAccess.put(getServiceURL(ownerType, ownerId), asJson(policy));
    assertResponseStatus(200, response);
    final Policy policy2 = JsonHelpers.fromJson(response.getResponseBody(), Policy.class);
    Assert.assertEquals("PolicyResourceTest updated policy", policy2.getName());

    // Get all policies
    response = AuthedRestAccess.get(getServiceURL(ownerType, ownerId));
    assertResponseStatus(200, response);
    policies = JsonHelpers.fromJson(response.getResponseBody(), Policy[].class);
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
    policies = JsonHelpers.fromJson(response.getResponseBody(), Policy[].class);
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
    Response response = AuthedRestAccess.post(getServiceURL(ownerType, ownerId), asJson(policy));
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
    policy.setName("PolicyResourceTest-testUpdateInvalidPolicy");
    Constraint constraint = new Constraint();
    constraint.setName("PolicyResourceTest new constraint");
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    Response response = AuthedRestAccess.post(getServiceURL(ownerType, ownerId), asJson(policy));
    assertResponseStatus(200, response);
    policy = JsonHelpers.fromJson(response.getResponseBody(), Policy.class);

    // Update invalid policy
    policy.setName(null);
    response = AuthedRestAccess.put(getServiceURL(ownerType, ownerId), asJson(policy));
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
    Application app = new Application(appPublicId, appName, orgId);
    ApplicationDAO appDAO = new ApplicationDAO();
    appDAO.insert(app);
    tempEntity.register(app);
    String appId = app.getId();

    // Verify the applicable policies for the application
    Response response = AuthedRestAccess.get(getServiceURL(APP, appPublicId) + "/applicable");
    assertResponseStatus(200, response);
    ApplicablePolicies applicablePolicies = JsonHelpers.fromJson(response.getResponseBody(), ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    Assert.assertEquals(2, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(appId, appName, "application", 0, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get(1));

    // Verify the applicable policies for the organization
    response = AuthedRestAccess.get(getServiceURL(ORG, orgId) + "/applicable");
    assertResponseStatus(200, response);
    applicablePolicies = JsonHelpers.fromJson(response.getResponseBody(), ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    Assert.assertEquals(1, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get(0));

    // Create a policy for the application
    Policy appPolicy = new Policy();
    appPolicy.setName("testGetApplicablePolicies App Policy");
    Constraint constraint = new Constraint();
    constraint.setName("testGetApplicablePolicies App constraint");
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    appPolicy.addConstraint(constraint);
    response = AuthedRestAccess.post(getServiceURL(APP, appPublicId), asJson(appPolicy));
    assertResponseStatus(200, response);
    appPolicy = JsonHelpers.fromJson(response.getResponseBody(), Policy.class);

    // Verify the applicable policies for the application
    response = AuthedRestAccess.get(getServiceURL(APP, appPublicId) + "/applicable");
    assertResponseStatus(200, response);
    applicablePolicies = JsonHelpers.fromJson(response.getResponseBody(), ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    Assert.assertEquals(2, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(appId, appName, "application", 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get(1));
    Assert.assertEquals(appPolicy.getId(), applicablePolicies.policiesByOwner.get(0).policies.get(0).getId());

    // Verify the applicable policies for the organization
    response = AuthedRestAccess.get(getServiceURL(ORG, orgId) + "/applicable");
    assertResponseStatus(200, response);
    applicablePolicies = JsonHelpers.fromJson(response.getResponseBody(), ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    Assert.assertEquals(1, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get(0));

    // Create a policy for the organization
    Policy orgPolicy = new Policy();
    orgPolicy.setName("testGetApplicablePolicies Org Policy");
    constraint = new Constraint();
    constraint.setName("testGetApplicablePolicies Org constraint");
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    orgPolicy.addConstraint(constraint);
    response = AuthedRestAccess.post(getServiceURL(ORG, orgId), asJson(orgPolicy));
    assertResponseStatus(200, response);
    orgPolicy = JsonHelpers.fromJson(response.getResponseBody(), Policy.class);

    // Verify the applicable policies for the application
    response = AuthedRestAccess.get(getServiceURL(APP, appPublicId) + "/applicable");
    assertResponseStatus(200, response);
    applicablePolicies = JsonHelpers.fromJson(response.getResponseBody(), ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    Assert.assertEquals(2, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(appId, appName, "application", 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, "organization", 1, applicablePolicies.policiesByOwner.get(1));
    Assert.assertEquals(appPolicy.getId(), applicablePolicies.policiesByOwner.get(0).policies.get(0).getId());
    Assert.assertEquals(orgPolicy.getId(), applicablePolicies.policiesByOwner.get(1).policies.get(0).getId());

    // Verify the applicable policies for the organization
    response = AuthedRestAccess.get(getServiceURL(ORG, orgId) + "/applicable");
    assertResponseStatus(200, response);
    applicablePolicies = JsonHelpers.fromJson(response.getResponseBody(), ApplicablePolicies.class);
    assertNotNull(applicablePolicies);
    Assert.assertEquals(1, applicablePolicies.policiesByOwner.size());
    assertPoliciesByOwner(orgId, orgName, "organization", 1, applicablePolicies.policiesByOwner.get(0));
    Assert.assertEquals(orgPolicy.getId(), applicablePolicies.policiesByOwner.get(0).policies.get(0).getId());
  }

  /**
   *
   * @since 1.7
   */
  @Test
  public void testImportDeletionOfExistingOrgPolicy() throws Exception{
    Organization org = createOrganizationWithPolicy();
    Application app = createApplicationWithPolicy(org);

    // import a policy with no data to the org
    Response response = AuthedRestAccess.put(getServiceURL(ORG, org.getId()) + "/import",
        asJson(createPolicyExportResult()));
    assertResponseStatus(200, response);

    //verify that we delete all data from the org
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(org.getId()), is(empty()));
    assertThat(licenseThreatGroupDAO.getByOwnerId(org.getId()), is(empty()));
    assertThat(policyDAO().getByOwnerId(org.getId()), is(empty()));
    assertThat(labelDAO.getByOwnerId(org.getId()), is(empty()));
    assertThat(componentLabelDao.getByOwnerId(org.getId()), is(empty()));

    //verify that we delete all data from the app
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(app.getId()), is(empty()));
    assertThat(licenseThreatGroupDAO.getByOwnerId(app.getId()), is(empty()));
    assertThat(policyDAO().getByOwnerId(app.getId()), is(empty()));
    assertThat(componentLabelDao.getByOwnerId(app.getId()), is(empty()));
    assertThat(labelDAO.getByOwnerId(app.getId()), is(empty()));
  }

  /**
   *
   * @since 1.7
   */
  @Test
  public void testImportDeletionOfExistingAppPolicy() throws Exception{
    Organization org = createOrganizationWithPolicy();
    Application app = createApplicationWithPolicy(org);

    // import a policy with no data to the app
    Response response = AuthedRestAccess.put(getServiceURL(APP, app.getPublicId()) + "/import",
        asJson(createPolicyExportResult()));
    assertResponseStatus(200, response);

    // verify that org data is untouched
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(org.getId()).size(), is(greaterThan(100))); //127 at time of writing, should only break if we remove many
    assertThat(licenseThreatGroupDAO.getByOwnerId(org.getId()), hasSize(5));
    assertThat(policyDAO().getByOwnerId(org.getId()), hasSize(1));
    assertThat(labelDAO.getByOwnerId(org.getId()), hasSize(1));
    assertThat(componentLabelDao.getByOwnerId(org.getId()), hasSize(1));

    //verify that we delete all data from the app
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(app.getId()), is(empty()));
    assertThat(licenseThreatGroupDAO.getByOwnerId(app.getId()), is(empty()));
    assertThat(policyDAO().getByOwnerId(app.getId()), is(empty()));
    assertThat(componentLabelDao.getByOwnerId(app.getId()), is(empty()));
    assertThat(labelDAO.getByOwnerId(app.getId()), is(empty()));
  }

  @Test
  public void testImportToOrg() throws Exception{
    Organization org = createOrganizationWithPolicy();
    Application app = createApplicationWithPolicy(org);

    // import policy into the org
    String importId = "importId";
    PolicyExportResult policyExportResult = createPolicyExportResult();
    LicenseThreatGroup ltg = createDetachedLTG(importId);
    ltg.setId(id());
    policyExportResult.licenseThreatGroups.add(ltg);
    LicenseThreatGroupLicense ltgl = createDetachedLTGL(importId, ltg.getId());
    ltgl.setId(id());
    policyExportResult.licenseThreatGroupLicenses.add(ltgl);
    //label uses same name as existing and will be updated, not deleted. Preserves existing component label
    Label label = new Label(importId, org.getId(), Color.black);
    label.setId(id());
    policyExportResult.labels.add(label);
    Policy policy = createDefaultPolicy(label.getId(), ltg.getId(), importId);
    policy.setId(id());
    policyExportResult.policies.add(policy);
    Tag tag = new Tag(org.getId(), "tagName", "tagDescription", Color.black);
    tag.setId(id());
    policyExportResult.tags = Arrays.asList(tag);
    PolicyTag policyTag = new PolicyTag(policy.getId(), tag.getId());
    policyExportResult.policyTags = Arrays.asList(policyTag);
    Response response = AuthedRestAccess.put(getServiceURL(ORG, org.getId()) + "/import",
        asJson(policyExportResult));
    assertResponseStatus(200, response);

    // verify that org data is as expected
    List<LicenseThreatGroupLicense> ltgls = licenseThreatGroupLicenseDAO.getByOwnerId(org.getId());
    assertThat(ltgls, hasSize(1));
    assertThat(ltgls.get(0).getId(), is(not(ltgl.getId())));
    List<LicenseThreatGroup> ltgs = licenseThreatGroupDAO.getByOwnerId(org.getId());
    assertThat(ltgs, hasSize(1));
    assertThat(ltgs.get(0).getId(), is(not(ltg.getId())));
    assertThat(ltgs.get(0).getName(), is(ltg.getName()));
    List<Policy> policies = policyDAO().getByOwnerId(org.getId());
    assertThat(policies, hasSize(1));
    assertThat(policies.get(0).getId(), is(not(policy.getId())));
    assertThat(policies.get(0).getName(), is(policy.getName()));
    List<Label> labels = labelDAO.getByOwnerId(org.getId());
    assertThat(labels, hasSize(1));
    assertThat(labels.get(0).getId(), is(not(label.getId())));
    assertThat(labels.get(0).getLabel(), is(label.getLabel()));
    assertThat(labels.get(0).getColor(), is(label.getColor()));
    assertThat(componentLabelDao.getByOwnerId(org.getId()), hasSize(1)); //preserved by import of labels
    List<Tag> tags = tagDAO.getByOrganizationId(org.getId());
    assertThat(tags, hasSize(1));
    assertThat(tags.get(0).getId(), is(not(tag.getId())));
    assertThat(tags.get(0).getName(), is(tag.getName()));
    assertThat(tags.get(0).getDescription(), is(tag.getDescription()));
    assertThat(tags.get(0).getColor(), is(tag.getColor()));
    List<PolicyTag> policyTags = policyTagDAO.getByPolicyId(policies.get(0).getId());
    assertThat(policyTags, hasSize(1));
    assertThat(policyTags.get(0).getTagId(), is(tags.get(0).getId()));

    //verify that we delete all data from the app
    assertThat(licenseThreatGroupDAO.getByOwnerId(app.getId()), is(empty()));
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(app.getId()), is(empty()));
    assertThat(policyDAO().getByOwnerId(app.getId()), is(empty()));
    assertThat(componentLabelDao.getByOwnerId(app.getId()), is(empty()));
    assertThat(labelDAO.getByOwnerId(app.getId()), is(empty()));
  }

  @Test
  public void testImportToApplicationWithTags() throws Exception {
    Application app = tempEntity.newApplicationWithParent("testAppPublicId");
    PolicyExportResult policyExportResult = createPolicyExportResult();
    Tag tag = new Tag("orgId", "tagName", "tagDescription", Color.black);
    tag.setId(id());
    policyExportResult.tags = Arrays.asList(tag);
    PolicyTag policyTag = new PolicyTag("policyId", tag.getId());
    policyExportResult.policyTags = Arrays.asList(policyTag);
    Response response = AuthedRestAccess.put(getServiceURL(APP, app.getPublicId()) + "/import",
        asJson(policyExportResult));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Importing policies with applied tags to an application is not supported"));
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
  public void testImportOfJsonFileIncorrectFormat() throws Exception{
    Organization org = tempEntity.newOrganization();
    Response response = AuthedRestAccess.put(getServiceURL(ORG, org.getId()) + "/import",
        "{\"notPolicy\":\"anything\"}");
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("The file you selected failed to upload correctly, are you certain " +
        "it is a properly formatted policy import json file?"));
  }

  @Test
  public void testExportOfTags() throws Exception {
    Organization org = tempEntity.newOrganization("orgName");

    Policy policy = new Policy();
    policy.setName("policy1");
    Constraint constraint = new Constraint();
    constraint.setName("constraintName");
    constraint.addCondition(new Condition(MatchStateConditionType.ID, "is", "similar"));
    policy.addConstraint(constraint);
    Response response = AuthedRestAccess.post(getServiceURL(ORG, org.getId()), asJson(policy));
    assertResponseStatus(200, response);
    Policy policy1 = JsonHelpers.fromJson(response.getResponseBody(), Policy.class);

    policy.setName("policy2");
    response = AuthedRestAccess.post(getServiceURL(ORG, org.getId()), asJson(policy));
    assertResponseStatus(200, response);
    Policy policy2 = JsonHelpers.fromJson(response.getResponseBody(), Policy.class);

    Tag tag1 = tempEntity.newTag(org.getId(), "tag1");
    tempEntity.newPolicyTag(policy1.getId(), tag1.getId());

    Tag tag2 = tempEntity.newTag(org.getId(), "tag2");
    tempEntity.newPolicyTag(policy2.getId(), tag2.getId());

    Tag tag3 = tempEntity.newTag(org.getId(), "tag3");
    tempEntity.newPolicyTag(policy1.getId(), tag3.getId());
    tempEntity.newPolicyTag(policy2.getId(), tag3.getId());

    // Export
    response = AuthedRestAccess.get(getServiceURL(ORG, org.getId()) + "/export");
    assertResponseStatus(200, response);
    PolicyExportResult policyExportResult = JsonHelpers.fromJson(response.getResponseBody(), PolicyExportResult.class);
    assertThat(policyExportResult, notNullValue());
    assertThat(policyExportResult.tags, notNullValue());
    assertThat(policyExportResult.tags, hasSize(3));
    assertThat(policyExportResult.policyTags, notNullValue());
    assertThat(policyExportResult.policyTags, hasSize(4));
  }

  private Application createApplicationWithPolicy(final Organization org) throws Exception {Response response;
    Application app = tempEntity.newApplication("appWithExistingPolicy", "appWithExistingPolicy", org.getId());
    Label label = tempEntity.newLabel(app.getId(), Color.white);
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(app.getId());
    tempEntity.newLicenseThreatGroupLicense(app.getId(), licenseThreatGroup.getId());
    // use both the label and the LTG in Policy Conditions to ensure that they are deleted as part of the import
    Policy appPolicy = createDefaultPolicy(label.getId(), licenseThreatGroup.getId(), app.getName());
    appPolicy.getConstraints().get(0).getConditions()
        .add(new Condition(LicenseThreatGroupConditionType.ID, "is", licenseThreatGroup.getId()));
    response = AuthedRestAccess.post(getServiceURL(APP, app.getPublicId()), asJson(appPolicy));
    assertResponseStatus(200, response);
    // label a (fake)component with our app label
    tempEntity.newComponentLabel(app.getId(), label.getId());
    return app;
  }

  private Organization createOrganizationWithPolicy() throws Exception {
    Organization org = tempEntity.newOrganization("orgWithExistingPolicy");
    Label label = tempEntity.newLabel(org.getId(), org.getId(), Color.white);
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(org.getId());
    tempEntity.newLicenseThreatGroupLicense(org.getId(), licenseThreatGroup.getId());
    // use both the label and the LTG in Policy Conditions to ensure that they are deleted as part of the import
    Policy orgPolicy = createDefaultPolicy(label.getId(), licenseThreatGroup.getId(),  org.getName());
    orgPolicy.getConstraints().get(0).getConditions()
        .add(new Condition(LicenseThreatGroupConditionType.ID, "is", licenseThreatGroup.getId()));
    Response response = AuthedRestAccess.post(getServiceURL(ORG, org.getId()), asJson(orgPolicy));
    assertResponseStatus(200, response);
    // label a (fake)component with our org label
    tempEntity.newComponentLabel(org.getId(), label.getId());
    return org;
  }

  private String getServiceURL(final String ownerType, final String ownerId) {
    return getRestUrl(PolicyResource.SERVICE_PATH, ownerType, ownerId);
  }

  private String getServiceURL(final String ownerType, final String ownerId, final String policyId) {
    return getServiceURL(ownerType, ownerId) + "/" + policyId;
  }

  private PolicyExportResult createPolicyExportResult() {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.licenseThreatGroupLicenses = Lists.newArrayList();
    policyExportResult.licenseThreatGroups = Lists.newArrayList();
    policyExportResult.policies = Lists.newArrayList();
    policyExportResult.labels = Lists.newArrayList();
    return policyExportResult;
  }

  private Policy createDefaultPolicy(String labelId, String ltgId, String name) {
    Policy policy = new Policy();
    policy.setName(name);
    Constraint constraint = new Constraint();
    constraint.setName(name);
    constraint.addCondition(new Condition(LabelConditionType.ID, "is", labelId));
    constraint.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is", ltgId));
    policy.addConstraint(constraint);
    return policy;
  }

  private LicenseThreatGroup createDetachedLTG(String ownerId) {
    LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroup();
    licenseThreatGroup.setOwnerId(ownerId);
    licenseThreatGroup.setName(ownerId);
    return licenseThreatGroup;
  }

  private LicenseThreatGroupLicense createDetachedLTGL(String ownerId, String ltgid) {
    LicenseThreatGroupLicense ltgl = new LicenseThreatGroupLicense();
    ltgl.setLicenseId("UNKNOWN");
    ltgl.setLicenseThreatGroupId(ltgid);
    ltgl.setOwnerId(ownerId);
    return ltgl;
  }

  private PolicyDAO policyDAO() {
    return new PolicyDAO();
  }

  private String id(){
    return UUID.randomUUID().toString();
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
    Assert.assertEquals("Cannot find a policy with id " + policy.getId() + " for application id " + appPublicId2,
        response.getResponseBody());
    // Verify that the policy was not deleted
    assertThat(new PolicyDAO().getById(policy.getId()), notNullValue());
  }
}
