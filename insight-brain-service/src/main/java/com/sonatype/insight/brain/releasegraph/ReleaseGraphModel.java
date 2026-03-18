/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.sonatype.insight.brain.model.ComponentPopularity;

public class ReleaseGraphModel
{
  public static final int SLOTS = 50;

  private final int[] slotIndices;

  private final int[] popularity;

  private final int currentVersionIndex;

  private final int mostPopularVersionIndex;

  public ReleaseGraphModel(int[] slotIndices, int[] popularity, int currentVersionIndex, int mostPopularVersionIndex) {
    this.slotIndices = slotIndices;
    this.popularity = popularity;
    this.currentVersionIndex = currentVersionIndex;
    this.mostPopularVersionIndex = mostPopularVersionIndex;
  }

  public int[] getSlotIndices() {
    return slotIndices;
  }

  public int getCurrentVersionIndex() {
    return currentVersionIndex;
  }

  public int getMostPopularVersionIndex() {
    return mostPopularVersionIndex;
  }

  public int getMostRecentVersionIndex() {
    return getPopularity().length - 1;
  }

  public int[] getPopularity() {
    return popularity;
  }

  /*
   * Slots should be >=3
   */
  public static ReleaseGraphModel build(ComponentPopularity model, long startTime, long endTime, int slots) {
    final long period = endTime - startTime + 1;
    final double minDiff = ((double) period) / slots;
    int[] slotIndices = new int[slots];
    Arrays.fill(slotIndices, -1);

    ReleaseGraphModel pop = new ReleaseGraphModel(slotIndices, model.getPopularity(), model.getCurrentVersionIndex(),
        getMostPopularIndex(model.getPopularity()));

    @SuppressWarnings("unchecked")
    List<Integer>[] buckets = new List[slots];
    long[] catalogDates = model.getCatalogDates();
    for (int i = 0; i < catalogDates.length; i++) {
      add(i, (int) (((double) catalogDates[i] - startTime) / minDiff), buckets);
    }

    for (int i = 0; i < buckets.length; i++) {
      if (buckets[i] == null) {
        continue;
      }
      if (buckets[i].size() > 1) {
        if (i == 0) // first bucket
        {
          Iterator<Integer> iter = buckets[i].iterator();
          int mostPopulous = -1;
          while (iter.hasNext()) {
            Integer candidate = iter.next();
            if (isImportant(candidate, pop)) {
              mostPopulous = candidate;
              // bump upward
              int counter = 0;
              while (iter.hasNext()) {
                candidate = iter.next();
                if (isImportant(candidate, pop)) {
                  add(candidate, counter++, i + 1, buckets);
                }
              }
            }
            else {
              mostPopulous =
                  mostPopulous != -1 && pop.popularity[mostPopulous] > pop.popularity[candidate]
                      ? mostPopulous
                      : candidate;
            }
          }
          slotIndices[i] = mostPopulous;
        }
        else if (i == buckets.length - 1) // last bucket
        {
          int[] importantVersions = findImportantVersions(buckets[i], pop);
          if (importantVersions[1] == -1) // 0-1 important version
          {
            // choose important or most populous
            slotIndices[i] = getMostPopulousOrImportant(buckets[i].iterator(), pop);
          }
          else if (importantVersions[2] == -1) // 2 important versions
          {
            if (isImportant(slotIndices[i - 1], pop)) {
              // previous slot is also important, push it down
              slotIndices[i - 2] = slotIndices[i - 1];
            }
            slotIndices[i - 1] = importantVersions[0];
            slotIndices[i] = importantVersions[1];
          }
          else {
            // 3 important versions
            slotIndices[i - 2] = importantVersions[0];
            slotIndices[i - 1] = importantVersions[1];
            slotIndices[i] = importantVersions[2];
          }
        }
        else {
          int[] importantVersions = findImportantVersions(buckets[i], pop);
          if (importantVersions[1] == -1) // 0-1 important version
          {
            // choose important or most populous
            slotIndices[i] = getMostPopulousOrImportant(buckets[i].iterator(), pop);
          }
          else if (importantVersions[2] == -1) // 2 important versions
          {
            // contains 2, if either side already contains important, push other direction
            // Next pile already contains an important item, or the previous pile does not contain an
            // important item and the second important item i
            if (containsImportant(buckets[i + 1], pop)
                || (importantVersions[1] == pop.currentVersionIndex && !containsImportant(buckets[i - 1], pop)))
            {
              // push first down
              slotIndices[i - 1] = importantVersions[0];
              slotIndices[i] = importantVersions[1];
            }
            else {
              // push second up
              slotIndices[i] = importantVersions[0];
              add(importantVersions[1], 0, i + 1, buckets);
            }
          }
          else {
            // 3 important versions
            slotIndices[i - 1] = importantVersions[0];
            slotIndices[i] = importantVersions[1];
            slotIndices[i + 1] = importantVersions[2];
          }
        }
      }
      else if (buckets[i].size() == 1) {
        slotIndices[i] = buckets[i].get(0);
      }
    }
    return pop;
  }

  private static void add(Integer item, int bucketIndex, List<Integer>[] buckets) {
    if (buckets[bucketIndex] == null) {
      buckets[bucketIndex] = new LinkedList<>();
    }
    buckets[bucketIndex].add(item);
  }

  private static void add(Integer item, int position, int bucketIndex, List<Integer>[] buckets) {
    if (buckets[bucketIndex] == null) {
      buckets[bucketIndex] = new LinkedList<>();
    }
    buckets[bucketIndex].add(position, item);
  }

  private static int getMostPopulousOrImportant(Iterator<Integer> iter, ReleaseGraphModel pop) {
    int mostPopulous = -1;
    while (iter.hasNext()) {
      Integer candidate = iter.next();
      if (isImportant(candidate, pop)) {
        return candidate;
      }
      else {
        mostPopulous = mostPopulous != -1 && pop.popularity[mostPopulous] > pop.popularity[candidate]
            ? mostPopulous
            : candidate;
      }
    }
    return mostPopulous;
  }

  private static boolean containsImportant(List<Integer> bucket, ReleaseGraphModel model) {
    if (bucket == null) {
      return false;
    }
    for (Integer i : bucket) {
      if (isImportant(i, model)) {
        return true;
      }
    }
    return false;
  }

  /*
   * Find the most popular index
   */
  private static int getMostPopularIndex(int[] popularity) {
    int maxPopularity = -1;
    int mostPopularIndex = -1;
    for (int i = 0; i < popularity.length; i++) {
      if (maxPopularity < popularity[i]) {
        maxPopularity = popularity[i];
        mostPopularIndex = i;
      }
    }
    return mostPopularIndex;
  }

  private static int[] findImportantVersions(List<Integer> bucket, ReleaseGraphModel model) {
    int[] importantVersions = new int[3];
    Arrays.fill(importantVersions, -1);
    int index = 0;
    for (Integer candidate : bucket) {
      if (isImportant(candidate, model)) {
        importantVersions[index++] = candidate;
        if (index == 3) {
          break;
        }
      }
    }
    return importantVersions;
  }

  /*
   * Determine if the release is important
   */
  private static boolean isImportant(int index, ReleaseGraphModel pop) {
    return index == pop.getCurrentVersionIndex() || index == pop.getMostPopularVersionIndex()
        || index == pop.getMostRecentVersionIndex();
  }
}
