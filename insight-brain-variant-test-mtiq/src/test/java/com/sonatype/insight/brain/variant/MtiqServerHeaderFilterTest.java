/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.regex.Pattern;

import com.google.common.net.HttpHeaders;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MTIQ variant conversion of {@code MultiTenantServerHeaderFilterTest} (which extended
 * {@code AbstractMultiTenantBaseIntegrationTest}). No base class, an injected {@link MtiqTestContext} supplies
 * the reused multi-tenant server and a fresh per-test tenant. The legacy test's own {@code provisionTenant} call
 * is dropped since {@link MtiqTestContext} auto-provisions a fresh tenant per test.
 */
@MtiqTest
class MtiqServerHeaderFilterTest
{
  private MtiqTestContext ctx;

  private final Pattern buildRegex = Pattern.compile("NexusIQ/1\\.[0-9]+.*-(build-number|SNAPSHOT-\\d+)$");

  @Test
  void testServerHeaderPresent() throws Exception {
    assertThat(ctx.adminRequest().path("api", "admin").get().getHeader(HttpHeaders.SERVER)).matches(buildRegex);
    assertThat(ctx.restRequest().get().getHeader(HttpHeaders.SERVER)).matches(buildRegex);
  }
}
