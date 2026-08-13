/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** Page object for the CI/CD Configuration modal (opened from the risk table's CI/CD button). */
public class CiCdConfigurationModalPage
    extends BasePage
{
  // Wizard panel is a styled div with no ARIA role; id is the only stable anchor.
  private static final String WIZARD_ROOT = "#iq-integrations-cicd-wizard";

  private static final String SNIPPET_ID = "#jenkins-pipeline-script";

  public CiCdConfigurationModalPage() {
    super();
  }

  // Match by role+contained heading: NxModal doesn't surface its title as the dialog's accessible name.
  public Locator modal() {
    return byRole(AriaRole.DIALOG).filter(new Locator.FilterOptions()
        .setHas(page.getByRole(AriaRole.HEADING,
            new Page.GetByRoleOptions().setLevel(1).setName("CI/CD Configuration"))));
  }

  public Locator modalTitle() {
    return modal().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(1).setName("CI/CD Configuration"));
  }

  public Locator wizardContainer() {
    return modal().locator(WIZARD_ROOT);
  }

  public Locator stepCard(String accessibleName) {
    return wizardContainer().getByRole(AriaRole.REGION,
        new Locator.GetByRoleOptions().setName(accessibleName));
  }

  public Locator viewDocumentationLinks() {
    return wizardContainer().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("View documentation"));
  }

  public Locator moreInfoLink() {
    return wizardContainer().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("Sonatype Documentation"));
  }

  public Locator pipelineSnippet() {
    return wizardContainer().locator(SNIPPET_ID);
  }

  public Locator copyToClipboardButton() {
    return pipelineSnippet().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Copy to clipboard"));
  }

  public Locator parameterDescriptionList() {
    return wizardContainer().locator(".iq-integrations-description-list-cicd");
  }

  public Locator closeButton() {
    return modal().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Close"));
  }
}
