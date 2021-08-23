/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.concurrent.SemaphorePool;
import com.sonatype.insight.brain.git.GitCommitStatusService;
import com.sonatype.insight.brain.git.PullRequestCommentingEventHandler;
import com.sonatype.insight.brain.git.PullRequestRemediationService;
import com.sonatype.insight.brain.git.SourceControlScanService;
import com.sonatype.insight.brain.git.SourceControlService;
import com.sonatype.insight.brain.git.VerifiableLoggingTestBase;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.nexus.git.utils.api.GitException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Stubber;

import static com.sonatype.insight.brain.git.event.EventTestUtils.createEvent;
import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceControlEventProcessorTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private PullRequestCommentingEventHandler mockPullRequestCommentingEventHandler;

  @Mock
  private PullRequestRemediationService mockPullRequestRemediationService;

  @Mock
  private GitCommitStatusService mockGitCommitStatusService;

  @Mock
  private SemaphorePool mockRepoAccessController;

  @Mock
  private SourceControlScanService mockSourceControlScanService;

  @Mock
  private SourceControlService mockSourceControlService;

  @Mock
  private SourceControlEventStatusListener mockStatusListener;

  @Mock
  private CurrentUser mockCurrentUser;

  private SourceControlEventProcessor sourceControlEventProcessor;

  public SourceControlEventProcessorTest() {
    super(SourceControlEventProcessor.class);
  }

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.openMocks(this);
    super.setup();
    when(mockCurrentUser.getUsernameOrSystem()).thenReturn(CurrentUser.SYSTEM);
    sourceControlEventProcessor =
        spy(new SourceControlEventProcessor(mockPullRequestCommentingEventHandler, mockPullRequestRemediationService,
            mockGitCommitStatusService, mockSourceControlScanService, mockSourceControlService, mockCurrentUser));
  }

  @Test
  public void testProcessEvent_applicationEvaluation() throws InterruptedException {
    SourceControlEvent event = createEvent().forApplicationEvaluation();

    processEventAndWaitForCompletion(event);

    verify(mockPullRequestCommentingEventHandler, times(1)).onApplicationEvaluation(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_discoveredPullRequest() throws InterruptedException {
    SourceControlEvent event = createEvent().forDiscoveredPullRequest();

    processEventAndWaitForCompletion(event);

    verify(mockPullRequestCommentingEventHandler, times(1)).onDiscoveredPullRequest(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_remediationPullRequest() throws IOException, InterruptedException {
    SourceControlEvent event = createEvent().forRemediationPullRequest();

    processEventAndWaitForCompletion(event);

    verify(mockPullRequestRemediationService, times(1)).onRemediateComponent(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_repositoryUrlUpdated() throws InterruptedException {
    SourceControlEvent event = createEvent().forRepositoryUrlUpdated();

    processEventAndWaitForCompletion(event);

    verify(mockSourceControlService, times(1)).onRepositoryUrlUpdated(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_sourceControlEvaluation() throws GitException, IOException, InterruptedException {
    SourceControlEvent event = createEvent().forSourceControlEvaluation();

    processEventAndWaitForCompletion(event);

    verify(mockSourceControlScanService, times(1)).onSourceControlScan(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_statusUpdate() throws InterruptedException {
    SourceControlEvent event = createEvent().forStatusUpdate();

    processEventAndWaitForCompletion(event);

    verify(mockGitCommitStatusService, times(1)).onSendCommitStatus(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_updatedPullRequest() throws InterruptedException {
    SourceControlEvent event = createEvent().forUpdatedPullRequest();

    processEventAndWaitForCompletion(event);

    verify(mockPullRequestCommentingEventHandler, times(1)).onUpdatedPullRequest(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_runAsNonSystemUser() throws Exception {
    when(mockCurrentUser.getUsernameOrSystem()).thenReturn("JohnDoe");
    SourceControlEvent event = createEvent().forUpdatedPullRequest();

    processEventAndWaitForCompletion(event);

    verify(mockPullRequestCommentingEventHandler, never()).onUpdatedPullRequest(any());
    verifyEventNotStarted(event);
    verifyEventError(event,
        "SourceControlEvent with ID " + event.getId() + " processed as user 'JohnDoe' instead of 'system'");
  }

  @Test
  public void testProcessEvents_interruptOnAcquireRepoAccessControl() throws Exception {
    // given: DAO setup to throw an exception
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    event.setId("event-1");

    sourceControlEventProcessor.setRepoAccessController(mockRepoAccessController);
    doThrow(new InterruptedException("simulated")).when(mockRepoAccessController).acquire(eq(event.getApplicationId()));

    // when:
    processEventAndWaitForCompletion(event);

    // then:
    verifyEventStarted(event);
    assertThatLogMessagesContain(
        debug("Unable to acquire repo access for application 'app1'")
    );
  }

  @Test
  public void testProcessEvents_interruptOnReleaseRepoAccessControl() throws Exception {
    // given: DAO setup to throw an exception
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    event.setId("hij789");

    sourceControlEventProcessor.setRepoAccessController(mockRepoAccessController);
    doThrow(new InterruptedException("simulated")).when(mockRepoAccessController).release(eq(event.getApplicationId()));

    // when:
    processEventAndWaitForCompletion(event);

    // then:
    verifyEventStarted(event);
    verifyEventCompleted(event);
    assertThatLogMessagesEqual(
        debug(getProcessedEventMessage(event)),
        warn("Unable to release repo access for application 'app1'")
    );
  }

  private CountDownLatch createOnEventFinishedLatch(SourceControlEvent event) {
    CountDownLatch latch = new CountDownLatch(1);
    unlatch(latch).when(sourceControlEventProcessor).notifyFinishedProcessingEvent(eq(event));
    return latch;
  }

  private void processEventAndWaitForCompletion(SourceControlEvent event) throws InterruptedException {
    CountDownLatch eventProcessedLatch = createOnEventFinishedLatch(event);
    sourceControlEventProcessor.processEvent(event, mockStatusListener);
    verifyUnlatched(eventProcessedLatch);
  }

  private Stubber unlatch(CountDownLatch latch) {
    return doAnswer(a -> {
      latch.countDown();
      return null;
    });
  }

  private void verifyEventCompleted(SourceControlEvent event) {
    verify(mockStatusListener, times(1)).onEventCompleted(eq(event));
  }

  private void verifyEventStarted(SourceControlEvent event) {
    verify(mockStatusListener, times(1)).onEventStarted(eq(event));
  }

  private void verifyEventNotStarted(SourceControlEvent event) {
    verify(mockStatusListener, never()).onEventStarted(eq(event));
  }

  private void verifyEventError(SourceControlEvent event, String errorMessage) {
    ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
    verify(mockStatusListener, times(1)).onEventError(eq(event), exceptionCaptor.capture());
    assertThat(exceptionCaptor.getValue().getMessage()).isEqualTo(errorMessage);
  }

  private void verifyUnlatched(CountDownLatch latch) throws InterruptedException {
    verifyUnlatched(latch, 5);
  }

  private void verifyUnlatched(CountDownLatch latch, long seconds) throws InterruptedException {
    assertThat(latch.await(seconds, TimeUnit.SECONDS)).isTrue();
  }

  private String getProcessedEventMessage(SourceControlEvent event) {
    return format("Processed event '%s' of type '%s' for application '%s'", event.getId(), event.getEventType(),
        event.getApplicationId());
  }
}
