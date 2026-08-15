/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.ApiNamespaceConfusionResource;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2ApiNamespaceConfusionResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() {
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

  @Test
  void testApiNamespaceConfusionResourceAddNamespace() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_PATH)
        .parameter("maven2")
        .body("[ \"org.sonatype\" ]")
        .post();

    ctx.assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ADD_PROPRIETARY_COMPONENT_NAMES, null);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_ROOT);
    assertCustomData(auditDTO, "repositoryPublicId", "nsc_maven2");
    assertCustomData(auditDTO, "addedPatternCount", 1);
  }

  @Test
  void testApiNamespaceConfusionResourceDeleteNamespace() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_PATH)
        .parameter("maven2")
        .delete();

    ctx.assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_PROPRIETARY_COMPONENT_NAMES, null);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_ROOT);
    assertCustomData(auditDTO, "repositoryPublicId", "nsc_maven2");
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
