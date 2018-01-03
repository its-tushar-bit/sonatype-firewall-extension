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

  private static final String APPLITOOLS_KEY = "bg21K3t6KY1073109q6J9lZzCQBfxEDGh5tHgYR9wl1kHxk110";

  static {
    String localBranchName = System.getProperty("branchName", System.getenv("bamboo_planRepository_branchName"));
    eyes.setIsDisabled(!Boolean.getBoolean("visualTestingEnabled"));

    if (localBranchName != null) {
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
          "The branchName parameter or the Bamboo environment variables are required if visualTestingEnabled is true");
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
    eyes.check(tag, Target.window().ignore(By.cssSelector(".iq-title__version")).ignoreCaret());
  }
}
