/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.label.ComponentLabelService.AppliedLabels;
import com.sonatype.insight.brain.label.ComponentLabelService.LabelsByOwner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentLabelResourceTest
    extends AbstractResourceTest
{
  private final String componentHash = "bababababa";

  private ComponentLabelDAO componentLabelDAO;

  private Organization org;

  private Application app;

  private Organization rootOrg;

  private Repository repository;

  private Label appLabel;

  private Label orgLabel;

  private Label rootOrgLabel;

  private HttpRequest restRequest(OwnerType ownerType, String ownerId, String hash) {
    return restRequest().path(ComponentLabelResource.RESOURCE_PATH).parameter(ownerType, ownerId, hash);
  }

  @Before
  public void init() {
    componentLabelDAO = lookup(ComponentLabelDAO.class);

    org = tempEntity.newOrganization();
    rootOrg = lookup(OrganizationDAO.class).getById(Organization.ROOT_ORGANIZATION_ID);
    app = tempEntity.newApplication("Test", "test-app", org.getId());
    repository = tempEntity.newRepository();
    appLabel = tempEntity.newLabel(app.getId(), "app");
    orgLabel = tempEntity.newLabel(org.getId(), "org");
    rootOrgLabel = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "rootOrg");
  }

  private void assertLabelsByOwner(LabelsByOwner labelsByOwner, Owner owner, String labelId) {
    assertThat(labelsByOwner).isNotNull();
    assertThat(labelsByOwner.ownerId).isEqualTo(owner.getPublicId());
    assertThat(labelsByOwner.ownerName).isEqualTo(owner.getName());
    assertThat(labelsByOwner.ownerType).isEqualTo(owner.getType());
    assertThat(labelsByOwner.labels).extracting(Label::getId).containsExactly(labelId);
  }

  @Test
  public void testGetComponentLabels() throws Exception {
    // No labels applied to componentHash
    // Verify app level
    HttpResponse response = restRequest(OwnerType.APPLICATION, app.getPublicId(), componentHash).get();
    assertResponseStatus(200, response);
    AppliedLabels componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner).isEmpty();
    // Verify org level
    response = restRequest(OwnerType.ORGANIZATION, org.getId(), componentHash).get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner).isEmpty();
    // Verify parent org level
    response = restRequest(OwnerType.ORGANIZATION, org.getParentOrganizationId(), componentHash).get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner).isEmpty();
    // Verify repository manager level
    response = restRequest(OwnerType.REPOSITORY_MANAGER, repository.getRepositoryManagerId(), componentHash).get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner).isEmpty();
    // Verify repository level
    response = restRequest(OwnerType.REPOSITORY, repository.getId(), componentHash).get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner).isEmpty();
    // Verify repository container level
    response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, componentHash)
        .get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner).isEmpty();

    // Labels applied to componentHash at all levels
    tempEntity.newComponentLabel(app.getId(), appLabel.getId(), componentHash);
    tempEntity.newComponentLabel(org.getId(), orgLabel.getId(), componentHash);
    tempEntity.newComponentLabel(Organization.ROOT_ORGANIZATION_ID, rootOrgLabel.getId(), componentHash);

    // Verify app level
    response = restRequest(OwnerType.APPLICATION, app.getPublicId(), componentHash).get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner).hasSize(3);
    assertLabelsByOwner(componentLabels.labelsByOwner.get(0), app, appLabel.getId());
    assertLabelsByOwner(componentLabels.labelsByOwner.get(1), org, orgLabel.getId());
    assertLabelsByOwner(componentLabels.labelsByOwner.get(2), rootOrg, rootOrgLabel.getId());
    // Verify org level
    response = restRequest(OwnerType.ORGANIZATION, org.getId(), componentHash).get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner).hasSize(2);
    assertLabelsByOwner(componentLabels.labelsByOwner.get(0), org, orgLabel.getId());
    assertLabelsByOwner(componentLabels.labelsByOwner.get(1), rootOrg, rootOrgLabel.getId());
    // Verify parent org level
    response = restRequest(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, componentHash).get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner).hasSize(1);
    assertLabelsByOwner(componentLabels.labelsByOwner.get(0), rootOrg, rootOrgLabel.getId());
    // Verify repository level
    // NOTE: Currently, only RootOrg labels are possible for a Repository.
    response = restRequest(OwnerType.REPOSITORY, repository.getId(), componentHash).get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner).hasSize(1);
    assertLabelsByOwner(componentLabels.labelsByOwner.get(0), rootOrg, rootOrgLabel.getId());
    // Verify repository container level
    // NOTE: Currently, only RootOrg labels are possible for a Repository container.
    response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, componentHash)
        .get();
    assertResponseStatus(200, response);
    componentLabels = response.getBody(AppliedLabels.class);
    assertThat(componentLabels.labelsByOwner).hasSize(1);
    assertLabelsByOwner(componentLabels.labelsByOwner.get(0), rootOrg, rootOrgLabel.getId());
  }

  @Test
  public void testSetComponentLabel_AppLevel() throws Exception {
    // we post a rest request with the public id but verify using app.getId()
    setComponentLabelAndVerify(OwnerType.APPLICATION, app.getPublicId(), appLabel, app.getId());
    List<ComponentLabel> componentLabels = componentLabelDAO
        .getByOwnerIdAndHashWithHierarchy(app.getOrganizationId(), componentHash);
    assertThat(componentLabels).isEmpty();
  }

  @Test
  public void testSetComponentLabel_OrgLevel() throws Exception {
    setComponentLabelAndVerify(OwnerType.ORGANIZATION, app.getOrganizationId(), orgLabel);
  }

  @Test
  public void testSetComponentLabel_RepositoryLevel() throws Exception {
    setComponentLabelAndVerify(OwnerType.REPOSITORY, repository.getId(), rootOrgLabel);
  }

  @Test
  public void testSetComponentLabel_RepositoryManagerLevel() throws Exception {
    setComponentLabelAndVerify(OwnerType.REPOSITORY_MANAGER, repository.getRepositoryManagerId(), rootOrgLabel);
  }

  @Test
  public void testSetComponentLabel_RepositoryContainerLevel() throws Exception {
    setComponentLabelAndVerify(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        rootOrgLabel);
  }

  @Test
  public void testDeleteComponentLabel_AppLevel() throws Exception {
    ComponentLabel componentLabel = tempEntity.newComponentLabel(app.getId(), appLabel.getId(), componentHash);
    deleteComponentLabelAndVerify(OwnerType.APPLICATION, app.getPublicId(), componentLabel);
  }

  @Test
  public void testDeleteComponentLabel_OrgLevel() throws Exception {
    ComponentLabel componentLabel = tempEntity.newComponentLabel(app.getOrganizationId(), orgLabel.getId(),
        componentHash);
    deleteComponentLabelAndVerify(OwnerType.ORGANIZATION, app.getOrganizationId(), componentLabel);
  }

  @Test
  public void testDeleteComponentLabel_RepositoryLevel() throws Exception {
    ComponentLabel componentLabel = tempEntity.newComponentLabel(repository.getId(), rootOrgLabel.getId(),
        componentHash);
    deleteComponentLabelAndVerify(OwnerType.REPOSITORY, repository.getId(), componentLabel);
  }

  @Test
  public void testDeleteComponentLabel_RepositoryManagerLevel() throws Exception {
    ComponentLabel componentLabel =
        tempEntity.newComponentLabel(repository.getRepositoryManagerId(), rootOrgLabel.getId(), componentHash);
    deleteComponentLabelAndVerify(OwnerType.REPOSITORY_MANAGER, repository.getRepositoryManagerId(), componentLabel);
  }

  @Test
  public void testDeleteComponentLabel_RepositoryContainerLevel() throws Exception {
    ComponentLabel componentLabel = tempEntity.newComponentLabel(RepositoryContainer.REPOSITORY_CONTAINER_ID,
        rootOrgLabel.getId(), componentHash);
    deleteComponentLabelAndVerify(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
        componentLabel);
  }

  /**
   * used when the rest request owner id and the label's owner id to verify are the same
   **/
  private void setComponentLabelAndVerify(
      OwnerType ownerType,
      String requestOwnerId,
      Label labelToAdd) throws Exception
  {
    setComponentLabelAndVerify(ownerType, requestOwnerId, labelToAdd, requestOwnerId);
  }

  private void setComponentLabelAndVerify(
      OwnerType ownerType,
      String requestOwnerId,
      Label labelToAdd,
      String ownerIdToVerify) throws Exception
  {
    HttpResponse response = restRequest(ownerType, requestOwnerId, componentHash).body(labelToAdd).post();
    assertResponseStatus(204, response);

    List<ComponentLabel> componentLabels =
        componentLabelDAO.getByOwnerIdAndHashWithHierarchy(ownerIdToVerify, componentHash);
    assertThat(componentLabels).isNotNull();
    assertThat(componentLabels).hasSize(1);
    assertThat(componentLabels.get(0).getLabelId()).isEqualTo(labelToAdd.getId());
  }

  private void deleteComponentLabelAndVerify(
      OwnerType ownerType,
      String requestOwnerId,
      ComponentLabel componentLabel) throws Exception
  {
    String labelId = componentLabel.getLabelId();
    String componentHash = componentLabel.getHash();

    HttpResponse response = restRequest(ownerType, requestOwnerId, componentHash).path(labelId).delete();
    assertResponseStatus(204, response);

    assertThat(componentLabelDAO.getById(componentLabel.getId())).isNull();

    response = restRequest(ownerType, requestOwnerId, componentHash).path(labelId).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find the label with ID " + labelId + " for "
        + ownerType.toString() + " ID " + requestOwnerId + " on the component " + componentHash + ".");
  }
}
