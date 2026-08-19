/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.sonatype.insight.brain.model.ComponentChangeDetectionConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentChangeDetectionConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private ComponentChangeDetectionConfigurationDAO underTest;

  @BeforeEach
  public void setUp() {
    underTest = daoFactory.createComponentChangeDetectionConfigurationDAO();
  }

  @Test
  public void test_CanAddToTable() {
    assertThat(underTest.getCount()).isEqualTo(0);

    tempEntity.addComponentChangeDetectionConfigurationItems(2, List.of(
        new ComponentChangeDetectionConfiguration("1.0", "purl1", null, null, new Date()),
        new ComponentChangeDetectionConfiguration("1.0", "purl2", null, null, new Date())));

    assertThat(underTest.getCount()).isEqualTo(2);

    tempEntity.addComponentChangeDetectionConfigurationItems(3, List.of(
        new ComponentChangeDetectionConfiguration("1.0", "purl1", null, null, new Date()),
        new ComponentChangeDetectionConfiguration("1.0", "purl3", null, null, new Date()),
        new ComponentChangeDetectionConfiguration("1.0", "purl3", null, null, new Date())));

    assertThat(underTest.getCount()).isEqualTo(3);
  }

  @Test
  public void test_CanAddToTableOldRemoved() {
    tempEntity.addComponentChangeDetectionConfigurationItems(2, List.of(
        new ComponentChangeDetectionConfiguration("1.0", "purl1", null, null, nowMinusSeconds(10)),
        new ComponentChangeDetectionConfiguration("1.0", "purl2", null, null, nowMinusSeconds(9))));
    assertThat(underTest.getCount()).isEqualTo(2);

    List<ComponentChangeDetectionConfiguration> response =
        tempEntity.addComponentChangeDetectionConfigurationItems(2, List.of(
            new ComponentChangeDetectionConfiguration("1.0", "purl3", null, null, new Date())));
    assertThat(response.size()).isEqualTo(1);
    assertThat(response.get(0).getPurl()).isEqualTo("purl1");

    // The oldest component was removed
    assertThat(underTest.getCount()).isEqualTo(2);
    assertThat(underTest.getComponents(1, 100).stream().map(ComponentChangeDetectionConfiguration::getPurl))
        .containsExactly("purl2", "purl3");
  }

  @Test
  public void test_CanUpdateHashOfAComponent() {
    tempEntity.addComponentChangeDetectionConfigurationItems(10, List.of(
        new ComponentChangeDetectionConfiguration("1.0", "purl1", null, null, new Date()),
        new ComponentChangeDetectionConfiguration("1.0", "purl2", null, null, new Date()),
        new ComponentChangeDetectionConfiguration("1.0", "purl3", null, null, new Date())));

    underTest.updateComparisonHashOfPurl("purl2", "newHash");
    assertThat(underTest.getComponents(1, 100)
        .stream()
        .filter(item -> item.getPurl().equals("purl2"))
        .findFirst()
        .map(ComponentChangeDetectionConfiguration::getComparisonHash)
        .orElse(null))
            .isEqualTo("newHash");
  }

  @Test
  public void test_CanGetByPage() {
    tempEntity.addComponentChangeDetectionConfigurationItems(
        100, List.of(
            new ComponentChangeDetectionConfiguration("1.0", "purl1", null, null, nowMinusSeconds(10)),
            new ComponentChangeDetectionConfiguration("1.0", "purl2", null, null, nowMinusSeconds(9)),
            new ComponentChangeDetectionConfiguration("1.0", "purl3", null, null, nowMinusSeconds(8)),
            new ComponentChangeDetectionConfiguration("1.0", "purl4", null, null, nowMinusSeconds(7)),
            new ComponentChangeDetectionConfiguration("1.0", "purl5", null, null, nowMinusSeconds(6)),
            new ComponentChangeDetectionConfiguration("1.0", "purl6", null, null, nowMinusSeconds(5))));

    assertThat(underTest.getComponents(2, 2).stream().map(ComponentChangeDetectionConfiguration::getPurl))
        .containsExactly("purl3", "purl4");
  }

  @Test
  public void testGetComponentsInBatches_ConsumeAll() {
    final int BATCH_SIZE = 4;

    tempEntity.addComponentChangeDetectionConfigurationItems(
        100, List.of(
            new ComponentChangeDetectionConfiguration("1.0", "purl1", null, null, nowMinusSeconds(10)),
            new ComponentChangeDetectionConfiguration("1.0", "purl2", null, null, nowMinusSeconds(9)),
            new ComponentChangeDetectionConfiguration("1.0", "purl3", null, null, nowMinusSeconds(8)),
            new ComponentChangeDetectionConfiguration("1.0", "purl4", null, null, nowMinusSeconds(7)),
            new ComponentChangeDetectionConfiguration("1.0", "purl5", null, null, nowMinusSeconds(6)),
            new ComponentChangeDetectionConfiguration("1.0", "purl6", null, null, nowMinusSeconds(5)),
            new ComponentChangeDetectionConfiguration("1.0", "purl7", null, null, nowMinusSeconds(4)),
            new ComponentChangeDetectionConfiguration("1.0", "purl8", null, null, nowMinusSeconds(3)),
            new ComponentChangeDetectionConfiguration("1.0", "purl9", null, null, nowMinusSeconds(2)),
            new ComponentChangeDetectionConfiguration("1.0", "purl10", null, null, nowMinusSeconds(1))));

    int continuationToken = 0;
    List<ComponentChangeDetectionConfiguration> components =
        underTest.getComponentsInBatches(BATCH_SIZE, continuationToken);
    while (!components.isEmpty()) {
      continuationToken += BATCH_SIZE;
      components = underTest.getComponentsInBatches(BATCH_SIZE, continuationToken);
    }
    assertThat(continuationToken).isEqualTo(12);
  }

  @Test
  public void testGetComponentsInBatches_WithOddBatchSize() {
    final int BATCH_SIZE = 3;
    tempEntity.addComponentChangeDetectionConfigurationItems(
        100, List.of(
            new ComponentChangeDetectionConfiguration("1.0", "purl1", null, null, nowMinusSeconds(10)),
            new ComponentChangeDetectionConfiguration("1.0", "purl2", null, null, nowMinusSeconds(9)),
            new ComponentChangeDetectionConfiguration("1.0", "purl3", null, null, nowMinusSeconds(8)),
            new ComponentChangeDetectionConfiguration("1.0", "purl4", null, null, nowMinusSeconds(7)),
            new ComponentChangeDetectionConfiguration("1.0", "purl5", null, null, nowMinusSeconds(6)),
            new ComponentChangeDetectionConfiguration("1.0", "purl6", null, null, nowMinusSeconds(5)),
            new ComponentChangeDetectionConfiguration("1.0", "purl7", null, null, nowMinusSeconds(4)),
            new ComponentChangeDetectionConfiguration("1.0", "purl8", null, null, nowMinusSeconds(3)),
            new ComponentChangeDetectionConfiguration("1.0", "purl9", null, null, nowMinusSeconds(2)),
            new ComponentChangeDetectionConfiguration("1.0", "purl10", null, null, nowMinusSeconds(1))));

    int continuationToken = 0;
    List<ComponentChangeDetectionConfiguration> components =
        underTest.getComponentsInBatches(BATCH_SIZE, continuationToken);
    while (!components.isEmpty()) {
      continuationToken += BATCH_SIZE;
      components = underTest.getComponentsInBatches(BATCH_SIZE, continuationToken);
    }
    assertThat(continuationToken).isEqualTo(12);
  }

  @Test
  public void testUpdateComparisonHashAndVersionOfPurl() {
    tempEntity.addComponentChangeDetectionConfigurationItems(10, List.of(
        new ComponentChangeDetectionConfiguration("1.0", "purl1", null, null, new Date()),
        new ComponentChangeDetectionConfiguration("1.0", "purl2", null, null, new Date()),
        new ComponentChangeDetectionConfiguration("1.0", "purl3", null, null, new Date()),
        new ComponentChangeDetectionConfiguration("1.0", "purl4", null, null, new Date())));

    underTest.updateComparisonHashAndVersionOfPurl("purl4", "newHash", "2.0");
    Optional<ComponentChangeDetectionConfiguration> componentChangeDetectionConfiguration =
        underTest.getComponents(1, 100)
            .stream()
            .filter(item -> item.getPurl().equals("purl4"))
            .findFirst();
    assertThat(componentChangeDetectionConfiguration).isPresent();
    assertThat(componentChangeDetectionConfiguration.get().getComparisonHash()).isEqualTo("newHash");
    assertThat(componentChangeDetectionConfiguration.get().getVersion()).isEqualTo("2.0");
  }

  private Date nowMinusSeconds(int seconds) {
    return Date.from(Instant.now().minusSeconds(seconds));
  }
}
