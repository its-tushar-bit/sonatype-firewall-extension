/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright helpers for Classic pages embedded in the Nexus One shell (CLM-41537).
 */
public class NexusOneClassicEmbedPage
    extends BasePage
{
  public NexusOneClassicEmbedPage() {
    super();
  }

  public static String embedUrl(String hashRoute) {
    return NexusOnePage.url(hashRoute);
  }

  public Locator leftNav() {
    return byTestId("nosc-leftnav");
  }

  public Locator leftNavLink(String label) {
    return leftNav().getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(label).setExact(true));
  }

  public Locator classicComponentMount() {
    return byTestId("nexus-one-classic-component-mount");
  }

  public Locator classicGlobalSidebar() {
    return locator(".nx-global-sidebar-2");
  }
}
