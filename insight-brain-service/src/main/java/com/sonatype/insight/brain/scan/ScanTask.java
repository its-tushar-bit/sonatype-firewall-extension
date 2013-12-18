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

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker task to process a single application bundle.
 * 
 * @since 1.7.1
 */
@Named
class ScanTask
    implements Runnable
{
  public enum State
  {
    PENDING, SCANNING_COMPONENTS, UPLOADING_SCAN, DOWNLOADING_REPORT, DONE
  }

  private static final Logger log = LoggerFactory.getLogger(ScanTask.class);

  private final Scanner scanner;

  private final String id;

  private File binFile;

  private Application app;

  private volatile State state = State.PENDING;

  private volatile Throwable error;

  @Inject
  public ScanTask(Scanner scanner) {
    this.scanner = scanner;
    id = UUID.randomUUID().toString().replace("-", "");
  }

  public void init(String applicationPublicId, File binFile) {
    app = new ApplicationDAO().getByPublicIdNotNull(applicationPublicId);
    this.binFile = binFile;
  }

  public String getId() {
    return id;
  }

  public State getState() {
    return state;
  }

  public Throwable getError() {
    return error;
  }

  @Override
  public void run() {
    try {
      state = State.SCANNING_COMPONENTS;
      File scanFile = scanner.scan(binFile);
      state = State.UPLOADING_SCAN;
      // upload the scan
      state = State.DOWNLOADING_REPORT;
      // get the report/evalution
    }
    catch (Throwable e) {
      error = e;
      log.error("Failed to evaluate policies on uploaded binary for application {}", app.getPublicId(), e);
    }
    finally {
      state = State.DONE;
    }
  }
}
