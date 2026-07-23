/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.util.List;

import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlPlanEvidenceSupport.CaptureSource.RECONSTRUCTED;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlPlanEvidenceSupport.Metric.APPLICATIONS;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlPlanEvidenceSupport.Metric.ORGANIZATIONS;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlPlanEvidenceSupport.Metric.POLICIES;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlPlanEvidenceSupport.Metric.VIOLATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DashboardMetricsSqlPlanEvidenceSupportTest
{
  @Test
  public void selectedIndexIdentityDoesNotAffectClassification() {
    List<String> plan = List.of(
        "Index Scan using another_valid_index on policy_violation",
        "  Buffers: shared hit=21",
        "Planning Time: 0.036 ms",
        "Execution Time: 0.019 ms");

    assertThat(DashboardMetricsSqlPlanEvidenceSupport.assess(VIOLATIONS, plan).classification()).isEqualTo("GREEN");
  }

  @Test
  public void absentHeapFetchCounterIsReportedAsNotAvailable() {
    assertThat(DashboardMetricsSqlPlanEvidenceSupport.heapFetchEvidence(List.of("Seq Scan on application")))
        .isEqualTo("Heap Fetches: N/A (not reported by PostgreSQL)");
  }

  @Test
  public void reconstructedSqlCannotBePresentedAsRuntimeCapture() {
    var reconstructed = new DashboardMetricsSqlPlanEvidenceSupport.CapturedStatement(
        "select count(*) from application",
        List.of(),
        RECONSTRUCTED);

    assertThatThrownBy(() -> DashboardMetricsSqlPlanEvidenceSupport.requireRuntimeCapture(reconstructed))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("runtime JDBC capture");
  }

  @Test
  public void metricThresholdsAreFixedBeforeEvidenceExecution() {
    assertThat(ORGANIZATIONS.executionTargetMillis()).isEqualTo(50.0);
    assertThat(POLICIES.executionTargetMillis()).isEqualTo(50.0);
    assertThat(APPLICATIONS.executionTargetMillis()).isEqualTo(200.0);
    assertThat(VIOLATIONS.executionTargetMillis()).isEqualTo(500.0);
  }

  @Test
  public void executionAboveMetricThresholdIsRed() {
    List<String> plan = List.of(
        "Seq Scan on application",
        "  Buffers: shared hit=21",
        "Planning Time: 0.036 ms",
        "Execution Time: 200.001 ms");

    assertThat(DashboardMetricsSqlPlanEvidenceSupport.assess(APPLICATIONS, plan).classification()).isEqualTo("RED");
  }
}
