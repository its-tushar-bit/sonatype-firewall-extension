/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

/**
 * Total numbers of policy violations.
 * 
 * @since 1.7
 */
public class PoliciesSummary
{
  private int critical;
  private int severe;
  private int moderate;
  private int none;

  public PoliciesSummary() {
  }

  public PoliciesSummary(int critical, int severe, int moderate, int none) {
    this.critical = critical;
    this.severe = severe;
    this.moderate = moderate;
    this.none = none;
  }

  /**
   * Returns number of critical policy violations
   * 
   * @since 1.7
   */
  public int getCritical() {
    return critical;
  }

  /**
   * Returns number of severe policy violations
   * 
   * @since 1.7
   */
  public int getSevere() {
    return severe;
  }

  /**
   * Returns number of moderate policy violations
   * 
   * @since 1.7
   */
  public int getModerate() {
    return moderate;
  }

  /**
   * Returns number of other policy violations
   * 
   * @since 1.7
   */
  public int getNone() {
    return none;
  }

  public int getTotal() {
    return critical + severe + moderate + none;
  }
}
