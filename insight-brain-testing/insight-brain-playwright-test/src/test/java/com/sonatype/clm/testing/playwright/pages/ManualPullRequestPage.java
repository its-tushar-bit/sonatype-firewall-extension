/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Page object for {@code CreatePRModal.jsx} — the Manual Pull Request creation modal.
 * Divergence: all fields are read-only; no description field.
 */
public class ManualPullRequestPage
    extends BasePage
{
  public Locator container() {
    return locator("#iq-create-pr-modal");
  }

  public Locator header() {
    return container().locator("#iq-create-pr-modal-header");
  }

  public Locator prTitleField() {
    return container().locator("#iq-create-pr-modal-pr-title");
  }

  public Locator targetBranchField() {
    return container().locator("#iq-create-pr-modal-default-branch");
  }

  public Locator createButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Create"));
  }

}
