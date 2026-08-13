/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Page object for the Public Data Sources (CPE matching) editor
 * ({@code /publicDataSourcesEditor} under org and app edit shells).
 * License-gated: requires {@code CPE_MATCHING}. Inherit radio absent at root org;
 * Allow Override absent for apps; controls disabled when parent {@code allowOverride=false}.
 */
public class PublicDataSourcesEditorPage
    extends BasePage
{
  /** URL path suffix shared by Lifecycle org and app Public Data Sources edit routes. */
  public static final String PUBLIC_DATA_SOURCES_URL_FRAGMENT = "/publicDataSourcesEditor";

  public PublicDataSourcesEditorPage() {
    super();
  }

  public static String orgUrl(String orgId) {
    return OwnerSummaryPage.editOrganizationUrl(orgId, PUBLIC_DATA_SOURCES_URL_FRAGMENT);
  }

  public static String appUrl(String appPublicId) {
    return OwnerSummaryPage.editApplicationUrl(appPublicId, PUBLIC_DATA_SOURCES_URL_FRAGMENT);
  }

  /** Deep-link via the SBOM Manager route prefix — sets {@code isSbomManager=true}, hiding the submit button. */
  public static String sbomManagerOrgUrl(String orgId) {
    return "/assets/index.html#/sbomManager/management/edit/organization/" + orgId
        + PUBLIC_DATA_SOURCES_URL_FRAGMENT;
  }

  public Locator settingsTile() {
    return locator("#public-data-sources-settings");
  }

  /**
   * "Inherit from parent" radio — absent at root org.
   * Label text is dynamic: "Inherit from parent (Enabled)" or "Inherit from parent (Disabled)".
   * Pattern used so both variants match without requiring an exact string.
   */
  public NxRadioComponent inheritRadio() {
    return new NxRadioComponent(Pattern.compile("Inherit from parent"), settingsTile());
  }

  public NxRadioComponent enabledRadio() {
    return new NxRadioComponent("Enabled", locator("#use-public-data-sources"));
  }

  public NxRadioComponent disabledRadio() {
    return new NxRadioComponent("Disabled", locator("#use-public-data-sources"));
  }

  public NxCheckboxComponent allowOverrideCheckbox() {
    return new NxCheckboxComponent("Allow users to enable public data sources", settingsTile());
  }

  public Locator licenseErrorAlert() {
    return locator("#public-data-license-error");
  }

  /**
   * CSS class selector (not scoped to container) — NxInfoAlert renders as a sibling of NxLoadWrapper, outside
   * {@code #public-data-sources-loader}.
   */
  public Locator sbomManagerInfoAlert() {
    return locator(".nx-alert--info")
        .filter(new Locator.FilterOptions().setHasText("Public Data Sources are configured within Lifecycle"));
  }

  /** Hidden via CSS {@code hidden} class when accessed via the SBOM Manager route ({@code isSbomManager=true}). */
  public Locator submitButton() {
    return byRole(AriaRole.BUTTON, "Update");
  }
}
