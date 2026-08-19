/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.jaxrs.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiRepositoryIdentifiedComponentResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.REPOSITORY_IDENTIFIED_COMPONENT_PATH_V2).auth();
  }

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
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
  void testDeleteRepositoryIdentifiedComponent_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertAuditLog(AuditEvent.DELETE_REPOSITORY_IDENTIFIED_COMPONENT, "bad-request");
  }

  @Test
  void testDeleteRepositoryIdentifiedComponent_Hash() throws Exception {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = ctx.tempEntity().newRepositoryIdentifiedComponent();

    HttpResponse response = restRequest()
        .query("hash", repositoryIdentifiedComponent.getHash())
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_REPOSITORY_IDENTIFIED_COMPONENT, null);
    assertCustomData(auditDTO, "hash", repositoryIdentifiedComponent.getHash());
  }

  @Test
  void testDeleteRepositoryIdentifiedComponent_ComponentIdentifier() throws Exception {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = ctx.tempEntity().newRepositoryIdentifiedComponent();

    HttpResponse response = restRequest()
        .query("componentIdentifier", URLEncoder.encode(
            JsonUtils.toJson(repositoryIdentifiedComponent.getComponentIdentifier()), StandardCharsets.UTF_8.name()))
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_REPOSITORY_IDENTIFIED_COMPONENT, null);
    assertCustomObject(auditDTO, "componentIdentifier", repositoryIdentifiedComponent.getComponentIdentifier());
  }

  @Test
  void testDeleteRepositoryIdentifiedComponent_PackageUrl() throws Exception {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = ctx.tempEntity().newRepositoryIdentifiedComponent();
    String packageUrl =
        PackageUrlIdentifier.fromComponentIdentifier(repositoryIdentifiedComponent.getComponentIdentifier())
            .getPackageUrl();

    HttpResponse response = restRequest()
        .query("packageUrl", URLEncoder.encode(packageUrl, StandardCharsets.UTF_8.name()))
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_REPOSITORY_IDENTIFIED_COMPONENT, null);
    assertCustomData(auditDTO, "packageUrl", packageUrl);
  }

  @Test
  void testDeleteAllRepositoryIdentifiedComponents() throws Exception {
    ctx.tempEntity().newRepositoryIdentifiedComponent();
    HttpResponse response =
        ctx.restRequest().path(PublicApiPaths.REPOSITORY_IDENTIFIED_COMPONENT_PATH_V2 + "/clear").delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
    assertAuditLog(AuditEvent.PURGE_REPOSITORY_IDENTIFIED_COMPONENTS, null);
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
