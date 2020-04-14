/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLabelDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.label.LabelService;
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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLabelResourceTest
    extends AbstractResourceTest
{
  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PublicApiPaths.LABEL_RESOURCE_PATH).parameter(ownerType, ownerId);
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
    ApiLabelDTO[] labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).isEmpty();

    // Add a label
    ApiLabelDTO labelDTO = new ApiLabelDTO("MyLabel", "My Description", "light-green");
    response = request.body(labelDTO).post();
    assertResponseStatus(200, response);
    labelDTO = response.getBody(ApiLabelDTO.class);

    assertLabel(application.getId(), OwnerType.APPLICATION, "MyLabel", "My Description", Color.light_green, labelDTO);

    // Get all labels
    response = request.get();
    assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).hasSize(1);
    assertLabel(application.getId(), OwnerType.APPLICATION, "MyLabel", "My Description", Color.light_green, labels[0]);

    // Update a label
    labelDTO.label = "MyUpdatedLabel";
    labelDTO.description = "Description updated";
    labelDTO.color = "light-green";
    response = request.body(labelDTO).put();
    assertResponseStatus(200, response);
    labelDTO = response.getBody(ApiLabelDTO.class);

    assertLabel(application.getId(), OwnerType.APPLICATION, "MyUpdatedLabel", "Description updated", Color.light_green,
        labelDTO);

    // Get all labels
    response = request.get();
    assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).hasSize(1);
    assertLabel(application.getId(), OwnerType.APPLICATION, "MyUpdatedLabel", "Description updated", Color.light_green,
        labels[0]);

    // Delete a label
    response = request.subpath(labelDTO.id).delete();
    assertResponseStatus(204, response);

    // Get all labels
    response = request.get();
    assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).isEmpty();
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
    assertThat(response.getBodyText()).isEqualTo("Cannot find a label with ID YettiId.");
  }

  @Test
  public void testDeleteAppLabel_OwnerIdMismatch() throws Exception {
    String appPublicId1 = "LabelResourceTest_AppId1";
    Application application1 = tempEntity.newApplicationWithParent(appPublicId1);
    String appPublicId2 = "LabelResourceTest_AppId2";
    Application application2 = tempEntity.newApplicationWithParent(appPublicId2);
    Label label = tempEntity.newLabel(application1.getId(), "MyLabel", Color.dark_blue);

    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId2).path(label.getId()).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a label with ID " + label.getId() + " for application ID " + application2.getId());
    // Verify that the label was not deleted
    response = restRequest(OwnerType.APPLICATION, appPublicId1).get();
    assertResponseStatus(200, response);
    ApiLabelDTO[] labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).hasSize(1);
    assertLabel(application1.getId(), OwnerType.APPLICATION, "MyLabel", null, Color.dark_blue, labels[0]);
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
    ApiLabelDTO[] labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).isEmpty();

    // Add a label
    ApiLabelDTO labelDTO = new ApiLabelDTO("MyLabel", "My Description", "light-green");

    response = request.body(labelDTO).post();
    assertResponseStatus(200, response);
    labelDTO = response.getBody(ApiLabelDTO.class);

    assertLabel(organization.getId(), OwnerType.ORGANIZATION, "MyLabel", "My Description", Color.light_green, labelDTO);

    // Get all labels
    response = request.get();
    assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).hasSize(1);
    assertLabel(organization.getId(), OwnerType.ORGANIZATION, "MyLabel", "My Description", Color.light_green,
        labels[0]);

    // Update a label
    labelDTO.label = "MyUpdatedLabel";
    labelDTO.description = "Description Update";
    labelDTO.color = "light-green";
    response = request.body(labelDTO).put();
    assertResponseStatus(200, response);
    labelDTO = response.getBody(ApiLabelDTO.class);

    assertLabel(organization.getId(), OwnerType.ORGANIZATION, "MyUpdatedLabel", "Description Update", Color.light_green,
        labelDTO);

    // Get all labels
    response = request.get();
    assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).hasSize(1);
    assertLabel(organization.getId(), OwnerType.ORGANIZATION, "MyUpdatedLabel", "Description Update", Color.light_green,
        labels[0]);

    // Delete a label
    response = request.subpath(labelDTO.id).delete();
    assertResponseStatus(204, response);

    // Get all labels
    response = request.get();
    assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).isEmpty();
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

  private void testDelete_InUseByPolicy(
      OwnerType ownerType,
      String ownerPublicId,
      String ownerId,
      String policyOwnerId,
      String policyLocation) throws Exception
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
    assertThat(response.getBodyText()).isEqualTo(error);

    assertThat(new LabelDAO().getById(label.getId())).isNotNull();
  }

  @Test
  public void testDeleteOrgLabel_Nonexistent() throws Exception {
    String orgName = "LabelResourceTestOrgName";
    Organization organization = tempEntity.newOrganization(orgName);

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, organization.getId()).path("YettiId").delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a label with ID YettiId.");
  }

  @Test
  public void testDeleteOrgLabel_OwnerIdMismatch() throws Exception {
    String orgName1 = "LabelResourceTestOrgName1";
    Organization organization1 = tempEntity.newOrganization(orgName1);
    String orgName2 = "LabelResourceTestOrgName2";
    Organization organization2 = tempEntity.newOrganization(orgName2);

    Label label = tempEntity.newLabel(organization1.getId(), "MyLabel", Color.dark_blue);

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, organization2.getId()).path(label.getId()).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a label with ID " + label.getId() + " for organization ID " + organization2.getId());
    // Verify that the label was not deleted
    response = restRequest(OwnerType.ORGANIZATION, organization1.getId()).get();
    assertResponseStatus(200, response);
    ApiLabelDTO[] labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).hasSize(1);
    assertLabel(organization1.getId(), OwnerType.ORGANIZATION, "MyLabel", null, Color.dark_blue, labels[0]);
  }

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
    assertThat(applicableLabels).isNotNull();
    // One for the application, one for it's org and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(3);
    assertLabelsByOwner(app, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(org, 0, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(2));

    // Verify the applicable labels for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    // One for the org and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(2);
    assertLabelsByOwner(org, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(1));

    // Verify the applicable labels for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    // One for the repository, one for the repositoryContainer, and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(3);
    assertLabelsByOwner(repository, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(RepositoryContainer.SINGLETON, 0, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(2));

    // Create a label for the application
    Label appLabel = tempEntity.newLabel(app.getId(), "testGetApplicableLabels_App_label");

    // Verify the applicable labels for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    // One for the application, one for it's org and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(3);
    assertLabelsByOwner(app, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(org, 0, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(2));
    assertThat(applicableLabels.labelsByOwner.get(0).labels.get(0).id).isEqualTo(appLabel.getId());

    // Verify the applicable labels for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    // One the org and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(2);
    assertLabelsByOwner(org, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(1));

    // Create a label for the organization
    Label orgLabel = tempEntity.newLabel(orgId, "testGetApplicableLabels_Org_label");

    // Verify the applicable labels for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    assertThat(applicableLabels.labelsByOwner).hasSize(3);
    assertLabelsByOwner(app, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(org, 1, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(2));
    assertThat(applicableLabels.labelsByOwner.get(0).labels.get(0).id).isEqualTo(appLabel.getId());
    assertThat(applicableLabels.labelsByOwner.get(1).labels.get(0).id).isEqualTo(orgLabel.getId());

    // Verify the applicable labels for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    // One the org and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(2);
    assertLabelsByOwner(org, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(1));
    assertThat(applicableLabels.labelsByOwner.get(0).labels.get(0).id).isEqualTo(orgLabel.getId());

    // Create a label for the parent organization
    Label rootOrgLabel = tempEntity.newLabel(parentOrg.getId(), "testGetApplicableLabels_ParentOrg_label");

    // Verify the applicable labels for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    assertThat(applicableLabels.labelsByOwner).hasSize(3);
    assertLabelsByOwner(app, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(org, 1, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 1, applicableLabels.labelsByOwner.get(2));
    assertThat(applicableLabels.labelsByOwner.get(0).labels.get(0).id).isEqualTo(appLabel.getId());
    assertThat(applicableLabels.labelsByOwner.get(1).labels.get(0).id).isEqualTo(orgLabel.getId());
    assertThat(applicableLabels.labelsByOwner.get(2).labels.get(0).id).isEqualTo(rootOrgLabel.getId());

    // Verify the applicable labels for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    // One for the org and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(2);
    assertLabelsByOwner(org, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(parentOrg, 1, applicableLabels.labelsByOwner.get(1));
    assertThat(applicableLabels.labelsByOwner.get(0).labels.get(0).id).isEqualTo(orgLabel.getId());
    assertThat(applicableLabels.labelsByOwner.get(1).labels.get(0).id).isEqualTo(rootOrgLabel.getId());

    // Verify the applicable labels for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId).path("applicable").get();
    assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    // One for the repository, one for the repositoryContainer, and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(3);
    assertLabelsByOwner(repository, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(RepositoryContainer.SINGLETON, 0, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 1, applicableLabels.labelsByOwner.get(2));
    assertThat(applicableLabels.labelsByOwner.get(2).labels.get(0).id).isEqualTo(rootOrgLabel.getId());
  }

  @Test
  public void testGetApplicableContexts() throws Exception {
    Organization org = tempEntity.newOrganization("orgName");
    Application app = tempEntity.newApplication("appName", "appPublicId", org.getId());
    Label parentOrgLabel = tempEntity.newLabel(org.getParentOrganizationId(), "rootOrgLabel");
    Label orgLabel = tempEntity.newLabel(org.getId(), "orgLabel");
    Label appLabel = tempEntity.newLabel(app.getId(), "appLabel");
    HttpRequest request = restRequest(OwnerType.APPLICATION, app.getPublicId()).subpath("applicable/context/{labelId}");

    final Repository repository = tempEntity.newRepository();

    HttpResponse response = request.parameter(appLabel.getId()).get();
    assertResponseStatus(200, response);
    ApplicableContext context = response.getBody(ApplicableContext.class);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(app.getPublicId());
    assertThat(context.getName()).isEqualTo(app.getName());
    assertThat(context.getType()).isEqualTo(OwnerType.APPLICATION);
    assertThat(context.getChildren()).isNull();

    final HttpRequest requestRepository = restRequest(OwnerType.REPOSITORY, repository.getId()).subpath(
        "applicable/context/{labelId}");
    response = requestRepository.parameter(appLabel.getId()).get();
    assertResponseStatus(200, response);
    context = response.getBody(ApplicableContext.class);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(org.getParentOrganizationId());
    assertThat(context.getName()).isEqualTo("Root Organization");
    assertThat(context.getType()).isEqualTo(OwnerType.ORGANIZATION);
    assertThat(context.getChildren()).hasSize(1);
    context = context.getChildren().get(0);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(context.getName()).isEqualTo(RepositoryContainer.SINGLETON.getName());
    assertThat(context.getType()).isEqualTo(OwnerType.REPOSITORY_CONTAINER);
    assertThat(context.getChildren()).hasSize(1);
    context = context.getChildren().get(0);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(repository.getId());
    assertThat(context.getName()).isEqualTo(repository.getName());
    assertThat(context.getType()).isEqualTo(repository.getType());
    assertThat(context.getChildren()).isNull();

    response = request.parameter(orgLabel.getId()).get();
    assertResponseStatus(200, response);
    context = response.getBody(ApplicableContext.class);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(org.getId());
    assertThat(context.getName()).isEqualTo(org.getName());
    assertThat(context.getType()).isEqualTo(OwnerType.ORGANIZATION);
    assertThat(context.getChildren()).hasSize(1);
    context = context.getChildren().get(0);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(app.getPublicId());
    assertThat(context.getName()).isEqualTo(app.getName());
    assertThat(context.getType()).isEqualTo(OwnerType.APPLICATION);
    assertThat(context.getChildren()).isNull();

    response = request.parameter(parentOrgLabel.getId()).get();
    assertResponseStatus(200, response);
    context = response.getBody(ApplicableContext.class);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(org.getParentOrganizationId());
    assertThat(context.getName()).isEqualTo("Root Organization");
    assertThat(context.getType()).isEqualTo(OwnerType.ORGANIZATION);
    assertThat(context.getChildren()).hasSize(1);
    context = context.getChildren().get(0);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(org.getId());
    assertThat(context.getName()).isEqualTo(org.getName());
    assertThat(context.getType()).isEqualTo(OwnerType.ORGANIZATION);
    assertThat(context.getChildren()).hasSize(1);
    context = context.getChildren().get(0);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(app.getPublicId());
    assertThat(context.getName()).isEqualTo(app.getName());
    assertThat(context.getType()).isEqualTo(OwnerType.APPLICATION);
    assertThat(context.getChildren()).isNull();

    // Test that a user with application WRITE permissions can only see contexts for which they have WRITE permissions
    // (i.e. the application context).
    User applicationUser = tempEntity.newUser();
    Role writeRole = tempEntity.newRole(false /* global */, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), writeRole.getId(), applicationUser.getUsername());

    response = request.parameter(orgLabel.getId()).auth(applicationUser).get();
    assertResponseStatus(200, response);
    context = response.getBody(ApplicableContext.class);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(app.getPublicId());
    assertThat(context.getName()).isEqualTo(app.getName());
    assertThat(context.getType()).isEqualTo(OwnerType.APPLICATION);
    assertThat(context.getChildren()).isNull();
  }

  @Test
  public void testUpdateLabel_DifferentOwnerId() throws Exception {
    Organization ownerOrg = tempEntity.newOrganization();
    Label label = tempEntity.newLabel(ownerOrg.getId());

    Organization otherOrg = tempEntity.newOrganization();

    ApiLabelDTO apiLabelDTO = LabelService.toDTO(label, OwnerType.ORGANIZATION);
    apiLabelDTO.ownerId = otherOrg.getId();

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, otherOrg.getId()).body(apiLabelDTO).put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a label with id " + label.getId() + " for owner id " + otherOrg.getId());
  }

  private void assertLabel(
      String ownerId,
      OwnerType ownerType,
      String label,
      String description,
      Color color,
      ApiLabelDTO labelDTO)
  {
    assertThat(labelDTO.ownerId).isEqualTo(ownerId);
    assertThat(labelDTO.ownerType).isEqualTo(ownerType.name());
    assertThat(labelDTO.label).isEqualTo(label);
    assertThat(labelDTO.description).isEqualTo(description);
    assertThat(labelDTO.color).isEqualTo(color.toValue());
  }

  private void assertLabelsByOwner(Owner owner, int labelsCount, LabelsByOwner actual) {
    assertThat(actual.ownerId).isEqualTo(owner.getId());
    assertThat(actual.ownerName).isEqualTo(owner.getName());
    assertThat(actual.ownerType).isEqualTo(owner.getType());
    assertThat(actual.labels).hasSize(labelsCount);
  }
}
