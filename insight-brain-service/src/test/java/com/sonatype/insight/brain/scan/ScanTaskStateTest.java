/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.ScanTask.State;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultsProcessor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

  ScanPolicyEvaluator scanPolicyEvaluator = mock(ScanPolicyEvaluator.class);

  PolicyAlertNotifier notifier = mock(PolicyAlertNotifier.class);

  InsightWork work = mock(InsightWork.class);

  FileCleaner fileCleaner = mock(FileCleaner.class);

  private ProprietaryConfigService proprietaryConfigService = mock(ProprietaryConfigService.class);

  private ThirdPartyScanResultsProcessor thirdPartyScanResultsProcessor = mock(ThirdPartyScanResultsProcessor.class);
  
  ScanTask task = new ScanTask(scanner, uploader, scanPolicyEvaluator, notifier, work, fileCleaner,
      proprietaryConfigService, thirdPartyScanResultsProcessor);

  TaskStateCapturer captureState = new TaskStateCapturer();

  @Before
  public void init() throws Exception {
    File binFile = new File("any");
    task.init(new Application("any", "MyApp", null), binFile, "any", new Stage(Stage.ID_BUILD), false, "", "");
    when(scanner.scan(any(), any(), any(), any())).thenReturn(new ScanResult(binFile, false));
  }

  @Test
  public void notStarted() {
    assertThat(task.getState()).as("ScanTask state when not started").isEqualTo(ScanTask.State.PENDING);
    assertThat(task.getTicket().currentStep).isEqualTo(0);
  }

  @Test
  public void scanning() throws IOException {
    when(scanner.scan((File) any(), (String) any(), (File) any(), (ProprietaryConfig) any()))
        .then(captureState);

    task.run();

    assertThat(captureState.getState()).as("ScanTask state when scanning")
        .isEqualTo(ScanTask.State.SCANNING_COMPONENTS);
  }

  @Test
  public void uploading() throws IOException {
    when(uploader.upload(any(), any(Application.class), anyString())).then(captureState);

    task.run();

    assertThat(captureState.getState()).as("ScanTask state when uploading").isEqualTo(ScanTask.State.UPLOADING_SCAN);
  }

  @Test
  public void waitingForReport() throws IOException, InterruptedException {
    ScanReceipt scanReciept = mock(ScanReceipt.class);
    when(uploader.upload(any(), any(Application.class), anyString())).thenReturn(scanReciept);

    doAnswer(captureState).when(scanReciept).waitForReport();

    task.run();

    assertThat(captureState.getState()).as("ScanTask state when waiting").isEqualTo(ScanTask.State.WAITING_FOR_REPORT);
  }

  @Test
  public void evaluating() throws IOException {
    ScanReceipt scanReciept = mock(ScanReceipt.class);
    when(uploader.upload((File) any(), any(Application.class), anyString())).thenReturn(scanReciept);

    when(scanPolicyEvaluator.evaluate(any(), (String) any(), (Stage) any())).then(captureState);

    task.run();

    assertThat(captureState.getState()).as("ScanTask state when evaluating")
        .isEqualTo(ScanTask.State.EVALUATING_POLICY);
  }

  @Test
  public void done() {
    task.run();

    assertThat(task.getState()).as("ScanTask state when done").isEqualTo(ScanTask.State.DONE);
  }

  @Test
  public void error() throws IOException {
    when(scanner.scan((File) any(), (String) any(), (File) any(), (ProprietaryConfig) any()))
        .thenThrow(RuntimeException.class);

    task.run();

    assertThat(task.getState()).as("ScanTask state when in error").isEqualTo(ScanTask.State.DONE);
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
