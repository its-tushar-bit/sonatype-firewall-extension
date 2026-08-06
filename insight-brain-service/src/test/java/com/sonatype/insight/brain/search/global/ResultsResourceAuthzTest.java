/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Framework-level test for {@code GET /rest/search/results}: exercises the real JAX-RS binding, the
 * Shiro auth filter, the feature-flag gate, and the registered exception mappers end-to-end through the
 * HTTP stack (not by calling resource methods directly). The stale-cursor (410) and rate-limit (429)
 * mapper paths are covered at unit level in {@link ResultsEndpointTest} / {@link PerUserRateLimiterTest};
 * this test proves the auth + flag + validation surface works through the real container.
 */
public class ResultsResourceAuthzTest
    extends AbstractResourceTest
{
  private static final String RESULTS_PATH = "rest/search/results";

  @Before
  public void enableGlobalSearch() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
  }

  @After
  public void disableGlobalSearch() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
  }

  @Test
  public void unauthenticatedRequest_returns401() throws Exception {
    HttpResponse response = restRequest()
        .path(RESULTS_PATH)
        .parameter("q", "react")
        .parameter("tab", "APPLICATION")
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void flagOff_returns404() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = restRequest()
        .path(RESULTS_PATH)
        .parameter("q", "react")
        .parameter("tab", "APPLICATION")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void blankQuery_returns400() throws Exception {
    HttpResponse response = restRequest()
        .path(RESULTS_PATH)
        .parameter("q", "  ")
        .parameter("tab", "APPLICATION")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
  }

  @Test
  public void unknownTab_returns400() throws Exception {
    HttpResponse response = restRequest()
        .path(RESULTS_PATH)
        .parameter("q", "react")
        .parameter("tab", "NOT_A_TAB")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
  }
}
