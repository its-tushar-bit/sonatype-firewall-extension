/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.policy.PolicyAlert;

/**
 * Carries the results from {@link PolicyEvaluator}.
 * 
 * @since 1.9
 */
public class PolicyResults
{
  private List<PolicyAlert> activeAlerts = Collections.emptyList();

  private List<PolicyAlert> waivedAlerts = Collections.emptyList();

  /**
   * Gets the alerts that have not been waived.
   */
  public List<PolicyAlert> getActiveAlerts() {
    return activeAlerts;
  }

  void setActiveAlerts(List<PolicyAlert> activeAlerts) {
    this.activeAlerts = (activeAlerts != null) ? activeAlerts : Collections.<PolicyAlert> emptyList();
  }

  /**
   * Gets the alerts that have been waived.
   */
  public List<PolicyAlert> getWaivedAlerts() {
    return waivedAlerts;
  }

  void setWaivedAlerts(List<PolicyAlert> waivedAlerts) {
    this.waivedAlerts = (waivedAlerts != null) ? waivedAlerts : Collections.<PolicyAlert> emptyList();
  }
}
