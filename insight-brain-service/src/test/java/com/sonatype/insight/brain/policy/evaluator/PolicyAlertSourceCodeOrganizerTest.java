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

    PolicyFact policyFact1 = new PolicyFact("policyid-1", "policyname-1", 3);
    policyFact1.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("Package1", "1.2.3"), randomString()));
    policyFact1.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("Package2", "1.3.4"), randomString()));
    policyFact1.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("AA-Package", "1.0.1"), randomString()));

    PolicyFact policyFact2 = new PolicyFact("policyid-2", "policyname-2", 3);
    policyFact2.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("Package1-2", "1.2.3"), randomString()));
    policyFact2.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("Package2-2", "1.3.4"), randomString()));
    policyFact2.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("AA-Package-2", "1.0.1"), randomString()));

    Notifications notifications = new Notifications(
        new UserNotification("foo@mail.com", "release")
    );
    PolicyNotification policyNotification1 = new PolicyNotification(policyFact1, notifications);
    PolicyNotification policyNotification2 = new PolicyNotification(policyFact2, notifications);
    List<PolicyNotification> policyNotifications = Arrays.asList(
        policyNotification1, policyNotification2
    );

    Map<ComponentIdentifier, List<PolicyNotification>> sortedNotifications =
        policyAlertSourceCodeOrganizer.getNotificationsForScm(policyNotifications);

    assertThat(sortedNotifications.keySet()).extracting(componentIdentifier -> componentIdentifier.get("packageId"))
        .containsExactly("AA-Package", "AA-Package-2", "Package1", "Package1-2", "Package2", "Package2-2");
    assertThat(sortedNotifications.keySet()).extracting(componentIdentifier -> componentIdentifier.get("packageId"))
        .isSorted();

    assertThat(sortedNotifications.get(ComponentIdentifier.createNugetCoordinates("AA-Package", "1.0.1")))
        .containsExactly(policyNotification1);
  }

  private String randomString() {
    return UUID.randomUUID().toString();
  }
}
