/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import com.sonatype.insight.brain.model.ComponentPopularity;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.releasegraph.ReleaseGraphModel.SLOTS;
import static org.assertj.core.api.Assertions.assertThat;

public class ReleaseGraphModelTest
{
  @Test
  public void test3ImportantVersionsInLastBucket() {
    long[] catalogDates = new long[]{100, 100, 100};
    int[] popularity = new int[]{98, 100, 99};

    ReleaseGraphModel model = ReleaseGraphModel.build(buildGavPopularity(catalogDates, popularity, 0), 0, 100, SLOTS);
    assertThat(model.getMostPopularVersionIndex()).isEqualTo(1);
    assertThat(model.getMostRecentVersionIndex()).isEqualTo(2);
    assertThat(model.getSlotIndices()[SLOTS - 1]).isEqualTo(2);
    assertThat(model.getSlotIndices()[SLOTS - 2]).isEqualTo(1);
    assertThat(model.getSlotIndices()[SLOTS - 3]).isEqualTo(0);
  }

  @Test
  public void test3ImportantVersionsInFirstBucket() {
    long[] catalogDates = new long[]{0, 0, 0};
    int[] popularity = new int[]{98, 100, 99};

    ReleaseGraphModel model = ReleaseGraphModel.build(buildGavPopularity(catalogDates, popularity, 0), 0, 100, SLOTS);
    assertThat(model.getMostPopularVersionIndex()).isEqualTo(1);
    assertThat(model.getMostRecentVersionIndex()).isEqualTo(2);
    assertThat(model.getSlotIndices()[0]).isEqualTo(0);
    assertThat(model.getSlotIndices()[1]).isEqualTo(1);
    assertThat(model.getSlotIndices()[2]).isEqualTo(2);
  }

  @Test
  public void test3ImportantVersionsInteriorBucket() {
    long[] catalogDates = new long[]{5, 5, 5};
    int[] popularity = new int[]{98, 100, 99};

    ReleaseGraphModel model = ReleaseGraphModel.build(buildGavPopularity(catalogDates, popularity, 0), 0, 100, SLOTS);
    assertThat(model.getMostPopularVersionIndex()).isEqualTo(1);
    assertThat(model.getMostRecentVersionIndex()).isEqualTo(2);
    assertThat(model.getSlotIndices()[1]).isEqualTo(0);
    assertThat(model.getSlotIndices()[2]).isEqualTo(1);
    assertThat(model.getSlotIndices()[3]).isEqualTo(2);
  }

  @Test
  public void test2First1SecondBucket() {
    // This tests that the value is pushed backwards
    long[] catalogDates = new long[]{0, 1, 4};
    int[] popularity = new int[]{98, 100, 99};

    ReleaseGraphModel model = ReleaseGraphModel.build(buildGavPopularity(catalogDates, popularity, 0), 0, 100, SLOTS);
    assertThat(model.getMostPopularVersionIndex()).isEqualTo(1);
    assertThat(model.getMostRecentVersionIndex()).isEqualTo(2);
    assertThat(model.getSlotIndices()[0]).isEqualTo(0);
    assertThat(model.getSlotIndices()[1]).isEqualTo(1);
    assertThat(model.getSlotIndices()[2]).isEqualTo(2);
  }

  @Test
  public void testLastPushDown() {
    // Push down into unoccupied
    long[] catalogDates = new long[]{97, 100, 100};
    int[] popularity = new int[]{98, 100, 99};

    ReleaseGraphModel model = ReleaseGraphModel.build(buildGavPopularity(catalogDates, popularity, 0), 0, 100, SLOTS);
    assertThat(model.getMostPopularVersionIndex()).isEqualTo(1);
    assertThat(model.getMostRecentVersionIndex()).isEqualTo(2);
    assertThat(model.getSlotIndices()[SLOTS - 1]).isEqualTo(2);
    assertThat(model.getSlotIndices()[SLOTS - 2]).isEqualTo(1);
    assertThat(model.getSlotIndices()[SLOTS - 3]).isEqualTo(0);

    // Push down into occupied interesting
    catalogDates = new long[]{97, 100, 100};
    popularity = new int[]{98, 100, 99};

    model = ReleaseGraphModel.build(buildGavPopularity(catalogDates, popularity, 0), 0, 100, SLOTS);
    assertThat(model.getMostPopularVersionIndex()).isEqualTo(1);
    assertThat(model.getMostRecentVersionIndex()).isEqualTo(2);
    assertThat(model.getSlotIndices()[SLOTS - 1]).isEqualTo(2);
    assertThat(model.getSlotIndices()[SLOTS - 2]).isEqualTo(1);
    assertThat(model.getSlotIndices()[SLOTS - 3]).isEqualTo(0);

    // Push down into occupied uninteresting
    catalogDates = new long[]{0, 97, 100, 100};
    popularity = new int[]{50, 9, 100, 99};

    model = ReleaseGraphModel.build(buildGavPopularity(catalogDates, popularity, 0), 0, 100, SLOTS);
    assertThat(model.getMostPopularVersionIndex()).isEqualTo(2);
    assertThat(model.getMostRecentVersionIndex()).isEqualTo(3);
    assertThat(model.getSlotIndices()[SLOTS - 1]).isEqualTo(3);
    assertThat(model.getSlotIndices()[SLOTS - 2]).isEqualTo(2);
    assertThat(model.getSlotIndices()[SLOTS - 3]).isEqualTo(-1);
  }

  @Test
  public void testPushDown() {
    // next box has interesting, we
    // Push down into unoccupied
    long[] catalogDates = new long[]{95, 95, 97};
    int[] popularity = new int[]{98, 100, 99};

    ReleaseGraphModel model = ReleaseGraphModel.build(buildGavPopularity(catalogDates, popularity, 0), 0, 100, SLOTS);
    assertThat(model.getMostPopularVersionIndex()).isEqualTo(1);
    assertThat(model.getMostRecentVersionIndex()).isEqualTo(2);
    assertThat(model.getSlotIndices()[SLOTS - 2]).isEqualTo(2);
    assertThat(model.getSlotIndices()[SLOTS - 3]).isEqualTo(1);
    assertThat(model.getSlotIndices()[SLOTS - 4]).isEqualTo(0);
  }

  @Test
  public void testPushUpMiddle() {
    // Current has 2, uninteresting up, down
    long[] catalogDates = new long[]{95, 95, 100};
    int[] popularity = new int[]{98, 100, 99};

    ReleaseGraphModel model = ReleaseGraphModel.build(buildGavPopularity(catalogDates, popularity, 0), 0, 100, SLOTS);
    assertThat(model.getMostPopularVersionIndex()).isEqualTo(1);
    assertThat(model.getMostRecentVersionIndex()).isEqualTo(2);
    assertThat(model.getSlotIndices()[SLOTS - 1]).isEqualTo(2);
    assertThat(model.getSlotIndices()[SLOTS - 2]).isEqualTo(1);
    assertThat(model.getSlotIndices()[SLOTS - 3]).isEqualTo(0);
  }

  @Test
  public void testMostPopularChosen() {
    long[] catalogDates = new long[]{0, 3, 50, 50, 100};
    int[] popularity = new int[]{98, 100, 50, 25, 99};

    ReleaseGraphModel model = ReleaseGraphModel.build(buildGavPopularity(catalogDates, popularity, 0), 0, 100, SLOTS);
    assertThat(model.getMostPopularVersionIndex()).isEqualTo(1);
    assertThat(model.getMostRecentVersionIndex()).isEqualTo(4);
    assertThat(model.getSlotIndices()[24]).isEqualTo(2);
  }

  private static ComponentPopularity buildGavPopularity(
      long[] catalogDates,
      int[] popularity,
      int currentVersionIndex)
  {
    ComponentPopularity gav = new ComponentPopularity();
    gav.setCatalogDates(catalogDates);
    gav.setPopularity(popularity);
    gav.setCurrentVersionIndex(currentVersionIndex);
    return gav;
  }
}
