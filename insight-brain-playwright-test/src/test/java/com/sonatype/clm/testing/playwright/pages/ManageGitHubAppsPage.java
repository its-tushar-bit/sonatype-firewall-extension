/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * The Manage GitHub Applications page for an owner
 * ({@code #manage-github-applications}), reached via the "Manage GitHub Apps" button on the
 * Source Control configuration editor.
 *
 * <p>
 * URL fragment: {@code /manage-github-apps} appended to the owner edit route
 * (e.g. {@code /management/edit/organization/{id}/manage-github-apps}).
 */
public class ManageGitHubAppsPage
    extends BasePage
{
  /** Hash-route suffix used in {@code OrgsAndPolicies/route.js}. */
  public static final String URL_FRAGMENT = "/manage-github-apps";

  public ManageGitHubAppsPage() {
    super();
  }

  /** React renders a plain {@code <div>}, not {@code NxPageMain}, so no {@code role="main"} is reachable. */
  public Locator container() {
    return locator("#manage-github-applications");
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Manage GitHub Applications"));
  }

  /**
   * Table body rows — one per registered GitHub App. Scoped to {@code <tbody>} so the
   * column-header row inside {@code <thead>} is structurally excluded; no fragile text filter.
   */
  public Locator appTableRows() {
    return container().locator("tbody").getByRole(AriaRole.ROW);
  }

  public Locator deleteButtonForApp(String slug) {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Delete GitHub App " + slug));
  }

  /** Row in the apps table whose "GitHub Application" cell contains the given slug. */
  public Locator rowForSlug(String slug) {
    return appTableRows().filter(new Locator.FilterOptions().setHasText(slug));
  }

  public Locator addGitHubAppButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Add GitHub App"));
  }

  public Locator deleteConfirmModal() {
    return page.getByRole(AriaRole.DIALOG)
        .filter(
            new Locator.FilterOptions().setHasText("Remove GitHub App configuration?"));
  }

  public Locator deleteConfirmModalHeading() {
    return deleteConfirmModal().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Remove GitHub App configuration?"));
  }

  public Locator deleteConfirmButton() {
    return deleteConfirmModal().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Confirm Deletion").setExact(true));
  }

  public Locator deleteCancelButton() {
    return deleteConfirmModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator emptyStateParagraph() {
    return container().getByText("There are no GitHub Apps configured for this organization.");
  }
}
