/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class ComponentLabelResourceAuditTest
    extends AbstractAuditTest
{
  private static final String COMPONENT_HASH = "bababababa";

  private Label label;

  private Label labelWithIdOnly;

  private HttpRequest restRequest(final Owner owner, final String hash) {
    return restRequest().path(ComponentLabelResource.RESOURCE_PATH)
        .parameter(owner.getType(), owner.getType().equals(OwnerType.APPLICATION) ? owner.getPublicId() : owner.getId(),
            hash);
  }

  private HttpRequest restRequest(Owner owner, String hash, String labelId) {
    return restRequest(owner, hash).path(labelId);
  }

  private void assertComponentLabelData(final AuditDTO auditDTO) {
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
    assertCustomData(auditDTO, "labelId", label.getId());
    assertCustomData(auditDTO, "labelName", label.getLabel());
  }

  @Before
  public void init() {
    label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    labelWithIdOnly = new Label();
    labelWithIdOnly.setId(label.getId());
  }

  @Test
  public void testSetComponentLabel_AppLevel() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    restRequest(application, COMPONENT_HASH).body(labelWithIdOnly).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertComponentLabelData(auditDTO);
  }

  @Test
  public void testSetComponentLabel_OrgLevel() throws Exception {
    Organization organization = tempEntity.newOrganization();
    restRequest(organization, COMPONENT_HASH).body(labelWithIdOnly).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertComponentLabelData(auditDTO);
  }

  @Test
  public void testSetComponentLabel_RepoLevel() throws Exception {
    Repository repository = tempEntity.newRepository();
    restRequest(repository, COMPONENT_HASH).body(labelWithIdOnly).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, null);
    assertRepositoryData(auditDTO, repository);
    assertComponentLabelData(auditDTO);
  }

  @Test
  public void testSetComponentLabel_RepoContainerLevel() throws Exception {
    restRequest(RepositoryContainer.SINGLETON, COMPONENT_HASH).body(labelWithIdOnly).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, null);
    assertRepositoryContainerData(auditDTO);
    assertComponentLabelData(auditDTO);
  }

  @Test
  public void testSetComponentLabel_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    restRequest(application, COMPONENT_HASH).body(labelWithIdOnly).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testDeleteComponentLabel_AppLevel() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newComponentLabel(application.getId(), label.getId(), COMPONENT_HASH);
    restRequest(application, COMPONENT_HASH, label.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertComponentLabelData(auditDTO);
  }

  @Test
  public void testDeleteComponentLabel_OrgLevel() throws Exception {
    Organization organization = tempEntity.newOrganization();
    tempEntity.newComponentLabel(organization.getId(), label.getId(), COMPONENT_HASH);
    restRequest(organization, COMPONENT_HASH, label.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertComponentLabelData(auditDTO);
  }

  @Test
  public void testDeleteComponentLabel_RepoLevel() throws Exception {
    Repository repository = tempEntity.newRepository();
    tempEntity.newComponentLabel(repository.getId(), label.getId(), COMPONENT_HASH);
    restRequest(repository, COMPONENT_HASH, label.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, null);
    assertRepositoryData(auditDTO, repository);
    assertComponentLabelData(auditDTO);
  }

  @Test
  public void testDeleteComponentLabel_RepoContainerLevel() throws Exception {
    tempEntity.newComponentLabel(RepositoryContainer.REPOSITORY_CONTAINER_ID, label.getId(), COMPONENT_HASH);
    restRequest(RepositoryContainer.SINGLETON, COMPONENT_HASH, label.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, null);
    assertRepositoryContainerData(auditDTO);
    assertComponentLabelData(auditDTO);
  }

  @Test
  public void testDeleteComponentLabel_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newComponentLabel(application.getId(), label.getId(), COMPONENT_HASH);
    restRequest(application, COMPONENT_HASH, label.getId()).with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testGetComponentLabels_Application() throws Exception {
    final Application application = tempEntity.newApplicationWithParent();
    restRequest(application, COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  public void testGetComponentLabels_Organization() throws Exception {
    final Organization organization = tempEntity.newOrganization();
    restRequest(organization, COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  public void testGetComponentLabels_Repository() throws Exception {
    final Repository repository = tempEntity.newRepository();
    restRequest(repository, COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  public void testGetComponentLabels_RepositoryContainer() throws Exception {
    restRequest(RepositoryContainer.SINGLETON, COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertRepositoryContainerData(auditDTO);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  public void testGetComponentLabels_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    restRequest(application, COMPONENT_HASH).with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }
}
