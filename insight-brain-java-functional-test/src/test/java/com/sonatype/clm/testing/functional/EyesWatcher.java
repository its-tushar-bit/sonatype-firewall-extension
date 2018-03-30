/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import java.util.Date;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.fluent.SeleniumCheckSettings;
import com.applitools.eyes.selenium.fluent.Target;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.internal.WrapsDriver;

public class EyesWatcher
    extends TestWatcher
{
  public static Eyes eyes = new Eyes();

  private String testName;

  private static BatchInfo batch;

  private static final String APPLITOOLS_KEY = System.getProperty("applitoolsKey");

  static {
    String localBranchName = System.getProperty("branchName", System.getenv("bamboo_planRepository_branchName"));
    eyes.setIsDisabled(APPLITOOLS_KEY == null);

    if (!eyes.getIsDisabled() && localBranchName != null) {
      String buildNumber = System.getenv("bamboo_buildNumber");
      batch = new BatchInfo(localBranchName + (buildNumber != null ? " #" + buildNumber : ""));

      // Aggregates tests under the same batch when tests are run in different processes (e.g. split tests in bamboo).
      batch.setId(buildNumber != null ? batch.getName() : batch.getName() + new Date().toString());
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

      eyes.open(remoteDriver, "IQ Server", testName, new RectangleSize(1366, 1024));
    }
    By iqVersion = By.cssSelector(".iq-title__version");
    SeleniumCheckSettings settings = Target.window();
    if (!WebDriverRunner.getWebDriver().findElements(iqVersion).isEmpty()) {
      settings = settings.ignore(iqVersion);
    }
    
    eyes.check(tag, settings.ignoreCaret());
  }
}
