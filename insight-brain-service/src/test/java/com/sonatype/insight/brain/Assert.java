/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.util.List;

import javax.mail.Message;

import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.tag.Tag;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Additional assertion methods specific to our entities.
 */
public class Assert
{
  public static void assertProprietaryConfig(ProprietaryConfig expected, ProprietaryConfig actual) {
    assertThat(expected.getOwnerId(), is(actual.getOwnerId()));
    assertThat(expected.getId(), is(actual.getId()));
    assertThat(expected.getPackages(), is(actual.getPackages()));
    assertThat(expected.getRegexes(), is(actual.getRegexes()));
  }

  public static void assertTag(Tag expected, Tag actual) {
    assertThat(actual.getOrganizationId(), is(expected.getOrganizationId()));
    assertThat(actual.getName(), is(expected.getName()));
    assertThat(actual.getNameLowercaseNoWhitespace(), is(expected.getNameLowercaseNoWhitespace()));
    assertThat(actual.getDescription(), is(expected.getDescription()));
    assertThat(actual.getColor(), is(expected.getColor()));
  }

  public static void assertNotifications(List<Message> notifications, int notificationCount, long timeoutMillisecs)
      throws InterruptedException
  {
    if (notificationCount == 0) {
      Thread.sleep(timeoutMillisecs);
      assertThat(notifications, hasSize(notificationCount));
      return;
    }

    long start = System.currentTimeMillis();
    do {
      if (notifications.size() == notificationCount) {
        return;
      }
      Thread.sleep(50);
    }
    while (System.currentTimeMillis() - start <= timeoutMillisecs);
    assertThat("Not found " + notificationCount + " notifications after " + (System.currentTimeMillis() - start)
        + " ms", notifications, hasSize(notificationCount));
  }
}
