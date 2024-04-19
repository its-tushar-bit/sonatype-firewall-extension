/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is essentially a copy of the example on
 * https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/PriorityBlockingQueue.html and allows
 * first-in-first-out tie-breaking to comparable elements
 */
public class FIFOEntry<E extends Comparable<? super E>>
    implements Comparable<FIFOEntry<E>>
{
  private static final AtomicLong seq = new AtomicLong(0);

  private final long seqNum;

  private final E entry;

  public FIFOEntry(final E entry) {
    seqNum = seq.getAndIncrement();
    this.entry = entry;
  }

  public E getEntry() {
    return entry;
  }

  @Override
  public int compareTo(final FIFOEntry<E> other) {
    if (other == null) {
      return -1;
    }
    if (this == other) {
      return 0;
    }
    E thisEntry = getEntry();
    E otherEntry = other.getEntry();
    if (thisEntry == null && otherEntry == null) {
      return 0;
    }
    if (otherEntry == null) {
      return -1;
    }
    if (thisEntry == null) {
      return 1;
    }
    int entryComparison = thisEntry.compareTo(otherEntry);
    if (entryComparison != 0) {
      return entryComparison;
    }
    return Long.compare(seqNum, other.seqNum);
  }
}
