/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the Orgs and Policies "Inheritance Hierarchy" tree-view page
 * rendered by {@code OrgsAndPolicies/ownersTreePage/OwnersTreePage.jsx} at the route
 * {@code #/orgsAndPolicies/treeView}.
 * <p>
 * The page tree is anchored at {@link #ROOT} ({@code NxPageMain.ownersTreeView}). All locators
 * are scoped under {@code ROOT} so chrome elements rendered at body level (left-rail nav, owner
 * detail sidebar, modals) cannot trip Playwright's strict-mode guard. See
 * {@code PLAYWRIGHT_TEST_AUTHORING_GUIDE.md} §4a.
 * <p>
 * Modern selectors (RSC {@code NxTree} + {@code data-testid}) are preferred over the legacy
 * {@code .iq-owner-tree-view} / {@code .nx-tree__item--clickable} class names that the old
 * Selenide page used — those classes were renamed when the screen was rebuilt on
 * {@code OwnerTreeTile} / {@code OwnerTree}.
 */
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

  /** URL fragment for the tree page — used with {@code navigateAndWaitForUrl}. */
  public static final String TREE_URL_FRAGMENT = "/management/tree";

  public static String url() {
    return "/assets/index.html#/management/tree";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING, HEADING_LEVEL_1_OPTS);
  }

  /** All visible item labels (orgs + apps). Anchored under {@link #TREE}. */
  public Locator itemLabels() {
    return locator(TREE).getByTestId(ITEM_LABEL_TESTID);
  }

  public Locator firstItemLabel() {
    return itemLabels().first();
  }

  /**
   * Anchor link inside an item label. Synthetic owners (e.g. "Repositories") render as a plain
   * {@code <span>} with no anchor; non-synthetic owners render an {@code <a>} produced by
   * {@code NxTextLink}.
   */
  public Locator anchorIn(Locator itemLabel) {
    return itemLabel.locator("a");
  }

  public Locator filterInput() {
    return locator(ROOT + " #iq-owner-tree-filter-input");
  }

  public Locator expandAllButton() {
    return container().getByRole(AriaRole.BUTTON, EXPAND_ALL_OPTS);
  }

  public Locator collapseAllButton() {
    return container().getByRole(AriaRole.BUTTON, COLLAPSE_ALL_OPTS);
  }

  /**
   * Click the first clickable (non-synthetic) owner in the tree and return the {@link Locator}
   * the caller can assert against — typically the destination owner-summary container.
   * <p>
   * Skips synthetic rows (which render only a {@code <span>}) by searching for the first label
   * that contains an anchor.
   */
  public void clickFirstClickableOwner() {
    Locator firstClickableAnchor = itemLabels().locator("a").first();
    firstClickableAnchor.waitFor();
    firstClickableAnchor.click();
  }

  public void clickItemWithText(String name) {
    filterInput().fill(name);
    Locator item = itemLabels().filter(new Locator.FilterOptions().setHasText(name)).first();
    item.waitFor();
    item.locator("a").click();
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
