/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

public class ComponentsSummary
{
  private int exact;
  private int partial;
  private int unknown;

  public ComponentsSummary() {
  }

  public ComponentsSummary(int exact, int partial, int unknown) {
    this.exact = exact;
    this.partial = partial;
    this.unknown = unknown;
  }

  public int getExact() {
    return exact;
  }

  public int getPartial() {
    return partial;
  }

  public int getUnknown() {
    return unknown;
  }

  public int getInApplication() {
    return exact + partial + unknown;
  }
}
