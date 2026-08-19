/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.util.List;

import jakarta.mail.Message;

import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.tag.Tag;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional assertion methods specific to our entities.
 */
public class Assert
{
  public static void assertProprietaryConfig(ProprietaryConfig expected, ProprietaryConfig actual) {
    assertThat(expected.getOwnerId()).isEqualTo(actual.getOwnerId());
    assertThat(expected.getId()).isEqualTo(actual.getId());
    assertThat(expected.getPackages()).isEqualTo(actual.getPackages());
    assertThat(expected.getRegexes()).isEqualTo(actual.getRegexes());
  }

  public static void assertTag(Tag expected, Tag actual) {
    assertThat(actual.getOrganizationId()).isEqualTo(expected.getOrganizationId());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getNameLowercaseNoWhitespace()).isEqualTo(expected.getNameLowercaseNoWhitespace());
    assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
    assertThat(actual.getColor()).isEqualTo(expected.getColor());
  }

  public static void assertNotifications(
      List<Message> notifications,
      int notificationCount,
      long timeoutMillisecs) throws InterruptedException
  {
    if (notificationCount == 0) {
      Thread.sleep(timeoutMillisecs);
      assertThat(notifications).hasSize(notificationCount);
      return;
    }

    long start = System.currentTimeMillis();
    do {
      if (notifications.size() == notificationCount) {
        System.out.println("Found " + notificationCount + " expected notifications in "
            + (System.currentTimeMillis() - start) + " ms");
        return;
      }
      Thread.sleep(50);
    }
    while (System.currentTimeMillis() - start <= timeoutMillisecs);
    assertThat(notifications)
        .as("Not found " + notificationCount + " notifications after " + (System.currentTimeMillis() - start) + " ms")
        .hasSize(notificationCount);
  }
}
