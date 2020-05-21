/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import java.util.Locale;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.fluent.SeleniumCheckSettings;
import com.applitools.eyes.selenium.fluent.Target;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;

public class EyesWatcher
    extends TestWatcher
{
  public static Eyes eyes = new Eyes();

  private String testName;

  private static BatchInfo batch;

  private static String batchId;

  private static String localBranchName;

  private static final String APPLITOOLS_KEY = System.getProperty("applitoolsKey");

  static {
    localBranchName = System.getProperty("branchName", System.getenv("GIT_LOCAL_BRANCH"));
    eyes.setIsDisabled(APPLITOOLS_KEY == null || !isApplitoolsEnabled());

    if (!eyes.getIsDisabled()) {
      batchId = System.getenv("APPLITOOLS_BATCH_ID"); // batch id set by Applitools jenkins plugin

      // Set only once per Jenkins job. Note, we set the batch name to null if we are building for a pr - the github
      // integration takes care of this. We are making some assumptions here since there is no easy way atm to know if
      // there is a pr associated with the branch that is under test (parameterized builds aren't available for the
      // brain just yet). For local testing (no batchId) we use the branch name.
      batch = new BatchInfo(batchId == null ? localBranchName : null);
      if (batchId != null) { // no need to set the id for local testing
        batch.setId(batchId);
      }

      // For local testing or ci runs with master set the branchName and parentBranchName
      if ((batchId != null && "master".equalsIgnoreCase(localBranchName)) || batchId == null) {
        eyes.setBranchName(
            localBranchName.equalsIgnoreCase("master") ? "sonatype/insight-brain/master" : localBranchName);
        eyes.setParentBranchName(System.getProperty("parentBranchName", "sonatype/insight-brain/master"));
      }

      eyes.setApiKey(APPLITOOLS_KEY);
      eyes.setBatch(batch);
      eyes.setHideCaret(false);
      eyes.setHideScrollbars(false);
    }
  }

  @Override
  protected void starting(Description description) {
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
        // only fail the build if on master
        eyes.close(isMaster());
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
    By ignoreRegion = By.className("visual-testing-ignore");
    SeleniumCheckSettings settings = Target.window();
    for (WebElement element : remoteDriver.findElements(ignoreRegion)) {
      settings = settings.ignore(element);
    }

    eyes.check(tag, settings.ignoreDisplacements(ignoreDisplacements));
  }

  private static boolean isMaster() {
    return "master".equals(localBranchName);
  }

  private static boolean isApplitoolsEnabled() {
    return isMaster() || localBranchName.toLowerCase(Locale.ENGLISH).contains("_ui");
  }
}
