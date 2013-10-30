/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

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

  public int getCritical() {
    return critical;
  }

  public int getSevere() {
    return severe;
  }

  public int getModerate() {
    return moderate;
  }

  public int getNone() {
    return none;
  }

  public int getTotal() {
    return critical + severe + moderate + none;
  }
}
