/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.utils;

import java.net.URL;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringWebDriver;

/**
 * Tweaks loaded pages to ease testing, mostly exists to work around EventFiringWebDriver's inability to notify
 * listeners of calls to {@code refresh()} which would have done the trick otherwise.
 */
public class PageTweakingWebDriver
    extends EventFiringWebDriver
{
  public PageTweakingWebDriver(WebDriver driver) {
    super(driver);
  }

  protected void injectTweaks() {
    executeScript("jQuery(document).ready(function() { jQuery('head').append('<style>"
        // Fully disabling transitions breaks bootstrap, so we're a little more selective
        + ".fade { transition: opacity 1ms } "
        + ".modal.fade { transition: top 0ms, opacity 0ms } "
        + ".collapse { transition: height 1ms }" + "</style>'); });");
  }

  @Override
  public void get(String url) {
    super.get(url);
    injectTweaks();
  }

  @Override
  public Navigation navigate() {
    return new PageTweakingNavigation(super.navigate());
  }

  private class PageTweakingNavigation
      implements Navigation
  {
    private final Navigation navigation;

    public PageTweakingNavigation(Navigation navigation) {
      this.navigation = navigation;
    }

    @Override
    public void back() {
      navigation.back();
      injectTweaks();
    }

    @Override
    public void forward() {
      navigation.forward();
      injectTweaks();
    }

    @Override
    public void to(String url) {
      navigation.to(url);
      injectTweaks();
    }

    @Override
    public void to(URL url) {
      navigation.to(url);
      injectTweaks();
    }

    @Override
    public void refresh() {
      navigation.refresh();
      injectTweaks();
    }
  }
}
