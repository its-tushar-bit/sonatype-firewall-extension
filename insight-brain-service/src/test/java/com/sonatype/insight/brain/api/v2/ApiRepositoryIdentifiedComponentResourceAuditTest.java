/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.jaxrs.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiRepositoryIdentifiedComponentResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.REPOSITORY_IDENTIFIED_COMPONENT_PATH_V2).auth();
  }

  @Before
  public void before() {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertAuditLog(AuditEvent.DELETE_REPOSITORY_IDENTIFIED_COMPONENT, "bad-request");
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Hash() throws Exception {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = tempEntity.newRepositoryIdentifiedComponent();

    HttpResponse response = restRequest()
        .query("hash", repositoryIdentifiedComponent.getHash())
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_REPOSITORY_IDENTIFIED_COMPONENT, null);
    assertCustomData(auditDTO, "hash", repositoryIdentifiedComponent.getHash());
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_ComponentIdentifier() throws Exception {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = tempEntity.newRepositoryIdentifiedComponent();

    HttpResponse response = restRequest()
        .query("componentIdentifier", URLEncoder.encode(
            JsonUtils.toJson(repositoryIdentifiedComponent.getComponentIdentifier()), StandardCharsets.UTF_8.name()))
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_REPOSITORY_IDENTIFIED_COMPONENT, null);
    assertCustomObject(auditDTO, "componentIdentifier", repositoryIdentifiedComponent.getComponentIdentifier());
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_PackageUrl() throws Exception {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = tempEntity.newRepositoryIdentifiedComponent();
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
  public void testDeleteAllRepositoryIdentifiedComponents() throws Exception {
    tempEntity.newRepositoryIdentifiedComponent();
    HttpResponse response =
        super.restRequest().path(PublicApiPaths.REPOSITORY_IDENTIFIED_COMPONENT_PATH_V2 + "/clear").delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
    assertAuditLog(AuditEvent.PURGE_REPOSITORY_IDENTIFIED_COMPONENTS, null);
  }
}
