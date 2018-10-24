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

  private Application application;

  @Before
  public void init() {
    application = tempEntity.newApplicationWithParent();
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId, Label label) {
    return restRequest().path(LabelResource.RESOURCE_PATH).parameter(ownerType, ownerId).body(label);
  }

  private AuditDTO assertAuditLog(final AuditEvent auditEvent, final String error) {
    final AuditDTO auditDTO = awaitLogEntries(auditEvent, 1).get(0);
    assertStandardData(auditDTO, auditEvent, error);
    return auditDTO;
  }

  private void assertLabelData(final AuditDTO auditDTO, final Label label) {
    assertCustomData(auditDTO, "labelId", label.getId());
    assertCustomData(auditDTO, "labelName", label.getLabel());
    assertCustomData(auditDTO, "labelDescription", label.getDescription());
    assertCustomData(auditDTO, "labelColor", label.getColor().toValue());
  }

  @Test
  public void testCreateLabel_AppLevel() throws Exception {
    final Label label = new Label(application.getId(), LABEL_NAME, LABEL_DESCRIPTION, Color.dark_blue);
    final Label addedLabel = restRequest(OwnerType.APPLICATION, application.getPublicId(), label).post()
        .getBody(Label.class);

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertLabelData(auditDTO, addedLabel);
  }

  @Test
  public void testCreateLabel_OrgLevel() throws Exception {
    final Organization organization = tempEntity.newOrganization();
    final Label label = new Label(organization.getId(), LABEL_NAME, LABEL_DESCRIPTION, Color.dark_blue);
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
    final Label label = new Label(application.getId(), LABEL_NAME, LABEL_DESCRIPTION, Color.dark_blue);
    restRequest(OwnerType.APPLICATION, application.getPublicId(), label)
        .auth(unauthorizedUser.getUsername(), unauthorizedUser.getPassword()).post();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }
}
