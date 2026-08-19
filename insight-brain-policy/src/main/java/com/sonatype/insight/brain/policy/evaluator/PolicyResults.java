/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;

/**
 * Carries the results from {@link ComponentPolicyEvaluator}.
 *
 * @since 1.9
 */
public class PolicyResults
{
  private List<PolicyAlert> activeAlerts = new ArrayList<>();

  private List<PolicyNotification> activeNotifications = new ArrayList<>();

  private List<PolicyAlert> waivedAlerts = new ArrayList<>();

  private Map<ComponentFact, PolicyWaiver> policyWaiversByComponentFacts = new HashMap<>();

  /**
   * Gets the alerts that have not been waived.
   */
  public List<PolicyAlert> getActiveAlerts() {
    return activeAlerts;
  }

  void addActiveAlert(PolicyAlert activeAlert) {
    activeAlerts.add(activeAlert);
  }

  /**
   * Gets the alerts that have been waived.
   */
  public List<PolicyAlert> getWaivedAlerts() {
    return waivedAlerts;
  }

  void addWaivedAlert(PolicyAlert waivedAlert) {
    waivedAlerts.add(waivedAlert);
  }

  public List<PolicyNotification> getActiveNotifications() {
    return activeNotifications;
  }

  void addActiveNotification(PolicyNotification activeNotification) {
    activeNotifications.add(activeNotification);
  }

  void addPolicyWaiver(ComponentFact componentFact, PolicyWaiver policyWaiver) {
    policyWaiversByComponentFacts.put(componentFact, policyWaiver);
  }

  /**
   * Gets the policy waiver for a waived component fact.
   */
  public PolicyWaiver getPolicyWaiver(ComponentFact componentFact) {
    return policyWaiversByComponentFacts.get(componentFact);
  }
}
