/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class GitHubAppAuthPageAssertions
{
  private final GitHubAppAuthPage page;

  public GitHubAppAuthPageAssertions(GitHubAppAuthPage page) {
    this.page = page;
  }

  public void shouldShowRegistrationModal() {
    assertThat(page.registrationModal()).isVisible();
    // NxRadio's <input> is CSS-hidden; assert via isAttached on .input(), not isVisible.
    assertThat(page.registrationModalOrgAccountRadio().input()).isAttached();
    assertThat(page.registrationModalPersonalAccountRadio().input()).isAttached();
  }

  /** Submit with an empty Organization Name surfaces the form-level validation error. */
  public void shouldShowOrgNameValidationError() {
    assertThat(page.registrationModalValidationError())
        .containsText("Organization name is required");
  }

  public void shouldShowInheritanceRadios() {
    assertThat(page.inheritRadio().input()).isAttached();
    assertThat(page.overrideRadio().input()).isAttached();
  }

  public void shouldHaveInheritRadioChecked() {
    assertThat(page.inheritRadio().input()).isChecked();
  }

  public void shouldShowOrgNameInput() {
    assertThat(page.registrationModalOrgNameInput()).isVisible();
  }

  /** Conditionally rendered — {@code hasCount(0)} when accountType is Personal. */
  public void shouldHideOrgNameInput() {
    assertThat(page.registrationModalOrgNameInput()).hasCount(0);
  }

  public void shouldShowPatTokenInput() {
    assertThat(page.patTokenInput()).isVisible();
  }

  public void shouldHidePatTokenInput() {
    assertThat(page.patTokenInput()).hasCount(0);
  }

  public void shouldShowGitHubAppSection() {
    assertThat(page.gitHubAppStatusSection()).isVisible();
  }
}
