/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.chrome.ChromeOptions;

public class SeleniumChromeOptions
{
  public static ChromeOptions chromeOptions(final int viewportWidth, final int viewportHeight) {
    ChromeOptions options = new ChromeOptions();
    options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.IGNORE);

    options.addArguments("enable-automation"); // https://stackoverflow.com/a/43840128/1689770

    options.addArguments("--headless=new");
    options.addArguments("--no-sandbox"); //https://stackoverflow.com/a/50725918/1689770
    options.addArguments("--disable-dev-shm-usage"); //https://stackoverflow.com/a/50725918/1689770
    options.addArguments("--disable-browser-side-navigation"); //https://stackoverflow.com/a/49123152/1689770
    options.addArguments("--window-size=" + viewportWidth + "," + viewportHeight);

    options.addArguments("test-type");
    options.addArguments("--disable-gpu");

    // latest version of chrome will pop-up warnings about passwords from data breaches
    options.addArguments("--disable-extensions");
    options.addArguments("--disable-notifications");
    Map<String, Object> prefs = new HashMap<>();
    prefs.put("credentials_enable_service", false);
    prefs.put("profile.password_manager_enabled", false);
    options.setExperimentalOption("prefs", prefs);

    return options;
  }
}
