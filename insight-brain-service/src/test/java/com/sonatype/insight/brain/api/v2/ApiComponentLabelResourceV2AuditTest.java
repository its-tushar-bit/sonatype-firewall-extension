/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class ApiComponentLabelResourceV2AuditTest
    extends AbstractAuditTest
{
  private static final String COMPONENT_HASH = "componentHash";

  private Application application;

  private Label label;

  @Before
  public void before() {
    application = tempEntity.newApplicationWithParent();
    label = tempEntity.newLabel(application.getId());
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.APP_COMPONENT_LABELS_PATH_V2);
  }

  @Test
  public void testAssignComponentLabelToApplication() throws Exception {
    restRequest().parameter(COMPONENT_HASH, label.getLabel(), application.getId()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertComponentLabelData(auditDTO, label);
  }

  @Test
  public void testAssignComponentLabelToApplication_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).parameter(COMPONENT_HASH, label.getLabel(), application.getId()).post();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testRemoveComponentLabelFromApplication() throws Exception {
    tempEntity.newComponentLabel(application.getId(), label.getId(), COMPONENT_HASH);
    restRequest().parameter(COMPONENT_HASH, label.getLabel(), application.getId()).delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertComponentLabelData(auditDTO, label);
  }

  @Test
  public void testRemoveComponentLabelFromApplication_Unauthorized() throws Exception {
    tempEntity.newComponentLabel(application.getId(), label.getId(), COMPONENT_HASH);
    restRequest().with(unauthorizedUser()).parameter(COMPONENT_HASH, label.getLabel(), application.getId()).delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private AuditDTO assertAuditLog(final AuditEvent auditEvent, final String error) {
    final AuditDTO auditDTO = awaitLogEntries(auditEvent, 1).get(0);
    assertStandardData(auditDTO, auditEvent, error);
    return auditDTO;
  }

  private void assertComponentLabelData(final AuditDTO auditDTO, final Label label) {
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
    assertCustomData(auditDTO, "labelId", label.getId());
    assertCustomData(auditDTO, "labelName", label.getLabel());
  }
}
