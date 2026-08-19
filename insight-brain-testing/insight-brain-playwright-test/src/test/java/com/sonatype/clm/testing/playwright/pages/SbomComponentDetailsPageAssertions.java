/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class SbomComponentDetailsPageAssertions
{
  private final SbomComponentDetailsPage page;

  public SbomComponentDetailsPageAssertions(SbomComponentDetailsPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldBeLoaded() {
    assertThat(page.container()).isVisible();
    assertThat(page.title()).isVisible();
  }

  public void shouldShowPageTitle(String expectedTitle) {
    assertThat(page.title()).containsText(expectedTitle);
  }

  public void shouldShowReportInfoItems(String orgName, String appName) {
    assertThat(page.reportInfoItems().filter(new Locator.FilterOptions().setHasText(orgName))).isVisible();
    assertThat(page.reportInfoItems().filter(new Locator.FilterOptions().setHasText(appName))).isVisible();
    Locator bomItem = page.bomInfoItem();
    assertThat(bomItem).isVisible();
    assertThat(bomItem).containsText(Pattern.compile("BOM \\d{4}-\\d{2}-\\d{2}"));
  }

  public void shouldShowFormatTag(String expectedFormat) {
    assertThat(page.formatTag()).containsText(expectedFormat);
  }

  public void shouldShowPurlTag(String expectedPurl) {
    assertThat(page.purlTag()).containsText(expectedPurl);
  }

  public void shouldHaveTabVisible(String label) {
    assertThat(page.tab(label)).isVisible();
  }

  public void shouldShowComponentSummary(String highestScore, String sonatypeVerified, String unverified) {
    assertThat(page.componentSummaryTile()).isVisible();
    assertThat(page.highestCvssScore()).containsText(highestScore);
    assertThat(page.sonatypeVerifiedCount()).containsText(sonatypeVerified);
    assertThat(page.unverifiedCount()).containsText(unverified);
  }

  public void shouldShowCriticalViolationCount(String expectedCount) {
    assertThat(page.criticalViolationCount()).containsText(expectedCount);
  }

  public void shouldHaveDisclosedVulnerabilityColumnCount(int expectedCount) {
    assertThat(page.disclosedVulnerabilityHeaders()).hasCount(expectedCount);
  }

  public void shouldHaveDisclosedVulnerabilityRowCount(int expectedCount) {
    assertThat(page.disclosedVulnerabilityRows()).hasCount(expectedCount);
  }

  public void shouldShowFirstDisclosedVulnerabilityRow(String cvss, String issue) {
    Locator firstRow = page.disclosedVulnerabilityRows().first();
    assertThat(firstRow).containsText(cvss);
    assertThat(firstRow).containsText(issue);
  }

  public void shouldHaveSonatypeVulnerabilityColumnCount(int expectedCount) {
    assertThat(page.sonatypeVulnerabilityHeaders()).hasCount(expectedCount);
  }

  public void shouldShowFirstSonatypeVulnerabilityRow(String cvss, String issue) {
    Locator firstRow = page.sonatypeVulnerabilityRows().first();
    assertThat(firstRow).containsText(cvss);
    assertThat(firstRow).containsText(issue);
  }

  public void shouldShowPolicyViolationsTile() {
    assertThat(page.policyViolationsTile()).isVisible();
    assertThat(page.policyViolationsTile()).containsText("Policy Violations");
  }

  public void shouldShowFirstPolicyViolationRow(String threat, String policy, String constraint) {
    Locator firstRow = page.policyViolationRows().first();
    assertThat(firstRow).containsText(threat);
    assertThat(firstRow).containsText(policy);
    assertThat(firstRow).containsText(constraint);
  }

  public void shouldShowPolicyViolationDetailsDrawer() {
    assertThat(page.policyViolationDetailsDrawer()).isVisible();
  }

  public void shouldShowDrawerConditionText(String conditionText) {
    assertThat(page.policyViolationDetailsDrawer()).containsText(conditionText);
  }

  public void shouldShowVulnerabilityDetailsPopover(String expectedTitle) {
    assertThat(page.vulnerabilityDetailsPopover()).isVisible();
    assertThat(page.popoverTitle()).containsText(expectedTitle);
  }

  public void shouldShowLegalTabWith3ColumnLayout() {
    LocatorAssertions.IsVisibleOptions opts =
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);
    assertThat(page.legalLicenseDetectionsTile()).isVisible(opts);
    for (String label : new String[]{"Effective Licenses", "Declared Licenses", "Observed Licenses"}) {
      assertThat(page.legalColumnLabel(label)).isVisible(opts);
    }
  }
}
