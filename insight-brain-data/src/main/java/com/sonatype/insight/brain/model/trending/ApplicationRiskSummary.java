/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

/**
 * Total numbers of policy violations in a particular application.
 * 
 * @since 1.7
 */
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

  /**
   * Returns application name
   * 
   * @since 1.7
   */
  public String getName() {
    return name;
  }

  /**
   * Returns total number of critical policy violations in the application.
   * 
   * @since 1.7
   */
  public int getCritical() {
    return critical;
  }

  /**
   * Returns total number of severe policy violations in the application.
   * 
   * @since 1.7
   */
  public int getSevere() {
    return severe;
  }

  /**
   * Returns total number of moderate policy violations in the application.
   * 
   * @since 1.7
   */
  public int getModerate() {
    return moderate;
  }

  /**
   * Returns total number of other policy violations in the application.
   * 
   * @since 1.7
   */
  public int getNone() {
    return none;
  }

  /**
   * Returns overall application policy violations risk. The returned value is meant to compare relative risk of
   * different applications.
   * 
   * @since 1.7
   */
  public int getRisk() {
    return (critical * 100) + (severe * 20) + (moderate * 5);
  }
}
