/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiTypeTest
{
  @Test
  public void testToString() {
    for (ApiType apiType : ApiType.values()) {
      assertThat(apiType).hasToString(apiType.name().toLowerCase(Locale.ROOT));
    }
  }

  @Test
  public void testFromString() {
    for (ApiType apiType : ApiType.values()) {
      assertThat(ApiType.fromString(apiType.name())).isEqualTo(apiType);
      assertThat(ApiType.fromString(apiType.name().toLowerCase(Locale.ROOT))).isEqualTo(apiType);
    }
  }

  @Test
  public void testGetPathPrefix() {
    assertThat(ApiType.PUBLIC.getPathPrefix()).isEqualTo("/api/v2/");
    assertThat(ApiType.EXPERIMENTAL.getPathPrefix()).isEqualTo("/api/experimental/");
  }
}
