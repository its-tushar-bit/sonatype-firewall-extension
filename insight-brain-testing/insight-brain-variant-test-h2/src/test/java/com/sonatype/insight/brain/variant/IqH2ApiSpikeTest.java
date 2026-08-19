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
 * IQ Server on H2 — five tests, each makes a real HTTP call to the running server and asserts the
 * response. Note there is NO base class: the {@link IqH2Test} annotation supplies everything.
 *
 * <p>
 * All five hit {@code ui/links/*}, which is {@code @UnlicensedPath} and returns a redirect, so the
 * tests are meaningful across all three variants without heavy fixtures. The injected
 * {@link SpikeRestClient} does not follow redirects, so we assert on the 3xx status and
 * {@code Location} header directly.
 */
@IqH2Test
class IqH2ApiSpikeTest
{
  // Injected by IqH2ServerExtension: the extension owns the shared, reused server.
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
  void redirectHonoursXForwardedProto() {
    SpikeRestClient.Response response =
        rest.get("/ui/links/developer/dashboard", Map.of("X-Forwarded-Proto", "https"));

    assertThat(response.is3xxRedirection()).isTrue();
    assertThat(response.location()).startsWith("https");
  }

  @Test
  void unknownLinkReturnsNotFound() {
    SpikeRestClient.Response response = rest.get("/ui/links/does-not-exist");

    assertThat(response.status()).isEqualTo(404);
  }
}
