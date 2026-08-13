/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.utils;

import java.util.regex.Pattern;

import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Shared URL assertion helpers for Playwright page objects.
 * <p>
 * Uses regex partial matching so SPA hash routes (e.g. {@code #/advancedSearch?...}) are
 * asserted reliably without glob limitations on URL fragments.
 */
public final class PlaywrightUrlAssertions
{
  private PlaywrightUrlAssertions() {
  }

  /**
   * Builds a regex that matches any URL containing {@code urlSubstring} literally.
   *
   * <p>
   * {@code Pattern.quote()} is not used here because it wraps the input in {@code \Q...\E}
   * quotation markers — a Java-only extension that Playwright's JavaScript regex engine does not
   * understand. Metacharacters are therefore escaped manually to produce a pattern valid in both
   * Java and JavaScript.
   */
  public static Pattern urlContainingPattern(String urlSubstring) {
    String escaped = urlSubstring.replaceAll("([\\\\^$.|?*+()\\[\\]{}])", "\\\\$1");
    return Pattern.compile(".*" + escaped + ".*");
  }

  /**
   * Asserts {@code page} navigated to a URL containing {@code urlSubstring} (a literal substring,
   * not a URL fragment in the RFC 3986 sense).
   */
  public static void assertUrlContaining(Page page, String urlSubstring) {
    assertThat(page).hasURL(urlContainingPattern(urlSubstring));
  }
}
