/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;

public class PolicyAlertSourceCodeOrganizer
{
  /**
   * Returns a sorted Map for all notifications ready for SCM notification.
   * Note that the keySet is sorted, and the entries in the value list are also sorted.
   */
  public SortedMap<ComponentIdentifier, List<PolicyNotification>> getNotificationsForScm(
      List<PolicyNotification> policyNotifications)
  {
    if (policyNotifications == null) {
      return null;
    }

    SortedMap<ComponentIdentifier, List<PolicyNotification>> componentMap = new TreeMap<>();
    for (PolicyNotification policyNotification : policyNotifications) {
      for (ComponentFact componentFact : policyNotification.getPolicyFact().getComponentFacts()) {
        ComponentIdentifier componentIdentifier = componentFact.getComponentIdentifier();
        componentMap.computeIfAbsent(componentIdentifier, k -> new ArrayList<>());
        componentMap.get(componentIdentifier).add(policyNotification);
      }
    }

    // sort all lists by priority order
    Comparator<PolicyNotification> notificationComparator =
        Comparator.comparing(policyNotification -> policyNotification.getPolicyFact().getThreatLevel());
    for (List<PolicyNotification> componentPolicyNotifications : componentMap.values()) {
      Collections.sort(componentPolicyNotifications, notificationComparator.reversed());
    }

    return componentMap;
  }
}
