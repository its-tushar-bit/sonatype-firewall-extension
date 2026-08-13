/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.PolicyWaiverResource;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqPostgresTest
class IqPostgresPolicyWaiverResourceAuditTest
    implements AuditTestSupport
{
  private static final String COMPONENT_HASH = "hash";

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private User unauthorizedUser;

  @BeforeEach
  void setupCommonFixture() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
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
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @Test
  void testGetPolicyWaiversByHash_Application() throws Exception {
    final Application application = ctx.tempEntity().newApplicationWithParent();
    restRequest(application).path("component", COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  void testGetPolicyWaiversByHash_Organization() throws Exception {
    final Organization organization = ctx.tempEntity().newOrganization();
    restRequest(organization).path("component", COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  void testGetPolicyWaiversByHash_Repository() throws Exception {
    final Repository repository = ctx.tempEntity().newRepository();
    restRequest(repository).path("component", COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  void testGetPolicyWaiversByHash_RepositoryContainer() throws Exception {
    restRequest(RepositoryContainer.SINGLETON).path("component", COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertRepositoryContainerData(auditDTO);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  void testGetPolicyWaiversByHash_Unauthorized() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();

    restRequest(application).path("component", COMPONENT_HASH).with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private HttpRequest restRequest(Owner owner) {
    return ctx.restRequest()
        .path(PolicyWaiverResource.RESOURCE_PATH)
        .parameter(owner.getType(),
            owner.getType().equals(OwnerType.APPLICATION) ? owner.getPublicId() : owner.getId());
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
