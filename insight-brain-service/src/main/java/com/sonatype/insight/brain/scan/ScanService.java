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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletRequest;

import static com.sonatype.insight.brain.scan.ScanResource.WEB_UI_REQUEST_ATTRIBUTE;

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
import com.sonatype.insight.brain.security.OneTimeSystemRunnable;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.error.exception.BadRequestException;
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
public class ScanService
{
  private static final int SCAN_CORE_THREAD_POOL_SIZE_PER_TENANT = 2;

  private static final int SCAN_MAX_THREAD_POOL_SIZE_PER_TENANT = 2;

  private static final long SCAN_THREAD_KEEP_ALIVE_TIME_SECONDS = 5L;

  public static final Logger log = LoggerFactory.getLogger(ScanService.class);

  private final FileCleaner fileCleaner;

  private final Provider<ScanTask> scanTaskProvider;

  private final PersistedScanTicketDAO persistedScanTicketDAO;

  private final ApplicationDAO applicationDAO;

  private final TenantReference<TenantThreadPoolExecutor> executors;

  private final ProductLicense productLicense;

  @Inject
  public ScanService(
      FileCleaner fileCleaner,
      Provider<ScanTask> scanTaskProvider,
      PersistedScanTicketDAO persistedScanTicketDAO,
      ApplicationDAO applicationDAO,
      ProductLicense productLicense,
      ShutdownHandler shutdownHandler)
  {
    this.fileCleaner = fileCleaner;
    this.scanTaskProvider = scanTaskProvider;
    this.persistedScanTicketDAO = persistedScanTicketDAO;
    this.applicationDAO = applicationDAO;
    this.productLicense = productLicense;
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ScanTask-%s").build();
    executors = new TenantReference<>(() -> {
      TenantThreadPoolExecutor tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
          SCAN_CORE_THREAD_POOL_SIZE_PER_TENANT,
          SCAN_MAX_THREAD_POOL_SIZE_PER_TENANT,
          SCAN_THREAD_KEEP_ALIVE_TIME_SECONDS,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          threadFactory,
          new AbortPolicy(),
          "scan",
          "ScanService"
      );
      tenantThreadPoolExecutor.allowCoreThreadTimeOut(true);
      shutdownHandler.add(tenantThreadPoolExecutor);
      return tenantThreadPoolExecutor;
    });
  }

  // Visible for testing
  TenantReference<TenantThreadPoolExecutor> getExecutors() {
    return executors;
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
      String scanType,
      HttpServletRequest request) throws IOException
  {
    log.debug("Request to scan binary '{}' for application public id '{}'", filename, appPublicId);

    validateFileName(filename);

    if (!productLicense.hasFeature(LicensedFeature.CLI_INTEGRATION)) {
      log.debug("License does not support application evaluation");
      throw new InvalidLicenseException();
    }

    if (!Stage.isValidStageTypeId(stage.getStageTypeId())) {
      throw new InvalidStageException(stage.getStageTypeId());
    }

    boolean isWebUIRequest = request != null && Boolean.TRUE.equals(request.getAttribute(WEB_UI_REQUEST_ATTRIBUTE));

    File binFile = saveBinary(is, filename);

    ScanTask scanTask = newScanTask(appPublicId, binFile, filename, stage, sendNotifications, userAgent, scanType,
        isWebUIRequest);
    return toScanTicket(scanTask);
  }

  private void validateFileName(String fileName) {
    if (fileName.contains("/") || fileName.contains("\\")) {
      throw new BadRequestException("Filename must not be a directory: " + fileName);
    }
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
    // Prevent a user from inputting directory information as part of filename
    String sanitizedFileName = Paths.get(filename).getFileName().toString();

    try (InputStream in = is) {
      File file = new File(Files.createTempDirectory(null).toFile(), sanitizedFileName);
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

  private ScanTask newScanTask(
      String appPublicId,
      File binFile,
      String filename,
      Stage stage,
      boolean sendNotifications,
      String userAgent,
      String scanType,
      boolean isWebUIRequest)
  {
    Application app = applicationDAO.getByPublicIdNotNull(appPublicId);
    ScanTask scanTask = scanTaskProvider.get();
    scanTask.init(app, binFile, filename, stage, sendNotifications, userAgent, scanType, isWebUIRequest);
    persistedScanTicketDAO.insert(scanTask.toPersistedScanTicket());
    log.debug("Scheduling scan task {}", scanTask.getId());
    AuditData.get().continueAsync(new OneTimeSystemRunnable(scanTask), executors.get()::submit);
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
