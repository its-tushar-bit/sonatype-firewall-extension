/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

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
  /** Outer page container ({@code OwnersTreePage.jsx} → {@code <NxPageMain className="ownersTreeView">}). */
  private static final String ROOT = ".ownersTreeView";

  /** The tree itself ({@code OwnerTree.jsx} → {@code <NxTree className="… iq-owner-tree">}). */
  private static final String TREE = ROOT + " .iq-owner-tree";

  /**
   * Every clickable owner row exposes its label via a stable test id. Both organizations and
   * applications share this attribute (see {@code OwnerTree.jsx} {@code <NxTree.ItemLabel
   * data-testid="owners-tree-item-label">}).
   */
  private static final String ITEM_LABEL_TESTID = "owners-tree-item-label";

  public OwnersTreePage() {
    super();
  }

  /**
   * Hash route used by the OrgsAndPolicies SPA. Mirrors the {@code management.tree} state
   * registered in {@code OrgsAndPolicies/route.js} (URL fragment {@code /management/tree}).
   */
  public static String url() {
    return "/assets/index.html#/management/tree";
  }

  // --------------- Roots ---------------

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  // --------------- Tree items ---------------

  /** All visible item labels (orgs + apps). Anchored under {@link #TREE}. */
  public Locator itemLabels() {
    return locator(TREE).getByTestId(ITEM_LABEL_TESTID);
  }

  /**
   * The first visible owner row in the tree. Disambiguated at the page-object boundary
   * (authoring guide §4a) so tests don't need to call {@code .first()} themselves.
   */
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

  // --------------- Tile chrome (filter + expand/collapse) ---------------

  public Locator filterInput() {
    return locator(ROOT + " #iq-owner-tree-filter-input");
  }

  public Locator expandAllButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Expand All"));
  }

  public Locator collapseAllButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Collapse All"));
  }

  // --------------- Business actions ---------------

  /**
   * Click the first clickable (non-synthetic) owner in the tree and return the {@link Locator}
   * the caller can assert against — typically the destination owner-summary container.
   * <p>
   * Skips synthetic rows (which render only a {@code <span>}) by searching for the first label
   * that contains an anchor.
   */
  public void clickFirstClickableOwner() {
    Locator firstClickableAnchor = itemLabels().locator("a").first();
    assertThat(firstClickableAnchor).isVisible();
    firstClickableAnchor.click();
  }

}
