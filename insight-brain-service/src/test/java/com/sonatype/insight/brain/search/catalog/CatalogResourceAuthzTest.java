/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Drives the catalog endpoint through the real REST/Shiro/Jackson stack to pin the authz matrix:
 * 401 (unauthenticated, produced upstream by Shiro), 403 (authenticated but no readable context),
 * and 200 (authorized). Status-mapping edge cases (404/400) are covered by the fast
 * {@link CatalogEndpointTest}; service behaviour by {@link CatalogServiceTest}.
 */
public class CatalogResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Before
  public void enableGlobalSearch() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    grantReadPermission(org.getId());
  }

  @After
  public void disableGlobalSearch() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(CatalogResource.RESOURCE_PATH);
  }

  private static CatalogRequest catalogComponentRequest() {
    return new CatalogRequest("COMPONENT", "catalog", Map.of(), 1, 25, null, null, false);
  }

  @Test
  public void unauthenticated_returns401() throws Exception {
    HttpResponse response = restRequest().anon().body(catalogComponentRequest()).post();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void authenticatedWithoutReadContext_returns403() throws Exception {
    HttpResponse response = restRequest().auth(unauthorized).body(catalogComponentRequest()).post();
    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void authorized_returns200() throws Exception {
    HttpResponse response = restRequest().auth(authorized).body(catalogComponentRequest()).post();
    assertThat(response.getStatusCode()).isEqualTo(200);
    CatalogResponse body = response.getBody(CatalogResponse.class);
    assertThat(body.entityType()).isEqualTo("COMPONENT");
  }
}
