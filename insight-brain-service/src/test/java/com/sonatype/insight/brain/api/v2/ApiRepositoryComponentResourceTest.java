/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.jaxrs.JsonUtils;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiRepositoryComponentResourceTest
    extends AbstractResourceTest
{
  private RepositoryManager repositoryManager;

  private Repository hostedRepo;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    repositoryManager = tempEntity.newRepositoryManager();
    hostedRepo = tempEntity.newHostedRepository(repositoryManager, "test-hosted-repo", "maven2", false);
  }

  private HttpRequest componentsRequest() {
    return super.restRequest()
        .path(PublicApiPaths.REPOSITORIES_RESOURCE_PATH + "/" + repositoryManager.getInstanceId() + "/components")
        .auth();
  }

  private HttpRequest repositoriesRequest() {
    return super.restRequest()
        .path(PublicApiPaths.REPOSITORIES_RESOURCE_PATH + "/" + repositoryManager.getInstanceId() + "/repositories")
        .auth();
  }

  @Test
  public void testDeleteComponents_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
    RepositoryComponent component =
        tempEntity.newRepositoryComponentWithComponentId(hostedRepo.getId(), "test-nxrm-id-1");

    HttpResponse response = componentsRequest().body(JsonUtils.toJson(List.of(component.getComponentId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void testDeleteComponents_Success() throws Exception {
    RepositoryComponent component =
        tempEntity.newRepositoryComponentWithComponentId(hostedRepo.getId(), "test-nxrm-id-1");

    HttpResponse response = componentsRequest().body(JsonUtils.toJson(List.of(component.getComponentId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  public void testDeleteComponents_EmptyBody_Returns204() throws Exception {
    HttpResponse response = componentsRequest().body(JsonUtils.toJson(List.of())).delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  public void testDeleteComponents_UnknownComponent_Returns404() throws Exception {
    HttpResponse response =
        componentsRequest().body(JsonUtils.toJson(List.of("nonexistent-component-id"))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void testDeleteRepositoryComponents_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);

    HttpResponse response =
        repositoriesRequest().body(JsonUtils.toJson(List.of(hostedRepo.getPublicId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void testDeleteRepositoryComponents_Success() throws Exception {
    tempEntity.newRepositoryComponent(hostedRepo.getId());
    tempEntity.newRepositoryComponent(hostedRepo.getId(), "path-2");

    HttpResponse response =
        repositoriesRequest().body(JsonUtils.toJson(List.of(hostedRepo.getPublicId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  public void testDeleteRepositoryComponents_EmptyBody_Returns204() throws Exception {
    HttpResponse response = repositoriesRequest().body(JsonUtils.toJson(List.of())).delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  public void testDeleteRepositoryComponents_UnknownRepository_Returns404() throws Exception {
    HttpResponse response =
        repositoriesRequest().body(JsonUtils.toJson(List.of("nonexistent-repo-id"))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void testDeleteRepositoryComponents_ProxyRepo_Returns404() throws Exception {
    Repository proxyRepo =
        tempEntity.newProxyRepository(repositoryManager, "proxy-repo", "maven2", false, false);

    HttpResponse response =
        repositoriesRequest().body(JsonUtils.toJson(List.of(proxyRepo.getPublicId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void testDeleteComponents_ProxyRepoComponent_Returns404() throws Exception {
    Repository proxyRepo =
        tempEntity.newProxyRepository(repositoryManager, "proxy-repo", "maven2", false, false);
    RepositoryComponent proxyComponent =
        tempEntity.newRepositoryComponentWithComponentId(proxyRepo.getId(), "test-nxrm-proxy-id");

    HttpResponse response =
        componentsRequest().body(JsonUtils.toJson(List.of(proxyComponent.getComponentId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }
}
