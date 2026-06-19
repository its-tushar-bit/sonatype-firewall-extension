/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.AttributionReportFormPage;
import com.sonatype.clm.testing.playwright.pages.AttributionReportFormPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.playwright.pages.LicenseFilesAccordionPage;
import com.sonatype.clm.testing.playwright.pages.LicenseFilesAccordionPageAssertions;
import com.sonatype.clm.testing.playwright.utils.HdsStubs;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ComponentLegalAttributionAndLicenseFilesPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "pw-attr-license-org";

  private static final String APP_NAME_PREFIX = "pw-attr-license-app";

  private static final String APP_PUBLIC_ID_PREFIX = "pw-attr-license";

  private static final String COMPONENT_GROUP_ID = "g";

  private static final String COMPONENT_ARTIFACT_ID = "a";

  private static final String COMPONENT_VERSION = "v";

  private static final String COMPONENT_HASH = "033e7a20b23ea284d474";

  private static final String COMPONENT_LICENSE_ID = "MIT";

  private String appPublicId;

  private AttributionReportFormPage reportPage;

  private AttributionReportFormPageAssertions reportAssertions;

  private LicenseFilesAccordionPage licenseFilesPage;

  private LicenseFilesAccordionPageAssertions licenseFilesAssertions;

  @Before
  public void setUp() throws Exception {
    seedTestData();
    playwrightRefreshOrOpen(ComponentLegalOverviewPage.url(appPublicId, COMPONENT_HASH));
    playwrightLogin();

    reportPage = new AttributionReportFormPage();
    reportAssertions = new AttributionReportFormPageAssertions(reportPage);
    licenseFilesPage = new LicenseFilesAccordionPage();
    licenseFilesAssertions = new LicenseFilesAccordionPageAssertions(licenseFilesPage);
  }

  private void seedTestData() throws IOException {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);

    String orgName = ORG_NAME_PREFIX + TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(orgName);
    appPublicId = APP_PUBLIC_ID_PREFIX + TemporaryEntity.uuid();
    Application app = tempEntity.newApplication(APP_NAME_PREFIX + TemporaryEntity.uuid(), appPublicId, org.getId());

    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates(
        COMPONENT_GROUP_ID, COMPONENT_ARTIFACT_ID, COMPONENT_VERSION, "", "jar");
    ApplicationComponent appComponent = tempEntity.newApplicationComponent(
        app.getId(), BuildStageType.ID, COMPONENT_HASH, componentId);
    tempEntity.newApplicationComponentLicense(appComponent.getId(), COMPONENT_LICENSE_ID);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());

    HdsStubs.legalOverview(testCLMServer.getHdsServer());
  }

  @Test
  @Category(RegressionTest.class)
  public void testAttributionReportForm_rendersWithOptions() {
    playwrightRefreshOrOpen(AttributionReportFormPage.url(appPublicId, BuildStageType.ID));

    reportAssertions.shouldShowContainer();
    reportAssertions.shouldShowForm();
    reportAssertions.shouldShowReportTitleInput();
    reportAssertions.shouldShowGenerateReportButton();
    reportAssertions.shouldShowManageTemplatesButton();
    reportAssertions.shouldShowTableOfContentsCheckbox();
    reportAssertions.shouldShowIncludeLicenseCheckbox();
    reportAssertions.shouldShowAppendixCheckbox();
    reportAssertions.shouldShowAdditionalNoticeFilesSection();
    reportAssertions.shouldShowAttachFilesButton();
  }

  @Test
  @Category(RegressionTest.class)
  public void testAttributionReportForm_generateButtonEnabledWithValidTitle() {
    playwrightRefreshOrOpen(AttributionReportFormPage.url(appPublicId, BuildStageType.ID));

    reportAssertions.shouldShowContainer();
    reportAssertions.shouldShowReportTitleInput();
    reportAssertions.shouldShowGenerateReportButton();
    reportAssertions.shouldHaveGenerateReportButtonEnabled();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLicenseFilesAccordion_addIconWhenEmpty() {
    playwrightRefreshOrOpen(ComponentLegalOverviewPage.url(appPublicId, COMPONENT_HASH));

    licenseFilesAssertions.shouldShowTile();
    licenseFilesAssertions.shouldShowEditButton();
    licenseFilesAssertions.shouldShowAddIcon();
    licenseFilesAssertions.shouldShowNoneFound();

    licenseFilesPage.openLicenseFilesModal();
    licenseFilesAssertions.shouldShowModal();
    licenseFilesAssertions.shouldShowAddLicenseButton();

    licenseFilesPage.clickCancel();
    licenseFilesAssertions.shouldHideModal();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLicenseFilesAccordion_editIconWhenFilesExist() throws IOException {
    testCLMServer.getHdsServer()
        .respondWith(readClasspathUtf8(getClass(), "/legal/legalFileHdsResponse.json"))
        .atUri("/rest/legal/file");
    playwrightRefreshOrOpen(ComponentLegalOverviewPage.url(appPublicId, COMPONENT_HASH));

    licenseFilesAssertions.shouldShowTile();
    licenseFilesAssertions.shouldShowEditButton();
    licenseFilesAssertions.shouldShowEditIcon();

    licenseFilesPage.openLicenseFilesModal();
    licenseFilesAssertions.shouldShowModal();

    licenseFilesPage.clickCancel();
    licenseFilesAssertions.shouldHideModal();
  }

}
