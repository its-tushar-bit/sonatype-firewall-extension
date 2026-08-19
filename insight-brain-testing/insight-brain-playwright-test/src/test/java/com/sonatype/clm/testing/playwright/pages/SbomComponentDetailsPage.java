/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** SBOM Manager Component Details page — root: {@code #sbom-manager-component-details}. */
public class SbomComponentDetailsPage
    extends BasePage
{
  private static final String ROOT = "#sbom-manager-component-details";

  private static final String DISCLOSED_TILE_ID =
      "sbom-manager-cdp-vulnerabilities-tile__disclosedVulnerabilities";

  private static final String SONATYPE_TILE_ID =
      "sbom-manager-cdp-vulnerabilities-tile__sonatypeIdentifiedVulnerabilities";

  public SbomComponentDetailsPage() {
    super();
  }

  public static String url(String appPublicId, String sbomVersion, String componentHash) {
    return "/assets/index.html#/sbomManager/application/" + appPublicId
        + "/bom/" + sbomVersion
        + "/componentDetails/" + componentHash + "/overview";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator title() {
    return locator("#component-details-title");
  }

  public Locator reportInfoItems() {
    return locator(".component-details-header__reportinfo-item");
  }

  public Locator bomInfoItem() {
    return locator(".component-details-header__reportinfo-item:has(.visual-testing-ignore)");
  }

  public Locator formatTag() {
    return locator(".iq-component-format-tag");
  }

  public Locator purlTag() {
    return locator(".purl-container");
  }

  public Locator tab(String label) {
    return page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(label).setExact(true));
  }

  public Locator componentSummaryTile() {
    return locator(".sbom-manager-component-detail-tile");
  }

  public Locator highestCvssScore() {
    return locator("[data-testid='highestCvssScore']");
  }

  public Locator sonatypeVerifiedCount() {
    return locator("[data-testid='verified']");
  }

  public Locator unverifiedCount() {
    return locator("[data-testid='unverified']");
  }

  public Locator criticalViolationCount() {
    return locator("[data-testid='critical-threat-counter']");
  }

  public Locator disclosedVulnerabilitiesTile() {
    return locator("#" + DISCLOSED_TILE_ID);
  }

  public Locator disclosedVulnerabilityHeaders() {
    return locator("#" + DISCLOSED_TILE_ID + " thead th.nx-cell--header");
  }

  public Locator disclosedVulnerabilityRows() {
    return locator("#" + DISCLOSED_TILE_ID + " tbody tr");
  }

  public Locator disclosedVulnerabilityIssueLinkAt(int rowIndex) {
    return disclosedVulnerabilityRows().nth(rowIndex).locator("a.nx-text-link");
  }

  public Locator sonatypeVulnerabilitiesTile() {
    return locator("#" + SONATYPE_TILE_ID);
  }

  public Locator sonatypeVulnerabilityHeaders() {
    return locator("#" + SONATYPE_TILE_ID + " thead th.nx-cell--header");
  }

  public Locator sonatypeVulnerabilityRows() {
    return locator("#" + SONATYPE_TILE_ID + " tbody tr");
  }

  public Locator policyViolationsTile() {
    return locator("#sbom-manager-policy-violations-tile");
  }

  public Locator policyViolationRows() {
    return locator("#sbom-manager-policy-violations-tile tbody tr");
  }

  public Locator policyViolationDetailsDrawer() {
    return locator("#sbom-manager-policy-violation-details-drawer");
  }

  public Locator vulnerabilityDetailsPopover() {
    return locator("#sbom-component-details-vulnerability-details-popover");
  }

  public Locator popoverTitle() {
    return locator("#vulnerability-detail-header .iq-popover-header__title-text");
  }

  /** The shared {@code LicenseDetectionsTile} section rendered inside the Legal tab. */
  public Locator legalLicenseDetectionsTile() {
    return locator("#component-details-legal-license-detections-tile");
  }

  /** Valid labels: "Effective Licenses", "Declared Licenses", "Observed Licenses". */
  public Locator legalColumnLabel(String label) {
    return legalLicenseDetectionsTile().locator("dt.nx-read-only__label")
        .filter(new Locator.FilterOptions().setHasText(label));
  }

  public void clickTab(String label) {
    tab(label).click();
  }

  public void clickDisclosedVulnerabilityIssueLink(int rowIndex) {
    Locator link = disclosedVulnerabilityIssueLinkAt(rowIndex);
    assertThat(link).isVisible();
    link.click();
  }

  public void clickPolicyViolationRow(int rowIndex) {
    policyViolationRows().nth(rowIndex).click();
  }
}
