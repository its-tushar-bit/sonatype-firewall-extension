/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.functional.utils.NameSupplierDictionary;
import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.OrgsAndPoliciesSidebarComponent;
import com.sonatype.clm.testing.playwright.pages.OrgsAndPoliciesSidebarComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;

import com.microsoft.playwright.Locator;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * "Import Applications" is gated on {@code saas-lifecycle-scm-enabled}, which is
 * {@code enabledWhenAbsent=true} for MTIQ — the default suffices without mocking features.
 */
@Category(MtiqTest.class)
public class MtiqOrgsAndPoliciesSidebarPlaywrightTest
    extends AbstractMtiqUiTest
{
  // Minimal 1-child × depth-2 × 1-app tree — enough to reach the "Import Applications" option.
  private static final int ROOT_CHILD_ORG_COUNT = 1;

  private static final int TOTAL_DEPTH = 2;

  private static final int LEAF_CHILD_ORG_COUNT = 1;

  @Before
  public void seedTreeAndOpenRootOrgAsAdmin() {
    tempEntity.newRelatedOrganizationsAsMap(
        null,
        ROOT_CHILD_ORG_COUNT,
        TOTAL_DEPTH,
        LEAF_CHILD_ORG_COUNT,
        new NameSupplierDictionary());
    playwrightRefreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    playwrightLogin();
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_importApplications() {
    OrgsAndPoliciesSidebarComponent sidebar = new OrgsAndPoliciesSidebarComponent();
    new OrgsAndPoliciesSidebarComponentAssertions(sidebar).shouldBeVisibleWithSelectedOwner();

    // Select the first seeded child org so the Applications collapsible (hidden for the synthetic
    // root org) renders with its non-synthetic "Add Application" dropdown.
    Locator firstChildOrg = sidebar.organizationLinks().first();
    assertThat(firstChildOrg).isVisible();
    firstChildOrg.click();

    sidebar.addApplicationDropdownTrigger().click();
    assertThat(sidebar.importApplicationsOption()).isVisible();
  }
}
