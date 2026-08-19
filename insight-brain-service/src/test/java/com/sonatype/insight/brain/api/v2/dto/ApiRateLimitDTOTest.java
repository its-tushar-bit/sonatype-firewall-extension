/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.insight.brain.api.experimental.dto.ApiRateLimitDTO;
import com.sonatype.nexus.scm.github.dto.GithubRateLimitResponse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiRateLimitDTOTest
{
  @Test
  public void testConvert() {
    GithubRateLimitResponse githubRateLimitResponse = new GithubRateLimitResponse();
    githubRateLimitResponse.setCategory("category");
    githubRateLimitResponse.setRemaining(4);
    githubRateLimitResponse.setLimit(10);
    githubRateLimitResponse.setReset(4444);

    ApiRateLimitDTO dto = ApiRateLimitDTO.convert(githubRateLimitResponse);

    assertThat(dto.category).isEqualTo("category");
    assertThat(dto.remaining).isEqualTo(4);
    assertThat(dto.limit).isEqualTo(10);
    assertThat(dto.resetEpochTime).isEqualTo(4444);
  }
}
