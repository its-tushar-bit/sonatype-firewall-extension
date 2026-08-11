/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqPostgresTest
class IqPostgresFirewallMigrationResourceAuditTest
    implements AuditTestSupport
{
  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private IqTestContext ctx;

  private RepositoryDAO repositoryDAO;

  private RepositoryManager targetRepositoryManager;

  private Repository targetRepository;

  private RepositoryManager sourceRepositoryManager;

  private Repository sourceRepository;

  private User unauthorizedUser;

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();

    repositoryDAO = ctx.lookup(RepositoryDAO.class);
    targetRepositoryManager = ctx.tempEntity().newRepositoryManager();
    targetRepository = ctx.tempEntity().newRepository(targetRepositoryManager, "targetRepositoryPublicId");
    sourceRepositoryManager = ctx.tempEntity().newRepositoryManager();
    sourceRepository = ctx.tempEntity().newRepository(sourceRepositoryManager, "sourceRepositoryPublicId");
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
  void testMigrateRepositoryHistory_QuarantineDisabled() throws Exception {
    migrateRepositoryHistoryRequest().post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.MIGRATE_REPOSITORY, null);
    assertRepositoryData(auditDTO, targetRepository);
    assertRepositoryMigrateData(auditDTO);
  }

  @Test
  void testMigrateRepositoryHistory_QuarantineEnabled() throws Exception {
    sourceRepository.setQuarantineEnabled(true);
    repositoryDAO.update(sourceRepository);
    migrateRepositoryHistoryRequest().post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.MIGRATE_REPOSITORY, null);
    assertRepositoryMigrateData(auditDTO);
  }

  @Test
  void testMigrateRepositoryHistory_Unauthorized() throws Exception {
    migrateRepositoryHistoryRequest().with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.MIGRATE_REPOSITORY, "unauthorized");
    assertRepositoryMigrateBasicData(auditDTO);
  }

  private HttpRequest migrateRepositoryHistoryRequest() {
    return ctx.restRequest()
        .path(FirewallMigrationResource.RESOURCE_PATH, FirewallMigrationResource.HISTORY_PATH)
        .parameter(targetRepositoryManager.getInstanceId(), targetRepository.getPublicId())
        .query("sourceRepositoryManagerInstanceId", sourceRepositoryManager.getInstanceId())
        .query("sourceRepositoryPublicId", sourceRepository.getPublicId());
  }

  private void assertRepositoryMigrateData(AuditDTO auditDTO) {
    assertRepositoryMigrateBasicData(auditDTO);
    assertCustomData(auditDTO, "repositoryId", targetRepository.getId());
    assertCustomData(auditDTO, "sourceRepositoryId", sourceRepository.getId());
    assertCustomData(auditDTO, "quarantine", sourceRepository.isQuarantineEnabled() ? "enabled" : "disabled");
  }

  private void assertRepositoryMigrateBasicData(AuditDTO auditDTO) {
    assertCustomData(auditDTO, "repositoryManagerInstanceId", targetRepositoryManager.getInstanceId());
    assertCustomData(auditDTO, "repositoryPublicId", targetRepository.getPublicId());
    assertCustomData(auditDTO, "sourceRepositoryManagerInstanceId", sourceRepositoryManager.getInstanceId());
    assertCustomData(auditDTO, "sourceRepositoryPublicId", sourceRepository.getPublicId());
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
