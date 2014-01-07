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

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

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
   * Initiates scanning of the provided application bundle, providing the caller with a ticket that can be used to query
   * for the status/completion of the process.
   */
  @Authorize(permission = Permission.WRITE)
  public ScanTicket scanBinary(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String appPublicId, InputStream is)
      throws IOException
  {
    File binFile = saveBinary(is);
    ScanTask task = newScanTask(appPublicId, binFile);
    ScanTicket ticket = new ScanTicket();
    ticket.ticketId = task.getId();
    ticket.state = task.getState().toString();
    return ticket;
  }

  private File saveBinary(InputStream is) throws IOException {
    try {
      File file = File.createTempFile("clm-", ".tmp");
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

  private ScanTask newScanTask(String appPublicId, File binFile) {
    ScanTask scanTask = scanTaskProvider.get();
    scanTask.init(appPublicId, binFile);
    scanTasks.put(scanTask.getId(), scanTask);
    executor.submit(scanTask);
    return scanTask;
  }
}
