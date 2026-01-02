/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import com.google.common.net.HttpHeaders;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ServerHeaderFilterTest
    extends AbstractBrainServiceIntegrationTest
{
  @Test
  public void testServerHeaderPresent() throws Exception {
    assertThat(restRequest().get().getHeader(HttpHeaders.SERVER)).matches("NexusIQ/1\\.[0-9]+.*");
    assertThat(adminRequest().get().getHeader(HttpHeaders.SERVER)).matches("NexusIQ/1\\.[0-9]+.*");
  }
}
