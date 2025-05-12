/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.EyesRunner;
import com.applitools.eyes.FileLogger;
import com.applitools.eyes.selenium.ClassicRunner;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.parseBoolean;

/**
 * Singleton to store state shared across all Applitools tests
 */
public class EyesWatcherShared
{
  private static final Logger log = LoggerFactory.getLogger(EyesWatcherShared.class);

  private static final String APPLITOOLS_KEY = System.getProperty("applitoolsKey");

  private static final String APPLITOOLS_LOG_FILE_NAME = System.getProperty("applitoolsLogFileName");

  private static final boolean APPLITOOLS_ENABLED = parseBoolean(System.getProperty("applitoolsEnabled", "false"));

  public static final EyesWatcherShared INSTANCE = new EyesWatcherShared();

  private BatchInfo batch;

  private String batchId;

  private EyesRunner runner;

  private Configuration config;

  private String localBranchName;

  private EyesWatcherShared() {
    if (isDisabled()) {
      return;
    }
    try {
      localBranchName = System.getProperty("branchName", System.getenv("GIT_LOCAL_BRANCH"));

      runner = new ClassicRunner();

      batchId = System.getenv("APPLITOOLS_BATCH_ID"); // APPLITOOLS_BATCH_ID is mapped to COMMIT_ID in the Jenkinsfile
      batchId = StringUtils.equals(batchId, "null") ? null : batchId;

      // Set only once per Jenkins job. Note, we set the batch name to null if we are building for a pr - the github
      // integration takes care of this. We are making some assumptions here since there is no easy way atm to know if
      // there is a pr associated with the branch that is under test (parameterized builds aren't available for the
      // brain just yet). For local testing (no batchId) we use the branch name.
      batch = new BatchInfo(batchId == null ? localBranchName : null);
      if (batchId != null) { // no need to set the id for local testing
        batch.setId(batchId);
      }

      config = new Configuration();
      config.setBatch(batch);
      config.setApiKey(APPLITOOLS_KEY);
      config.setBatch(batch);
      config.setHideCaret(true);
      config.setHideScrollbars(false);
    }
    catch (Exception e) {
      log.error("", e);

      System.exit(0);
    }
  }

  public static boolean isDisabled() {
    return APPLITOOLS_KEY == null || !APPLITOOLS_ENABLED;
  }

  public Eyes createEyes() {
    if (isDisabled()) {
      return null;
    }

    Eyes eyes = new Eyes(runner);
    eyes.setConfiguration(config);

    // For local testing or ci runs with main branch, set the branchName and parentBranchName
    if (batchId == null || "main".equalsIgnoreCase(localBranchName)) {
      eyes.setBranchName(localBranchName.equalsIgnoreCase("main") ? "sonatype/insight-brain/main" : localBranchName);
      eyes.setParentBranchName(System.getProperty("parentBranchName", "sonatype/insight-brain/main"));
    }

    if (StringUtils.isNotBlank(APPLITOOLS_LOG_FILE_NAME)) {
      eyes.setLogHandler(new FileLogger(APPLITOOLS_LOG_FILE_NAME, true, true));
    }
    return eyes;
  }

  String getBatchId() {
    return batchId;
  }

  String getLocalBranchName() {
    return localBranchName;
  }
}
