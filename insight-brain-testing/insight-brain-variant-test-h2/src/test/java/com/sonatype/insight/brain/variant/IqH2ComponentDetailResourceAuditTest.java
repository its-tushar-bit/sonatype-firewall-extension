/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.component.ComponentDetailResource;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * IQ Server on H2 — {@code ComponentDetailResourceAuditTest} converted to the reused-server
 * {@link IqH2Test} pattern. No base class; wiring is via the injected {@link IqTestContext}.
 */
@IqH2Test
class IqH2ComponentDetailResourceAuditTest
    implements AuditTestSupport
{
  // Injected by IqH2ServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(ComponentDetailResource.RESOURCE_PATH);
  }

  @Test
  void testGetApplicationDetailsByHash() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    String hash = "dcbafedcba";
    ctx.tempEntity()
        .newApplicationComponent(app.getId(), BuildStageType.ID, hash,
            ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("org.apache.maven", "maven", "2.0");
    ctx.tempEntity()
        .newApplicationComponent(app.getId(), StageReleaseStageType.ID, hash, componentIdentifier, null,
            MatchState.EXACT, false, new Date(System.currentTimeMillis() + 1000));

    restRequest().path("applications").query("hash", hash).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_DASHBOARD_COMPONENT_DETAILS, null);
    assertGetApplicationDetailsByHashAuditData(auditDTO, hash, componentIdentifier, null, 1, 1);
  }

  @Test
  void testGetApplicationDetailsByHash_HashNotFound() throws Exception {
    ctx.tempEntity().newApplicationWithParent();
    String hash = "nonexistent";

    restRequest().path("applications").query("hash", hash).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_DASHBOARD_COMPONENT_DETAILS, null);
    assertGetApplicationDetailsByHashAuditData(auditDTO, hash, null, null, 1, 0);
  }

  @Test
  void testGetApplicationDetailsByHash_NoComponentIdentifierForHash() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    String hash = "abcdefabcd";
    String pathname = "pathname";
    ctx.tempEntity().newApplicationComponent(app.getId(), BuildStageType.ID, hash, null, pathname);

    restRequest().path("applications").query("hash", hash).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_DASHBOARD_COMPONENT_DETAILS, null);
    assertGetApplicationDetailsByHashAuditData(auditDTO, hash, null, pathname, 1, 1);
  }

  @Test
  void testGetApplicationDetailsByHash_NoComponentIdentifierOrFilenameForHash() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    String hash = "abcdefabcd";
    ctx.tempEntity().newApplicationComponent(app.getId(), BuildStageType.ID, hash, null, null);

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

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
