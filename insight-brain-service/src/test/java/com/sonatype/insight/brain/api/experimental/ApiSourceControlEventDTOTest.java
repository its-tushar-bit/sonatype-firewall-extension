/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Date;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSourceControlEventDTOTest
{
  @Test
  public void testConvert_EventCompleted() {
    SourceControlEvent sourceControlEvent = new SourceControlEvent();
    sourceControlEvent.setId("eventId");
    sourceControlEvent.setScmUsername("scmUsername");
    sourceControlEvent.setApplicationId("dtoTest");
    sourceControlEvent.setEventPriority(1);
    sourceControlEvent.setEventStatus("eventStatus");
    sourceControlEvent.setEventStatusDetails("eventStatusDetails");
    sourceControlEvent.setEventErrorDetails("eventErrorDetails");
    sourceControlEvent.setEventType("eventType");
    long now = System.currentTimeMillis();
    sourceControlEvent.setCreateTime(new Date(now - 10000));
    sourceControlEvent.setStartTime(new Date(now - 5000));
    sourceControlEvent.setCompleteTime(new Date(now - 1000));

    ApiSourceControlEventDTO actual = ApiSourceControlEventAdapterDTO.convert(sourceControlEvent);

    assertThat(actual.getId()).isEqualTo(sourceControlEvent.getId());
    assertThat(actual.getUser()).isEqualTo(sourceControlEvent.getScmUsername());
    assertThat(actual.getApplicationId()).isEqualTo(sourceControlEvent.getApplicationId());
    assertThat(actual.getType()).isEqualTo(sourceControlEvent.getEventType());
    assertThat(actual.getPriority()).isEqualTo(sourceControlEvent.getEventPriority());
    assertThat(actual.getStatus()).isEqualTo(sourceControlEvent.getEventStatus());
    assertThat(actual.getCreateTime()).isEqualTo(sourceControlEvent.getCreateTime());
    assertThat(actual.getStartTime()).isEqualTo(sourceControlEvent.getStartTime());
    assertThat(actual.getCompleteTime()).isEqualTo(sourceControlEvent.getCompleteTime());
    assertThat(actual.getTimeWaiting()).isEqualTo(5000);
    assertThat(actual.getTimeExecuting()).isEqualTo(4000);
  }

  @Test
  public void testConvert_EventHasNotStarted() {
    SourceControlEvent sourceControlEvent = new SourceControlEvent();

    ApiSourceControlEventDTO actual = ApiSourceControlEventAdapterDTO.convert(sourceControlEvent);

    assertThat(actual.getTimeWaiting()).isNull();
    assertThat(actual.getTimeExecuting()).isNull();
  }

  @Test
  public void testConvert_EventHasNotCompleted() {
    long now = System.currentTimeMillis();
    SourceControlEvent sourceControlEvent = new SourceControlEvent();
    sourceControlEvent.setCreateTime(new Date(now - 10000));
    sourceControlEvent.setStartTime(new Date(now - 5000));

    ApiSourceControlEventDTO actual = ApiSourceControlEventAdapterDTO.convert(sourceControlEvent);

    assertThat(actual.getTimeWaiting()).isEqualTo(5000);
    assertThat(actual.getTimeExecuting()).isPositive();
  }
}
