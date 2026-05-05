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
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.jaxrs.JsonUtils;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiRepositoryComponentResourceAuditTest
    extends AbstractAuditTest
{
  private RepositoryManager repositoryManager;

  private Repository hostedRepo;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    repositoryManager = tempEntity.newRepositoryManager();
    hostedRepo = tempEntity.newHostedRepository(repositoryManager, "audit-test-repo", "maven2", false);
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
  public void testDeleteComponents_AuditLogged() throws Exception {
    RepositoryComponent component = tempEntity.newRepositoryComponent(hostedRepo.getId());

    HttpResponse response = componentsRequest().body(JsonUtils.toJson(List.of(component.getId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
    assertAuditLog(AuditEvent.REMOVE_REPOSITORY, null);
  }

  @Test
  public void testDeleteComponents_Error_AuditLogged() throws Exception {
    HttpResponse response =
        componentsRequest().body(JsonUtils.toJson(List.of("nonexistent-component-id"))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
    assertAuditLog(AuditEvent.REMOVE_REPOSITORY, "not-found");
  }

  @Test
  public void testDeleteRepositoryComponents_AuditLogged() throws Exception {
    tempEntity.newRepositoryComponent(hostedRepo.getId());

    HttpResponse response =
        repositoriesRequest().body(JsonUtils.toJson(List.of(hostedRepo.getPublicId()))).delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
    assertAuditLog(AuditEvent.REMOVE_REPOSITORY, null);
  }

  @Test
  public void testDeleteRepositoryComponents_Error_AuditLogged() throws Exception {
    HttpResponse response =
        repositoriesRequest().body(JsonUtils.toJson(List.of("nonexistent-repo-public-id"))).delete();

    assertThat(response.getStatusCode()).isEqualTo(404);
    assertAuditLog(AuditEvent.REMOVE_REPOSITORY, "not-found");
  }
}
