/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Regression-only extension of {@link OwnerSummaryPage} exposing locator accessors for
 * Actions-menu items whose text is dynamically composed from the owner name at runtime,
 * making role/name selectors fragile. ID anchors are used; see per-method Javadoc.
 */
public class OwnerSummaryRegressionPage
    extends OwnerSummaryPage
{
  /**
   * The "Edit Org Name / Icon" link in the Actions menu.
   * ID anchor used: button text includes the runtime owner name.
   */
  public Locator applicationOrgLink() {
    return ownerActionsMenu().locator("#app-org-link");
  }

  /**
   * The "Delete {ownerName}" link in the Actions menu.
   * ID anchor used: button text includes the runtime owner name.
   */
  public Locator deleteOwnerLink() {
    return ownerActionsMenu().locator("#delete-owner-link");
  }

  /**
   * The "Move {ownerName}" link in the Actions menu.
   * ID anchor used: button text includes the runtime owner name.
   */
  public Locator ownerMoveLink() {
    return ownerActionsMenu().locator("#owner-move-link");
  }
}
