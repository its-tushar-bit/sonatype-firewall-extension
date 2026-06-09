/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class OrgsAndPoliciesSidebarComponent
    extends BasePage
{
  private static final String ROOT = ".nx-page-sidebar.iq-orgs-and-policies-summary-sidebar";

  private static final String ORGANIZATIONS_GROUP = "#organizations-collapsible";

  private static final String APPLICATIONS_GROUP = "#applications-collapsible";

  private static final String OWNER_EDITOR_MODAL = "#owner-editor";

  private static final Locator.GetByRoleOptions ADD_APPLICATION_OPTS =
      new Locator.GetByRoleOptions().setName("Add Application");

  private static final Locator.GetByRoleOptions NEW_APPLICATION_OPTS =
      new Locator.GetByRoleOptions().setName("New Application");

  public OrgsAndPoliciesSidebarComponent() {
    super();
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator selectedOwner() {
    return locator(ROOT + " .iq-selected-org");
  }

  public Locator organizationsGroup() {
    return locator(ROOT + " " + ORGANIZATIONS_GROUP);
  }

  public Locator organizationLinks() {
    return locator(ROOT + " " + ORGANIZATIONS_GROUP + " a[role=\"menuitem\"]");
  }

  public Locator applicationsGroup() {
    return locator(ROOT + " " + APPLICATIONS_GROUP);
  }

  public Locator addApplicationDropdownTrigger() {
    return container().getByRole(AriaRole.BUTTON, ADD_APPLICATION_OPTS);
  }

  public Locator newApplicationOption() {
    return container().getByRole(AriaRole.BUTTON, NEW_APPLICATION_OPTS);
  }

  public Locator ownerEditorModal() {
    return locator(OWNER_EDITOR_MODAL);
  }

  public Locator treeViewLink() {
    return container().getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Tree View"));
  }

  public void openTreeView() {
    assertThat(container()).isVisible();
    treeViewLink().click();
    page.waitForURL(java.util.regex.Pattern.compile(".*/management/tree.*"));
  }

  public boolean isTreeViewLinkVisible() {
    return treeViewLink().isVisible();
  }

  public Locator openNewApplicationModal() {
    addApplicationDropdownTrigger().click();
    Locator option = newApplicationOption();
    assertThat(option).isVisible();
    option.click();
    Locator modal = ownerEditorModal();
    assertThat(modal).isVisible();
    return modal;
  }

}
