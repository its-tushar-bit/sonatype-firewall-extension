/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FIFOEntryTest
{
  @Test
  public void testCompareTo_SameObject() {
    FIFOEntry<Integer> fifoEntry1 = new FIFOEntry<>(1);

    assertThat(fifoEntry1.compareTo(fifoEntry1)).isZero();
  }

  @Test
  public void testCompareTo_EntriesHaveSameOrder() {
    FIFOEntry<Integer> fifoEntry1 = new FIFOEntry<>(1);
    FIFOEntry<Integer> fifoEntry2 = new FIFOEntry<>(1);

    assertThat(fifoEntry1.compareTo(fifoEntry2)).isEqualTo(-1);
    assertThat(fifoEntry2.compareTo(fifoEntry1)).isEqualTo(1);
  }

  @Test
  public void testCompareTo_EntriesHaveDifferentOrder() {
    FIFOEntry<Integer> fifoEntry1 = new FIFOEntry<>(1);
    FIFOEntry<Integer> fifoEntry2 = new FIFOEntry<>(2);

    assertThat(fifoEntry1.compareTo(fifoEntry2)).isEqualTo(-1);
    assertThat(fifoEntry2.compareTo(fifoEntry1)).isEqualTo(1);
  }

  @Test
  public void testCompareTo_NullLast() {
    FIFOEntry<Integer> fifoEntry1 = new FIFOEntry<>(1);

    assertThat(fifoEntry1.compareTo(null)).isEqualTo(-1);
  }

  @Test
  public void testCompareTo_NullEntryLast() {
    FIFOEntry<Integer> fifoEntry1 = new FIFOEntry<>(1);
    FIFOEntry<Integer> fifoEntry2 = new FIFOEntry<>(null);

    assertThat(fifoEntry1.compareTo(fifoEntry2)).isEqualTo(-1);
    assertThat(fifoEntry2.compareTo(fifoEntry1)).isEqualTo(1);
  }

  @Test
  public void testCompareTo_BothEntriesNull() {
    FIFOEntry<Integer> fifoEntry1 = new FIFOEntry<>(null);
    FIFOEntry<Integer> fifoEntry2 = new FIFOEntry<>(null);

    assertThat(fifoEntry1.compareTo(fifoEntry2)).isZero();
  }
}
