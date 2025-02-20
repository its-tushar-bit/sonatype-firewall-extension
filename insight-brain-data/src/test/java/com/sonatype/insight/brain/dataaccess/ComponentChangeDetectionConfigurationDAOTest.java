/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.ComponentChangeDetectionConfigurationDAO.ComponentChangeConfiguration;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentChangeDetectionConfigurationDAOTest
{
  private ComponentChangeDetectionConfigurationDAO underTest;

  @Before
  public void setUp() {
    underTest = new ComponentChangeDetectionConfigurationDAO();
  }

  @Test
  public void test_CanAddToTable() {
    assertThat(underTest.getCount()).isEqualTo(0);

    underTest.addComponents(2, List.of(
        new ComponentChangeConfiguration("purl1", null, DateTime.now()),
        new ComponentChangeConfiguration("purl2", null, DateTime.now()))
    );
    assertThat(underTest.getCount()).isEqualTo(2);
  }

  @Test
  public void test_CanAddToTableOldRemoved() {
    DateTime now = DateTime.now();

    underTest.addComponents(2, List.of(
        new ComponentChangeConfiguration("purl1", null, now.minusSeconds(10)),
        new ComponentChangeConfiguration("purl2", null, now.minusSeconds(9)))
    );
    assertThat(underTest.getCount()).isEqualTo(2);

    List<ComponentChangeConfiguration> response = underTest.addComponents(2, List.of(
        new ComponentChangeConfiguration("purl3", null, now)
    ));
    assertThat(response.size()).isEqualTo(1);
    assertThat(response.get(0).purl()).isEqualTo("purl1");

    // The oldest component was removed
    assertThat(underTest.getCount()).isEqualTo(2);
    assertThat(underTest.getComponents(1, 100).stream().map(ComponentChangeConfiguration::purl))
        .containsExactly("purl2", "purl3");
  }

  @Test
  public void test_CanUpdateHashOfAComponent() {
    underTest.addComponents(10, List.of(
        new ComponentChangeConfiguration("purl1", null, DateTime.now()),
        new ComponentChangeConfiguration("purl2", null, DateTime.now()),
        new ComponentChangeConfiguration("purl3", null, DateTime.now()))
    );

    underTest.updateHashOfPurl("purl2", "newHash");
    assertThat(underTest.getComponents(1, 100).stream()
        .filter(item -> item.purl().equals("purl2"))
        .findFirst()
        .map(ComponentChangeConfiguration::comparisonHash)
        .orElse(null))
        .isEqualTo("newHash");
  }

  @Test
  public void test_CanGetByPage() {
    DateTime now = DateTime.now();
    underTest = new ComponentChangeDetectionConfigurationDAO();

    underTest.addComponents(
        100, List.of(
            new ComponentChangeConfiguration("purl1", null, now.minusSeconds(10)),
            new ComponentChangeConfiguration("purl2", null, now.minusSeconds(9)),
            new ComponentChangeConfiguration("purl3", null, now.minusSeconds(8)),
            new ComponentChangeConfiguration("purl4", null, now.minusSeconds(7)),
            new ComponentChangeConfiguration("purl5", null, now.minusSeconds(6)),
            new ComponentChangeConfiguration("purl6", null, now.minusSeconds(5))
        )
    );

    assertThat(underTest.getComponents(2, 2).stream().map(ComponentChangeConfiguration::purl))
        .containsExactly("purl3", "purl4");
  }

  @Test
  public void testGetComponentsInBatches_ConsumeAll() {
    final int BATCH_SIZE = 4;

    DateTime now = DateTime.now();
    underTest.addComponents(
        100, List.of(
            new ComponentChangeConfiguration("purl1", null, now.minusSeconds(10)),
            new ComponentChangeConfiguration("purl2", null, now.minusSeconds(9)),
            new ComponentChangeConfiguration("purl3", null, now.minusSeconds(8)),
            new ComponentChangeConfiguration("purl4", null, now.minusSeconds(7)),
            new ComponentChangeConfiguration("purl5", null, now.minusSeconds(6)),
            new ComponentChangeConfiguration("purl6", null, now.minusSeconds(5)),
            new ComponentChangeConfiguration("purl7", null, now.minusSeconds(4)),
            new ComponentChangeConfiguration("purl8", null, now.minusSeconds(3)),
            new ComponentChangeConfiguration("purl9", null, now.minusSeconds(2)),
            new ComponentChangeConfiguration("purl10", null, now.minusSeconds(1))
        )
    );

    int continuationToken = 0;
    List<ComponentChangeConfiguration> components = underTest.getComponentsInBatches(BATCH_SIZE, continuationToken);
    while (!components.isEmpty()) {
      continuationToken += BATCH_SIZE;
      components = underTest.getComponentsInBatches(BATCH_SIZE, continuationToken);
    }
    assertThat(continuationToken).isEqualTo(12);
  }

  @Test
  public void testGetComponentsInBatches_WithOddBatchSize() {
    final int BATCH_SIZE = 3;

    DateTime now = DateTime.now();
    underTest.addComponents(
        100, List.of(
            new ComponentChangeConfiguration("purl1", null, now.minusSeconds(10)),
            new ComponentChangeConfiguration("purl2", null, now.minusSeconds(9)),
            new ComponentChangeConfiguration("purl3", null, now.minusSeconds(8)),
            new ComponentChangeConfiguration("purl4", null, now.minusSeconds(7)),
            new ComponentChangeConfiguration("purl5", null, now.minusSeconds(6)),
            new ComponentChangeConfiguration("purl6", null, now.minusSeconds(5)),
            new ComponentChangeConfiguration("purl7", null, now.minusSeconds(4)),
            new ComponentChangeConfiguration("purl8", null, now.minusSeconds(3)),
            new ComponentChangeConfiguration("purl9", null, now.minusSeconds(2)),
            new ComponentChangeConfiguration("purl10", null, now.minusSeconds(1))
        )
    );

    int continuationToken = 0;
    List<ComponentChangeConfiguration> components = underTest.getComponentsInBatches(BATCH_SIZE, continuationToken);
    while (!components.isEmpty()) {
      continuationToken += BATCH_SIZE;
      components = underTest.getComponentsInBatches(BATCH_SIZE, continuationToken);
    }
    assertThat(continuationToken).isEqualTo(12);
  }

  @Test
  public void testUpdateHashAndVersionOfPurl() {
    underTest.addComponents(10, List.of(
        new ComponentChangeConfiguration("purl1", null, DateTime.now()),
        new ComponentChangeConfiguration("purl2", null, DateTime.now()),
        new ComponentChangeConfiguration("purl3", null, DateTime.now()),
        new ComponentChangeConfiguration("purl4", null, DateTime.now()))
    );

    underTest.updateHashAndVersionOfPurl("purl4", "newHash", "2.0");
    Optional<ComponentChangeConfiguration> componentChangeConfiguration = underTest.getComponents(1, 100).stream()
        .filter(item -> item.purl().equals("purl4"))
        .findFirst();
    assertThat(componentChangeConfiguration).isPresent();
    assertThat(componentChangeConfiguration.get().comparisonHash()).isEqualTo("newHash");
    assertThat(componentChangeConfiguration.get().version()).isEqualTo("2.0");
  }
}
