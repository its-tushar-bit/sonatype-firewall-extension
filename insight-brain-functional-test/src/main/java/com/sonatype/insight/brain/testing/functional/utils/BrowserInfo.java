/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WrapsDriver;

/**
 * Helps to account for browser-specific behavior.
 */
public class BrowserInfo
{
  private static boolean chrome;

  private static boolean firefox;

  private static boolean ie;

  private static boolean phantom;

  private static boolean safari;

  public static void init(WebDriver driver) {
    chrome = firefox = ie = phantom = safari = false;
    while (driver instanceof WrapsDriver) {
      driver = ((WrapsDriver) driver).getWrappedDriver();
    }
    String name = driver.getClass().getSimpleName();
    if (name.equals("PhantomJSDriver")) {
      phantom = true;
    }
    else if (name.equals("FirefoxDriver")) {
      firefox = true;
    }
    else if (name.equals("ChromeDriver")) {
      chrome = true;
    }
    else if (name.equals("SafariDriver")) {
      safari = true;
    }
    else if (name.equals("InternetExplorerDriver")) {
      ie = true;
    }
  }

  public static boolean isChrome() {
    return chrome;
  }

  public static boolean isFirefox() {
    return firefox;
  }

  public static boolean isIe() {
    return ie;
  }

  public static boolean isPhantom() {
    return phantom;
  }

  public static boolean isSafari() {
    return safari;
  }
}
