/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * MTIQ — the same five API assertions as the IQ variants, running on the multi-tenant server under
 * the global tenant. No base class — the {@link MtiqTest} annotation supplies the multi-tenant
 * server, PostgreSQL, and tenancy setup.
 *
 * <p>
 * The MTIQ test harness pins a fixed (forced) server base URL, so unlike the IQ variants the
 * {@code X-Forwarded-Proto} test only asserts that a redirect is produced.
 */
@MtiqTest
class MtiqApiSpikeTest
{
  // Injected by MtiqServerExtension: the extension owns the shared, reused multi-tenant server.
  private SpikeRestClient rest;

  @Test
  void developerDashboardLinkRedirects() {
    SpikeRestClient.Response response = rest.get("/ui/links/developer/dashboard");

    assertThat(response.is3xxRedirection()).isTrue();
    assertThat(response.location()).isNotNull();
    assertThat(response.location()).contains("index.html");
  }

  @Test
  void firewallDashboardLinkRedirects() {
    SpikeRestClient.Response response = rest.get("/ui/links/firewall/dashboard");

    assertThat(response.is3xxRedirection()).isTrue();
    assertThat(response.location()).isNotNull();
  }

  @Test
  void lifecycleDashboardLinkRedirects() {
    SpikeRestClient.Response response = rest.get("/ui/links/lifecycle/dashboard");

    assertThat(response.is3xxRedirection()).isTrue();
    assertThat(response.location()).contains("dashboard/violations");
  }

  @Test
  void redirectProducesRedirect() {
    SpikeRestClient.Response response =
        rest.get("/ui/links/developer/dashboard", Map.of("X-Forwarded-Proto", "https"));

    assertThat(response.is3xxRedirection()).isTrue();
    assertThat(response.location()).isNotNull();
  }

  @Test
  void unknownLinkReturnsNotFound() {
    SpikeRestClient.Response response = rest.get("/ui/links/does-not-exist");

    assertThat(response.status()).isEqualTo(404);
  }
}
