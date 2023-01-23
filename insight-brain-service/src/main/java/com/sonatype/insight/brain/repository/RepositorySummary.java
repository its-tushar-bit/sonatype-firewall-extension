/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

public class RepositorySummary
{
  public int knownComponentCount;

  public int totalComponentCount;

  public int criticalViolationCount;

  public int severeViolationCount;

  public int moderateViolationCount;

  public int affectedComponentCount;

  public int quarantinedComponentCount;
}
