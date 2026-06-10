/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class OwnersTreePage
    extends BasePage
{
  private static final String ROOT = ".ownersTreeView";

  private static final String TREE = ROOT + " .iq-owner-tree";

  private static final String ITEM_LABEL_TESTID = "owners-tree-item-label";

  private static final Locator.GetByRoleOptions HEADING_LEVEL_1_OPTS =
      new Locator.GetByRoleOptions().setLevel(1);

  private static final Locator.GetByRoleOptions EXPAND_ALL_OPTS =
      new Locator.GetByRoleOptions().setName("Expand All");

  private static final Locator.GetByRoleOptions COLLAPSE_ALL_OPTS =
      new Locator.GetByRoleOptions().setName("Collapse All");

  public OwnersTreePage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/management/tree";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING, HEADING_LEVEL_1_OPTS);
  }

  public Locator itemLabels() {
    return locator(TREE).getByTestId(ITEM_LABEL_TESTID);
  }

  public Locator firstItemLabel() {
    return itemLabels().first();
  }

  public Locator expandAllButton() {
    return container().getByRole(AriaRole.BUTTON, EXPAND_ALL_OPTS);
  }

  public Locator collapseAllButton() {
    return container().getByRole(AriaRole.BUTTON, COLLAPSE_ALL_OPTS);
  }

  public void clickFirstClickableOwner() {
    Locator firstClickableAnchor = itemLabels().locator("a").first();
    assertThat(firstClickableAnchor).isVisible();
    firstClickableAnchor.click();
  }

  public Locator expandedCollapsibleNodes() {
    return tree().getByRole(AriaRole.TREEITEM, new Locator.GetByRoleOptions().setExpanded(true));
  }

  public Locator collapsedNodes() {
    return tree().getByRole(AriaRole.TREEITEM, new Locator.GetByRoleOptions().setExpanded(false));
  }

  public void clickFirstOrganizationNode() {
    tree().getByRole(AriaRole.TREEITEM).first().getByRole(AriaRole.LINK).first().click();
  }

  public void clickFirstApplicationNode() {
    tree().getByRole(AriaRole.TREEITEM)
        .first()
        .getByRole(AriaRole.TREEITEM)
        .first()
        .getByRole(AriaRole.LINK)
        .first()
        .click();
  }

  public Locator backButton() {
    return container().getByRole(AriaRole.LINK, CommonButtonOptions.BACK_BUTTON_OPTS);
  }

  public Locator loadErrorAlert() {
    return container().getByRole(AriaRole.ALERT);
  }

  public Locator retryButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.RETRY_BUTTON_OPTS);
  }

  public Locator tree() {
    return container().getByRole(AriaRole.TREE);
  }
}
