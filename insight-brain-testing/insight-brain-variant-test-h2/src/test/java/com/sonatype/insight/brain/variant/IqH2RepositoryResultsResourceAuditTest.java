/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsDetailsRequestDto;
import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsResource;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * H2 port of {@code RepositoryResultsResourceAuditTest}.
 */
@IqH2Test
class IqH2RepositoryResultsResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private RepositoryManager repositoryManager;

  private Repository repository;

  private RepositoryResultsDetailsRequestDto detailsRequest;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setup() {
    logOutput.before();
    logOutput.clear();
    repositoryManager = ctx.tempEntity().newRepositoryManager();
    repository = ctx.tempEntity().newRepository(repositoryManager, "publicId");
    detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
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
        .path(RepositoryResultsResource.RESOURCE_PATH, RepositoryResultsResource.DETAILS_BY_OWNER_PATH);
  }

  @Test
  void testGetDetails_RepositoryContainer() throws Exception {
    restRequest().parameter("repository_container", RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .body(detailsRequest)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, null);
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  void testGetDetails_RepositoryManager() throws Exception {
    restRequest().parameter("repository_manager", repositoryManager.getId())
        .body(detailsRequest)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, null);
    assertRepositoryManagerData(auditDTO, repositoryManager);
  }

  @Test
  void testGetDetails_Repository() throws Exception {
    restRequest().parameter("repository", repository.getId())
        .body(detailsRequest)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, null);
    assertRepositoryData(auditDTO, repository);
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
