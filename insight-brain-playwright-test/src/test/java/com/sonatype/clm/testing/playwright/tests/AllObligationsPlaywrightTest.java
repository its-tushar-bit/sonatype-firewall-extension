/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.playwright.pages.ComponentLegalOverviewPageAssertions;
import com.sonatype.clm.testing.playwright.pages.EditAllObligationsModal;
import com.sonatype.clm.testing.playwright.pages.EditAllObligationsModalAssertions;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import org.apache.commons.io.IOUtils;
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

  @Before
  public void openComponentLegalOverviewAsAdmin() throws Exception {
    seedTestData();

    playwrightRefreshOrOpen(ComponentLegalOverviewPage.url(APP_PUBLIC_ID, COMPONENT_HASH));
    playwrightLogin();
  }

  private void seedTestData() throws IOException {
    seedLicenseThreatGroups();
    seedOrgsAndApp();
    seedHdsStubs();
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

  private void seedHdsStubs() throws IOException {
    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(
            getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
            StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(
            getClass().getResourceAsStream("/legal/legalCommentHdsResponse.json"),
            StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/file");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/source-link");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(
            getClass().getResourceAsStream("/legal/componentDetails.json"),
            StandardCharsets.UTF_8))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(
            getClass().getResourceAsStream("/legal/componentDetailsList.json"),
            StandardCharsets.UTF_8))
        .atUri("rest/ci/componentDetails/list");
  }

  private void resolveRootOrg() {
    Organization rootOrg = lookup(OrganizationDAO.class).getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrgDisplayText = "Organization - " + rootOrg.getName();
    rootOrgId = rootOrg.getId();
  }

  @Test
  @Category(SanityTest.class)
  public void testComponentLegalOverviewPage() {
    ComponentLegalOverviewPage overviewPage = new ComponentLegalOverviewPage();
    verifyObligationsInitialState(overviewPage);
    verifyFulfillAllObligations(overviewPage);
    verifyAttributionAccordions(overviewPage);
  }

  @Test
  @Category(SanityTest.class)
  public void testNoObligationsHidesResolveButton() {
    testCLMServer.getHdsServer().respondWith("[]").atUri("/rest/license/metadata");
    playwrightRefreshOrOpen(ComponentLegalOverviewPage.url(APP_PUBLIC_ID, COMPONENT_HASH));

    ComponentLegalOverviewPage overviewPage = new ComponentLegalOverviewPage();
    new ComponentLegalOverviewPageAssertions(overviewPage).shouldBeLoaded();
    verifyNoObligationsState(overviewPage);
  }

  private void verifyObligationsInitialState(ComponentLegalOverviewPage overviewPage) {
    ComponentLegalOverviewPageAssertions overviewAssertions = new ComponentLegalOverviewPageAssertions(overviewPage);
    overviewAssertions.shouldHaveObligationCount(TOTAL_OBLIGATION_COUNT);
    overviewAssertions.shouldShowAllObligationStatus(TOTAL_OBLIGATION_COUNT, INITIAL_OBLIGATION_STATUS);
  }

  private void verifyFulfillAllObligations(ComponentLegalOverviewPage overviewPage) {
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

    new ComponentLegalOverviewPageAssertions(overviewPage)
        .shouldShowAllObligationStatus(TOTAL_OBLIGATION_COUNT, FULFILLED_OBLIGATION_STATUS);
  }

  private void verifyAttributionAccordions(ComponentLegalOverviewPage overviewPage) {
    new ComponentLegalOverviewPageAssertions(overviewPage)
        .shouldHaveExpandedAccordionCount(ATTRIBUTION_ACCORDION_COUNT);
  }

  private void verifyNoObligationsState(ComponentLegalOverviewPage overviewPage) {
    ComponentLegalOverviewPageAssertions overviewAssertions = new ComponentLegalOverviewPageAssertions(overviewPage);
    overviewAssertions.shouldHaveObligationCount(0);
    overviewAssertions.shouldHideResolveAllButton();
  }
}
