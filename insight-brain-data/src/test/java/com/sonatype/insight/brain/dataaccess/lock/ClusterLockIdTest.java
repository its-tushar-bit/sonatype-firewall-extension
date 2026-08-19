/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClusterLockIdTest
{
  @Test
  public void testForSchemaMigration() {
    ClusterLockId clusterLockId = ClusterLockId.forSchemaMigration();
    assertThat(clusterLockId).isNotNull();
  }

  @Test
  public void testForDataMigration() {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    assertThat(clusterLockId).isNotNull();
  }

  @Test
  public void testForNewInstancePopulation() {
    ClusterLockId clusterLockId = ClusterLockId.forNewInstancePopulation();
    assertThat(clusterLockId).isNotNull();
  }

  @Test
  public void testForInactiveRepositoryViolationCleaner() {
    ClusterLockId clusterLockId = ClusterLockId.forInactiveRepositoryViolationCleaner();
    assertThat(clusterLockId).isNotNull();
  }

  @Test
  public void testForPolicyViolations() {
    ClusterLockId clusterLockId = ClusterLockId.forPolicyViolations("applicationId");
    assertThat(clusterLockId).isNotNull();
  }

  @Test
  public void testForPolicyViolations_NullParameter() {
    assertThatThrownBy(() -> ClusterLockId.forPolicyViolations(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testForPolicyViolationAggregations() {
    ClusterLockId clusterLockId = ClusterLockId.forPolicyViolationAggregations("applicationId");
    assertThat(clusterLockId).isNotNull();
  }

  @Test
  public void testForPolicyViolationAggregations_NullParameter() {
    assertThatThrownBy(() -> ClusterLockId.forPolicyViolationAggregations(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testForRepositoryComponent() {
    ClusterLockId clusterLockId = ClusterLockId.forRepositoryComponent("repositoryId", "componentId");
    assertThat(clusterLockId).isNotNull();
  }

  @Test
  public void testForRepositoryComponent_NullRepositoryId() {
    assertThatThrownBy(() -> ClusterLockId.forRepositoryComponent(null, "componentId"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testForRepositoryComponent_NullComponentId() {
    assertThatThrownBy(() -> ClusterLockId.forRepositoryComponent("repositoryId", null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testForRepositoryReevaluation() {
    ClusterLockId clusterLockId = ClusterLockId.forRepositoryReevaluation("repositoryId");
    assertThat(clusterLockId).isNotNull();
  }

  @Test
  public void testForRepositoryReevaluation_NullParameter() {
    assertThatThrownBy(() -> ClusterLockId.forRepositoryReevaluation(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testForPolicyEvaluation() {
    ClusterLockId clusterLockId = ClusterLockId.forPolicyEvaluation("applicationId", "scanId");
    assertThat(clusterLockId).isNotNull();
  }

  @Test
  public void testForPolicyEvaluation_NullApplicationId() {
    assertThatThrownBy(() -> ClusterLockId.forPolicyEvaluation(null, "scanId"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testForPolicyEvaluation_NullScanId() {
    assertThatThrownBy(() -> ClusterLockId.forPolicyEvaluation("applicationId", null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testForAuditJsonFileStore() {
    ClusterLockId clusterLockId = ClusterLockId.forAuditJsonFileStore("ownerId");
    assertThat(clusterLockId).isNotNull();
  }

  @Test
  public void testForAuditJsonFileStore_NullParameter() {
    assertThatThrownBy(() -> ClusterLockId.forAuditJsonFileStore(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testForPdfGeneration() {
    ClusterLockId clusterLockId = ClusterLockId.forPdfGeneration("applicationId", "scanId");
    assertThat(clusterLockId).isNotNull();
  }

  @Test
  public void testForPdfGeneration_NullApplicationId() {
    assertThatThrownBy(() -> ClusterLockId.forPdfGeneration(null, "scanId"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testForPdfGeneration_NullScanId() {
    assertThatThrownBy(() -> ClusterLockId.forPdfGeneration("applicationId", null))
        .isInstanceOf(NullPointerException.class);
  }
}
