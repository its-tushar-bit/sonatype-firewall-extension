/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import com.sonatype.insight.brain.model.searchindex.SearchIndexHealth;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchIndexHealthThresholdsTest
{
  @Test
  public void derive_healthyWhenIdle() {
    var derived = SearchIndexHealthThresholds.derive(0, 0, 0, false);
    assertThat(derived.healthStatus()).isEqualTo(SearchIndexHealth.STATUS_HEALTHY);
    assertThat(derived.recommendedOp()).isEqualTo(SearchIndexHealth.OP_NONE);
  }

  @Test
  public void derive_warningOnLagAndPrefersScopedCleanup() {
    var derived = SearchIndexHealthThresholds.derive(600, 0, 0, false);
    assertThat(derived.healthStatus()).isEqualTo(SearchIndexHealth.STATUS_WARNING);
    assertThat(derived.recommendedOp()).isEqualTo(SearchIndexHealth.OP_SCOPED_CLEANUP);
  }

  @Test
  public void derive_pointRepairWhenFewFailures() {
    var derived = SearchIndexHealthThresholds.derive(0, 100, 3, false);
    assertThat(derived.healthStatus()).isEqualTo(SearchIndexHealth.STATUS_WARNING);
    assertThat(derived.recommendedOp()).isEqualTo(SearchIndexHealth.OP_POINT_REPAIR);
  }

  @Test
  public void derive_notHealthyOnHighLag() {
    var derived = SearchIndexHealthThresholds.derive(3600, 0, 0, false);
    assertThat(derived.healthStatus()).isEqualTo(SearchIndexHealth.STATUS_NOT_HEALTHY);
    assertThat(derived.recommendedOp()).isEqualTo(SearchIndexHealth.OP_SCOPED_CLEANUP);
  }

  @Test
  public void derive_rebuildInProgressTakesPrecedence() {
    var derived = SearchIndexHealthThresholds.derive(7200, 200_000, 500, true);
    assertThat(derived.healthStatus()).isEqualTo(SearchIndexHealth.STATUS_REBUILD_IN_PROGRESS);
    assertThat(derived.recommendedOp()).isEqualTo(SearchIndexHealth.OP_NONE);
  }
}
