/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.search.indexquery.ApplicationStageSeverityBreakdown.Breakdown;

import org.junit.Test;

public class ApplicationStageSeverityBreakdownTest
{
  @Test
  public void parsesWellFormedTokensIntoPerStageBucketsWithTotalRollup() {
    Breakdown b = ApplicationStageSeverityBreakdown.parse(
        List.of("build:critical:3", "build:low:2", "release:severe:1"));
    assertThat(b).isNotNull();
    assertThat(b.stages()).containsKeys("build", "release");
    assertThat(b.stages().get("build"))
        .containsEntry("critical", 3)
        .containsEntry("low", 2)
        .containsEntry("severe", 0)
        .containsEntry("moderate", 0);
    assertThat(b.stages().get("release")).containsEntry("severe", 1);
    assertThat(b.totalRisk()).containsEntry("critical", 3).containsEntry("low", 2).containsEntry("severe", 1);
  }

  @Test
  public void aggregatesRepeatedStageSeverityTokens() {
    Breakdown b = ApplicationStageSeverityBreakdown.parse(List.of("build:critical:2", "build:critical:5"));
    assertThat(b.stages().get("build")).containsEntry("critical", 7);
    assertThat(b.totalRisk()).containsEntry("critical", 7);
  }

  @Test
  public void returnsNullForNullOrEmptyInput() {
    assertThat(ApplicationStageSeverityBreakdown.parse(null)).isNull();
    assertThat(ApplicationStageSeverityBreakdown.parse(List.of())).isNull();
  }

  @Test
  public void skipsMalformedTokensButKeepsValidOnes() {
    Breakdown b = ApplicationStageSeverityBreakdown.parse(Arrays.asList(
        "build:critical:3",
        "missing-parts",
        "build:critical",
        "build:bogusseverity:1",
        "build:critical:notanumber",
        null,
        "release:low:4"));
    assertThat(b).isNotNull();
    assertThat(b.stages().get("build")).containsEntry("critical", 3);
    assertThat(b.stages().get("release")).containsEntry("low", 4);
    assertThat(b.stages()).doesNotContainKey("missing-parts");
  }

  @Test
  public void returnsNullWhenEveryTokenIsMalformed() {
    assertThat(ApplicationStageSeverityBreakdown.parse(List.of("garbage", "a:b"))).isNull();
  }
}
