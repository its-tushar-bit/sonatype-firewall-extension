/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** Page object for the SCM Integrations modal opened from the Developer Dashboard risk table. */
public class ScmWizardPage
    extends BasePage
{
  public ScmWizardPage() {
    super();
  }

  // Exception to byRole(DIALOG, name): NxModal doesn't surface its title heading as the dialog's accessible name.
  public Locator modal() {
    return byRole(AriaRole.DIALOG).filter(new Locator.FilterOptions()
        .setHas(page.getByRole(AriaRole.HEADING,
            new Page.GetByRoleOptions().setLevel(1).setName("SCM Integrations"))));
  }

  public Locator modalTitle() {
    return modal().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(1).setName("SCM Integrations"));
  }

  public Locator wizardCard() {
    return modal().getByRole(AriaRole.REGION, new Locator.GetByRoleOptions().setName("SCM Wizard"));
  }

  public Locator sectionHeading(String headingText) {
    return wizardCard().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(3).setName(headingText));
  }

  public Locator configureBaseUrlSection() {
    return sectionHeading("Configure Base URL");
  }

  public Locator automaticSourceControlLink() {
    return wizardCard().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("Automatic Source Control"));
  }

  public Locator applicationSourceControlLink() {
    return wizardCard().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("click here"));
  }

  public Locator tokenUrlCode(String tokenUrl) {
    return wizardCard().locator("code")
        .filter(
            new Locator.FilterOptions().setHasText(tokenUrl));
  }

}
