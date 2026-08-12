/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class ComponentDetailResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ComponentDetailResource.RESOURCE_PATH);
  }

  @Test
  public void testGetApplicationDetailsByHash() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String hash = "dcbafedcba";
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("org.apache.maven", "maven", "2.0");
    tempEntity.newApplicationComponent(app.getId(), StageReleaseStageType.ID, hash, componentIdentifier, null,
        MatchState.EXACT, false, new Date(System.currentTimeMillis() + 1000));

    restRequest().path("applications").query("hash", hash).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_DASHBOARD_COMPONENT_DETAILS, null);
    assertGetApplicationDetailsByHashAuditData(auditDTO, hash, componentIdentifier, null, 1, 1);
  }

  @Test
  public void testGetApplicationDetailsByHash_HashNotFound() throws Exception {
    tempEntity.newApplicationWithParent();
    String hash = "nonexistent";

    restRequest().path("applications").query("hash", hash).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_DASHBOARD_COMPONENT_DETAILS, null);
    assertGetApplicationDetailsByHashAuditData(auditDTO, hash, null, null, 1, 0);
  }

  @Test
  public void testGetApplicationDetailsByHash_NoComponentIdentifierForHash() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String hash = "abcdefabcd";
    String pathname = "pathname";
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, null, pathname);

    restRequest().path("applications").query("hash", hash).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_DASHBOARD_COMPONENT_DETAILS, null);
    assertGetApplicationDetailsByHashAuditData(auditDTO, hash, null, pathname, 1, 1);
  }

  @Test
  public void testGetApplicationDetailsByHash_NoComponentIdentifierOrFilenameForHash() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String hash = "abcdefabcd";
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, null, null);

    restRequest().path("applications").query("hash", hash).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_DASHBOARD_COMPONENT_DETAILS, null);
    assertGetApplicationDetailsByHashAuditData(auditDTO, hash, null, null, 1, 1);
  }

  private void assertGetApplicationDetailsByHashAuditData(
      AuditDTO auditDTO,
      String hash,
      ComponentIdentifier componentIdentifier,
      String componentFilename,
      int appCount,
      int recordCount)
  {
    assertCustomData(auditDTO, "componentHash", hash);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    assertCustomData(auditDTO, "componentFilename", componentFilename);
    assertCustomData(auditDTO, "inspectedApplicationCount", appCount);
    assertCustomData(auditDTO, "resultRecordCount", recordCount);
  }
}
