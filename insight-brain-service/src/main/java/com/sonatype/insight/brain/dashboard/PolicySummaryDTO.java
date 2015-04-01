/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.List;

/**
 * Carries the data backing the Policy Summary view.
 * 
 * @since 1.11.0
 */
public class PolicySummaryDTO
{
  public long timestamp;

  public int totalNew;
  public int totalWaived;
  public int totalFixed;
  public int currentUnresolved;

  public List<Integer> weeklyDeltaNew = new ArrayList<>();
  public List<Integer> weeklyDeltaWaived = new ArrayList<>();
  public List<Integer> weeklyDeltaFixed = new ArrayList<>();
  public List<Integer> weeklyDeltaUnresolved = new ArrayList<>();

  public long ageAverageWaived;
  public long agePercentile90Waived;
  public long ageAverageFixed;
  public long agePercentile90Fixed;
  public long ageAverageUnresolved;
  public long agePercentile90Unresolved;
}
