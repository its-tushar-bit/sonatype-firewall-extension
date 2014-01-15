/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.io.FileCleaner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.saas.ScanUploader;
import com.sonatype.insight.brain.scan.ScanTask.State;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that the scan state reflects the orchestrated action within the {@link ScanTask} being executed.
 * 
 * Uses Mockito's stubbing to capture the state and verify, see {@link TaskStateCapturer}.
 * 
 * Refer to {@link ScanStateToTicketTranslatorTest} for translation from state to ticket steps.
 */
public class ScanTaskStateTest
{
  Scanner scanner = mock(Scanner.class);
  ScanUploader uploader = mock(ScanUploader.class);
  PolicyEvaluationUtils evaluator = mock(PolicyEvaluationUtils.class);
  PolicyAlertNotifier notifier = mock(PolicyAlertNotifier.class);
  InsightWork work = mock(InsightWork.class);
  FileCleaner fileCleaner = mock(FileCleaner.class);
  ScanTask task = new ScanTask(scanner, uploader, evaluator, notifier, work, fileCleaner);

  TaskStateCapturer captureState = new TaskStateCapturer();

  @Before
  public void init() {
    task.init(new Application("any", "MyApp", null), new File("any"), new Stage(Stage.ID_BUILD), false);
  }

  @Test
  public void notStarted() {
    assertThat("ScanTask state when not started", task.getState(), equalTo(ScanTask.State.PENDING));
    assertThat(task.getTicket().currentStep, equalTo(0));
  }

  @Test
  public void scanning() throws IOException {
    when(scanner.scan((File) any(), (File) any())).then(captureState);

    task.run();

    assertThat("ScanTask state when scanning", captureState.getState(), equalTo(ScanTask.State.SCANNING_COMPONENTS));
  }

  @Test
  public void uploading() throws IOException {
    when(uploader.upload((File) any(), anyString(), anyString())).then(captureState);

    task.run();

    assertThat("ScanTask state when uploading", captureState.getState(), equalTo(ScanTask.State.UPLOADING_SCAN));
  }

  @Test
  public void waitingForReport() throws IOException, InterruptedException {
    ScanReceipt scanReciept = mock(ScanReceipt.class);
    when(uploader.upload((File) any(), anyString(), anyString())).thenReturn(scanReciept);

    doAnswer(captureState).when(scanReciept).waitForReport();

    task.run();

    assertThat("ScanTask state when waiting", captureState.getState(), equalTo(ScanTask.State.WAITING_FOR_REPORT));
  }

  @Test
  public void evaluating() throws IOException {
    ScanReceipt scanReciept = mock(ScanReceipt.class);
    when(uploader.upload((File) any(), anyString(), anyString())).thenReturn(scanReciept);

    when(evaluator.evaluate(anyString(), anyString(), (Stage) any())).then(captureState);

    task.run();

    assertThat("ScanTask state when evaluating", captureState.getState(), equalTo(ScanTask.State.EVALUATING_POLICY));
  }

  @Test
  public void done() {
    task.run();

    assertThat("ScanTask state when done", task.getState(), equalTo(ScanTask.State.DONE));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void error() throws IOException {
    when(scanner.scan((File) any(), (File) any())).thenThrow(RuntimeException.class);

    task.run();

    assertThat("ScanTask state when in error", task.getState(), equalTo(ScanTask.State.DONE));
  }

  /**
   * Use Mockito's Answer stubbing to capture the state at the time of the orchestrated call.
   */
  private final class TaskStateCapturer
      implements Answer<Object>
  {
    private State capturedState;

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
      // can't assert conditions when answering because assertion checks throw exception to indicate failure and the
      // ScanTask catches all exceptions and wraps them.
      capturedState = task.getState();

      return null;
    }

    public State getState() {
      return capturedState;
    }
  }
}
