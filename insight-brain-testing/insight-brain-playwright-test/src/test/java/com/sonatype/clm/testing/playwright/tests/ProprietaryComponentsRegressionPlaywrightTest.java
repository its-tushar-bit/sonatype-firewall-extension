/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.List;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.SelectOption;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.ProprietaryComponentsRegressionPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.Organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Regression tests for the Proprietary Component Configuration editor. */
public class ProprietaryComponentsRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.IsHiddenOptions HIDDEN_OPTS =
      new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.HasTextOptions TEXT_OPTS =
      new LocatorAssertions.HasTextOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.IsEnabledOptions ENABLED_OPTS =
      new LocatorAssertions.IsEnabledOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.IsDisabledOptions DISABLED_OPTS =
      new LocatorAssertions.IsDisabledOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.HasValueOptions VALUE_OPTS =
      new LocatorAssertions.HasValueOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final String PROPRIETARY_URL_FRAGMENT = "/proprietary";

  private static final String PACKAGE_MATCHER = "com.example.pkgtest";

  private static final String REGEX_MATCHER = "test-regex-1";

  private static final String INHERITED_PACKAGE = "root.inherited.package";

  private static final String PERSIST_PACKAGE = "com.example.persist";

  private static final String PACKAGE_TYPE = "Package";

  private static final String REGEX_TYPE = "RegEx";

  private static final String REGEX_OPTION_LABEL = "Regular Expression";

  private static final String NO_MATCHERS_TEXT = "No matchers configured";

  private static final String ADDED_REGEX = "test-regex-added";

  private static final Pattern INHERITED_HEADING_PATTERN = Pattern.compile("Inherited from .+");

  @BeforeEach
  public void openDashboardAndLoginAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  private void navigateToOrgProprietaryConfig(String orgId) {
    navigateAndWaitForUrl(
        OwnerSummaryPage.editOrganizationUrl(orgId, PROPRIETARY_URL_FRAGMENT),
        PROPRIETARY_URL_FRAGMENT);
  }

  @Test
  @Tag("regression")
  public void testPageRenders_localMatchersListAndEmptyState() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newProprietaryConfig(org.getId(),
        List.of(PACKAGE_MATCHER),
        List.of(REGEX_MATCHER));
    navigateToOrgProprietaryConfig(org.getId());
    ProprietaryComponentsRegressionPage regPage = new ProprietaryComponentsRegressionPage();
    Locator packageRow = regPage.listRowForMatcher(PACKAGE_MATCHER);
    assertThat(packageRow.getByText(PACKAGE_MATCHER)).isVisible(VISIBLE_OPTS);
    assertThat(regPage.subtextInRow(packageRow)).hasText(PACKAGE_TYPE, TEXT_OPTS);

    Locator regexRow = regPage.listRowForMatcher(REGEX_MATCHER);
    assertThat(regexRow.getByText(REGEX_MATCHER)).isVisible(VISIBLE_OPTS);
    assertThat(regPage.subtextInRow(regexRow)).hasText(REGEX_TYPE, TEXT_OPTS);

    Organization emptyOrg = tempEntity.newOrganization();
    tempEntity.newProprietaryConfig(emptyOrg.getId(), List.of(), List.of());
    playwrightRefreshOrOpen(OwnerSummaryPage.editOrganizationUrl(emptyOrg.getId(), PROPRIETARY_URL_FRAGMENT));
    playwrightWaitUntilUrlContains(emptyOrg.getId());
    assertThat(regPage.localMatchersList().getByText(NO_MATCHERS_TEXT)).isVisible(VISIBLE_OPTS);
  }

  @Test
  @Tag("regression")
  public void testAddAndDeleteMatchers() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newProprietaryConfig(org.getId(), List.of(PACKAGE_MATCHER), List.of());
    navigateToOrgProprietaryConfig(org.getId());
    ProprietaryComponentsRegressionPage regPage = new ProprietaryComponentsRegressionPage();

    regPage.deleteButtonForMatcher(PACKAGE_MATCHER).click();
    assertThat(regPage.localMatchersList().getByText(PACKAGE_MATCHER)).isHidden(HIDDEN_OPTS);

    assertThat(regPage.matcherTypeOptions()).hasText(new String[]{PACKAGE_TYPE, REGEX_OPTION_LABEL}, TEXT_OPTS);

    assertThat(regPage.addButton()).isDisabled(DISABLED_OPTS);

    regPage.matcherTypeSelect().selectOption(new SelectOption().setLabel(REGEX_OPTION_LABEL));
    assertThat(regPage.addButton()).isDisabled(DISABLED_OPTS);

    regPage.valueInput().fill(ADDED_REGEX);
    assertThat(regPage.addButton()).isEnabled(ENABLED_OPTS);
    regPage.addButton().click();
    Locator addedRow = regPage.listRowForMatcher(ADDED_REGEX);
    assertThat(addedRow.getByText(ADDED_REGEX)).isVisible(VISIBLE_OPTS);
    assertThat(regPage.subtextInRow(addedRow)).hasText(REGEX_TYPE, TEXT_OPTS);
    assertThat(regPage.valueInput()).hasValue("", VALUE_OPTS);
  }

  @Test
  @Tag("regression")
  public void testInheritedMatchersSection_shownForChildOrg_hiddenAtRootOrg() {
    tempEntity.newProprietaryConfig(Organization.ROOT_ORGANIZATION_ID,
        List.of(INHERITED_PACKAGE),
        List.of());

    Organization childOrg = tempEntity.newOrganization();
    navigateToOrgProprietaryConfig(childOrg.getId());
    ProprietaryComponentsRegressionPage regPage = new ProprietaryComponentsRegressionPage();

    Locator inheritedSection = regPage.inheritedMatchersSection();
    assertThat(inheritedSection).isVisible(VISIBLE_OPTS);
    assertThat(regPage.inheritedSectionHeading()).hasText(INHERITED_HEADING_PATTERN, TEXT_OPTS);
    assertThat(inheritedSection.getByText(INHERITED_PACKAGE)).isVisible(VISIBLE_OPTS);

    playwrightRefreshOrOpen(
        OwnerSummaryPage.editOrganizationUrl(Organization.ROOT_ORGANIZATION_ID, PROPRIETARY_URL_FRAGMENT));
    playwrightWaitUntilUrlContains(Organization.ROOT_ORGANIZATION_ID);
    assertThat(regPage.inheritedMatchersSection()).isHidden(HIDDEN_OPTS);
  }

  @Test
  @Tag("regression")
  public void testSaveConfig_persistsOnReload_andNoChangesGuard() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newProprietaryConfig(org.getId(), List.of(), List.of());
    navigateToOrgProprietaryConfig(org.getId());
    ProprietaryComponentsRegressionPage regPage = new ProprietaryComponentsRegressionPage();

    regPage.updateButton().click();
    assertThat(regPage.formValidationErrors()).isVisible(VISIBLE_OPTS);

    regPage.valueInput().fill(PERSIST_PACKAGE);
    regPage.addButton().click();
    assertThat(regPage.updateButton()).isEnabled(ENABLED_OPTS);

    regPage.clickUpdateAndWaitForSave();

    page.reload();
    playwrightWaitUntilUrlContains(org.getId());
    assertThat(regPage.localMatchersList().getByText(PERSIST_PACKAGE)).isVisible(VISIBLE_OPTS);

    regPage.updateButton().click();
    assertThat(regPage.formValidationErrors()).isVisible(VISIBLE_OPTS);
  }
}
