/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import com.sonatype.clm.dto.model.policy.PolicyFact;

/**
 * Summarizes the policy alert counts by threat-level color.
 *
 * Needs to be public with getters to play nice with ftl.
 *
 * @since 1.21.0
 */
public class PolicyAlertCounts
{
  private int red;

  private int orange;

  private int yellow;

  private int darkBlue;

  private int blue;

  public PolicyAlertCounts(final int red, final int orange, final int yellow, final int darkBlue, final int blue) {
    this.red = red;
    this.orange = orange;
    this.yellow = yellow;
    this.darkBlue = darkBlue;
    this.blue = blue;
  }

  public PolicyAlertCounts(final List<PolicyFact> facts) {
    for (PolicyFact fact : facts) {
      int level = fact.getThreatLevel();
      int components = fact.getComponentFacts().size();

      if (level > 7) {
        red += components;
      }
      else if (level > 3) {
        orange += components;
      }
      else if (level > 1) {
        yellow += components;
      }
      else if (level == 1) {
        darkBlue += components;
      }
      else {
        blue += components;
      }
    }
  }

  public int getRed() {
    return red;
  }

  public int getOrange() {
    return orange;
  }

  public int getYellow() {
    return yellow;
  }

  public int getDarkBlue() {
    return darkBlue;
  }

  public int getBlue() {
    return blue;
  }

  public int getTotal() {
    return getRed() + getOrange() + getYellow() + getDarkBlue() + getBlue();
  }
}
