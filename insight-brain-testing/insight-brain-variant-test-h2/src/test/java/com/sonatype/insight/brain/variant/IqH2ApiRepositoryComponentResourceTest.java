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
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.jaxrs.JsonUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiRepositoryComponentResourceTest
{
  private IqTestContext ctx;

  private RepositoryManager repositoryManager;

  private Repository hostedRepo;

  @BeforeEach
  void setUp() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    repositoryManager = ctx.tempEntity().newRepositoryManager();
    hostedRepo = ctx.tempEntity().newHostedRepository(repositoryManager, "test-hosted-repo", "maven2", false);
  }

  @AfterEach
  void after() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
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
  void testDeleteComponents_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
    ProxyRepositoryComponent component =
        ctx.tempEntity().newRepositoryComponentWithComponentId(hostedRepo.getId(), "test-nxrm-id-1");

    HttpResponse response = componentsRequest().body(JsonUtils.toJson(List.of(component.getComponentId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  void testDeleteComponents_Success() throws Exception {
    ProxyRepositoryComponent component =
        ctx.tempEntity().newRepositoryComponentWithComponentId(hostedRepo.getId(), "test-nxrm-id-1");

    HttpResponse response = componentsRequest().body(JsonUtils.toJson(List.of(component.getComponentId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  void testDeleteComponents_EmptyBody_Returns204() throws Exception {
    HttpResponse response = componentsRequest().body(JsonUtils.toJson(List.of())).delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  void testDeleteComponents_UnknownComponent_Returns404() throws Exception {
    HttpResponse response =
        componentsRequest().body(JsonUtils.toJson(List.of("nonexistent-component-id"))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  void testDeleteRepositoryComponents_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);

    HttpResponse response =
        repositoriesRequest().body(JsonUtils.toJson(List.of(hostedRepo.getPublicId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  void testDeleteRepositoryComponents_Success() throws Exception {
    ctx.tempEntity().newRepositoryComponent(hostedRepo.getId());
    ctx.tempEntity().newRepositoryComponent(hostedRepo.getId(), "path-2");

    HttpResponse response =
        repositoriesRequest().body(JsonUtils.toJson(List.of(hostedRepo.getPublicId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  void testDeleteRepositoryComponents_EmptyBody_Returns204() throws Exception {
    HttpResponse response = repositoriesRequest().body(JsonUtils.toJson(List.of())).delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  void testDeleteRepositoryComponents_UnknownRepository_Returns404() throws Exception {
    HttpResponse response =
        repositoriesRequest().body(JsonUtils.toJson(List.of("nonexistent-repo-id"))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  void testDeleteRepositoryComponents_ProxyRepo_Returns404() throws Exception {
    Repository proxyRepo =
        ctx.tempEntity().newProxyRepository(repositoryManager, "proxy-repo", "maven2", false, false);

    HttpResponse response =
        repositoriesRequest().body(JsonUtils.toJson(List.of(proxyRepo.getPublicId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  void testDeleteComponents_ProxyRepoComponent_Returns404() throws Exception {
    Repository proxyRepo =
        ctx.tempEntity().newProxyRepository(repositoryManager, "proxy-repo", "maven2", false, false);
    ProxyRepositoryComponent proxyComponent =
        ctx.tempEntity().newRepositoryComponentWithComponentId(proxyRepo.getId(), "test-nxrm-proxy-id");

    HttpResponse response =
        componentsRequest().body(JsonUtils.toJson(List.of(proxyComponent.getComponentId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }
}
