/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import org.junit.Before;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright test for the Root Organization Summary view.
 * <p>
 * Selectors live on {@link OwnerSummaryPage}, which already mirrors the live DOM
 * ({@code OwnerSummary.jsx} and {@code ActionDropdown.jsx}). This test only orchestrates.
 */
public class RootOrganizationSummaryViewPlaywrightTest
    extends AbstractIqUiTest
{
  private Organization rootOrg;

  @Before
  public void init() {
    rootOrg = lookup(OrganizationDAO.class).getById(ROOT_ORGANIZATION_ID);
    playwrightRefreshOrOpen(ReportListPage.url());
    playwrightLogin();
    playwrightRefreshOrOpen(OwnerSummaryPage.url(rootOrg));
  }

  @Test
  @Category(SanityTest.class)
  public void testRootOrgSummaryLoads() {
    OwnerSummaryPage summaryPage = new OwnerSummaryPage();
    assertThat(summaryPage.container()).isVisible();
    assertThat(summaryPage.ownerName()).containsText(rootOrg.getName());
  }

  /**
   * Open the page-title "Actions" dropdown (NxDropdown {@code #iq-owner-actions-dropdown})
   * and assert the root-org option set: <em>Org ID to Clipboard</em>,
   * <em>Edit Org Name / Icon</em>, <em>Import Policies</em>. Root-org-only options like
   * Move/Delete are intentionally not shown ({@code ActionDropdown.jsx:220, 244}).
   */
  @Test
  @Category(SanityTest.class)
  public void testActionsDropdownOptions() {
    OwnerSummaryPage summaryPage = new OwnerSummaryPage();
    summaryPage.openOwnerActionsDropdown();
    new OwnerSummaryPageAssertions(summaryPage).shouldShowOrganizationActionsMenu();
  }
}
