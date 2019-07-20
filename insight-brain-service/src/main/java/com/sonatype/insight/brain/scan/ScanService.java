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

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

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

  private final ScanTaskRepository taskRepository;

  private final FileCleaner fileCleaner;

  @Inject
  public ScanService(ScanTaskRepository scanTaskRepository, FileCleaner fileCleaner) {
    this.taskRepository = scanTaskRepository;
    this.fileCleaner = fileCleaner;
  }

  /**
   * Initiates scanning of the provided application bundle and policy evaluation for the specified stage, providing the
   * caller with a ticket that can be used to query for the status/completion of the process.
   */
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ScanTicket scanBinary(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String appPublicId,
                               InputStream is,
                               String filename,
                               Stage stage,
                               boolean sendNotifications) throws IOException
  {
    log.debug("Request to scan binary '{}' for application public id '{}'", filename, appPublicId);

    if (!Stage.isValidStageTypeId(stage.getStageTypeId())) {
      throw new BadRequestException("Invalid Nexus IQ stage: " + stage.getStageTypeId() + ".");
    }
    File binFile = saveBinary(is, filename);

    ScanTask task = newScanTask(appPublicId, binFile, filename, stage, sendNotifications);
    return task.getTicket();
  }

  /**
   * @throws NotFoundException if there is no ticket for the given ticketId
   */
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ScanTicket getTicket(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String appPublicId,
      String ticketId) throws NotFoundException
  {
    ScanTask task = taskRepository.getByIdNotNull(ticketId);
    ScanTicket ticket = task.getTicket();

    if (ticket.currentStep >= ticket.totalSteps) {
      taskRepository.remove(ticketId);
    }

    return ticket;
  }

  File saveBinary(InputStream is, String filename) throws IOException {
    try (InputStream in = is) {
      String ext = getFileExtension(filename);
      File file = File.createTempFile("clm-", ext);
      log.debug("Saving binary to {}", file);
      try {
        Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
      catch (RuntimeException | IOException e) {
        try {
          fileCleaner.delete(file);
        }
        catch (FileDeletionException fde) {
          log.error("Could not delete binary file: {}", file, fde);
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

  private ScanTask newScanTask(String appPublicId,
                               File binFile,
                               String filename,
                               Stage stage,
                               boolean sendNotifications)
  {
    Application app = new ApplicationDAO().getByPublicIdNotNull(appPublicId);
    ScanTask scanTask = taskRepository.newScanTask(app, binFile, filename, stage, sendNotifications);
    return scanTask;
  }
}
