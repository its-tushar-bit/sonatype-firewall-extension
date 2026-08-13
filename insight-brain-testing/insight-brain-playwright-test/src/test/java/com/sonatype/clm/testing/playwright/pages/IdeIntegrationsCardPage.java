/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/** IDE Integrations card on the Developer Dashboard Overview. */
public class IdeIntegrationsCardPage
    extends BasePage
{
  public IdeIntegrationsCardPage() {
    super();
  }

  public Locator card() {
    return byRole(AriaRole.REGION, "Integrate using IDEs");
  }

  public Locator heading() {
    return card().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(3).setName("Sync with IDEs"));
  }

  public Locator footerLink() {
    return card().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("See our list of IDE integrations"));
  }

}
