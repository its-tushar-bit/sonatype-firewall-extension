/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

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
import org.openqa.selenium.internal.WrapsDriver;

public class EyesWatcher
    extends TestWatcher
{
  public static Eyes eyes = new Eyes();

  private String testName;

  private static BatchInfo batch;

  private static final String APPLITOOLS_KEY = System.getProperty("applitoolsKey");

  static {
    String localBranchName = System.getProperty("branchName", System.getenv("GIT_LOCAL_BRANCH"));
    eyes.setIsDisabled(APPLITOOLS_KEY == null);

    if (!eyes.getIsDisabled() && localBranchName != null) {
      String batchName = System.getenv("APPLITOOLS_BATCH_NAME"); // batch name set by Applitools jenkins plugin
      String batchId = System.getenv("APPLITOOLS_BATCH_ID"); // batch id set by Applitools jenkins plugin

      // Set only once per Jenkins job
      batch = new BatchInfo(batchName != null ? batchName : localBranchName);
      if (batchId != null) { // no need to set the id for local testing
        batch.setId(batchId);
      }

      eyes.setApiKey(APPLITOOLS_KEY);
      eyes.setBatch(batch);

      eyes.setBranchName(localBranchName);

      // set the default parent branch to master if the parent branch is not specified
      eyes.setParentBranchName(System.getProperty("parentBranchName", "master"));
    }
  }

  @Override
  protected void starting(Description description) {
    if (!eyes.getIsDisabled() && eyes.getBatch() == null) {
      throw new IllegalArgumentException(
          "The branchName parameter or the Bamboo environment variables are required if visual testing is enabled " + 
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

  /**
   * Convenience method for performing the applitools validation. This method also adds an ignore region for the iq
   * version number. Additionally, the validation call is configured to ignore cursors.
   *
   * @param tag or step name of the validation
   */
  public void eyesCheck(String tag) {
    if (!eyes.getIsOpen()) {
      WebDriver remoteDriver = WebDriverRunner.getAndCheckWebDriver();

      if (remoteDriver instanceof WrapsDriver) {
        remoteDriver = ((WrapsDriver) remoteDriver).getWrappedDriver();
      }

      eyes.open(remoteDriver, "IQ Server", testName);
    }
    By ignoreRegion = By.className("visual-testing-ignore");
    SeleniumCheckSettings settings = Target.window();
    for (WebElement element : WebDriverRunner.getWebDriver().findElements(ignoreRegion)) {
      settings = settings.ignore(element);
    }
    
    eyes.check(tag, settings.ignoreCaret());
  }
}
