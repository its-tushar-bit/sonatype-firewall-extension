/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.dataaccess.scan.PersistedScanTicketDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.scan.PersistedScanTicket;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluatorResults;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanService;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.telemetry.TelemetryUtils.buildThirdPartyScanTelemetryData;

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

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyAlertNotifier policyAlertNotifier;

  private final InsightWork work;

  private final PersistedScanTicketDAO persistedScanTicketDAO;

  private FileCleaner fileCleaner;

  private final ProprietaryConfigService proprietaryConfigService;

  private final ThirdPartyScanService thirdPartyScanService;

  private final String id;

  private Application app;

  private File binFile;

  private String filename;

  private String userAgent;

  private String scanType;

  private Stage stage;

  private boolean sendNotifications;

  private volatile State state = State.PENDING;

  private volatile Exception error;

  private volatile String errorId;

  private volatile String scanId;

  @Inject
  public ScanTask(
      Scanner scanner,
      ScanUploader uploader,
      ScanPolicyEvaluator scanPolicyEvaluator,
      PolicyAlertNotifier policyAlertNotifier,
      InsightWork work,
      FileCleaner fileCleaner,
      ProprietaryConfigService proprietaryConfigService,
      ThirdPartyScanService thirdPartyScanService,
      PersistedScanTicketDAO persistedScanTicketDAO)
  {
    this.scanner = scanner;
    this.uploader = uploader;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
    this.work = work;
    this.fileCleaner = fileCleaner;
    this.proprietaryConfigService = proprietaryConfigService;
    this.thirdPartyScanService = thirdPartyScanService;
    this.persistedScanTicketDAO = persistedScanTicketDAO;
    id = UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * @param binFile the binary file of what to scan
   */
  public void init(
      Application app,
      File binFile,
      String filename,
      Stage stage,
      boolean sendNotifications,
      String userAgent,
      String scanType)
  {
    this.app = app;
    this.binFile = binFile;
    this.filename = filename;
    this.stage = stage;
    this.sendNotifications = sendNotifications;
    this.userAgent = userAgent;
    this.scanType = scanType;
  }

  public String getId() {
    return id;
  }

  public Application getApp() {
    return app;
  }

  public State getState() {
    return state;
  }

  public String getScanId() {
    return scanId;
  }

  public Exception getError() {
    return error;
  }

  public String getErrorId() {
    return errorId;
  }

  public PersistedScanTicket toPersistedScanTicket() {
    PersistedScanTicket persistedScanTicket = new PersistedScanTicket();
    persistedScanTicket.setId(id);
    persistedScanTicket.setApplicationId(app.getId());
    persistedScanTicket.setScanId(scanId);
    persistedScanTicket.setStateId(state.name());
    persistedScanTicket.setErrorId(errorId);
    return persistedScanTicket;
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
      persistedScanTicketDAO.update(toPersistedScanTicket());
      ProprietaryConfig proprietaryConfig = proprietaryConfigService.getProprietaryConfig(OwnerType.APPLICATION,
          app.getPublicId());
      ScanResult scanResult = scanner.scan(binFile, filename, work.getScanDir(app.getId()), proprietaryConfig);

      // upload the scan
      state = State.UPLOADING_SCAN;
      persistedScanTicketDAO.update(toPersistedScanTicket());

      ScanReceipt scanReceipt;
      if (scanResult != null && scanResult.hasThirdPartyScanContent()) {
        scanReceipt = thirdPartyScanService.filterAndUpload(scanResult.getScanFile(), app, stage.getStageTypeId(),
            buildThirdPartyScanTelemetryData(appPublicId, stage, scanType, userAgent));
      }
      else {
        scanReceipt = uploader.upload(scanResult.getScanFile(), app, stage.getStageTypeId());
      }

      if (StringUtils.isNotBlank(scanReceipt.getScanId())) {
        FileUtils.rename(scanResult.getScanFile(), work.getScanFile(app.getId(), scanReceipt.getScanId()));
      }

      // wait for the report
      state = State.WAITING_FOR_REPORT;
      persistedScanTicketDAO.update(toPersistedScanTicket());
      scanReceipt.waitForReport();

      // get report/perform evaluation
      state = State.EVALUATING_POLICY;
      persistedScanTicketDAO.update(toPersistedScanTicket());
      // The ScanPolicyEvaluator will fetch the report if it's not there
      ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(app, scanReceipt.getScanId(), stage);
      if (sendNotifications) {
        policyAlertNotifier.sendNotifications(app, results);
      }

      // provide report/scanId once evaluation is completed successfully
      scanId = scanReceipt.getScanId();
      persistedScanTicketDAO.update(toPersistedScanTicket());
    }
    catch (Exception e) {
      AuditData.get().setException(e);
      error = e;
      errorId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
      persistedScanTicketDAO.update(toPersistedScanTicket());

      log.error("Failed to evaluate policies on uploaded binary for application {} (Error ID {})", appPublicId,
          errorId, e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(2);
    }
    finally {
      state = State.DONE;
      persistedScanTicketDAO.update(toPersistedScanTicket());

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
