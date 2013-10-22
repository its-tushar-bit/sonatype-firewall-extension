/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

public class ApplicationRiskSummary
{
  private String name;
  private int critical;
  private int severe;
  private int moderate;
  private int none;

  public ApplicationRiskSummary() {
  }

  public ApplicationRiskSummary(String name, int critical, int severe, int moderate, int none) {
    this.name = name;
    this.critical = critical;
    this.severe = severe;
    this.moderate = moderate;
    this.none = none;
  }

  public String getName() {
    return name;
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

  public int getRisk() {
    return critical * 100 + severe * 10 + moderate;
  }
}
