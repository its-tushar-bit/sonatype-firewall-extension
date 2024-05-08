/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.FileLogger;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.fluent.SeleniumCheckSettings;
import com.applitools.eyes.selenium.fluent.Target;
import com.codeborne.selenide.WebDriverRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.parseBoolean;

public class EyesWatcher
    extends TestWatcher
{
  public static final Eyes eyes = new Eyes();

  private String testName;

  private static BatchInfo batch;

  private static String batchId;

  private static final String localBranchName;

  private static final String APPLITOOLS_KEY = System.getProperty("applitoolsKey");

  private static final String APPLITOOLS_LOG_FILE_NAME = System.getProperty("applitoolsLogFileName");

  private static final boolean APPLITOOLS_ENABLED = parseBoolean(System.getProperty("applitoolsEnabled", "false"));

  static {
    localBranchName = System.getProperty("branchName", System.getenv("GIT_LOCAL_BRANCH"));
    eyes.setIsDisabled(APPLITOOLS_KEY == null || !APPLITOOLS_ENABLED);

    if (!eyes.getIsDisabled()) {
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

      // For local testing or ci runs with main branch, set the branchName and parentBranchName
      if (batchId == null || "main".equalsIgnoreCase(localBranchName)) {
        eyes.setBranchName(
            localBranchName.equalsIgnoreCase("main") ? "sonatype/insight-brain/main" : localBranchName);
        eyes.setParentBranchName(System.getProperty("parentBranchName", "sonatype/insight-brain/main"));
      }

      eyes.setApiKey(APPLITOOLS_KEY);
      eyes.setBatch(batch);
      eyes.setHideCaret(false);
      eyes.setHideScrollbars(false);

      if (StringUtils.isNotBlank(APPLITOOLS_LOG_FILE_NAME)) {
        eyes.setLogHandler(new FileLogger(APPLITOOLS_LOG_FILE_NAME, true, true));
      }
    }
  }

  @Override
  protected void starting(Description description) {
    Logger log = LoggerFactory.getLogger(EyesWatcher.class);
    log.info("Starting EyesWatcher with batch id: {}, localBranchName: {}, eyes enabled? {}.", batchId, localBranchName,
        !eyes.getIsDisabled());
    if (!eyes.getIsDisabled() && batchId == null && localBranchName == null) {
      throw new IllegalArgumentException(
          "The branchName parameter or the Jenkins environment variables are required if visual testing is enabled " +
              "(the applitoolsKey property is provided).");
    }
    testName = description.getTestClass().getSimpleName() + "." + description.getMethodName();
  }

  @Override
  protected void finished(Description description) {
    try {
      // End visual testing. Validate visual correctness.
      if (eyes.getIsOpen()) {
        eyes.close(true);
      }
    }
    finally {
      testName = null;
      // Abort test in case of an unexpected error.
      eyes.abortIfNotClosed();
    }
  }

  public void eyesCheck() {
    eyesCheck(null);
  }

  public void eyesCheck(boolean ignoreDisplacements) {
    eyesCheck(null, ignoreDisplacements);
  }

  public void eyesCheck(String tag) {
    eyesCheck(tag, false);
  }

  /**
   * Convenience method for performing the applitools validation. This method also adds an ignore region for the iq
   * version number. Additionally, the validation call is configured to ignore cursors.
   *
   * @param tag or step name of the validation
   */
  public void eyesCheck(String tag, boolean ignoreDisplacements) {
    if (eyes.getIsDisabled()) {
      return;
    }

    WebDriver remoteDriver = WebDriverRunner.getAndCheckWebDriver();

    // Unwrap the the driver if needed to get the remote driver which is required by applitools.
    if (remoteDriver instanceof WrapsDriver) {
      remoteDriver = ((WrapsDriver) remoteDriver).getWrappedDriver();
    }
    if (!eyes.getIsOpen()) {
      eyes.open(remoteDriver, "IQ Server", testName);
    }

    Iterable<By> ignoreRegions = Arrays.asList(
        By.className("visual-testing-ignore"),
        By.className("nx-global-sidebar__release")
    );
    SeleniumCheckSettings settings = Target.window();
    for (By ignoreRegion : ignoreRegions) {
      settings = ignoreBySelector(ignoreRegion, remoteDriver, settings);
    }

    eyes.check(tag, settings.ignoreDisplacements(ignoreDisplacements));
  }

  private SeleniumCheckSettings ignoreBySelector(By selector, WebDriver remoteDriver, SeleniumCheckSettings settings) {
    for (WebElement element : remoteDriver.findElements(selector)) {
      settings = settings.ignore(element);
    }

    return settings;
  }

  public static File screenshot(String destFilename) {
    try {
      WebDriver driver = WebDriverRunner.getWebDriver();
      File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
      File destFile = new File(destFilename);
      FileUtils.copyFile(scrFile, destFile);
      return destFile;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
