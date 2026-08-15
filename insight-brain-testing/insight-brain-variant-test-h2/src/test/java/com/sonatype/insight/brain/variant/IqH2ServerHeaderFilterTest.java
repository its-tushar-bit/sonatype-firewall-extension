/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.google.common.net.HttpHeaders;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ServerHeaderFilterTest
{
  private IqTestContext ctx;

  @Test
  void testServerHeaderPresent() throws Exception {
    assertThat(ctx.restRequest().get().getHeader(HttpHeaders.SERVER)).matches("NexusIQ/1\\.[0-9]+.*");
    assertThat(ctx.adminRequest().get().getHeader(HttpHeaders.SERVER)).matches("NexusIQ/1\\.[0-9]+.*");
  }
}
