/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;

public class PolicyAlertSourceCodeOrganizer
{
  private static final int INITIAL_DEPTH = 0;

  private final Comparator<PolicyNotification> threatLevelComparator =
      Comparator
          .comparing((PolicyNotification policyNotification) -> policyNotification.getPolicyFact().getThreatLevel())
          .thenComparing(policyNotification -> policyNotification.getPolicyFact().getPolicyId());

  /**
   * Returns a map of all policy notifications, aggregated by component and sorted by policy threats.
   * Note that the map itself is sorted, and also that the entries in the value list are also sorted.
   */
  public Map<ComponentIdentifier, List<PolicyNotification>> getNotificationsForScm(
      final List<PolicyNotification> policyNotifications)
  {
    if (policyNotifications == null) {
      return null;
    }

    // Aggregate components
    Map<ComponentIdentifier, Set<PolicyNotification>> componentMap = new HashMap<>();
    for (PolicyNotification policyNotification : policyNotifications) {
      for (ComponentFact componentFact : policyNotification.getPolicyFact().getComponentFacts()) {
        ComponentIdentifier componentIdentifier = componentFact.getComponentIdentifier();
        if (componentIdentifier == null) {
          continue;
        }
        componentMap.computeIfAbsent(componentIdentifier, k -> new TreeSet<>(threatLevelComparator.reversed()));
        componentMap.get(componentIdentifier).add(policyNotification);
      }
    }

    // Finally, re-sort the components by highest threats within the policies, and then the component name
    return componentMap.entrySet()
        .stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> (List<PolicyNotification>) new ArrayList<>(entry.getValue())))
        .entrySet()
        .stream()
        .sorted(this::compareEntries)
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
  }

  private int compareEntries(
      final Entry<ComponentIdentifier, List<PolicyNotification>> entry1,
      final Entry<ComponentIdentifier, List<PolicyNotification>> entry2)
  {
    int listCompare = comparePolicyViolationThreats(entry1.getValue(), entry2.getValue(), INITIAL_DEPTH);
    // If the policy notification lists are equal, fall back to a name comparison
    return listCompare == 0 ? nameCompare(entry1.getKey(), entry2.getKey()) : listCompare;
  }

  /**
   * Compare two policy notification lists and sort by highest threat level at each depth. i.e. A '10 10' will sort
   * ahead of a '10 9' which will sort ahead of a '10'
   */
  private int comparePolicyViolationThreats(
      final List<PolicyNotification> policyNotifications1,
      final List<PolicyNotification> policyNotifications2,
      final int depth)
  {
    // Short-circuit if the depths don't match
    if (policyNotifications1.size() == depth && policyNotifications2.size() >= depth) {
      return checkDepth(policyNotifications1, policyNotifications2, depth, 1);
    }
    if (policyNotifications2.size() <= depth && policyNotifications1.size() >= depth) {
      return checkDepth(policyNotifications1, policyNotifications2, depth, -1);
    }

    int threatLevel1 = policyNotifications1.get(depth).getPolicyFact().getThreatLevel();
    int threatLevel2 = policyNotifications2.get(depth).getPolicyFact().getThreatLevel();

    return threatLevel1 > threatLevel2
        ? -1
        : threatLevel1 < threatLevel2
            ? 1
            : comparePolicyViolationThreats(policyNotifications1, policyNotifications2, depth + 1);
  }

  /**
   * Compare by component id, but without the format
   */
  private int nameCompare(
      final ComponentIdentifier componentIdentifier1,
      final ComponentIdentifier componentIdentifier2)
  {
    return componentNameWithoutFormat(componentIdentifier1).compareTo(componentNameWithoutFormat(componentIdentifier2));
  }

  private String componentNameWithoutFormat(final ComponentIdentifier componentIdentifier) {
    return String.join(":", componentIdentifier.getCoordinates().values());
  }

  private int checkDepth(
      final List<PolicyNotification> policyNotifications1,
      final List<PolicyNotification> policyNotifications2,
      final int depth,
      final int defaultValue)
  {
    // if both lists are the same size, they are equal
    if (policyNotifications1.size() == depth && policyNotifications2.size() == depth) {
      return 0;
    }
    else {
      return defaultValue;
    }
  }
}
