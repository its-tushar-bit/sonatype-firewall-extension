/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;
import java.util.Comparator;
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

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyAlertSourceCodeOrganizerTest
{
  private PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer;

  @Before
  public void setup() {
    policyAlertSourceCodeOrganizer = new PolicyAlertSourceCodeOrganizer();
  }

  @Test
  public void testGetNotificationsForScm__basic() {
    PolicyFact policyFactMid = new PolicyFact("policyid-1", "Mid", 5);
    policyFactMid.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("Package1", "1.2.3"), randomString()));
    policyFactMid.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("Package2", "1.3.4"), randomString()));
    policyFactMid.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("AA-Package", "1.0.1"), randomString()));

    PolicyFact policyFactCritical = new PolicyFact("policyid-2", "Critical", 9);
    policyFactCritical.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("Package1-2", "1.2.3"), randomString()));
    policyFactCritical.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("Package2-2", "1.3.4"), randomString()));
    policyFactCritical.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("Package2", "1.3.4"), randomString()));
    policyFactCritical.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("AA-Package-2", "1.0.1"), randomString()));

    // components appear in multiple policies with different threat levels
    PolicyFact policyFactLow = new PolicyFact("policyid-3", "Low", 1);
    policyFactLow.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("Package1", "1.2.3"), randomString()));
    policyFactLow.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("AA-Package", "1.0.1"), randomString()));

    Notifications notifications = new Notifications(
        new UserNotification("foo@mail.com", "release")
    );
    PolicyNotification policyNotificationMid = new PolicyNotification(policyFactMid, notifications);
    PolicyNotification policyNotificationCritical = new PolicyNotification(policyFactCritical, notifications);
    PolicyNotification policyNotificationLow = new PolicyNotification(policyFactLow, notifications);
    List<PolicyNotification> policyNotifications = Arrays.asList(
        policyNotificationMid, policyNotificationCritical, policyNotificationLow
    );

    Map<ComponentIdentifier, List<PolicyNotification>> sortedNotifications =
        policyAlertSourceCodeOrganizer.getNotificationsForScm(policyNotifications);

    assertThat(sortedNotifications.keySet()).extracting(componentIdentifier -> componentIdentifier.get("packageId"))
        .containsExactly("AA-Package", "AA-Package-2", "Package1", "Package1-2", "Package2", "Package2-2");
    assertThat(sortedNotifications.keySet()).extracting(componentIdentifier -> componentIdentifier.get("packageId"))
        .isSorted();
    Comparator<PolicyNotification> notificationComparator =
        Comparator.comparing(policyNotification -> policyNotification.getPolicyFact().getThreatLevel());
    for (List<PolicyNotification> resultNotificationList : sortedNotifications.values()) {
      assertThat(resultNotificationList).isSortedAccordingTo(notificationComparator.reversed());
    }

    validateComponentsPolicies(sortedNotifications, ComponentIdentifier.createNugetCoordinates("Package1", "1.2.3"),
        new String[]{"Mid: 5", "Low: 1"});
    validateComponentsPolicies(sortedNotifications, ComponentIdentifier.createNugetCoordinates("Package2", "1.3.4"),
        new String[]{"Critical: 9", "Mid: 5"});
    validateComponentsPolicies(sortedNotifications, ComponentIdentifier.createNugetCoordinates("AA-Package", "1.0.1"),
        new String[]{"Mid: 5", "Low: 1"});
    validateComponentsPolicies(sortedNotifications, ComponentIdentifier.createNugetCoordinates("Package1-2", "1.2.3"),
        new String[]{"Critical: 9"});
    validateComponentsPolicies(sortedNotifications, ComponentIdentifier.createNugetCoordinates("Package2-2", "1.3.4"),
        new String[]{"Critical: 9"});
    validateComponentsPolicies(sortedNotifications, ComponentIdentifier.createNugetCoordinates("AA-Package-2", "1.0.1"),
        new String[]{"Critical: 9"});

    assertThat(sortedNotifications.get(ComponentIdentifier.createNugetCoordinates("AA-Package-2", "1.0.1")))
        .containsExactly(policyNotificationCritical);
  }

  private void validateComponentsPolicies(Map<ComponentIdentifier, List<PolicyNotification>> sortedNotifications,
                                          ComponentIdentifier componentIdentifier,
                                          String[] matchedPolicies)
  {
    assertThat(sortedNotifications.get(componentIdentifier)
        .stream().map(policyNotification -> policyNotification.getPolicyFact())
        .map(policyFact -> String.format("%s: %d", policyFact.getPolicyName(), policyFact.getThreatLevel()))
        .collect(Collectors.toList()))
        .containsExactly(matchedPolicies);
  }

  private String randomString() {
    return UUID.randomUUID().toString();
  }
}
