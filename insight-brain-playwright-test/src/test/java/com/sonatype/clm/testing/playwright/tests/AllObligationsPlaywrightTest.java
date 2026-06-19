/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.playwright.pages.ComponentLegalOverviewPageAssertions;
import com.sonatype.clm.testing.playwright.pages.EditAllObligationsModal;
import com.sonatype.clm.testing.playwright.pages.EditAllObligationsModalAssertions;
import com.sonatype.clm.testing.playwright.utils.HdsStubs;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AllObligationsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String APP_NAME = "AllObligationsTest";

  private static final String APP_PUBLIC_ID = "app";

  private static final String ORG_NAME = "org";

  private static final String COMPONENT_GROUP_ID = "g";

  private static final String COMPONENT_ARTIFACT_ID = "a";

  private static final String COMPONENT_VERSION = "v";

  private static final String COMPONENT_HASH = "033e7a20b23ea284d474";

  private static final String COMPONENT_LICENSE_ID = "MIT";

  private static final String FULFILL_COMMENT = "my comment";

  private static final String INITIAL_OBLIGATION_STATUS = "Unreviewed";

  private static final String FULFILLED_OBLIGATION_STATUS = "Fulfilled";

  private static final String DEFAULT_MODAL_STATUS = "Fulfilled";

  private static final int TOTAL_OBLIGATION_COUNT = 10;

  private static final int ATTRIBUTION_ACCORDION_COUNT = 7;

  private ComponentIdentifier componentIdentifier;

  private String rootOrgDisplayText;

  private String rootOrgId;

  private ComponentLegalOverviewPage overviewPage;

  private ComponentLegalOverviewPageAssertions overviewAssertions;

  @Before
  public void openComponentLegalOverviewAsAdmin() throws Exception {
    seedTestData();

    playwrightRefreshOrOpen(ComponentLegalOverviewPage.url(APP_PUBLIC_ID, COMPONENT_HASH));
    playwrightLogin();

    overviewPage = new ComponentLegalOverviewPage();
    overviewAssertions = new ComponentLegalOverviewPageAssertions(overviewPage);
  }

  private void seedTestData() throws IOException {
    seedLicenseThreatGroups();
    seedOrgsAndApp();
    HdsStubs.legalOverview(testCLMServer.getHdsServer());
    resolveRootOrg();
  }

  private void seedLicenseThreatGroups() {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);
  }

  private void seedOrgsAndApp() {
    Organization org = tempEntity.newOrganization(ORG_NAME);
    Application app = tempEntity.newApplication(APP_NAME, APP_PUBLIC_ID, org.getId());

    componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        COMPONENT_GROUP_ID, COMPONENT_ARTIFACT_ID, COMPONENT_VERSION, "", "jar");
    ApplicationComponent applicationComponent = tempEntity.newApplicationComponent(
        app.getId(), BuildStageType.ID, COMPONENT_HASH, componentIdentifier);
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), COMPONENT_LICENSE_ID);
  }

  private void resolveRootOrg() {
    Organization rootOrg = lookup(OrganizationDAO.class).getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrgDisplayText = "Organization - " + rootOrg.getName();
    rootOrgId = rootOrg.getId();
  }

  @Test
  @Category(SanityTest.class)
  public void testComponentLegalOverviewPage() {
    verifyObligationsInitialState();
    verifyFulfillAllObligations();
    verifyAttributionAccordions();
  }

  @Test
  @Category(SanityTest.class)
  public void testNoObligationsHidesResolveButton() {
    testCLMServer.getHdsServer().respondWith("[]").atUri("/rest/license/metadata");
    playwrightRefreshOrOpen(ComponentLegalOverviewPage.url(APP_PUBLIC_ID, COMPONENT_HASH));

    overviewAssertions.shouldBeLoaded();
    verifyNoObligationsState();
  }

  private void verifyObligationsInitialState() {
    overviewAssertions.shouldHaveObligationCount(TOTAL_OBLIGATION_COUNT);
    overviewAssertions.shouldShowAllObligationStatus(TOTAL_OBLIGATION_COUNT, INITIAL_OBLIGATION_STATUS);
  }

  private void verifyFulfillAllObligations() {
    EditAllObligationsModal modal = overviewPage.clickResolveAllObligations();
    EditAllObligationsModalAssertions modalAssertions = new EditAllObligationsModalAssertions(modal);
    modalAssertions.shouldBeVisible();
    modalAssertions.shouldShowDefaultStatus(DEFAULT_MODAL_STATUS);
    modalAssertions.shouldHaveEmptyComment();
    modalAssertions.shouldShowScopeContaining(rootOrgDisplayText);
    modalAssertions.shouldShowScopeValue(rootOrgId);

    modal.fillComment(FULFILL_COMMENT);
    modal.save();
    modalAssertions.shouldBeHidden();

    overviewAssertions.shouldShowAllObligationStatus(TOTAL_OBLIGATION_COUNT, FULFILLED_OBLIGATION_STATUS);
  }

  private void verifyAttributionAccordions() {
    overviewAssertions.shouldHaveExpandedAccordionCount(ATTRIBUTION_ACCORDION_COUNT);
  }

  private void verifyNoObligationsState() {
    overviewAssertions.shouldHaveObligationCount(0);
    overviewAssertions.shouldHideResolveAllButton();
  }

  @Test
  @Category(RegressionTest.class)
  public void testObligationSegmentedButton_modalVsDirectStatus() {
    overviewAssertions.shouldHaveObligationCount(TOTAL_OBLIGATION_COUNT);

    overviewPage.clickSegmentedButtonMain(0);
    overviewAssertions.shouldHideSegmentedButtonDropdownMenu(0);

    playwrightRefreshOrOpen(ComponentLegalOverviewPage.url(APP_PUBLIC_ID, COMPONENT_HASH));

    overviewPage.clickSegmentedButtonDropdownToggle(0);
    overviewAssertions.shouldShowSegmentedButtonDropdownMenu(0);
    overviewAssertions.shouldHaveDropdownOptionCount(0, 3);
  }

  @Test
  @Category(RegressionTest.class)
  public void testObligationAccordions_collapsedByDefaultWithHeaderAndStatusIcons() {
    overviewAssertions.shouldHaveObligationCount(TOTAL_OBLIGATION_COUNT);

    overviewAssertions.shouldHaveObligationAccordionCollapsed(0);
    overviewAssertions.shouldHaveObligationHeaderCountText(0, "Inclusion of Copyright");

    EditAllObligationsModal modal = overviewPage.clickResolveAllObligations();
    modal.fillComment(FULFILL_COMMENT);
    modal.save();
    new EditAllObligationsModalAssertions(modal).shouldBeHidden();

    overviewAssertions.shouldShowFulfilledStatusIcon();
  }

  @Test
  @Category(RegressionTest.class)
  public void testAdditionalAttributionsSectionRenders() {
    overviewAssertions.shouldBeLoaded();
    overviewAssertions.shouldShowAdditionalAttributionTile();
    overviewAssertions.shouldShowAdditionalAttributionEditButton();
  }
}
