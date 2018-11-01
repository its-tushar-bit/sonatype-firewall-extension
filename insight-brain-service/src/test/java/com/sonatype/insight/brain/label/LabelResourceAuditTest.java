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
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class LabelResourceAuditTest
    extends AbstractAuditTest
{
  private static final String LABEL_NAME = "babababa";

  private static final String LABEL_DESCRIPTION = "dadadada";

  private static final Color LABEL_COLOR = Color.dark_blue;

  private static final String NEW_LABEL_NAME = "hohohoho";

  private static final String NEW_LABEL_DESCRIPTION = "hehehehe";

  private static final Color NEW_LABEL_COLOR = Color.dark_red;

  private Application application;

  private Organization organization;

  @Before
  public void init() {
    application = tempEntity.newApplicationWithParent();
    organization = tempEntity.newOrganization();
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId, Label label) {
    return restRequest().path(LabelResource.RESOURCE_PATH).parameter(ownerType, ownerId).body(label);
  }

  private void assertLabelData(final AuditDTO auditDTO, final Label label) {
    assertCustomData(auditDTO, "labelId", label.getId());
    assertCustomData(auditDTO, "labelName", label.getLabel());
    assertCustomData(auditDTO, "labelDescription", label.getDescription());
    assertCustomData(auditDTO, "labelColor", label.getColor().toValue());
  }

  @Test
  public void testCreateLabel_AppLevel() throws Exception {
    final Label label = new Label(application.getId(), LABEL_NAME, LABEL_DESCRIPTION, LABEL_COLOR);
    final Label addedLabel = restRequest(OwnerType.APPLICATION, application.getPublicId(), label).post()
        .getBody(Label.class);

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertLabelData(auditDTO, addedLabel);
  }

  @Test
  public void testCreateLabel_OrgLevel() throws Exception {
    final Label label = new Label(organization.getId(), LABEL_NAME, LABEL_DESCRIPTION, LABEL_COLOR);
    final Label addedLabel = restRequest(OwnerType.ORGANIZATION, organization.getPublicId(), label).post()
        .getBody(Label.class);

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertLabelData(auditDTO, addedLabel);
  }

  @Test
  public void testCreateLabel_RepoLevel() throws Exception {
    final Repository repository = tempEntity.newRepository();
    final Label label = new Label(repository.getId(), LABEL_NAME, LABEL_DESCRIPTION, Color.dark_blue);
    restRequest(OwnerType.REPOSITORY, repository.getId(), label).post();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LABEL, "not-found");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testCreateLabel_Unauthorized() throws Exception {
    final Label label = new Label(application.getId(), LABEL_NAME, LABEL_DESCRIPTION, LABEL_COLOR);
    restRequest(OwnerType.APPLICATION, application.getPublicId(), label).with(unauthorizedUser()).post();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testUpdateLabel_AppLevel() throws Exception {
    final Label label = tempEntity.newLabel(application.getId(), LABEL_NAME, LABEL_DESCRIPTION, LABEL_COLOR);
    label.setLabel(NEW_LABEL_NAME);
    label.setDescription(NEW_LABEL_DESCRIPTION);
    label.setColor(NEW_LABEL_COLOR);
    restRequest(OwnerType.APPLICATION, application.getPublicId(), label).put();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertLabelData(auditDTO, label);
  }

  @Test
  public void testUpdateLabel_OrgLevel() throws Exception {
    final Label label = tempEntity.newLabel(organization.getId(), LABEL_NAME, LABEL_DESCRIPTION, LABEL_COLOR);
    label.setLabel(NEW_LABEL_NAME);
    label.setDescription(NEW_LABEL_DESCRIPTION);
    label.setColor(NEW_LABEL_COLOR);
    restRequest(OwnerType.ORGANIZATION, organization.getPublicId(), label).put();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertLabelData(auditDTO, label);
  }

  @Test
  public void testDeleteLabel_AppLevel() throws Exception {
    Label toBeDeleted = tempEntity.newLabel(application.getId(), LABEL_NAME, LABEL_DESCRIPTION, LABEL_COLOR);

    restRequest().path(LabelResource.RESOURCE_PATH).parameter(OwnerType.APPLICATION, application.getPublicId())
        .subpath(toBeDeleted.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertLabelData(auditDTO, toBeDeleted);
  }

  @Test
  public void testDeleteLabel_OrgLevel() throws Exception {
    Label toBeDeleted = tempEntity.newLabel(organization.getId());

    restRequest().path(LabelResource.RESOURCE_PATH).parameter(OwnerType.ORGANIZATION, organization.getPublicId())
        .subpath(toBeDeleted.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertLabelData(auditDTO, toBeDeleted);
  }

  @Test
  public void testDeleteLabel_Unauthorized() throws Exception {
    restRequest().path(LabelResource.RESOURCE_PATH).parameter(OwnerType.APPLICATION, application.getPublicId())
        .path("labelId").with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testUpdateLabel_Unauthorized() throws Exception {
    final Label label = tempEntity.newLabel(application.getId(), LABEL_NAME, LABEL_DESCRIPTION, LABEL_COLOR);
    restRequest(OwnerType.APPLICATION, application.getPublicId(), label).with(unauthorizedUser()).put();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }
}
