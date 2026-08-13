/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.jaxrs.JsonUtils;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiRepositoryComponentResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  private RepositoryManager repositoryManager;

  private Repository hostedRepo;

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    repositoryManager = ctx.tempEntity().newRepositoryManager();
    hostedRepo = ctx.tempEntity().newHostedRepository(repositoryManager, "audit-test-repo", "maven2", false);
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private HttpRequest componentsRequest() {
    return ctx.restRequest()
        .path(PublicApiPaths.REPOSITORIES_RESOURCE_PATH + "/" + repositoryManager.getInstanceId() + "/components")
        .auth();
  }

  private HttpRequest repositoriesRequest() {
    return ctx.restRequest()
        .path(PublicApiPaths.REPOSITORIES_RESOURCE_PATH + "/" + repositoryManager.getInstanceId() + "/repositories")
        .auth();
  }

  @Test
  void testDeleteComponents_AuditLogged() throws Exception {
    ProxyRepositoryComponent component =
        ctx.tempEntity().newRepositoryComponentWithComponentId(hostedRepo.getId(), "test-nxrm-id-1");

    HttpResponse response = componentsRequest().body(JsonUtils.toJson(List.of(component.getComponentId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
    assertAuditLog(AuditEvent.REMOVE_REPOSITORY, null);
  }

  @Test
  void testDeleteComponents_Error_AuditLogged() throws Exception {
    HttpResponse response =
        componentsRequest().body(JsonUtils.toJson(List.of("nonexistent-component-id"))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
    assertAuditLog(AuditEvent.REMOVE_REPOSITORY, "not-found");
  }

  @Test
  void testDeleteRepositoryComponents_AuditLogged() throws Exception {
    ctx.tempEntity().newRepositoryComponent(hostedRepo.getId());

    HttpResponse response =
        repositoriesRequest().body(JsonUtils.toJson(List.of(hostedRepo.getPublicId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
    assertAuditLog(AuditEvent.REMOVE_REPOSITORY, null);
  }

  @Test
  void testDeleteRepositoryComponents_Error_AuditLogged() throws Exception {
    HttpResponse response =
        repositoriesRequest().body(JsonUtils.toJson(List.of("nonexistent-repo-public-id"))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
    assertAuditLog(AuditEvent.REMOVE_REPOSITORY, "not-found");
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
