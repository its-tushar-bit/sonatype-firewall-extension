/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Single source of truth for the default IQ admin credentials used by test infrastructure
 * that needs to call the IQ REST API directly (i.e. outside the Playwright browser session,
 * such as pre-test data seeding).
 * <p>
 * Honors the same {@code IQ_ADMIN_USERNAME} / {@code IQ_ADMIN_PASSWORD} system-property
 * overrides used by the Playwright UI login (see
 * {@code AbstractIqUiTest#playwrightLogin()} and
 * {@code LoginPage#getAdminPassword()}), so a single override applies everywhere.
 * <p>
 * <b>Not for in-browser API calls.</b> Tests that already have a logged-in Playwright
 * {@code BrowserContext} should rely on the existing session cookie rather than send a
 * Basic auth header. This helper is for setup paths that run from the JUnit JVM before
 * the browser session exists.
 */
public final class TestCredentials
{
  public static final String ADMIN_USERNAME = System.getProperty("IQ_ADMIN_USERNAME", "admin");

  public static final String ADMIN_PASSWORD = System.getProperty("IQ_ADMIN_PASSWORD", "admin123");

  /** Display name of the built-in admin account (first_name + last_name from schema.sql). */
  public static final String ADMIN_DISPLAY_NAME = "Admin BuiltIn";

  private TestCredentials() {
  }

  /**
   * @return a {@code Basic <base64>} value for the {@code Authorization} HTTP header,
   *         using the default admin credentials.
   */
  public static String basicAuthHeader() {
    String token = ADMIN_USERNAME + ":" + ADMIN_PASSWORD;
    return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
  }
}
