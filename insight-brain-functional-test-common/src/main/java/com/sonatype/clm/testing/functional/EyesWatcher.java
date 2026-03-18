/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.fluent.SeleniumCheckSettings;
import com.applitools.eyes.selenium.fluent.Target;
import com.codeborne.selenide.WebDriverRunner;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EyesWatcher
    extends TestWatcher
{
  private Eyes eyes;

  private String testName;

  private final PointerInput mouse = new PointerInput(PointerInput.Kind.MOUSE, "default mouse");

  private final Sequence mouseToOriginSequence = new Sequence(mouse, 1)
      .addAction(mouse.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 0, 0));

  private void initEyes() {
    eyes = EyesWatcherShared.INSTANCE.createEyes();
  }

  @Override
  protected void starting(Description description) {
    if (EyesWatcherShared.isDisabled()) {
      return;
    }
    initEyes();

    String batchId = EyesWatcherShared.INSTANCE.getBatchId();
    String localBranchName = EyesWatcherShared.INSTANCE.getLocalBranchName();

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
    if (EyesWatcherShared.isDisabled()) {
      return;
    }
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
   * @param shouldUnsetCursorAndFocus If true, prior to taking the screenshot the cursor will be moved to the top left
   *          corner of the viewport and the focused elements will be unfocused. After the screenshot, focus will be
   *          restored
   *          (the mouse will not be moved back however)
   */
  public void eyesCheck(String tag, boolean ignoreDisplacements, boolean shouldUnsetCursorAndFocus) {
    if (EyesWatcherShared.isDisabled()) {
      return;
    }

    WebDriver driver = WebDriverRunner.getAndCheckWebDriver();
    RemoteWebDriver remoteDriver;

    // Unwrap the the driver if needed to get the remote driver which is required by applitools.
    if (driver instanceof WrapsDriver) {
      driver = ((WrapsDriver) driver).getWrappedDriver();
    }

    if (driver instanceof RemoteWebDriver) {
      remoteDriver = (RemoteWebDriver) driver;
    }
    else {
      throw new IllegalStateException("WebDriver must be a RemoteWebDriver, but was " + driver);
    }

    if (!eyes.getIsOpen()) {
      eyes.open(remoteDriver, "IQ Server", testName);
    }

    Iterable<By> ignoreRegions = Arrays.asList(
        By.className("visual-testing-ignore"),
        By.className("nx-global-sidebar__release"));
    SeleniumCheckSettings settings = Target.window();
    for (By ignoreRegion : ignoreRegions) {
      settings = ignoreBySelector(ignoreRegion, remoteDriver, settings);
    }

    WebElement focusedElement = null;
    if (shouldUnsetCursorAndFocus) {
      focusedElement = unsetCursorAndKeyboardFocus(remoteDriver);
    }

    eyes.check(tag, settings.ignoreDisplacements(ignoreDisplacements));

    if (focusedElement != null) {
      remoteDriver.executeScript("arguments[0].focus()", focusedElement);
    }
  }

  /**
   * By default the cursor and focus are unset
   */
  public void eyesCheck(String tag, boolean ignoreDisplacements) {
    eyesCheck(tag, ignoreDisplacements, true);
  }

  private WebElement unsetCursorAndKeyboardFocus(RemoteWebDriver webdriver) {
    webdriver.perform(Collections.singletonList(mouseToOriginSequence));
    WebElement focusedElement = webdriver.switchTo().activeElement();

    if (focusedElement != null && !"body".equalsIgnoreCase(focusedElement.getTagName())) {
      webdriver.executeScript("arguments[0].blur()", focusedElement);
    }
    else {
      // don't bother returning the focusedElement if it's the body
      // (which is what gets focus if nothing else is focused)
      focusedElement = null;
    }

    return focusedElement;
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
