/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

import java.util.UUID;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

public class EventTestUtils
{
  public static SourceControlEvent createEvent() {
    return new SourceControlEvent().withId(UUID.randomUUID().toString()).setApplicationId(UUID.randomUUID().toString());
  }

  public static SourceControlEvent createEventForApp(String applicationId) {
    return new SourceControlEvent().withId(UUID.randomUUID().toString()).setApplicationId(applicationId);
  }
}
