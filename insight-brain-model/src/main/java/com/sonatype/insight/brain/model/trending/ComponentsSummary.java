/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

/**
 * Summary information about components identified in all applications
 * 
 * @since 1.7
 */
public class ComponentsSummary
{
  private int exact;
  private int partial;
  private int unknown;
  private int proprietary;

  public ComponentsSummary() {
  }

  public ComponentsSummary(int exact, int partial, int unknown, int proprietary) {
    this.exact = exact;
    this.partial = partial;
    this.unknown = unknown;
    this.proprietary = proprietary;
  }

  /**
   * Returns number of exactly matched components.
   * 
   * @since 1.7
   */
  public int getExact() {
    return exact;
  }

  /**
   * Returns number of partially matched components.
   * 
   * @since 1.7
   */
  public int getPartial() {
    return partial;
  }

  /**
   * Returns number of unknown components.
   * 
   * @since 1.7
   */
  public int getUnknown() {
    return unknown;
  }

  /**
   * Returns total number of proprietary components
   * @since 1.8
   */
  public int getProprietary() {
    return proprietary;
  }

  /**
   * Returns total number of components.
   *
   * @since 1.7
   */
  public int getInApplication() {
    return exact + partial + unknown + proprietary;
  }
}
