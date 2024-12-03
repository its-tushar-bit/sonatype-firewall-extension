/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClusterLockIdTest
{
  @Test
  public void testForSchemaMigration() {
    ClusterLockId clusterLockId = ClusterLockId.forSchemaMigration();
    assertThat(clusterLockId).isNotNull();
    assertThat(clusterLockId.getOldStyleLockId()).isEqualTo("schema-migration");
  }

  @Test
  public void testForSchemaMigrationInProgress() {
    ClusterLockId clusterLockId = ClusterLockId.forSchemaMigrationInProgress();
    assertThat(clusterLockId).isNotNull();
    assertThat(clusterLockId.getOldStyleLockId()).isEqualTo("schema-migration-in-progress");
  }

  @Test
  public void testForDataMigration() {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    assertThat(clusterLockId).isNotNull();
    assertThat(clusterLockId.getOldStyleLockId()).isEqualTo("data-migration");
  }

  @Test
  public void testForNewInstancePopulation() {
    ClusterLockId clusterLockId = ClusterLockId.forNewInstancePopulation();
    assertThat(clusterLockId).isNotNull();
    assertThat(clusterLockId.getOldStyleLockId()).isEqualTo("new-instance-population");
  }

  @Test
  public void testForInactiveRepositoryViolationCleaner() {
    ClusterLockId clusterLockId = ClusterLockId.forInactiveRepositoryViolationCleaner();
    assertThat(clusterLockId).isNotNull();
    assertThat(clusterLockId.getOldStyleLockId()).isEqualTo("inactive-repository-violation-cleaner");
  }

  @Test
  public void testForPolicyViolations() {
    ClusterLockId clusterLockId = ClusterLockId.forPolicyViolations("applicationId");
    assertThat(clusterLockId).isNotNull();
    assertThat(clusterLockId.getOldStyleLockId()).isEqualTo("policy-violations-applicationId");
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
    assertThat(clusterLockId.getOldStyleLockId()).isEqualTo("policy-violation-aggregations-applicationId");
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
    assertThat(clusterLockId.getOldStyleLockId()).isEqualTo("repository-component-repositoryId-componentId");
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
  public void testPrefixForRepositoryComponents() {
    String prefix = ClusterLockId.prefixForRepositoryComponents("repositoryId");
    assertThat(prefix).isEqualTo("repository-component-repositoryId-");
  }

  @Test
  public void testPrefixForRepositoryComponents_NullParameter() {
    assertThatThrownBy(() -> ClusterLockId.prefixForRepositoryComponents(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testForRepositoryReevaluation() {
    ClusterLockId clusterLockId = ClusterLockId.forRepositoryReevaluation("repositoryId");
    assertThat(clusterLockId).isNotNull();
    assertThat(clusterLockId.getOldStyleLockId()).isEqualTo("repository-reevaluation-repositoryId");
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
    assertThat(clusterLockId.getOldStyleLockId()).isEqualTo("policy-evaluation-applicationId-scanId");
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
  public void testPrefixForPolicyEvaluations() {
    String prefix = ClusterLockId.prefixForPolicyEvaluations("applicationId");
    assertThat(prefix).isEqualTo("policy-evaluation-applicationId-");
  }

  @Test
  public void testPrefixForPolicyEvaluations_NullParameter() {
    assertThatThrownBy(() -> ClusterLockId.prefixForPolicyEvaluations(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testForAuditJsonFileStore() {
    ClusterLockId clusterLockId = ClusterLockId.forAuditJsonFileStore("ownerId");
    assertThat(clusterLockId).isNotNull();
    assertThat(clusterLockId.getOldStyleLockId()).isEqualTo("audit-json-file-store-ownerId");
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
    assertThat(clusterLockId.getOldStyleLockId()).isEqualTo("pdf-generation-applicationId-scanId");
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

  @Test
  public void testPrefixForPdfGeneration() {
    String prefix = ClusterLockId.prefixForPdfGeneration("applicationId");
    assertThat(prefix).isEqualTo("pdf-generation-applicationId-");
  }

  @Test
  public void testPrefixForPdfGeneration_NullParameter() {
    assertThatThrownBy(() -> ClusterLockId.prefixForPdfGeneration(null))
        .isInstanceOf(NullPointerException.class);
  }
}
