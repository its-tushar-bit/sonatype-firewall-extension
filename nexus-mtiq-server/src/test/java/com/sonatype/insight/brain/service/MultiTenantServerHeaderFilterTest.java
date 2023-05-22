/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.regex.Pattern;

import com.google.common.net.HttpHeaders;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantServerHeaderFilterTest
    extends AbstractMultiTenantResourceTest
{
  private String tenantName;

  private Pattern buildRegex = Pattern.compile("NexusIQ/1\\.[0-9]+.*-(build-number|SNAPSHOT-\\d+)$");

  @Before
  public void setUp() {
    tenantName = generateTestTenantName();
  }

  @Test
  public void testServerHeaderPresent() throws Exception {
    assertThat(adminRequest().path("api", "admin").get().getHeader(HttpHeaders.SERVER)).matches(buildRegex);
    provisionTenant(tenantName);
    assertThat(restRequest().get().getHeader(HttpHeaders.SERVER)).matches(buildRegex);
  }
}
