/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.dataaccess.scan.PersistedScanTicketDAO;
import com.sonatype.insight.brain.hds.ScanUploadService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.service.consumption.ConsumptionContext;
import com.sonatype.insight.brain.model.scan.PersistedScanTicket;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluatorResults;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker task to process a single application bundle.
 *
 * @since 1.8
 */
@Named
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ScanTask
    implements Runnable
{
  public enum State
  {
    PENDING("Queued"),
    SCANNING_COMPONENTS("Fingerprinting components"),
    // Treat uploading and waiting as the same state for user display
    UPLOADING_SCAN("Analyzing components"),
    WAITING_FOR_REPORT("Analyzing components"),
    EVALUATING_POLICY(
        "Evaluating policy"),
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

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyAlertNotifier policyAlertNotifier;

  private final PersistedScanTicketDAO persistedScanTicketDAO;

  private FileCleaner fileCleaner;

  private final ProprietaryConfigService proprietaryConfigService;

  private final ScanUploadService scanUploadService;

  private final TelemetryUtils telemetryUtils;

  private final ScanPersistenceService scanPersistenceService;

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

  private boolean isWebUIRequest;

  private ConsumptionContext.Snapshot consumptionSnapshot;

  @Inject
  public ScanTask(
      Scanner scanner,
      ScanPolicyEvaluator scanPolicyEvaluator,
      PolicyAlertNotifier policyAlertNotifier,
      FileCleaner fileCleaner,
      ProprietaryConfigService proprietaryConfigService,
      ScanUploadService scanUploadService,
      PersistedScanTicketDAO persistedScanTicketDAO,
      TelemetryUtils telemetryUtils,
      ScanPersistenceService scanPersistenceService)
  {
    this.scanner = scanner;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
    this.fileCleaner = fileCleaner;
    this.proprietaryConfigService = proprietaryConfigService;
    this.scanUploadService = scanUploadService;
    this.persistedScanTicketDAO = persistedScanTicketDAO;
    this.telemetryUtils = telemetryUtils;
    this.scanPersistenceService = scanPersistenceService;
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
      String scanType,
      boolean isWebUIRequest)
  {
    this.app = app;
    this.binFile = binFile;
    this.filename = filename;
    this.stage = stage;
    this.sendNotifications = sendNotifications;
    this.userAgent = userAgent;
    this.scanType = scanType;
    this.isWebUIRequest = isWebUIRequest;

    this.consumptionSnapshot = ConsumptionContext.snapshot();
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
    String appId = app != null ? app.getId() : null;
    try (ConsumptionContext.Scope consumptionCtx = ConsumptionContext.scopeRestored(
        consumptionSnapshot, appId, scanId))
    {
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
      ScanResult scanResult = scanner.scan(binFile, filename, app.getId(), proprietaryConfig);

      // upload the scan
      state = State.UPLOADING_SCAN;
      persistedScanTicketDAO.update(toPersistedScanTicket());

      ScanReceipt scanReceipt = scanUploadService.upload(scanResult.getScanEntity(), app, stage.getStageTypeId(),
          scanResult.getClientScanType(),
          userAgent,
          telemetryUtils.buildThirdPartyScanTelemetryData(app.getId(), stage, scanType, null /* scanTriggerType */,
              userAgent),
          null, isWebUIRequest);

      if (StringUtils.isNotBlank(scanReceipt.getScanId())) {
        scanPersistenceService.moveTempScan(scanResult.getScanEntity(), app.getId(), scanReceipt.getScanId());
      }

      // wait for the report
      state = State.WAITING_FOR_REPORT;
      persistedScanTicketDAO.update(toPersistedScanTicket());
      scanReceipt.waitForReport();

      // get report/perform evaluation
      state = State.EVALUATING_POLICY;
      persistedScanTicketDAO.update(toPersistedScanTicket());
      // The ScanPolicyEvaluator will fetch the report if it's not there
      ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(app, scanReceipt.getScanId(), stage,
          ScanTriggerType.WEB_UI, scanResult.getClientScanType(), false);
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
