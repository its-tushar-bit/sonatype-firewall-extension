/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.saas.ScanUploader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker task to process a single application bundle.
 *
 * @since 1.8
 */
@Named
class ScanTask
    implements Runnable
{
  public enum State
  {
    PENDING ("Queued"),
    SCANNING_COMPONENTS ("Fingerprinting components"),
    // Treat uploading and waiting as the same state for user display
    UPLOADING_SCAN ("Analyzing components"),
    WAITING_FOR_REPORT ("Analyzing components"),
    EVALUATING_POLICY ("Evaluating policy"),
    DONE("Done");

    private final String displayText;

    State(String displayText) {
      this.displayText = displayText;
    }

    @Override
    public String toString() {
      return displayText;
    }

    /**
     * Updates the provided {@link ScanTicket} with step information as translated from this state.
     */
    public void provideStepInfo(ScanTicket ticket) {
      ticket.totalSteps = State.values().length - 1; // Discount PENDING state as step.

      ticket.currentStep = this.ordinal();
      ticket.currentStepName = this.toString();
    }
  }

  private static final Logger log = LoggerFactory.getLogger(ScanTask.class);

  private final Scanner scanner;

  private final ScanUploader uploader;

  private final PolicyEvaluationUtils policyEvaluationUtils;

  private final String id;

  private String applicationPublicId;

  private File binFile;

  private Stage stage;

  private volatile State state = State.PENDING;

  private volatile Throwable error;

  private volatile String scanId;

  @Inject
  public ScanTask(Scanner scanner, ScanUploader uploader, PolicyEvaluationUtils policyEvaluationUtils) {
    this.scanner = scanner;
    this.uploader = uploader;
    this.policyEvaluationUtils = policyEvaluationUtils;
    id = UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * @param applicationPublicId a valid public application id to associate to the policy results
   * @param binFile the binary file of what to scan
   */
  public void init(String applicationPublicId, File binFile, Stage stage) {
    this.applicationPublicId = applicationPublicId;
    this.binFile = binFile;
    this.stage = stage;
  }

  public String getId() {
    return id;
  }

  public State getState() {
    return state;
  }

  /**
   * Creates a {@link ScanTicket} representing the current state of the task.
   */
  public ScanTicket getTicket() {
    ScanTicket ticket = new ScanTicket();

    state.provideStepInfo(ticket);

    ticket.ticketId = id;
    ticket.applicationPublicId = applicationPublicId;
    ticket.scanId = scanId;

    if(error != null) {
      ticket.error = "Failed to evaluate policies on uploaded binary for application " + applicationPublicId;
    }

    return ticket;
  }

  public Throwable getError() {
    return error;
  }

  @Override
  public void run() {
    try {
      // create the scan data
      state = State.SCANNING_COMPONENTS;
      File scanFile = scanner.scan(binFile);

      // upload the scan
      state = State.UPLOADING_SCAN;
      ScanReceipt scanReceipt = uploader.upload(scanFile, applicationPublicId, "rest/ci/scan");

      // wait for the report
      state = State.WAITING_FOR_REPORT;
      scanReceipt.waitForReport();

      // get report/perform evaluation
      state = State.EVALUATING_POLICY;
      // PolicyEvaluationUtils will fetch report if it's not there
      policyEvaluationUtils.evaluate(applicationPublicId, scanReceipt.getScanId(), stage);

      // provide report/scanId once evaluation is completed successfully
      scanId = scanReceipt.getScanId();
    }
    catch (Throwable e) {
      error = e;
      log.error("Failed to evaluate policies on uploaded binary for application {}", applicationPublicId, e);
    }
    finally {
      state = State.DONE;
    }
  }
}

