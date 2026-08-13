/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright component object for the Organization & Policies owner-detail sidebar
 * ({@code OwnerDetailSidebar.jsx}, root id {@code #owner-detail-sidebar}).
 * <p>
 * Provides locators for the collapsible groups (Application Categories, Policies, Component
 * Labels, License Threat Groups, Access) and for the non-collapsible standalone links (Legacy
 * Violations, Continuous Monitoring, Proprietary Components, Source Control, Auto-Waivers,
 * Public Data Sources). Visibility of any individual entry is gated by feature flags / owner
 * type in the JSX — see {@code OwnerDetailSidebar.jsx} for the conditions.
 */
public class OwnerDetailSidebarComponent
    extends BasePage
{
  private static final String ROOT = "#owner-detail-sidebar";

  private static final String ACCESS_GROUP = "#access-group";

  // --------------- Visible labels (matching OwnerDetailSidebar.jsx exactly) ---------------

  public static final String LABEL_APPLICATION_CATEGORIES = "Application Categories";

  public static final String LABEL_POLICIES = "Policies";

  public static final String LABEL_LEGACY_VIOLATIONS = "Legacy Violations";

  public static final String LABEL_CONTINUOUS_MONITORING = "Continuous Monitoring";

  public static final String LABEL_PROPRIETARY_COMPONENTS = "Proprietary Components";

  public static final String LABEL_COMPONENT_LABELS = "Component Labels";

  public static final String LABEL_LICENSE_THREAT_GROUPS = "License Threat Groups";

  public static final String LABEL_SOURCE_CONTROL = "Source Control";

  public static final String LABEL_ACCESS = "Access";

  public static final String LABEL_AUTO_WAIVERS = "Auto-Waivers";

  public static final String LABEL_PUBLIC_DATA_SOURCES = "Public Data Sources";

  public OwnerDetailSidebarComponent() {
    super();
  }

  public Locator container() {
    return locator(ROOT);
  }

  // --------------- Collapsible groups (NxCollapsibleItems with stable id) ---------------

  /** Application Categories group ({@code OwnerDetailSidebar.jsx:210}). */
  public Locator applicationCategoryGroup() {
    return locator(ROOT + " #application-category-group");
  }

  /** Policies group ({@code OwnerDetailSidebar.jsx:250}). */
  public Locator policyGroup() {
    return locator(ROOT + " #policy-group");
  }

  /** Component Labels group ({@code OwnerDetailSidebar.jsx:319}). */
  public Locator labelGroup() {
    return locator(ROOT + " #label-group");
  }

  /** License Threat Groups group ({@code OwnerDetailSidebar.jsx:350}). Hidden on applications. */
  public Locator licenseThreatGroupGroup() {
    return locator(ROOT + " #license-threat-group-group");
  }

  public Locator accessGroup() {
    return locator(ACCESS_GROUP);
  }

  public Locator accessGroupTrigger() {
    return locator(ACCESS_GROUP + " .nx-collapsible-items__trigger");
  }

  // --------------- Non-collapsible standalone links ---------------

  /** Legacy Violations link ({@code OwnerDetailSidebar.jsx:282}). */
  public Locator legacyViolationsLink() {
    return locator(ROOT + " #legacy-violations-link");
  }

  /**
   * Continuous Monitoring link ({@code OwnerDetailSidebar.jsx:294}).
   * <p>
   * The id is misspelled "continous" (missing second "u") in the JSX; we deliberately match the
   * shipped DOM rather than the corrected spelling.
   */
  public Locator continuousMonitoringLink() {
    return locator(ROOT + " #continous-monitoring-link");
  }

  /** Proprietary Components link ({@code OwnerDetailSidebar.jsx:308}). */
  public Locator proprietaryComponentsLink() {
    return locator(ROOT + " #proprietary-components-link");
  }

  /** Public Data Sources link ({@code OwnerDetailSidebar.jsx:448}). */
  public Locator publicDataSourcesLink() {
    return locator(ROOT + " #public-data-sources-link");
  }

  /**
   * Source Control link — has no stable id in the JSX, so we anchor by visible text.
   * Strict-mode-safe: only one Source Control link is rendered under the sidebar.
   */
  public Locator sourceControlLink() {
    return locator(ROOT + " a").filter(new Locator.FilterOptions().setHasText(LABEL_SOURCE_CONTROL));
  }

  /**
   * Auto-Waivers link — has no stable id in the JSX, so we anchor by visible text.
   * Strict-mode-safe: only one Auto-Waivers link is rendered under the sidebar.
   */
  public Locator autoWaiversLink() {
    return locator(ROOT + " a").filter(new Locator.FilterOptions().setHasText(LABEL_AUTO_WAIVERS));
  }

  public Locator addRoleLink() {
    return locator(ACCESS_GROUP + " a").filter(new Locator.FilterOptions().setHasText("Add a Role"));
  }

  // --------------- Actions ---------------

  public void openAccessSection() {
    if (addRoleLink().count() > 0 && addRoleLink().first().isVisible()) {
      return;
    }
    accessGroupTrigger().click();
  }

  public void clickAddRole() {
    openAccessSection();
    addRoleLink().first().click();
  }
}
