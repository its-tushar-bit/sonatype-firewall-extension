/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertions companion for {@link SourceControlConfigurationPage}.
 */
public class SourceControlConfigurationPageAssertions
{
  private final SourceControlConfigurationPage page;

  public SourceControlConfigurationPageAssertions(SourceControlConfigurationPage page) {
    this.page = page;
  }

  public void shouldShowEditorHeading() {
    assertThat(page.container()).isVisible();
    assertThat(page.pageHeading()).isVisible();
  }

  public void shouldShowProviderDropdown() {
    assertThat(page.providerLabel()).hasText("Source Control Management System");
    assertThat(page.providerSelect()).isVisible();
  }

  public void shouldListProviderOptions(List<String> expectedProviderLabels) {
    Locator options = page.providerSelect().locator("option");
    assertThat(options.first()).hasText(Pattern.compile("\\s*--\\s*Not Configured\\s*--\\s*"));
    for (String label : expectedProviderLabels) {
      assertThat(page.providerSelect().locator("option", new Locator.LocatorOptions().setHasText(label)))
          .hasCount(1);
    }
  }

  public void shouldShowAccessTokenField() {
    assertThat(page.accessTokenInput()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
    assertThat(page.accessTokenLabel()).isVisible();
    assertThat(page.accessTokenInput()).hasAttribute("type", "password");
  }

  public void shouldShowDefaultBranchField() {
    assertThat(page.defaultBranchLabel()).hasText("Default Branch");
    assertThat(page.defaultBranchInput()).isVisible();
    assertThat(page.defaultBranchInput()).hasValue("main");
  }

  public void shouldShowToggle(String toggleId, String title) {
    assertThat(page.toggle(toggleId)).isVisible();
    assertThat(page.toggleTitle(toggleId)).hasText(title);
  }

  public void shouldShowAutomatedRemediationCopy() {
    Locator toggleContent =
        page.toggle("source-control-remediation-pull-requests").locator(".iq-source-control-toggle__text");
    assertThat(toggleContent).containsText(
        "Create pull requests with remediation suggestions for policy violations");
    assertThat(toggleContent).containsText(
        "Pull requests for Maven dependencies are generated when the recommended version");
  }

  public void shouldShowAdvancedGitOptions() {
    assertThat(page.advancedGitOptionsBlock()).isVisible();
    assertThat(page.advancedGitOptionsBlock()).containsText("Advanced");
    assertThat(page.advancedGitOptionsBlock()).containsText(
        "Close AutoPRs that have not been merged or closed after:");
    assertThat(page.advancedGitOptionsBlock()).containsText("Days");
  }

  public void shouldShowCreateButton() {
    assertThat(page.submitButton()).isVisible();
    assertThat(page.submitButton()).hasText(Pattern.compile("Create|Update"));
  }
}
