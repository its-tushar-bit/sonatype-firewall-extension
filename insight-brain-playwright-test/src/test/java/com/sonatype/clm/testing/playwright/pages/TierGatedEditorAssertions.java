/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Shared assertion helpers for the Pro-tier gating contract described by
 * {@link TierGatedEditorPage}. Entity noun is passed by each caller since it appears in button
 * text ("Add a Label" / "Preview Add a Label", etc.).
 */
public final class TierGatedEditorAssertions
{
  private TierGatedEditorAssertions() {
  }

  public static void shouldShowAddButtonInPreviewMode(TierGatedEditorPage page, String entityNoun) {
    assertThat(page.addEntityButton()).hasText(Pattern.compile("^\\s*Preview Add a " + entityNoun + "\\s*$"));
  }

  /**
   * Asserts the Enterprise-mode button text — anchored regex so "Preview Add a X" cannot
   * spuriously satisfy this assertion. Caller-supplied entity nouns are simple words
   * ("Label", "Category", "Threat Group"); no JS-RegExp escaping needed.
   */
  public static void shouldShowAddButtonInEnterpriseMode(TierGatedEditorPage page, String entityNoun) {
    assertThat(page.addEntityButton()).hasText(Pattern.compile("^\\s*Add a " + entityNoun + "\\s*$"));
  }

  /**
   * Asserts read-only mode (Pro tier, edit path): Submit is CSS-hidden via
   * {@code iq-enterprise-mode-footer}, Delete is unmounted ({@code hasCount(0)}).
   */
  public static void shouldShowReadOnlyViewWithName(TierGatedEditorPage page, String expectedName) {
    assertThat(page.readOnlyEntityView()).isVisible();
    assertThat(page.readOnlyEntityName()).hasText(expectedName);
    assertThat(page.submitButton()).isHidden();
    assertThat(page.deleteButton()).hasCount(0);
  }
}
