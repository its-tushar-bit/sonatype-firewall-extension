/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.WebElementCondition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.NexusOnePage;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;

import static java.awt.Color.RGBtoHSB;
import static com.codeborne.selenide.Condition.visible;

/**
 * Verifies that the Nexus One SPA correctly reacts to theme settings,
 * including live OS color scheme changes via {@code prefers-color-scheme}.
 *
 * <p>
 * Rather than testing CSS class names, these tests verify actual rendered colors:
 * a dark theme should have a dark background (&lt; 50% brightness) with light text,
 * and a light theme should have a light background (&gt; 50% brightness) with dark text.
 * </p>
 *
 * <p>
 * System color-scheme changes are emulated via Chrome DevTools Protocol
 * {@code Emulation.setEmulatedMedia}, sent as an HTTP POST through the Selenium Grid's
 * {@code /goog/cdp/execute} endpoint.
 * </p>
 */
public class NexusOneThemeTest
    extends AbstractFunctionalTest
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final HttpClient HTTP = HttpClient.newHttpClient();

  /** Midpoint brightness — above is "light", below is "dark". */
  private static final float MIDPOINT = 0.5f;

  @Before
  public void enableNexusOneUiForTest() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    ensureLoggedInOnClassicShell();
  }

  @After
  public void resetThemeState() {
    // Clear emulated media features and localStorage so other tests aren't affected
    executeJavaScript("localStorage.removeItem('nosc.themeMode')");
    emulateColorScheme(null);
    hardreset();
  }

  private static void ensureLoggedInOnClassicShell() {
    hardreset();
    refreshOrOpen(IndexPage.url());
    LoginModal loginModal = new LoginModal();
    if (loginModal.getElement().is(visible)) {
      loginAsAdmin();
      return;
    }
    if (MainHeader.loginButton().is(visible)) {
      MainHeader.loginButton().click();
      loginAsAdmin();
      return;
    }
    MainHeader.userMenu().dropdownToggle().shouldBe(visible);
  }

  @Test
  public void testSystemLightModeProducesLightAppearance() {
    emulateColorScheme("light");
    var page = openNexusOnePage();

    assertLightAppearance(page);
  }

  @Test
  public void testSystemDarkModeProducesDarkAppearance() {
    emulateColorScheme("dark");
    var page = openNexusOnePage();

    assertDarkAppearance(page);
  }

  @Test
  public void testThemeUpdatesReactivelyWhenSystemColorSchemeChanges() {
    emulateColorScheme("light");
    var page = openNexusOnePage();
    assertLightAppearance(page);

    // Switch OS to dark mode without reloading the page
    emulateColorScheme("dark");

    assertDarkAppearance(page);
  }

  @Test
  public void testExplicitDarkThemeOverridesSystemPreference() {
    emulateColorScheme("light");
    var page = openNexusOnePage();
    assertLightAppearance(page);

    setThemeModeViaLocalStorage("dark");

    assertDarkAppearance(page);
  }

  @Test
  public void testExplicitLightThemeOverridesSystemPreference() {
    emulateColorScheme("dark");
    var page = openNexusOnePage();
    assertDarkAppearance(page);

    setThemeModeViaLocalStorage("light");

    assertLightAppearance(page);
  }

  private NexusOnePage openNexusOnePage() {
    refreshOrOpen(NexusOnePage.url("/home"));
    NexusOnePage page = new NexusOnePage();
    page.shouldBe(visible);
    return page;
  }

  // ---- assertions ----

  private void assertLightAppearance(NexusOnePage page) {
    page.shouldHave(lightBackground());
    page.heading().shouldHave(darkColor());
  }

  private void assertDarkAppearance(NexusOnePage page) {
    page.shouldHave(darkBackground());
    page.heading().shouldHave(lightColor());
  }

  // ---- custom Selenide conditions ----

  private static WebElementCondition lightBackground() {
    return brightnessCondition("light background", "background-color", true);
  }

  private static WebElementCondition darkBackground() {
    return brightnessCondition("dark background", "background-color", false);
  }

  private static WebElementCondition lightColor() {
    return brightnessCondition("light text color", "color", true);
  }

  private static WebElementCondition darkColor() {
    return brightnessCondition("dark text color", "color", false);
  }

  private static WebElementCondition brightnessCondition(String name, String cssProperty, boolean expectLight) {
    return new WebElementCondition(name)
    {
      @Override
      public CheckResult check(Driver driver, WebElement element) {
        float brightness = getBrightness(element.getCssValue(cssProperty));
        boolean pass = expectLight ? brightness > MIDPOINT : brightness < MIDPOINT;
        return new CheckResult(pass, String.format("brightness %.3f", brightness));
      }
    };
  }

  /**
   * Returns the HSB brightness/value (0..1) for a CSS color string.
   */
  private static float getBrightness(String cssColor) {
    var awt = org.openqa.selenium.support.Color.fromString(cssColor).getColor();
    return RGBtoHSB(awt.getRed(), awt.getGreen(), awt.getBlue(), null)[2];
  }

  // ---- browser helpers ----

  /**
   * Uses the Chrome DevTools Protocol {@code Emulation.setEmulatedMedia} command to emulate a
   * {@code prefers-color-scheme} media feature.
   *
   * <p>
   * Sends the command as an HTTP POST to the Selenium Grid's {@code /goog/cdp/execute}
   * endpoint, bypassing the {@code Augmenter} (which tries to open a WebSocket that can't
   * reach the container-internal CDP address).
   * </p>
   *
   * @param colorScheme "light", "dark", or {@code null} to clear the override
   */
  private static void emulateColorScheme(String colorScheme) {
    var driver = (RemoteWebDriver) WebDriverRunner.getWebDriver();
    var executor = (HttpCommandExecutor) driver.getCommandExecutor();
    var gridUrl = executor.getAddressOfRemoteServer();
    var sessionId = driver.getSessionId().toString();

    List<Map<String, Object>> featureList = colorScheme == null
        ? List.of()
        : List.of(Map.of("name", "prefers-color-scheme", "value", colorScheme));

    var body = Map.of(
        "cmd", "Emulation.setEmulatedMedia",
        "params", Map.of("features", featureList));

    URI cdpEndpoint = URI.create(
        gridUrl + "/session/" + sessionId + "/goog/cdp/execute");

    try {
      HttpRequest request = HttpRequest.newBuilder(cdpEndpoint)
          .header("Content-Type", "application/json")
          .POST(BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
          .build();

      HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400) {
        throw new RuntimeException(
            "CDP command failed (HTTP " + response.statusCode() + "): " + response.body());
      }
    }
    catch (IOException | InterruptedException e) {
      throw new RuntimeException("Failed to execute CDP command Emulation.setEmulatedMedia", e);
    }
  }

  /**
   * Sets the Nexus One theme mode in localStorage and dispatches the same-tab
   * event that {@code useNoscTheme} listens for.
   */
  private void setThemeModeViaLocalStorage(String mode) {
    executeJavaScript(
        "localStorage.setItem('nosc.themeMode', '" + mode + "');" +
            "window.dispatchEvent(new CustomEvent('nosc.themeMode.change', {" +
            "  detail: { themeMode: '" + mode + "' }" +
            "}))");
  }
}
