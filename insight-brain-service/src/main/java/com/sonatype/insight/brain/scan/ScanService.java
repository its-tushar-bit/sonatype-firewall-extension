/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
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

  private final Provider<ScanTask> scanTaskProvider;

  private final Map<String, ScanTask> scanTasks;

  private final ThreadPoolExecutor executor;

  @Inject
  public ScanService(Provider<ScanTask> scanTaskProvider) {
    this.scanTaskProvider = scanTaskProvider;
    scanTasks = new ConcurrentHashMap<>();
    executor = new ThreadPoolExecutor(1, 2, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ScanTask-%s").build());
    executor.allowCoreThreadTimeOut(true);
  }

  /**
   * Initiates scanning of the provided application bundle and policy evaluation for the specified stage, providing the
   * caller with a ticket that can be used to query for the status/completion of the process.
   */
  @Authorize(permission = Permission.WRITE)
  public ScanTicket scanBinary(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String appPublicId,
      InputStream is, String filename, Stage stage) throws IOException
  {
    if (!Stage.isValidStageTypeId(stage.getStageTypeId())) {
      throw new BadRequestException("Invalid CLM stage: " + stage.getStageTypeId());
    }
    File binFile = saveBinary(is, filename);
    ScanTask task = newScanTask(appPublicId, binFile, stage);
    return task.getTicket();
  }

  /**
   * @throws NotFoundException if there is no ticket for the given ticketId
   */
  @Authorize(permission = Permission.WRITE)
  public ScanTicket getTicket(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String appPublicId, String ticketId)
      throws NotFoundException
  {
    ScanTask task = scanTasks.get(ticketId);

    if (task == null) {
      throw new NotFoundException("Cannot find ScanTicket with id " + ticketId + ".");
    }

    return task.getTicket();
  }

  static File saveBinary(InputStream is, String filename) throws IOException {
    try {
      String ext = getFileExtension(filename);
      File file = File.createTempFile("clm-", ext);
      log.debug("Saving binary to {}", file);
      try {
        FileUtils.copyStreamToFile(new RawInputStreamFacade(is), file);
      }
      catch (RuntimeException | IOException e) {
        file.delete();
        throw e;
      }
      return file;
    }
    finally {
      IOUtil.close(is);
    }
  }

  private static String getFileExtension(String filename) {
    // NOTE: We don't want to error on the side of too few characters (gz vs tar.gz)
    int index = filename.indexOf('.');
    String ext = (index < 0) ? "" : filename.substring(index);
    return ext;
  }

  private ScanTask newScanTask(String appPublicId, File binFile, Stage stage) {
    validatePublicApplicationId(appPublicId);

    ScanTask scanTask = scanTaskProvider.get();
    scanTask.init(appPublicId, binFile, stage);
    scanTasks.put(scanTask.getId(), scanTask);
    executor.submit(scanTask);
    return scanTask;
  }

  private void validatePublicApplicationId(String appPublicId) {
    new ApplicationDAO().getByPublicIdNotNull(appPublicId);
  }
}
