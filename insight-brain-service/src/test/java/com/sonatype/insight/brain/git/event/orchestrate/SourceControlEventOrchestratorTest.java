/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceControlEventOrchestratorTest
{
  @Mock
  private InsightConfig mockInsightConfig;

  @Mock
  private SourceControlEventDAO mockSourceControlEventDAO;

  @Mock
  private SourceControlEventProcessor mockSourceControlEventProcessor;

  @Mock
  private SourceControlEventPublisher mockSourceControlEventPublisher;

  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testOnNewEvent_differentUsersDifferentListeners() {
    // given: an orchestrator and two events for different scm users
    SourceControlEventOrchestrator sourceControlEventOrchestrator = new SourceControlEventOrchestrator(
        mockInsightConfig, mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlEventPublisher,
        mockSourceControlUtils
    );

    SourceControlEvent user1Event =
        new SourceControlEvent().forRemediationPullRequest().withId("user1Event").setScmUsername("user1")
            .setApplicationId("user1App");
    SourceControlEvent user2Event =
        new SourceControlEvent().forRemediationPullRequest().withId("user2Event").setScmUsername("user2")
            .setApplicationId("user2App");

    // when: process the events and capture the callback references
    sourceControlEventOrchestrator.onNewEvent(user1Event);

    ArgumentCaptor<SourceControlEventStatusListener> user1StatusListenerCaptor =
        ArgumentCaptor.forClass(SourceControlEventStatusListener.class);
    verify(mockSourceControlEventProcessor).processEvent(eq(user1Event), user1StatusListenerCaptor.capture());

    sourceControlEventOrchestrator.onNewEvent(user2Event);
    ArgumentCaptor<SourceControlEventStatusListener> user2StatusListenerCaptor =
        ArgumentCaptor.forClass(SourceControlEventStatusListener.class);
    verify(mockSourceControlEventProcessor).processEvent(eq(user2Event), user2StatusListenerCaptor.capture());

    // then: the callback references are different, indicating that different user event managers were used
    assertThat(user1StatusListenerCaptor.getValue()).isNotEqualTo(user2StatusListenerCaptor.getValue());
  }

  @Test
  public void testOnNewEvent_sameUsersSameListeners() {
    // given: an orchestrator and two events for the same scm user
    SourceControlEventOrchestrator sourceControlEventOrchestrator = new SourceControlEventOrchestrator(
        mockInsightConfig, mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlEventPublisher,
        mockSourceControlUtils
    );

    SourceControlEvent user1Event1 =
        new SourceControlEvent().forRemediationPullRequest().withId("user1Event1").setScmUsername("user1")
            .setApplicationId("user1App1");
    SourceControlEvent user1Event2 =
        new SourceControlEvent().forStatusUpdate().withId("user1Event2").setScmUsername("user1")
            .setApplicationId("user1App2");

    // when: process both events and capture the callback references
    sourceControlEventOrchestrator.onNewEvent(user1Event1);

    ArgumentCaptor<SourceControlEventStatusListener> user1StatusListenerCaptor =
        ArgumentCaptor.forClass(SourceControlEventStatusListener.class);
    verify(mockSourceControlEventProcessor).processEvent(eq(user1Event1), user1StatusListenerCaptor.capture());

    sourceControlEventOrchestrator.onNewEvent(user1Event2);
    ArgumentCaptor<SourceControlEventStatusListener> user2StatusListenerCaptor =
        ArgumentCaptor.forClass(SourceControlEventStatusListener.class);
    verify(mockSourceControlEventProcessor).processEvent(eq(user1Event2), user2StatusListenerCaptor.capture());

    // then: the callback references should match indicating that the same user event manager processed both events
    assertThat(user1StatusListenerCaptor.getValue()).isEqualTo(user2StatusListenerCaptor.getValue());
  }

  @Test
  public void testStart_featureEnabled() {
    when(mockInsightConfig.isExperimentalFeatureEnabled(Feature.ORCHESTRATED_EVENT_PROCESSING)).thenReturn(true);

    SourceControlEventOrchestrator sourceControlEventOrchestrator = new SourceControlEventOrchestrator(
        mockInsightConfig, mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlEventPublisher,
        mockSourceControlUtils
    );

    sourceControlEventOrchestrator.start();

    verify(mockSourceControlEventPublisher, times(1)).setSourceControlEventListener(eq(sourceControlEventOrchestrator));
  }

  @Test
  public void testStart_featureDisabled() {
    // by default the feature is disabled

    SourceControlEventOrchestrator sourceControlEventOrchestrator = new SourceControlEventOrchestrator(
        mockInsightConfig, mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlEventPublisher,
        mockSourceControlUtils
    );

    sourceControlEventOrchestrator.start();

    verify(mockSourceControlEventPublisher, never()).setSourceControlEventListener(any());
  }
}
