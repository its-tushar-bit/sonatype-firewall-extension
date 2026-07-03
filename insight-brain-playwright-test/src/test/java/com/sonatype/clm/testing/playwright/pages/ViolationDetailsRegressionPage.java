/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Regression-only extension of {@link ViolationDetailsPage} with locators for the
 * SidebarNavViolationList.
 *
 * <p>
 * Note: the inherited {@link ViolationDetailsPage#sidebarNavItems()} is scoped to
 * {@code #violation-page} and silently returns 0 results for the sidebar nav list, which is
 * a React portal rendered outside that root. Use {@link #sidebarNavList()} and
 * {@link #sidebarNavSelectedItem()} from this class instead.
 */
public class ViolationDetailsRegressionPage
    extends ViolationDetailsPage
{
  /**
   * The violation sidebar navigation list.
   * Uses {@code page.locator()} directly because {@code #sidebar-nav-list} is a React portal
   * rendered outside {@code #violation-page} and cannot be scoped to the page container.
   */
  public Locator sidebarNavList() {
    return page.locator("#sidebar-nav-list li");
  }

  /**
   * The currently selected item in the sidebar navigation list.
   * Targets {@code li.selected} directly rather than a positional selector so the assertion
   * is not sensitive to the order in which violations appear in the list.
   */
  public Locator sidebarNavSelectedItem() {
    return page.locator("#sidebar-nav-list li.selected");
  }
}
