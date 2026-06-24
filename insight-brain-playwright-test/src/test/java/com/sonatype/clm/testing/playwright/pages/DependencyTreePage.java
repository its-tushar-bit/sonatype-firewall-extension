/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.insight.brain.model.Application;

/**
 * Playwright page object for the Dependency Tree page
 * ({@code #/applicationReport/{publicId}/{scanId}/dependencyTree}).
 */
public class DependencyTreePage
    extends BasePage
{
  private static final String ROOT = ".iq-dependency-tree-page";

  public DependencyTreePage() {
  }

  public static String url(Application app, String scanId) {
    return url(app.getPublicId(), scanId);
  }

  public static String url(String appPublicId, String scanId) {
    return "/assets/index.html#/applicationReport/" + appPublicId + "/" + scanId + "/dependencyTree";
  }

  /** Root {@code NxPageMain} container ({@code .iq-dependency-tree-page}). */
  public Locator container() {
    return locator(ROOT);
  }

  /**
   * Page {@code
   *
  <h1>} heading "Dependency Tree".
   */
  public Locator heading() {
    return locator(ROOT).getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  /**
   * "Expand All" control ({@code id="iq-dependency-tree__expand-all-button"}).
   * The stable component ID is used as the selector; the button renders as an icon-paired
   * control whose accessible name may vary with RSC version, making a role+name selector fragile.
   */
  public Locator expandAllButton() {
    return locator("#iq-dependency-tree__expand-all-button");
  }

  /**
   * "Collapse All" control ({@code id="iq-dependency-tree__collapse-all-button"}).
   * Same rationale as {@link #expandAllButton()} — stable component ID preferred over role+name.
   */
  public Locator collapseAllButton() {
    return locator("#iq-dependency-tree__collapse-all-button");
  }

  /** Root {@code NxTree} ({@code role="tree"}) inside the page container. */
  public Locator tree() {
    return locator(ROOT).getByRole(AriaRole.TREE);
  }

  /**
   * Label of the application root node — the first {@code NxTree.Item} in the tree, whose
   * label contains the application name.
   */
  public Locator treeRootLabel() {
    return locator(".iq-dependency-tree .nx-tree__item-label").first();
  }

  /**
   * Direct-dependency {@code NxTree.Item} elements rendered under the application root node.
   */
  public Locator treeChildItems() {
    return locator(".iq-dependency-tree > .nx-tree__item > .nx-tree > .nx-tree__item");
  }

  /**
   * First collapsible {@code NxTree.Item} ({@code .nx-tree__item--collapsible}) directly under
   * the application root node.
   */
  public Locator firstCollapsibleChildItem() {
    return locator(
        ".iq-dependency-tree > .nx-tree__item > .nx-tree > .nx-tree__item--collapsible").first();
  }

  /**
   * SVG {@code <rect class="nx-tree__collapse-click">} click target for expanding/collapsing
   * {@link #firstCollapsibleChildItem()}.
   *
   * <p>
   * CSS class selector is used because this element is an SVG {@code <rect>} — it has no ARIA
   * role and no accessible name, so {@code getByRole} is not applicable.
   *
   * <p>
   * {@code .first()} is required because a collapsible child that has its own collapsible
   * grandchildren will contain multiple {@code .nx-tree__collapse-click} rects in the DOM
   * (one per collapsible descendant). The collapse SVG is always the first child of the
   * {@code
   *
  <li>} in DOM order, so {@code .first()} reliably targets the item's own toggle.
   */
  public Locator firstCollapsibleChildToggle() {
    return firstCollapsibleChildItem().locator(".nx-tree__collapse-click").first();
  }

  /**
   * Nested {@code NxTree} ({@code .nx-tree}) inside {@link #firstCollapsibleChildItem()}.
   * Has {@code display:none} (hidden) when the item is collapsed;
   * {@code display:block} (visible) when expanded.
   */
  public Locator firstCollapsibleChildPanel() {
    return firstCollapsibleChildItem().locator(".nx-tree").first();
  }

  /**
   * All {@code NxThreatIndicator} circles inside the tree whose category is not {@code none}
   * ({@code .nx-threat-indicator:not(.nx-threat-indicator--none)}).
   * Present only when the seeded org has policies that produce violations on the report.
   */
  public Locator treeViolationIndicators() {
    return locator(ROOT + " .nx-threat-indicator:not(.nx-threat-indicator--none)");
  }

  /** {@code NxThreatIndicator} circles with {@code aria-label="threat level critical"}. */
  public Locator treeCriticalIndicators() {
    return locator(ROOT + " [aria-label='threat level critical']");
  }

  /** {@code NxThreatIndicator} circles with {@code aria-label="threat level severe"}. */
  public Locator treeSevereIndicators() {
    return locator(ROOT + " [aria-label='threat level severe']");
  }
}
