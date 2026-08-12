/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Converted from the legacy {@code ApiSearchResourceV2AuditTest}. Kept in the original package/simple name
 * because {@link #testSearchComponent_ByHash} and its siblings resolve the mock report fixture via
 * {@code getClass().getSimpleName()}.
 */
@IqH2Test
public class ApiSearchResourceV2AuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

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
    return ctx.restRequest()
        .path(com.sonatype.insight.brain.api.PublicApiPaths.SEARCH_RESOURCE_PATH_V2)
        .query("stageId", Stage.ID_BUILD);
  }

  @Test
  public void testSearchComponent_ByHash() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    String hash = "1249e25aebb15358bedd";
    ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId");
    ctx.tempEntity().newApplicationComponent(app.getId(), Stage.ID_BUILD, hash, null /* componentIdentifier */);
    ctx.tempEntity().newApplicationWithParent();
    ctx.mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    restRequest().query("hash", hash).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SEARCH_COMPONENT_USES, null);
    assertCustomData(auditDTO, "componentHash", hash);
    assertCustomObject(auditDTO, "componentIdentifier", null);
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
    assertCustomData(auditDTO, "resultRecordCount", 1);
  }

  @Test
  public void testSearchComponent_ByComponentIdentifier() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId");
    ctx.tempEntity().newApplicationComponent(app.getId(), Stage.ID_BUILD, "hash", componentIdentifier);
    ctx.tempEntity().newApplicationWithParent();
    ctx.mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    restRequest().query("componentIdentifier", componentIdentifier).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SEARCH_COMPONENT_USES, null);
    assertCustomData(auditDTO, "componentHash", null);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
    assertCustomData(auditDTO, "resultRecordCount", 1);
  }

  @Test
  public void testSearchComponent_ByPackageUrl() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    String packageUrl = "pkg:maven/g/a@v";
    ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId");
    ComponentIdentifier componentIdentifier = new PackageUrlIdentifier(packageUrl).toComponentIdentifier();
    ctx.tempEntity()
        .newApplicationComponent(app.getId(), Stage.ID_BUILD, "hash",
            componentIdentifier);
    ctx.tempEntity().newApplicationWithParent();
    ctx.mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    restRequest().query("packageUrl", packageUrl).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SEARCH_COMPONENT_USES, null);
    assertCustomData(auditDTO, "componentHash", null);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
    assertCustomData(auditDTO, "resultRecordCount", 1);
  }

  @Test
  public void testSearchComponent_ByHashAndComponentIdentifier() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    String hash = "1249e25aebb15358bedd";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId");
    ctx.tempEntity().newApplicationComponent(app.getId(), Stage.ID_BUILD, hash, componentIdentifier);
    ctx.tempEntity().newApplicationWithParent();
    ctx.mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    restRequest().query("hash", hash).query("componentIdentifier", componentIdentifier).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SEARCH_COMPONENT_USES, null);
    assertCustomData(auditDTO, "componentHash", hash);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
    assertCustomData(auditDTO, "resultRecordCount", 1);
  }

  @Test
  public void testSearchComponent_ByHashAndPackageUrl() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    String hash = "1249e25aebb15358bedd";
    String packageUrl = "pkg:maven/g/a@v";
    ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId");
    ComponentIdentifier componentIdentifier = new PackageUrlIdentifier(packageUrl).toComponentIdentifier();
    ctx.tempEntity().newApplicationComponent(app.getId(), Stage.ID_BUILD, hash, componentIdentifier);
    ctx.tempEntity().newApplicationWithParent();
    ctx.mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    restRequest().query("hash", hash).query("packageUrl", packageUrl).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SEARCH_COMPONENT_USES, null);
    assertCustomData(auditDTO, "componentHash", hash);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
    assertCustomData(auditDTO, "resultRecordCount", 1);
  }

  @Test
  public void testSearchComponent_Error() throws Exception {
    // Invalid hash should trigger a bad-request error.
    String hash = "foo";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    restRequest().query("hash", hash).query("componentIdentifier", componentIdentifier).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SEARCH_COMPONENT_USES, "bad-request");
    assertCustomData(auditDTO, "componentHash", hash);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
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
