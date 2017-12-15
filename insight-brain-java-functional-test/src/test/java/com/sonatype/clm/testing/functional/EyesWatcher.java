/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import java.text.SimpleDateFormat;
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

  private static BatchInfo batch = new BatchInfo(new SimpleDateFormat("MMM dd yyyy").format(new Date()));

  private static final String APPLITOOLS_KEY = "bg21K3t6KY1073109q6J9lZzCQBfxEDGh5tHgYR9wl1kHxk110";

  static {
    eyes.setIsDisabled(!Boolean.getBoolean("visualTestingEnabled"));

    batch.setId(batch.getName());
    eyes.setApiKey(APPLITOOLS_KEY);
    // used for filtering in applitools eyes
    eyes.addProperty("Build #", System.getenv("bamboo_buildNumber"));
    eyes.setBatch(batch);

    String localBranchName = System.getProperty("branchName");
    eyes.setBranchName(localBranchName != null ? localBranchName : System.getenv("bamboo_planRepository_branchName"));

    // needs to be set only if the parent branch is something other than master
    eyes.setParentBranchName(System.getProperty("parentBranchName"));
  }

  protected void starting(Description description) {
    WebDriver remoteDriver = WebDriverRunner.getAndCheckWebDriver();

    if (remoteDriver instanceof WrapsDriver) {
      remoteDriver = ((WrapsDriver) remoteDriver).getWrappedDriver();
    }

    // test name is derived from the class and test method name
    eyes.open(remoteDriver, "IQ Server",
        description.getTestClass().getSimpleName() + "." + description.getMethodName(),
        new RectangleSize(1366, 1024));
  }

  protected void finished(Description description) {
    try {
      // End visual testing. Validate visual correctness.
      eyes.close(true);
    }
    finally {
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
    eyes.check(tag, Target.window().ignore(By.cssSelector(".iq-title__version")).ignoreCaret());
  }
}
