/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Framework-level test for {@code GET /rest/search/results}: exercises the real JAX-RS binding, the
 * Shiro auth filter, the feature-flag gate, and the registered exception mappers end-to-end through the
 * HTTP stack (not by calling resource methods directly).
 */
@IqH2Test
class IqH2ResultsResourceAuthzTest
{
  // Injected by IqH2ServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private static final String RESULTS_PATH = "rest/search/results";

  @BeforeEach
  void enableGlobalSearch() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
  }

  @AfterEach
  void disableGlobalSearch() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
  }

  @Test
  void unauthenticatedRequest_returns401() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(RESULTS_PATH)
        .parameter("q", "react")
        .parameter("tab", "APPLICATION")
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void flagOff_returns404() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = ctx.restRequest()
        .path(RESULTS_PATH)
        .parameter("q", "react")
        .parameter("tab", "APPLICATION")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  void blankQuery_returns400() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(RESULTS_PATH)
        .parameter("q", "  ")
        .parameter("tab", "APPLICATION")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
  }

  @Test
  void unknownTab_returns400() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(RESULTS_PATH)
        .parameter("q", "react")
        .parameter("tab", "NOT_A_TAB")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
  }
}
