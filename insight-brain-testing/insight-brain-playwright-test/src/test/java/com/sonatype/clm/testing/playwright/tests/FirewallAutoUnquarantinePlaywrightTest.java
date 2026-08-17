/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * <p>
 * Authoring rules: see {@code TestAuthourskill.md}. Backend setup is encapsulated in the nested
 * {@link FirewallAutoUnquarantineSeeder} (§3c).
 */
public class FirewallAutoUnquarantinePlaywrightTest
    extends AbstractIqUiTest
{
  private static final String REPOSITORY_MANAGER_INSTANCE_ID = "1";

  private static final String REPOSITORY_PUBLIC_ID = "central";

  private static final String COMPONENT_COORDINATE_1 = "g:a:1";

  private static final String COMPONENT_COORDINATE_2 = "g:a:2";

  private static final String COMPONENT_COORDINATE_3 = "g:a:3";

  private static final String COMPONENT_COORDINATE_4 = "g:a:4";

  private static final int EXPECTED_AUTO_UNQUARANTINE_ROW_COUNT = 2;

  private static final String EXPECTED_COMPONENT_DETAILS_URL_FRAGMENT = "/firewall/repository/";

  @BeforeEach
  public void openAutoUnquarantineAsAdmin() {
    // FIREWALL is required by testRowClickNavigatesToComponentDetails for the route to
    // resolve; AUTO_UNQUARANTINE + RELEASE_INTEGRITY enable the page itself. Including all
    // three in @Before keeps every test seeing the same feature surface — avoiding the
    // mid-test setFeatures() the legacy class did, which fights the harness (skill §7b).
    setFeatures(LicensedFeature.FIREWALL, LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.RELEASE_INTEGRITY);

    seedRepositoryWithFourComponents();

    playwrightRefreshOrOpen(FirewallAutoUnquarantinePage.url());
    playwrightLogin();
  }

  @Test
  @Tag("sanity")
  public void testAutoUnquarantinePageLoads() {
    FirewallAutoUnquarantinePage autoPage = new FirewallAutoUnquarantinePage();

    assertThat(autoPage.container()).isVisible();
    assertThat(autoPage.title()).isVisible();
    assertThat(autoPage.configurationModal()).isHidden();
  }

  @Test
  @Tag("sanity")
  public void testOpenCloseModal() {
    FirewallAutoUnquarantinePage autoPage = new FirewallAutoUnquarantinePage();

    assertThat(autoPage.container()).isVisible();
    assertThat(autoPage.configurationModal()).isHidden();

    autoPage.openConfigurationModal();
    assertThat(autoPage.configurationModal()).isVisible();
    assertThat(autoPage.modalSaveButton()).isVisible();
    assertThat(autoPage.modalCancelButton()).isVisible();

    autoPage.cancelConfigurationModal();
    assertThat(autoPage.configurationModal()).isHidden();
  }

  @Test
  @Tag("sanity")
  public void testAutoUnquarantineTableBodyCount() {
    FirewallAutoUnquarantinePage autoPage = new FirewallAutoUnquarantinePage();

    assertThat(autoPage.container()).isVisible();
    assertThat(autoPage.unquarantineTableRows()).hasCount(EXPECTED_AUTO_UNQUARANTINE_ROW_COUNT);
  }

  @Test
  @Tag("sanity")
  public void testRowClickNavigatesToComponentDetails() {
    FirewallAutoUnquarantinePage autoPage = new FirewallAutoUnquarantinePage();

    assertThat(autoPage.container()).isVisible();
    assertThat(autoPage.unquarantineTableRows()).hasCount(EXPECTED_AUTO_UNQUARANTINE_ROW_COUNT);

    PlaywrightWaitUtils.clickAndWaitForUrlContains(
        page, autoPage.unquarantineTableRows().first(), EXPECTED_COMPONENT_DETAILS_URL_FRAGMENT);
    // PlaywrightWaitUtils#clickAndWaitForUrlContains has already asserted the URL contains
    // the fragment — no need for a second AssertJ check on the URL string.
  }

  /**
   * Seed one repository with four components: two with both {@code quarantineTime} and
   * {@code unquarantineTime} set (counted on the auto-unquarantine page, since the table
   * filters on {@code unquarantine_time IS NOT NULL}), and two still-quarantined / never-
   * quarantined components that should NOT show up.
   */
  private void seedRepositoryWithFourComponents() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager(REPOSITORY_MANAGER_INSTANCE_ID);
    Repository repository =
        tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true, false);

    ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    Date date1 = Date.from(LocalDateTime.now().withDayOfMonth(1).toInstant(offset));
    Date date2 = Date.from(LocalDateTime.now().withDayOfMonth(2).toInstant(offset));

    // Auto-unquarantined (will appear in the table).
    tempEntity.newRepositoryComponent(repository.getId(), COMPONENT_COORDINATE_1, date1, date1, true);
    tempEntity.newRepositoryComponent(repository.getId(), COMPONENT_COORDINATE_2, date2, date2, true);

    // Manually unquarantined (auto=false → should NOT show on the auto-unquarantine page).
    tempEntity.newRepositoryComponent(repository.getId(), COMPONENT_COORDINATE_3, new Date(), new Date(), false);

    // Currently quarantined (no unquarantineTime → should NOT show).
    tempEntity.newRepositoryComponent(repository.getId(), COMPONENT_COORDINATE_4, new Date(), null, false);
  }
}
