/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLabelDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.label.ApplicableLabels;
import com.sonatype.insight.brain.label.LabelsByOwner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiLabelResourceTest
{
  private IqTestContext ctx;

  private RepositoryManagerDAO repositoryManagerDAO;

  private OrganizationDAO organizationDAO;

  @BeforeEach
  void setUp() {
    repositoryManagerDAO = ctx.lookup(RepositoryManagerDAO.class);
    organizationDAO = ctx.lookup(OrganizationDAO.class);
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return ctx.restRequest().path(PublicApiPaths.LABEL_RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  @Test
  void testApplicationCRUD() throws Exception {
    // Create an application
    String appPublicId = "LabelResourceTest_AppId";
    Application application = ctx.tempEntity().newApplicationWithParent(appPublicId);
    HttpRequest request = restRequest(OwnerType.APPLICATION, appPublicId);

    // Get all labels
    HttpResponse response = request.get();
    ctx.assertResponseStatus(200, response);
    ApiLabelDTO[] labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).isEmpty();

    // Add a label
    ApiLabelDTO labelDTO = new ApiLabelDTO("MyLabel", "My Description", "light-green");
    response = request.body(labelDTO).post();
    ctx.assertResponseStatus(200, response);
    labelDTO = response.getBody(ApiLabelDTO.class);

    assertLabel(application.getId(), OwnerType.APPLICATION, "MyLabel", "My Description", Color.light_green, labelDTO);

    // Get all labels
    response = request.get();
    ctx.assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).hasSize(1);
    assertLabel(application.getId(), OwnerType.APPLICATION, "MyLabel", "My Description", Color.light_green, labels[0]);

    // Update a label
    labelDTO.label = "MyUpdatedLabel";
    labelDTO.description = "Description updated";
    labelDTO.color = "light-green";
    response = request.body(labelDTO).put();
    ctx.assertResponseStatus(200, response);
    labelDTO = response.getBody(ApiLabelDTO.class);

    assertLabel(application.getId(), OwnerType.APPLICATION, "MyUpdatedLabel", "Description updated", Color.light_green,
        labelDTO);

    // Get all labels
    response = request.get();
    ctx.assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).hasSize(1);
    assertLabel(application.getId(), OwnerType.APPLICATION, "MyUpdatedLabel", "Description updated", Color.light_green,
        labels[0]);

    // Delete a label
    response = request.subpath(labelDTO.id).delete();
    ctx.assertResponseStatus(204, response);

    // Get all labels
    response = request.get();
    ctx.assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).isEmpty();
  }

  @Test
  void testOrganizationCRUD() throws Exception {
    // Create an organization
    String orgName = "LabelResourceTestOrgName";
    Organization organization = ctx.tempEntity().newOrganization(orgName);
    HttpRequest request = restRequest(OwnerType.ORGANIZATION, organization.getId());

    // Get all labels
    HttpResponse response = request.get();
    ctx.assertResponseStatus(200, response);
    ApiLabelDTO[] labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).isEmpty();

    // Add a label
    ApiLabelDTO labelDTO = new ApiLabelDTO("MyLabel", "My Description", "light-green");

    response = request.body(labelDTO).post();
    ctx.assertResponseStatus(200, response);
    labelDTO = response.getBody(ApiLabelDTO.class);

    assertLabel(organization.getId(), OwnerType.ORGANIZATION, "MyLabel", "My Description", Color.light_green, labelDTO);

    // Get all labels
    response = request.get();
    ctx.assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).hasSize(1);
    assertLabel(organization.getId(), OwnerType.ORGANIZATION, "MyLabel", "My Description", Color.light_green,
        labels[0]);

    // Update a label
    labelDTO.label = "MyUpdatedLabel";
    labelDTO.description = "Description Update";
    labelDTO.color = "light-green";
    response = request.body(labelDTO).put();
    ctx.assertResponseStatus(200, response);
    labelDTO = response.getBody(ApiLabelDTO.class);

    assertLabel(organization.getId(), OwnerType.ORGANIZATION, "MyUpdatedLabel", "Description Update", Color.light_green,
        labelDTO);

    // Get all labels
    response = request.get();
    ctx.assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).hasSize(1);
    assertLabel(organization.getId(), OwnerType.ORGANIZATION, "MyUpdatedLabel", "Description Update", Color.light_green,
        labels[0]);

    // Delete a label
    response = request.subpath(labelDTO.id).delete();
    ctx.assertResponseStatus(204, response);

    // Get all labels
    response = request.get();
    ctx.assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).isEmpty();
  }

  @Test
  void testRepositoryContainerCRUD() throws Exception {
    HttpRequest request = restRequest(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID);

    // Get all labels
    HttpResponse response = request.get();
    ctx.assertResponseStatus(200, response);
    ApiLabelDTO[] labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).isEmpty();

    // Add a label
    ApiLabelDTO labelDTO = new ApiLabelDTO("MyLabel", "My Description", "light-green");

    response = request.body(labelDTO).post();
    ctx.assertResponseStatus(200, response);
    labelDTO = response.getBody(ApiLabelDTO.class);

    assertLabel(REPOSITORY_CONTAINER_ID, OwnerType.REPOSITORY_CONTAINER, "MyLabel", "My Description", Color.light_green,
        labelDTO);

    // Get all labels
    response = request.get();
    ctx.assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).hasSize(1);
    assertLabel(REPOSITORY_CONTAINER_ID, OwnerType.REPOSITORY_CONTAINER, "MyLabel", "My Description", Color.light_green,
        labels[0]);

    // Update a label
    labelDTO.label = "MyUpdatedLabel";
    labelDTO.description = "Description Update";
    labelDTO.color = "light-green";
    response = request.body(labelDTO).put();
    ctx.assertResponseStatus(200, response);
    labelDTO = response.getBody(ApiLabelDTO.class);

    assertLabel(REPOSITORY_CONTAINER_ID, OwnerType.REPOSITORY_CONTAINER, "MyUpdatedLabel", "Description Update",
        Color.light_green,
        labelDTO);

    // Get all labels
    response = request.get();
    ctx.assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).hasSize(1);
    assertLabel(REPOSITORY_CONTAINER_ID, OwnerType.REPOSITORY_CONTAINER, "MyUpdatedLabel", "Description Update",
        Color.light_green,
        labels[0]);

    // Delete a label
    response = request.subpath(labelDTO.id).delete();
    ctx.assertResponseStatus(204, response);

    // Get all labels
    response = request.get();
    ctx.assertResponseStatus(200, response);
    labels = response.getBody(ApiLabelDTO[].class);
    assertThat(labels).isEmpty();
  }

  @Test
  void testGetApplicableLabels() throws Exception {
    // Create an organization and an application
    Organization org = ctx.tempEntity().newOrganization("testGetApplicableLabelsOrg");
    String orgId = org.getId();
    String appPublicId = "testGetApplicableLabelsApp";
    Application app = ctx.tempEntity().newApplication(appPublicId, appPublicId, org.getId());
    Organization parentOrg = organizationDAO.getById(org.getParentOrganizationId());

    final Repository repository = ctx.tempEntity().newRepository();
    final String repoId = repository.getId();

    // Verify the applicable labels for the application
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    ctx.assertResponseStatus(200, response);
    ApplicableLabels applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    // One for the application, one for it's org and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(3);
    assertLabelsByOwner(app, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(org, 0, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(2));

    // Verify the applicable labels for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    ctx.assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    // One for the org and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(2);
    assertLabelsByOwner(org, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(1));

    // Verify the applicable labels for the repository manager
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
    response = restRequest(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId()).path("applicable").get();
    ctx.assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    // One for repo manager, one for the repositoryContainer, and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(3);
    assertLabelsByOwner(repositoryManager, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(RepositoryContainer.SINGLETON, 0, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(2));

    // Verify the applicable labels for the repository
    response = restRequest(OwnerType.REPOSITORY, repoId).path("applicable").get();
    ctx.assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    // One for the repository, one for repo manager, one for the repositoryContainer, and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(4);
    assertLabelsByOwner(repository, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(repositoryManager, 0, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(RepositoryContainer.SINGLETON, 0, applicableLabels.labelsByOwner.get(2));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(3));

    // Verify the applicable labels for the repository container
    response = restRequest(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID).path("applicable").get();
    ctx.assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    // One for the repositoryContainer, and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(2);
    assertLabelsByOwner(RepositoryContainer.SINGLETON, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(1));

    // Create a label for the application
    Label appLabel = ctx.tempEntity().newLabel(app.getId(), "testGetApplicableLabels_App_label");

    // Verify the applicable labels for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    ctx.assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    // One for the application, one for it's org and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(3);
    assertLabelsByOwner(app, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(org, 0, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(2));
    assertThat(applicableLabels.labelsByOwner.get(0).labels.get(0).id).isEqualTo(appLabel.getId());
    assertThat(applicableLabels.labelsByOwner.get(0).labels.get(0).ownerType).isEqualTo(OwnerType.APPLICATION.name());

    // Verify the applicable labels for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    ctx.assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    // One the org and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(2);
    assertLabelsByOwner(org, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(1));

    // Create a label for the organization
    Label orgLabel = ctx.tempEntity().newLabel(orgId, "testGetApplicableLabels_Org_label");

    // Verify the applicable labels for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    ctx.assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    assertThat(applicableLabels.labelsByOwner).hasSize(3);
    assertLabelsByOwner(app, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(org, 1, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(2));
    assertThat(applicableLabels.labelsByOwner.get(0).labels.get(0).id).isEqualTo(appLabel.getId());
    assertThat(applicableLabels.labelsByOwner.get(1).labels.get(0).id).isEqualTo(orgLabel.getId());
    assertThat(applicableLabels.labelsByOwner.get(1).labels.get(0).ownerType).isEqualTo(OwnerType.ORGANIZATION.name());

    // Verify the applicable labels for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    ctx.assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    // One the org and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(2);
    assertLabelsByOwner(org, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(parentOrg, 0, applicableLabels.labelsByOwner.get(1));
    assertThat(applicableLabels.labelsByOwner.get(0).labels.get(0).id).isEqualTo(orgLabel.getId());
    assertThat(applicableLabels.labelsByOwner.get(0).labels.get(0).ownerType).isEqualTo(OwnerType.ORGANIZATION.name());

    // Create a label for the parent organization
    Label rootOrgLabel = ctx.tempEntity().newLabel(parentOrg.getId(), "testGetApplicableLabels_ParentOrg_label");

    // Verify the applicable labels for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    ctx.assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    assertThat(applicableLabels.labelsByOwner).hasSize(3);
    assertLabelsByOwner(app, 1, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(org, 1, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(parentOrg, 1, applicableLabels.labelsByOwner.get(2));
    assertThat(applicableLabels.labelsByOwner.get(0).labels.get(0).id).isEqualTo(appLabel.getId());
    assertThat(applicableLabels.labelsByOwner.get(0).labels.get(0).ownerType).isEqualTo(OwnerType.APPLICATION.name());

    assertThat(applicableLabels.labelsByOwner.get(1).labels.get(0).id).isEqualTo(orgLabel.getId());
    assertThat(applicableLabels.labelsByOwner.get(1).labels.get(0).ownerType).isEqualTo(OwnerType.ORGANIZATION.name());

    assertThat(applicableLabels.labelsByOwner.get(2).labels.get(0).id).isEqualTo(rootOrgLabel.getId());
    assertThat(applicableLabels.labelsByOwner.get(2).labels.get(0).ownerType).isEqualTo(OwnerType.ORGANIZATION.name());

    // Verify the applicable labels for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    ctx.assertResponseStatus(200, response);
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
    ctx.assertResponseStatus(200, response);
    applicableLabels = response.getBody(ApplicableLabels.class);
    assertThat(applicableLabels).isNotNull();
    // One for the repository, one for the repo manager, one for the repositoryContainer, and one for the root org
    assertThat(applicableLabels.labelsByOwner).hasSize(4);
    assertLabelsByOwner(repository, 0, applicableLabels.labelsByOwner.get(0));
    assertLabelsByOwner(repositoryManager, 0, applicableLabels.labelsByOwner.get(1));
    assertLabelsByOwner(RepositoryContainer.SINGLETON, 0, applicableLabels.labelsByOwner.get(2));
    assertLabelsByOwner(parentOrg, 1, applicableLabels.labelsByOwner.get(3));
    assertThat(applicableLabels.labelsByOwner.get(3).labels.get(0).id).isEqualTo(rootOrgLabel.getId());
  }

  @Test
  void testGetApplicableContexts() throws Exception {
    Organization org = ctx.tempEntity().newOrganization("orgName");
    Application app = ctx.tempEntity().newApplication("appName", "appPublicId", org.getId());
    Label parentOrgLabel = ctx.tempEntity().newLabel(org.getParentOrganizationId(), "rootOrgLabel");
    Label orgLabel = ctx.tempEntity().newLabel(org.getId(), "orgLabel");
    Label appLabel = ctx.tempEntity().newLabel(app.getId(), "appLabel");
    HttpRequest request = restRequest(OwnerType.APPLICATION, app.getPublicId()).subpath("applicable/context/{labelId}");

    final Repository repository = ctx.tempEntity().newRepository();
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());

    HttpResponse response = request.parameter(appLabel.getId()).get();
    ctx.assertResponseStatus(200, response);
    ApplicableContext context = response.getBody(ApplicableContext.class);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(app.getPublicId());
    assertThat(context.getName()).isEqualTo(app.getName());
    assertThat(context.getType()).isEqualTo(OwnerType.APPLICATION);
    assertThat(context.getChildren()).isNull();

    final HttpRequest requestRepository = restRequest(OwnerType.REPOSITORY, repository.getId()).subpath(
        "applicable/context/{labelId}");
    response = requestRepository.parameter(appLabel.getId()).get();
    ctx.assertResponseStatus(200, response);
    context = response.getBody(ApplicableContext.class);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(org.getParentOrganizationId());
    assertThat(context.getName()).isEqualTo("Root Organization");
    assertThat(context.getType()).isEqualTo(OwnerType.ORGANIZATION);
    assertThat(context.getChildren()).hasSize(1);
    context = context.getChildren().get(0);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(REPOSITORY_CONTAINER_ID);
    assertThat(context.getName()).isEqualTo(RepositoryContainer.SINGLETON.getName());
    assertThat(context.getType()).isEqualTo(OwnerType.REPOSITORY_CONTAINER);
    assertThat(context.getChildren()).hasSize(1);
    context = context.getChildren().get(0);
    assertThat(context.getId()).isEqualTo(repositoryManager.getId());
    assertThat(context.getName()).isEqualTo(repositoryManager.getName());
    assertThat(context.getType()).isEqualTo(repositoryManager.getType());
    context = context.getChildren().get(0);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(repository.getId());
    assertThat(context.getName()).isEqualTo(repository.getName());
    assertThat(context.getType()).isEqualTo(repository.getType());
    assertThat(context.getChildren()).isNull();

    response = request.parameter(orgLabel.getId()).get();
    ctx.assertResponseStatus(200, response);
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
    ctx.assertResponseStatus(200, response);
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
    User applicationUser = ctx.tempEntity().newUser();
    Role writeRole = ctx.tempEntity().newRole(false /* global */, Permission.WRITE);
    ctx.tempEntity().newMembershipMapping(app.getId(), writeRole.getId(), applicationUser.getUsername());

    response = request.parameter(orgLabel.getId()).auth(applicationUser).get();
    ctx.assertResponseStatus(200, response);
    context = response.getBody(ApplicableContext.class);
    assertThat(context).isNotNull();
    assertThat(context.getId()).isEqualTo(app.getPublicId());
    assertThat(context.getName()).isEqualTo(app.getName());
    assertThat(context.getType()).isEqualTo(OwnerType.APPLICATION);
    assertThat(context.getChildren()).isNull();
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
