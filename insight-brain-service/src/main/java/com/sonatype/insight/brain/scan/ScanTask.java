/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.saas.ScanUploader;
import com.sonatype.insight.brain.service.InsightWork;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
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
    PENDING("Queued"), SCANNING_COMPONENTS("Fingerprinting components"),
    // Treat uploading and waiting as the same state for user display
    UPLOADING_SCAN("Analyzing components"), WAITING_FOR_REPORT("Analyzing components"), EVALUATING_POLICY(
        "Evaluating policy"), DONE("Done");

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

  private final PolicyAlertNotifier policyAlertNotifier;

  private final InsightWork work;

  private FileCleaner fileCleaner;

  private final String id;

  private Application app;

  private File binFile;

  private Stage stage;

  private boolean sendNotifications;

  private volatile State state = State.PENDING;

  private volatile Throwable error;

  private volatile String errorId;

  private volatile String scanId;

  private volatile long touched;

  @Inject
  public ScanTask(Scanner scanner, ScanUploader uploader, PolicyEvaluationUtils policyEvaluationUtils,
      PolicyAlertNotifier policyAlertNotifier, InsightWork work, FileCleaner fileCleaner)
  {
    this.scanner = scanner;
    this.uploader = uploader;
    this.policyEvaluationUtils = policyEvaluationUtils;
    this.policyAlertNotifier = policyAlertNotifier;
    this.work = work;
    this.fileCleaner = fileCleaner;
    id = UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * @param binFile the binary file of what to scan
   */
  public void init(Application app, File binFile, Stage stage, boolean sendNotifications) {
    this.app = app;
    this.binFile = binFile;
    this.stage = stage;
    this.sendNotifications = sendNotifications;
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
    ticket.applicationPublicId = app.getPublicId();
    ticket.scanId = scanId;

    if (error != null) {
      ticket.error = "An error occurred, and the application you uploaded has not been evaluated. Please contact your IT Administrator for troubleshooting options. Error ID "
          + errorId + " - Access CLM Log for details.";
    }

    ticket.isDone = (state == State.DONE);

    touched = System.currentTimeMillis();

    return ticket;
  }

  public Throwable getError() {
    return error;
  }

  public boolean isObsolete() {
    return System.currentTimeMillis() - touched > TimeUnit.MINUTES.toMillis(30);
  }

  @Override
  public void run() {
    String appPublicId = null;
    try {
      log.debug("Running scan task {}", id);

      if (app == null || stage == null || binFile == null) {
        throw new IllegalStateException("scan task has not been properly initialized");
      }
      appPublicId = app.getPublicId();

      // create the scan data
      state = State.SCANNING_COMPONENTS;
      File scanFile = scanner.scan(binFile, work.getScanDir(app.getId()));

      // upload the scan
      state = State.UPLOADING_SCAN;
      ScanReceipt scanReceipt = uploader.upload(scanFile, appPublicId, "rest/ci/scan");
      if (StringUtils.isNotBlank(scanReceipt.getScanId())) {
        FileUtils.rename(scanFile, work.getScanFile(app.getId(), scanReceipt.getScanId()));
      }

      // wait for the report
      state = State.WAITING_FOR_REPORT;
      scanReceipt.waitForReport();

      // get report/perform evaluation
      state = State.EVALUATING_POLICY;
      List<PolicyAlert> oldAlerts = null;
      if (sendNotifications) {
        oldAlerts = policyEvaluationUtils.findLastPrimaryPolicyAlerts(appPublicId, app.getId(), stage);
      }
      // PolicyEvaluationUtils will fetch report if it's not there
      PolicyEvaluationResult evalResult = policyEvaluationUtils.evaluate(appPublicId, scanReceipt.getScanId(), stage);
      if (sendNotifications) {
        List<PolicyAlert> newAlerts = evalResult.getAlerts();
        policyAlertNotifier.sendNotifications(appPublicId, app.getId(), scanReceipt.getScanId(), stage, newAlerts,
            oldAlerts);
      }

      // provide report/scanId once evaluation is completed successfully
      scanId = scanReceipt.getScanId();
    }
    catch (Throwable e) {
      error = e;
      errorId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

      log.error("Failed to evaluate policies on uploaded binary for application {} (Error ID {})", appPublicId,
          errorId, e);
    }
    finally {
      state = State.DONE;

      // remove the uploaded scanned app binary, user can resubmit if there was an error
      try {
        fileCleaner.delete(binFile);
      }
      catch (FileDeletionException e) {
        log.error("Can not delete temporary application binary", e);
      }

      log.debug("Completed scan task {}", id);
    }
  }
}
