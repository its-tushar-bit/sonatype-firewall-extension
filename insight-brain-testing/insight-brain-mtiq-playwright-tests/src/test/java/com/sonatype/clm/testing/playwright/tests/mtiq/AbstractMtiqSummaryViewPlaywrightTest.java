/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.model.Owner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Shared MTIQ owner-summary Playwright coverage. Verifies each nav pill on the owner summary
 * scrolls its associated tile into view. Ported from the Selenide
 * {@code AbstractMtiqSummaryViewTest}.
 */
@Tag("mtiq")
public abstract class AbstractMtiqSummaryViewPlaywrightTest
    extends AbstractMtiqUiTest
{
  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  protected Owner currentOwner;

  @BeforeEach
  public void boot() {
    playwrightLoginAdminAt("/");
  }

  protected void init(Owner owner) {
    this.currentOwner = owner;
    playwrightRefreshOrOpen(OwnerSummaryPage.url(owner));
    assertThat(new OwnerSummaryPage().ownerName()).containsText(owner.getName());
  }

  /**
   * Clicks every non-retention nav pill and verifies the corresponding owner-summary tile becomes
   * visible. Mirrors the Selenide {@code testNavigationPills} coverage:
   * <ul>
   * <li>Application Categories, Policy, Legacy Violations, Continuous Monitoring,
   * Proprietary Components, Component Labels, Source Control, License Threat Groups.</li>
   * <li>Retention pill/tile must NOT be present (MTIQ hides retention).</li>
   * <li>InnerSource Repositories and Auto-Waivers pills/tiles MUST be present.</li>
   * </ul>
   */
  @Test
  @Tag("mtiq")
  public void testNavigationPills_clickingEachPillRevealsCorrespondingTile() {
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_APP_CATEGORIES);
    assertThat(ownerSummary.categoriesTile()).isVisible();

    ownerSummary.openPoliciesSectionFromNavPills();
    assertThat(ownerSummary.policiesTile()).isVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_LEGACY_VIOLATIONS);
    assertThat(ownerSummary.legacyViolationsTile()).isVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_CONTINUOUS_MONITORING);
    assertThat(ownerSummary.continuousMonitoringTile()).isVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_PROPRIETARY_COMPONENTS);
    assertThat(ownerSummary.proprietaryComponentsTile()).isVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_COMPONENT_LABELS);
    assertThat(ownerSummary.componentLabelsTile()).isVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_SOURCE_CONTROL);
    assertThat(ownerSummary.sourceControlTile()).isVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_LICENSE_THREAT_GROUPS);
    assertThat(ownerSummary.licenseThreatGroupsTile()).isVisible();

    // Retention is not surfaced in MTIQ.
    assertThat(ownerSummary.navPillButton(OwnerSummaryPage.OWNER_PILL_RETENTION)).hasCount(0);
    assertThat(ownerSummary.dataRetentionTile()).hasCount(0);

    // InnerSource Repositories and Auto-Waivers should be present (though we don't click through
    // them explicitly here to keep the test focused on the previously-covered pill set).
    assertThat(ownerSummary.navPillButton(OwnerSummaryPage.OWNER_PILL_INNERSOURCE_REPOSITORY))
        .not()
        .hasCount(0);
    assertThat(ownerSummary.innerSourceRepositoryTile()).not().hasCount(0);

    assertThat(ownerSummary.navPillButton(OwnerSummaryPage.OWNER_PILL_AUTO_WAIVERS)).not().hasCount(0);
    assertThat(ownerSummary.autoWaiversTile()).not().hasCount(0);
  }
}
