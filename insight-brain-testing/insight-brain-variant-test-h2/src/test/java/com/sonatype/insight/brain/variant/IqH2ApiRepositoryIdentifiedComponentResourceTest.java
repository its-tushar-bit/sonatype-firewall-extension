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
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.jaxrs.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiRepositoryIdentifiedComponentResourceTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.REPOSITORY_IDENTIFIED_COMPONENT_PATH_V2).auth();
  }

  @Test
  void testDeleteRepositoryIdentifiedComponent_FeatureDisabled() throws Exception {
    HttpResponse response = restRequest().delete();

    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).isEqualTo(
        SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getId() + " feature is disabled");
  }

  @Test
  void testDeleteRepositoryIdentifiedComponent_Hash() throws Exception {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = ctx.tempEntity().newRepositoryIdentifiedComponent();

    HttpResponse response = restRequest()
        .query("hash", repositoryIdentifiedComponent.getHash())
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  void testDeleteRepositoryIdentifiedComponent_ComponentIdentifier() throws Exception {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = ctx.tempEntity().newRepositoryIdentifiedComponent();

    HttpResponse response = restRequest()
        .query("componentIdentifier", URLEncoder.encode(
            JsonUtils.toJson(repositoryIdentifiedComponent.getComponentIdentifier()), StandardCharsets.UTF_8.name()))
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  void testDeleteRepositoryIdentifiedComponent_PackageUrl() throws Exception {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = ctx.tempEntity().newRepositoryIdentifiedComponent();

    HttpResponse response = restRequest()
        .query("packageUrl", URLEncoder.encode(PackageUrlIdentifier.fromComponentIdentifier(
            repositoryIdentifiedComponent.getComponentIdentifier()).getPackageUrl(), StandardCharsets.UTF_8.name()))
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }
}
