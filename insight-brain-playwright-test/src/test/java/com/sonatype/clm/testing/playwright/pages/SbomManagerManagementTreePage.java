/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Page object for the SBOM Manager Management Tree page ({@code #/sbomManager/management/tree}).
 * Root selector is {@code .ownersTreeView} (no {@code id} on the NxPageMain element).
 */
public class SbomManagerManagementTreePage
    extends BasePage
{
  private static final String ROOT = ".ownersTreeView";

  public SbomManagerManagementTreePage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/sbomManager/management/tree";
  }

  public Locator heading() {
    return locator(ROOT).getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  public Locator filterInput() {
    return locator("#iq-owner-tree-filter-input");
  }

  public Locator expandAllButton() {
    return byRole(AriaRole.BUTTON, "Expand All");
  }

  public Locator collapseAllButton() {
    return byRole(AriaRole.BUTTON, "Collapse All");
  }

  public Locator treeItemLabels() {
    return locator("[data-testid='owners-tree-item-label']");
  }
}
