/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceControlEventPublisherTest
{
  @Mock
  private SourceControlEventDAO mockSourceControlEventDAO;

  private SourceControlEventPublisher sourceControlEventPublisher;

  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @Mock
  private ApiConfigFeaturesService mockApiConfigFeaturesService;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    sourceControlEventPublisher = new SourceControlEventPublisher(mockSourceControlEventDAO, mockSourceControlUtils,
        mockApiConfigFeaturesService);
  }

  @Test
  public void testPublishEvent_licensedFeature() {
    // given:
    when(mockSourceControlUtils.getScmUserIdForApplication(any())).thenReturn("scmUser");
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);

    // when: publish a null event
    sourceControlEventPublisher.publishEvent(null);

    // then: nothing saved to DB
    verify(mockSourceControlEventDAO, never()).insert(any());

    // and when: publish an event
    final String appId = "xyz-012";
    SourceControlEvent event = new SourceControlEvent().setApplicationId(appId);
    sourceControlEventPublisher.publishEvent(event);

    // then: DAO tries to save event
    ArgumentCaptor<SourceControlEvent> eventCaptor = ArgumentCaptor.forClass(SourceControlEvent.class);
    verify(mockSourceControlEventDAO, times(1)).insert(eventCaptor.capture());
    verify(mockSourceControlUtils, times(1)).getScmUserIdForApplication(anyString());
    SourceControlEvent persistedEvent = eventCaptor.getValue();
    assertThat(persistedEvent.getApplicationId()).isEqualTo(appId);

    // and: scm user for event is updated
    assertThat(persistedEvent.getScmUsername()).isEqualTo("scmUser");
  }

  @Test
  public void testPublishEvent_whenNull() {
    // given:
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);

    // when: publish a null event
    sourceControlEventPublisher.publishEvent(null);

    // then: nothing saved to DB
    verify(mockSourceControlEventDAO, never()).insert(any());
  }

  @Test
  public void testPublishEvent_whenFeatureIsNotEnabled() {
    // given:
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(false);

    // and when: publish an event
    final String appId = "xyz-012";
    SourceControlEvent event = new SourceControlEvent().setApplicationId(appId);
    sourceControlEventPublisher.publishEvent(event);

    // then: nothing saved to DB
    verify(mockSourceControlEventDAO, never()).insert(any());
  }

  @Test
  public void testDoesRemediationEventExistForBranch() {
    // given: DAO setup for scenarios
    when(mockSourceControlEventDAO.hasRemediationEventForBranch(any(), eq("no"))).thenReturn(false);
    when(mockSourceControlEventDAO.hasRemediationEventForBranch(any(), eq("yes"))).thenReturn(true);

    // then:
    assertThat(sourceControlEventPublisher.doesRemediationEventExistForBranch("app1", "no")).isFalse();
    assertThat(sourceControlEventPublisher.doesRemediationEventExistForBranch("app2", "yes")).isTrue();
  }

  @Test
  public void testClearEventsForApplicationAndPublishEvent() {
    // when: clear existing and publish an event
    final String appId = "xyz-012";
    SourceControlEvent event = new SourceControlEvent().setApplicationId(appId);
    sourceControlEventPublisher.clearEventsForApplicationAndPublishEvent(event);

    // then: DAO tries to clear events and save event
    ArgumentCaptor<SourceControlEvent> eventCaptor = ArgumentCaptor.forClass(SourceControlEvent.class);
    verify(mockSourceControlEventDAO, times(1)).clearEventsAndInsert(eventCaptor.capture());
    SourceControlEvent persistedEvent = eventCaptor.getValue();
    assertThat(persistedEvent.getApplicationId()).isEqualTo(appId);
  }
}
