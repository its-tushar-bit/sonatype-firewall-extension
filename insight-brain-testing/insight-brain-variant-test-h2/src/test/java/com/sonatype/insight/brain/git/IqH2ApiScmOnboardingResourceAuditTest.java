/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.EXPERIMENTAL_ONBOARDING_RESOURCE_PATH;

/**
 * Kept in the {@code com.sonatype.insight.brain.git} package (not the default
 * {@code com.sonatype.insight.brain.variant} package) because it references
 * {@link ApiScmOnboardingResource#IMPORT_REPO_PATH}, which is package-private. Reproduces the
 * {@code AbstractAuditTest} log-capture scaffolding that the legacy {@code ApiScmOnboardingResourceAuditTest}
 * inherited from its base class.
 */
@IqH2Test
class IqH2ApiScmOnboardingResourceAuditTest
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
    return ctx.restRequest().path(EXPERIMENTAL_ONBOARDING_RESOURCE_PATH);
  }

  @Test
  void testImportRepositories() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    ImportScmOrganizationRequest importRequest = new ImportScmOrganizationRequest();
    importRequest.scmHostUrl = "https://somescm/owner";
    importRequest.importLimit = 0; // to throw a bad request

    HttpResponse response = restRequest()
        .path(UriBuilder.fromPath(ApiScmOnboardingResource.IMPORT_REPO_PATH)
            .build(org.getId())
            .toString())
        .body(importRequest)
        .post();

    ctx.assertResponseStatus(400, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.SOURCE_CONTROL_IMPORT, "bad-request");
    assertOrganizationData(auditDTO, org);
    assertStandardData(auditDTO, AuditEvent.SOURCE_CONTROL_IMPORT, "bad-request");
    assertCustomData(auditDTO, "scmHostUrl", "https://somescm/owner");
    assertCustomData(auditDTO, "importLimit", 0);
    assertCustomData(auditDTO, "desiredSubOrganizationCount", 0);
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
