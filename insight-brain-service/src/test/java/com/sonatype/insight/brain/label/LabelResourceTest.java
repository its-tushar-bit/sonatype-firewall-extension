/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.Locale;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.label.LabelService.ApplicableLabels;
import com.sonatype.insight.brain.label.LabelService.LabelsByOwner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class LabelResourceTest
    extends AbstractResourceTest
{
  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(LabelResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  @Test
  public void testApplicationCRUD() throws Exception {
    // Create an application
    String appPublicId = "LabelResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);
    HttpRequest request = restRequest(OwnerType.APPLICATION, appPublicId);

    // Get all labels
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    Label[] labels = response.getBody(Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(0, labels.length);

    // Add a label
    Label label = new Label();
    label.setLabel("MyLabel");
    response = request.body(label).post();
    assertResponseStatus(200, response);
    label = response.getBody(Label.class);
    assertLabel(application.getId(), "MyLabel", Color.white, label);

    // Get all labels
    response = request.get();
    assertResponseStatus(200, response);
    labels = response.getBody(Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(application.getId(), "MyLabel", Color.white, labels[0]);

    // Update a label
    label.setLabel("MyUpdatedLabel");
    response = request.body(label).put();
    assertResponseStatus(200, response);
    label = response.getBody(Label.class);
    assertLabel(application.getId(), "MyUpdatedLabel", Color.white, label);

    // Get all labels
    response = request.get();
    assertResponseStatus(200, response);
    labels = response.getBody(Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(application.getId(), "MyUpdatedLabel", Color.white, labels[0]);

    // Delete a label
    response = request.subpath(label.getId()).delete();
    assertResponseStatus(204, response);

    // Get all labels
    response = request.get();
    assertResponseStatus(200, response);
    labels = response.getBody(Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(0, labels.length);
  }

  @Test
  public void testDeleteAppLabel_UsedInPolicyCondition() throws Exception {
    Application app = tempEntity.newApplicationWithParent("appPublicId");
    testDelete_InUseByPolicy(OwnerType.APPLICATION, app.getPublicId(), app.getId(), app.getId(), null);
  }

  @Test
  public void testDeleteAppLabel_Nonexistent() throws Exception {
    String appPublicId = "LabelResourceTest_AppId";
    tempEntity.newApplicationWithParent(appPublicId);

    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("YettiId").delete();
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a label with ID YettiId.", response.getBodyText());
  }

  @Test
  public void testDeleteAppLabel_OwnerIdMismatch() throws Exception {
    String appPublicId1 = "LabelResourceTest_AppId1";
    Application application1 = tempEntity.newApplicationWithParent(appPublicId1);
    String appPublicId2 = "LabelResourceTest_AppId2";
    tempEntity.newApplicationWithParent(appPublicId2);
    Label label = tempEntity.newLabel(application1.getId(), "MyLabel", Color.blue);
    
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId2).path(label.getId()).delete();
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a label with ID " + label.getId() + " for application ID " + appPublicId2,
        response.getBodyText());
    // Verify that the label was not deleted
    response = restRequest(OwnerType.APPLICATION, appPublicId1).get();
    assertResponseStatus(200, response);
    Label[] labels = response.getBody(Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(application1.getId(), "MyLabel", Color.blue, labels[0]);
  }

  @Test
  public void testOrganizationCRUD() throws Exception {
    // Create an organization
    String orgName = "LabelResourceTestOrgName";
    Organization organization = tempEntity.newOrganization(orgName);
    HttpRequest request = restRequest(OwnerType.ORGANIZATION, organization.getId());

    // Get all labels
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    Label[] labels = response.getBody(Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(0, labels.length);

    // Add a label
    Label label = new Label();
    label.setLabel("MyLabel");
    response = request.body(label).post();
    assertResponseStatus(200, response);
    label = response.getBody(Label.class);
    assertLabel(organization.getId(), "MyLabel", Color.white, label);

    // Get all labels
    response = request.get();
    assertResponseStatus(200, response);
    labels = response.getBody(Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(organization.getId(), "MyLabel", Color.white, labels[0]);

    // Update a label
    label.setLabel("MyUpdatedLabel");
    response = request.body(label).put();
    assertResponseStatus(200, response);
    label = response.getBody(Label.class);
    assertLabel(organization.getId(), "MyUpdatedLabel", Color.white, label);

    // Get all labels
    response = request.get();
    assertResponseStatus(200, response);
    labels = response.getBody(Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(organization.getId(), "MyUpdatedLabel", Color.white, labels[0]);

    // Delete a label
    response = request.subpath(label.getId()).delete();
    assertResponseStatus(204, response);

    // Get all labels
    response = request.get();
    assertResponseStatus(200, response);
    labels = response.getBody(Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(0, labels.length);
  }

  @Test
  public void testDeleteOrgLabel_UsedInPolicyCondition() throws Exception {
    Organization org = tempEntity.newOrganization();
    testDelete_InUseByPolicy(OwnerType.ORGANIZATION, org.getId(), org.getId(), org.getId(), null);
  }

  @Test
  public void testDeleteOrgLabel_UsedInAppPolicyCondition() throws Exception {
    Application app = tempEntity.newApplicationWithParent("appPublicId", "appName");
    testDelete_InUseByPolicy(OwnerType.ORGANIZATION, app.getOrganizationId(), app.getOrganizationId(), app.getId(),
        "in application 'appName'");
  }

  @Test
  public void testDeleteOrgLabel_UsedInGrandChildAppPolicyCondition() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("appName", "appPublicId", org.getId());
    testDelete_InUseByPolicy(OwnerType.ORGANIZATION, org.getParentOrganizationId(), org.getParentOrganizationId(),
        app.getId(), "in application 'appName'");
  }

  @Test
  public void testDeleteOrgLabel_UsedInChildOrgPolicyCondition() throws Exception {
    Organization org = tempEntity.newOrganization("orgName");
    testDelete_InUseByPolicy(OwnerType.ORGANIZATION, org.getParentOrganizationId(), org.getParentOrganizationId(),
        org.getId(), "in organization 'orgName'");
  }

  private void testDelete_InUseByPolicy(OwnerType ownerType, String ownerPublicId, String ownerId,
      String policyOwnerId, String policyLocation) throws Exception
  {
    Label label = tempEntity.newLabel(ownerId);

    Policy policy = new Policy(null, "policyName");
    policy.setOwnerId(policyOwnerId);
    Constraint constraint = new Constraint(null, "constraintName", LogicalOperator.AND);
    constraint.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    HttpResponse response = restRequest(ownerType, ownerPublicId).path(label.getId()).delete();
    assertResponseStatus(400, response);

    String error = "Cannot delete the label because it is used in a condition for the 'policyName' policy";
    if (null != policyLocation) {
      error = error + " " + policyLocation;
    }
    Assert.assertThat(response.getBodyText(), is(error));

    Assert.assertThat(new LabelDAO().getById(label.getId()), is(notNullValue()));
  }

  @Test
  public void testDeleteOrgLabel_Nonexistent() throws Exception {
    String orgName = "LabelResourceTestOrgName";
    Organization organization = tempEntity.newOrganization(orgName);

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, organization.getId()).path("YettiId").delete();
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a label with ID YettiId.", response.getBodyText());
  }

  @Test
  public void testDeleteOrgLabel_OwnerIdMismatch() throws Exception {
    String orgName1 = "LabelResourceTestOrgName1";
    Organization organization1 = tempEntity.newOrganization(orgName1);
    String orgName2 = "LabelResourceTestOrgName2";
    Organization organization2 = tempEntity.newOrganization(orgName2);

    Label label = tempEntity.newLabel(organization1.getId(), "MyLabel", Color.blue);
    
    HttpResponse response = restRequest(OwnerType.ORGANIZATION, organization2.getId()).path(label.getId()).delete();
    assertResponseStatus(404, response);
    Assert.assertEquals(
        "Cannot find a label with ID " + label.getId() + " for organization ID " + organization2.getId(),
        response.getBodyText());
    // Verify that the label was not deleted
    response = restRequest(OwnerType.ORGANIZATION, organization1.getId()).get();
    assertResponseStatus(200, response);
    Label[] labels = response.getBody(Label[].class);
    Assert.assertNotNull(labels);
    Assert.assertEquals(1, labels.length);
    assertLabel(organization1.getId(), "MyLabel", Color.blue, labels[0]);
  }

  /**
   * Tests for {@link LabelResource#getApplicableLabels(OwnerType, String)}.
   */
  @Test
  public void testGetApplicableLabels() throws Exception {
    // Create an organization and an application
    Organization org = tempEntity.newOrganization("testGetApplicableLabelsOrg");
    String orgId = org.getId();
    String appPublicId = "testGetApplicableLabelsApp";
    Application app = tempEntity.newApplication(appPublicId, appPublicId, org.getId());
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());

    final Repository repository = tempEntity.newRepository();
    final String repoId = repository.getId();

    // Verify the applicable labels for the application
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    ApplicableLabels applicableLabels = response.getBody(ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    // One for the application, one for it's org and one for the root org
    Assert.assertEquals(3, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(app, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(org, 0, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(2));

    // Verify the applicable labels for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    // One for the org and one for the root org
    Assert.assertEquals(2, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(org, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(1));

    // Verify the applicable labels for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    // One for the repository, one for the repositoryContainer, and one for the root org
    Assert.assertEquals(3, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(repository, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(RepositoryContainer.SINGLETON, 0, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(2));

    // Create a label for the application
    Label appLabel = tempEntity.newLabel(app.getId(), "testGetApplicableLabels_App_label");

    // Verify the applicable labels for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    // One for the application, one for it's org and one for the root org
    Assert.assertEquals(3, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(app, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(org, 0, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(2));
    Assert.assertEquals(appLabel.getId(), applicableLabels.labelsByOwner.get(0).labels.get(0).getId());

    // Verify the applicable labels for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    // One the org and one for the root org
    Assert.assertEquals(2, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(org, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(1));

    // Create a label for the organization
    Label orgLabel = tempEntity.newLabel(orgId, "testGetApplicableLabels_Org_label");

    // Verify the applicable labels for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    Assert.assertEquals(3, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(app, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(org, 1, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(2));
    Assert.assertEquals(appLabel.getId(), applicableLabels.labelsByOwner.get(0).labels.get(0).getId());
    Assert.assertEquals(orgLabel.getId(), applicableLabels.labelsByOwner.get(1).labels.get(0).getId());

    // Verify the applicable labels for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    // One the org and one for the root org
    Assert.assertEquals(2, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(org, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(1));
    Assert.assertEquals(orgLabel.getId(), applicableLabels.labelsByOwner.get(0).labels.get(0).getId());

    // Create a label for the parent organization
    Label rootOrgLabel = tempEntity.newLabel(parentOrg.getId(), "testGetApplicableLabels_ParentOrg_label");

    // Verify the applicable labels for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    Assert.assertEquals(3, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(app, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(org, 1, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 1, applicableLabels.labelsByOwner.get(2));
    Assert.assertEquals(appLabel.getId(), applicableLabels.labelsByOwner.get(0).labels.get(0).getId());
    Assert.assertEquals(orgLabel.getId(), applicableLabels.labelsByOwner.get(1).labels.get(0).getId());
    Assert.assertEquals(rootOrgLabel.getId(), applicableLabels.labelsByOwner.get(2).labels.get(0).getId());

    // Verify the applicable labels for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    // One for the org and one for the root org
    Assert.assertEquals(2, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(org, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(parentOrg, 1, applicableLabels.labelsByOwner.get(1));
    Assert.assertEquals(orgLabel.getId(), applicableLabels.labelsByOwner.get(0).labels.get(0).getId());
    Assert.assertEquals(rootOrgLabel.getId(), applicableLabels.labelsByOwner.get(1).labels.get(0).getId());

    // Verify the applicable labels for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    Assert.assertNotNull(applicableLabels);
    // One for the repository, one for the repositoryContainer, and one for the root org
    Assert.assertEquals(3, applicableLabels.labelsByOwner.size());
    assertLabelsByOwner(repository, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(RepositoryContainer.SINGLETON, 0, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 1, applicableLabels.labelsByOwner.get(2));
    Assert.assertEquals(rootOrgLabel.getId(), applicableLabels.labelsByOwner.get(2).labels.get(0).getId());
  }

  @Test
  public void testGetApplicableContexts() throws Exception {
    Organization org = tempEntity.newOrganization("orgName");
    Application app = tempEntity.newApplication("appName", "appPublicId", org.getId());
    Label parentOrgLabel = tempEntity.newLabel(org.getParentOrganizationId(), "rootOrgLabel");
    Label orgLabel = tempEntity.newLabel(org.getId(), "orgLabel");
    Label appLabel = tempEntity.newLabel(app.getId(), "appLabel");
    HttpRequest request = restRequest(OwnerType.APPLICATION, app.getPublicId()).subpath(
        "applicable/context/{labelId}");

    final Repository repository = tempEntity.newRepository();

    HttpResponse response = request.parameter(appLabel.getId()).get();
    assertResponseStatus(200, response);
    ApplicableContext context = response.getBody(ApplicableContext.class);
    Assert.assertThat(context, is(notNullValue()));
    Assert.assertThat(context.getId(), is(app.getPublicId()));
    Assert.assertThat(context.getName(), is(app.getName()));
    Assert.assertThat(context.getType(), is(OwnerType.APPLICATION));
    Assert.assertThat(context.getChildren(), is(nullValue()));

    final HttpRequest requestRepository = restRequest(OwnerType.REPOSITORY, repository.getId()).subpath(
        "applicable/context/{labelId}");
    response = requestRepository.parameter(appLabel.getId()).get();
    assertResponseStatus(200, response);
    context = response.getBody(ApplicableContext.class);
    Assert.assertThat(context, is(notNullValue()));
    Assert.assertThat(context.getId(), is(org.getParentOrganizationId()));
    Assert.assertThat(context.getName(), is("Root Organization"));
    Assert.assertThat(context.getType(), is(OwnerType.ORGANIZATION));
    Assert.assertThat(context.getChildren(), is(notNullValue()));
    Assert.assertThat(context.getChildren(), hasSize(1));
    context = context.getChildren().get(0);
    Assert.assertThat(context, is(notNullValue()));
    Assert.assertThat(context.getId(), is(RepositoryContainer.REPOSITORY_CONTAINER_ID));
    Assert.assertThat(context.getName(), is(RepositoryContainer.SINGLETON.getName()));
    Assert.assertThat(context.getType(), is(OwnerType.REPOSITORY_CONTAINER));
    Assert.assertThat(context.getChildren(), is(notNullValue()));
    Assert.assertThat(context.getChildren(), hasSize(1));
    context = context.getChildren().get(0);
    Assert.assertThat(context, is(notNullValue()));
    Assert.assertThat(context.getId(), is(repository.getId()));
    Assert.assertThat(context.getName(), is(repository.getName()));
    Assert.assertThat(context.getType(), is(repository.getType()));
    Assert.assertThat(context.getChildren(), is(nullValue()));

    response = request.parameter(orgLabel.getId()).get();
    assertResponseStatus(200, response);
    context = response.getBody(ApplicableContext.class);
    Assert.assertThat(context, is(notNullValue()));
    Assert.assertThat(context.getId(), is(org.getId()));
    Assert.assertThat(context.getName(), is(org.getName()));
    Assert.assertThat(context.getType(), is(OwnerType.ORGANIZATION));
    Assert.assertThat(context.getChildren(), is(notNullValue()));
    Assert.assertThat(context.getChildren(), hasSize(1));
    context = context.getChildren().get(0);
    Assert.assertThat(context, is(notNullValue()));
    Assert.assertThat(context.getId(), is(app.getPublicId()));
    Assert.assertThat(context.getName(), is(app.getName()));
    Assert.assertThat(context.getType(), is(OwnerType.APPLICATION));
    Assert.assertThat(context.getChildren(), is(nullValue()));

    response = request.parameter(parentOrgLabel.getId()).get();
    assertResponseStatus(200, response);
    context = response.getBody(ApplicableContext.class);
    Assert.assertThat(context, is(notNullValue()));
    Assert.assertThat(context.getId(), is(org.getParentOrganizationId()));
    Assert.assertThat(context.getName(), is("Root Organization"));
    Assert.assertThat(context.getType(), is(OwnerType.ORGANIZATION));
    Assert.assertThat(context.getChildren(), is(notNullValue()));
    Assert.assertThat(context.getChildren(), hasSize(1));
    context = context.getChildren().get(0);
    Assert.assertThat(context, is(notNullValue()));
    Assert.assertThat(context.getId(), is(org.getId()));
    Assert.assertThat(context.getName(), is(org.getName()));
    Assert.assertThat(context.getType(), is(OwnerType.ORGANIZATION));
    Assert.assertThat(context.getChildren(), is(notNullValue()));
    Assert.assertThat(context.getChildren(), hasSize(1));
    context = context.getChildren().get(0);
    Assert.assertThat(context, is(notNullValue()));
    Assert.assertThat(context.getId(), is(app.getPublicId()));
    Assert.assertThat(context.getName(), is(app.getName()));
    Assert.assertThat(context.getType(), is(OwnerType.APPLICATION));
    Assert.assertThat(context.getChildren(), is(nullValue()));

    // Test that a user with application WRITE permissions can only see contexts for which they have WRITE permissions
    // (i.e. the application context).
    User applicationUser = tempEntity.newUser();
    Role writeRole = tempEntity.newRole(false /* global */, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), writeRole.getId(), applicationUser.getUsername());

    response = request.parameter(orgLabel.getId()).auth(applicationUser.getUsername(), applicationUser.getPassword())
        .get();
    assertResponseStatus(200, response);
    context = response.getBody(ApplicableContext.class);
    Assert.assertThat(context, is(notNullValue()));
    Assert.assertThat(context.getId(), is(app.getPublicId()));
    Assert.assertThat(context.getName(), is(app.getName()));
    Assert.assertThat(context.getType(), is(OwnerType.APPLICATION));
    Assert.assertThat(context.getChildren(), is(nullValue()));
  }

  private void assertLabel(String ownerId, String label, Color color, Label actual) {
    Assert.assertEquals(ownerId, actual.getOwnerId());
    Assert.assertEquals(label, actual.getLabel());
    Assert.assertEquals(label.toLowerCase(Locale.ENGLISH), actual.getLabelLowercase());
    Assert.assertEquals(color, actual.getColor());
  }

  private void assertLabelsByOwner(Owner owner, int labelsCount, LabelsByOwner actual) {
    Assert.assertEquals(owner.getId(), actual.ownerId);
    Assert.assertEquals(owner.getName(), actual.ownerName);
    Assert.assertEquals(owner.getType(), actual.ownerType);
    Assert.assertEquals(labelsCount, actual.labels.size());
  }
}
