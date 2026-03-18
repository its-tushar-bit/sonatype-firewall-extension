/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createNugetCoordinates;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyAlertSourceCodeOrganizerTest
{
  private static final String CRITICAL = "Severity-Critical: 9";

  private static final String MEDIUM = "Severity-Medium: 5";

  private static final String LOW = "Severity-Low: 2";

  private static final ComponentFact EGGS = createComponentFact("Eggs", "1.2.3");

  private static final ComponentFact BACON = createComponentFact("Bacon", "2.0.1");

  private static final ComponentFact PANCAKES = createComponentFact("Pancakes", "0.2.1");

  private static final ComponentFact TOAST = createComponentFact("Toast", "5.7.0");

  private static final ComponentFact SAUSAGE = createComponentFact("Sausage", "3.2.1");

  private static final ComponentFact HASH_BROWNS = createComponentFact("Hash-Browns", "2.5");

  private static final ComponentFact SYRUP = createComponentFact("Syrup", "1.5.8");

  private static final ComponentFact SANDWICH = createMavenComponentFact("Sandwich", "0.8.2");

  private PolicyFact critical1;

  private PolicyFact critical2;

  private PolicyFact medium;

  private PolicyFact low;

  private PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer;

  private static String getComponentId(ComponentIdentifier componentIdentifier) {
    return String.join(":", componentIdentifier.getCoordinates().values());
  }

  private static String getComponentId(ComponentFact componentFact) {
    return getComponentId(componentFact.getComponentIdentifier());
  }

  @Before
  public void setup() {
    policyAlertSourceCodeOrganizer = new PolicyAlertSourceCodeOrganizer();

    critical1 = new PolicyFact("policyid-2", "Severity-Critical", 9);
    critical2 = new PolicyFact("policyid-2b", "Severity-Critical", 9);
    medium = new PolicyFact("policyid-3", "Severity-Medium", 5);
    low = new PolicyFact("policyid-1", "Severity-Low", 2);
  }

  @Test
  public void testGetNotificationsForScm__singleComponent_singleViolation() {
    medium.addComponentFact(EGGS);

    Map<ComponentIdentifier, List<PolicyNotification>> results = runTest();

    validateComponents(results, EGGS);
    validateComponentsPolicies(results, EGGS, new String[]{MEDIUM});
  }

  @Test
  public void testGetNotificationsForScm__singleComponent_multipleViolation() {
    medium.addComponentFact(EGGS);
    critical1.addComponentFact(EGGS);

    Map<ComponentIdentifier, List<PolicyNotification>> results = runTest();

    validateComponents(results, EGGS);
    validateComponentsPolicies(results, EGGS, new String[]{CRITICAL, MEDIUM});
  }

  @Test
  public void testGetNotificationsForScm__multipleComponents_singleViolation() {
    medium.addComponentFact(PANCAKES);
    medium.addComponentFact(EGGS);
    medium.addComponentFact(BACON);

    Map<ComponentIdentifier, List<PolicyNotification>> results = runTest();

    validateComponents(results, BACON, EGGS, PANCAKES);
    validateComponentsPolicies(results, EGGS, new String[]{MEDIUM});
    validateComponentsPolicies(results, BACON, new String[]{MEDIUM});
    validateComponentsPolicies(results, PANCAKES, new String[]{MEDIUM});
  }

  @Test
  public void testGetNotificationsForScm__singleUnknownComponent_singleViolation() {
    // 'component unknown' violation is a null ComponentIdentifier
    critical1.addComponentFact(new ComponentFact(null, UUID.randomUUID().toString()));

    Map<ComponentIdentifier, List<PolicyNotification>> results = runTest();

    assertThat(results).isEmpty();
  }

  private static ComponentFact createMavenComponentFact(final String name, final String version) {
    return new ComponentFact(createMavenCoordinates("groupid", name, version), UUID.randomUUID().toString());
  }

  @Test
  public void testGetNotificationsForScm__multipleComponents_multipleViolations() {
    // Bacon - critical & critical
    // Toast - critical & low
    // Sausage - critical
    // Eggs - medium & low
    // Pancakes - medium
    // Syrup - medium (Syrup should sort alphabetically after Pancakes)
    // Hashbrowns - low

    critical1.addComponentFact(BACON);
    critical1.addComponentFact(BACON); // model second CVE for same component and same policy
    critical1.addComponentFact(TOAST);
    critical1.addComponentFact(SAUSAGE);

    critical2.addComponentFact(BACON);

    medium.addComponentFact(EGGS);
    medium.addComponentFact(SYRUP);
    medium.addComponentFact(PANCAKES);

    low.addComponentFact(EGGS);
    low.addComponentFact(TOAST);
    low.addComponentFact(HASH_BROWNS);

    Map<ComponentIdentifier, List<PolicyNotification>> results = runTest();

    validateComponents(results, BACON, TOAST, SAUSAGE, EGGS, PANCAKES, SYRUP, HASH_BROWNS);
    validateComponentsPolicies(results, BACON, new String[]{CRITICAL, CRITICAL});
    validateComponentsPolicies(results, TOAST, new String[]{CRITICAL, LOW});
    validateComponentsPolicies(results, SAUSAGE, new String[]{CRITICAL});
    validateComponentsPolicies(results, EGGS, new String[]{MEDIUM, LOW});
    validateComponentsPolicies(results, PANCAKES, new String[]{MEDIUM});
    validateComponentsPolicies(results, SYRUP, new String[]{MEDIUM});
    validateComponentsPolicies(results, HASH_BROWNS, new String[]{LOW});
  }

  @Test
  public void testGetNotificationsForScm__obeyFormats() {
    critical1.addComponentFact(EGGS); // nuget
    critical1.addComponentFact(TOAST); // nuget
    critical1.addComponentFact(SANDWICH); // maven
    // so if format is ignored the resulting order should be: eggs, sandwich, toast

    Map<ComponentIdentifier, List<PolicyNotification>> results = runTest();

    validateComponents(results, EGGS, SANDWICH, TOAST);
    validateComponentsPolicies(results, EGGS, new String[]{CRITICAL});
    validateComponentsPolicies(results, SANDWICH, new String[]{CRITICAL});
    validateComponentsPolicies(results, TOAST, new String[]{CRITICAL});
  }

  private Map<ComponentIdentifier, List<PolicyNotification>> runTest() {
    Notifications notifications = new Notifications(new UserNotification("foo@mail.com", "release"));
    PolicyNotification policyNotificationMid = new PolicyNotification(medium, notifications);
    PolicyNotification policyNotificationCritical = new PolicyNotification(critical1, notifications);
    PolicyNotification policyNotificationCritical2 = new PolicyNotification(critical2, notifications);
    PolicyNotification policyNotificationLow = new PolicyNotification(low, notifications);
    List<PolicyNotification> policyNotifications = Arrays.asList(
        policyNotificationMid, policyNotificationCritical, policyNotificationCritical2, policyNotificationLow);

    return policyAlertSourceCodeOrganizer.getNotificationsForScm(policyNotifications);
  }

  private void validateComponentsPolicies(
      Map<ComponentIdentifier, List<PolicyNotification>> sortedNotifications,
      ComponentFact componentFact,
      String[] matchedPolicies)
  {
    assertThat(sortedNotifications.get(componentFact.getComponentIdentifier())
        .stream()
        .map(PolicyNotification::getPolicyFact)
        .map(policyFact -> String.format("%s: %d", policyFact.getPolicyName(), policyFact.getThreatLevel()))
        .collect(Collectors.toList()))
            .containsExactly(matchedPolicies);
  }

  private static ComponentFact createComponentFact(final String name, final String version) {
    return new ComponentFact(createNugetCoordinates(name, version), UUID.randomUUID().toString());
  }

  private void validateComponents(
      final Map<ComponentIdentifier, List<PolicyNotification>> results,
      final ComponentFact... componentFacts)
  {
    String[] packages = Arrays.stream(componentFacts)
        .map(PolicyAlertSourceCodeOrganizerTest::getComponentId)
        .toArray(String[]::new);

    assertThat(results.keySet())
        .extracting(PolicyAlertSourceCodeOrganizerTest::getComponentId)
        .containsExactly(packages);
  }
}
