/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * GitHub App auth surface on the SCM configuration editor (GitHubAppAuthenticationMethod.jsx).
 *
 * <p>
 * Radio controls are exposed via {@link NxRadio} — use {@code .input()} for
 * {@code isChecked}/{@code isAttached} assertions and {@code .label()} for clicks and
 * visibility checks.
 */
public class GitHubAppAuthPage
    extends BasePage
{
  // Inherit radio's accessible name is dynamic: "Inherit from <parent>" or "Inherit (Not Configured)".
  private static final Pattern INHERIT_RADIO_NAME = Pattern.compile("^Inherit\\b.*");

  // Status text is one of "N GitHub Apps configured" or "Inherit N GitHub Apps from <parent>".
  private static final Pattern GITHUB_APP_STATUS_TEXT =
      Pattern.compile("GitHub Apps? (configured|from )");

  public GitHubAppAuthPage() {
    super();
  }

  public Locator registrationModal() {
    return page.getByRole(AriaRole.DIALOG,
        new Page.GetByRoleOptions().setName("Connect to GitHub"));
  }

  public NxRadio registrationModalOrgAccountRadio() {
    return NxRadio.of(registrationModal(), "Organization Account (recommended)");
  }

  public NxRadio registrationModalPersonalAccountRadio() {
    return NxRadio.of(registrationModal(), "Personal Account");
  }

  public Locator registrationModalOrgNameInput() {
    return registrationModal().getByLabel("Organization Name");
  }

  public Locator registrationModalSubmitButton() {
    return registrationModal().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Register & Create GitHub App"));
  }

  public Locator registrationModalCancelButton() {
    return registrationModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator registrationModalValidationError() {
    return registrationModal().getByRole(AriaRole.ALERT,
        new Locator.GetByRoleOptions().setName("form validation errors"));
  }

  public Locator authMethodSection() {
    return page.getByRole(AriaRole.GROUP,
        new Page.GetByRoleOptions().setName("Authentication Method"));
  }

  public NxRadio inheritRadio() {
    return NxRadio.of(authMethodSection(), INHERIT_RADIO_NAME);
  }

  public NxRadio overrideRadio() {
    return NxRadio.of(authMethodSection(), "Override");
  }

  public NxRadio authTypeGitHubAppRadio() {
    return NxRadio.of(authMethodSection(), "GitHub App (Recommended)");
  }

  public NxRadio authTypePatRadio() {
    return NxRadio.of(authMethodSection(), "Personal Access Token");
  }

  // Exact match required — "Personal Access Token" radio's accessible name contains "Access Token".
  public Locator patTokenInput() {
    return authMethodSection().getByLabel("Access Token",
        new Locator.GetByLabelOptions().setExact(true));
  }

  public Locator gitHubAppStatusSection() {
    return authMethodSection().getByText(GITHUB_APP_STATUS_TEXT);
  }

  public Locator addGitHubAppButton() {
    return authMethodSection().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Add GitHub App"));
  }
}
