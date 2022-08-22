/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.scan.PersistedScanTicketDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.scan.PersistedScanTicket;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scan.ScanTask.State;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.SystemRunnable;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the services to scan and evaluate application binaries.
 *
 * @since 1.8
 */
@Named
class ScanService
{
  private static final Logger log = LoggerFactory.getLogger(ScanService.class);

  private final FileCleaner fileCleaner;

  private final Provider<ScanTask> scanTaskProvider;

  private final PersistedScanTicketDAO persistedScanTicketDAO;

  private final ApplicationDAO applicationDAO;

  private final ThreadPoolExecutor executor;

  private final ProductLicense productLicense;

  @Inject
  public ScanService(
      FileCleaner fileCleaner,
      Provider<ScanTask> scanTaskProvider,
      PersistedScanTicketDAO persistedScanTicketDAO,
      ApplicationDAO applicationDAO,
      ProductLicense productLicense)
  {
    this.fileCleaner = fileCleaner;
    this.scanTaskProvider = scanTaskProvider;
    this.persistedScanTicketDAO = persistedScanTicketDAO;
    this.applicationDAO = applicationDAO;
    this.productLicense = productLicense;
    executor = new ThreadPoolExecutor(2, 2, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ScanTask-%s").build());
    executor.allowCoreThreadTimeOut(true);
  }

  /**
   * Initiates scanning of the provided application bundle and policy evaluation for the specified stage, providing the
   * caller with a ticket that can be used to query for the status/completion of the process.
   */
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ScanTicket scanBinary(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String appPublicId,
      InputStream is,
      String filename,
      Stage stage,
      boolean sendNotifications,
      String userAgent,
      String scanType) throws IOException
  {
    log.debug("Request to scan binary '{}' for application public id '{}'", filename, appPublicId);

    if (!productLicense.hasFeature(LicensedFeature.CLI_INTEGRATION)) {
      log.debug("License does not support application evaluation");
      throw new InvalidLicenseException();
    }

    if (!Stage.isValidStageTypeId(stage.getStageTypeId())) {
      throw new InvalidStageException(stage.getStageTypeId());
    }
    File binFile = saveBinary(is, filename);

    ScanTask scanTask = newScanTask(appPublicId, binFile, filename, stage, sendNotifications, userAgent, scanType);
    return toScanTicket(scanTask);
  }

  /**
   * @throws NotFoundException if there is no ticket for the given ticketId
   */
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ScanTicket getTicket(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String appPublicId,
      String ticketId) throws NotFoundException
  {
    PersistedScanTicket persistedScanTicket = persistedScanTicketDAO.getById(ticketId);
    if (persistedScanTicket == null) {
      throw new NotFoundException("Cannot find ScanTicket with ID " + ticketId + ".");
    }
    ScanTicket scanTicket = toScanTicket(persistedScanTicket);
    if (scanTicket.currentStep >= scanTicket.totalSteps) {
      log.debug("Removing scan task {}", ticketId);
      persistedScanTicketDAO.delete(persistedScanTicket);
    }
    return scanTicket;
  }

  File saveBinary(InputStream is, String filename) throws IOException {
    try (InputStream in = is) {
      File file = new File(Files.createTempDirectory(null).toFile(), filename);
      log.debug("Saving file to {}", file);
      try {
        Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
      catch (RuntimeException | IOException e) {
        try {
          fileCleaner.delete(file.getParentFile());
        }
        catch (FileDeletionException fde) {
          log.error("Could not delete file folder: {}", file.getParentFile(), fde);
        }
        throw e;
      }
      return file;
    }
  }

  private static String getFileExtension(String filename) {
    // NOTE: We don't want to error on the side of too few characters (gz vs tar.gz)
    int index = filename.indexOf('.');
    String ext = (index < 0) ? "" : filename.substring(index);
    return ext;
  }

  private ScanTask newScanTask(
      String appPublicId,
      File binFile,
      String filename,
      Stage stage,
      boolean sendNotifications,
      String userAgent,
      String scanType)
  {
    Application app = applicationDAO.getByPublicIdNotNull(appPublicId);
    ScanTask scanTask = scanTaskProvider.get();
    scanTask.init(app, binFile, filename, stage, sendNotifications, userAgent, scanType);
    persistedScanTicketDAO.insert(scanTask.toPersistedScanTicket());
    log.debug("Scheduling scan task {}", scanTask.getId());
    AuditData.get().continueAsync(new SystemRunnable(scanTask), executor::submit);
    return scanTask;
  }

  private ScanTicket toScanTicket(ScanTask scanTask) {
    return createScanTicket(scanTask.getId(), scanTask.getApp(), scanTask.getState(), scanTask.getScanId(),
        scanTask.getErrorId());
  }

  private ScanTicket toScanTicket(PersistedScanTicket persistedScanTicket) {
    return createScanTicket(persistedScanTicket.getId(),
        applicationDAO.getByIdNotNull(persistedScanTicket.getApplicationId()),
        State.valueOf(persistedScanTicket.getStateId()), persistedScanTicket.getScanId(),
        persistedScanTicket.getErrorId());
  }

  private ScanTicket createScanTicket(
      String ticketId,
      Application application,
      State state,
      String scanId,
      String errorId)
  {
    ScanTicket ticket = new ScanTicket();

    state.provideStepInfo(ticket);

    ticket.ticketId = ticketId;
    ticket.applicationPublicId = application.getPublicId();
    ticket.scanId = scanId;

    if (errorId != null) {
      ticket.error = "An error occurred, and the application you uploaded has not been evaluated. "
          + "Please contact your IT Administrator for troubleshooting options. Error ID " + errorId
          + " - Access Nexus IQ Server log for details.";
    }

    return ticket;
  }
}
