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
import com.sonatype.clm.testing.playwright.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.playwright.pages.NoticeFilesAccordionPage;
import com.sonatype.clm.testing.playwright.pages.NoticeFilesAccordionPageAssertions;
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

public class ComponentLegalNoticeFilesPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "pw-notice-files-org";

  private static final String APP_NAME_PREFIX = "pw-notice-files-app";

  private static final String APP_PUBLIC_ID_PREFIX = "pw-notice-files";

  private static final String COMPONENT_GROUP_ID = "g";

  private static final String COMPONENT_ARTIFACT_ID = "a";

  private static final String COMPONENT_VERSION = "v";

  private static final String COMPONENT_HASH = "033e7a20b23ea284d474";

  private static final String COMPONENT_LICENSE_ID = "MIT";

  private String appPublicId;

  private NoticeFilesAccordionPage noticePage;

  private NoticeFilesAccordionPageAssertions assertions;

  @Before
  public void setUp() throws Exception {
    seedTestData();
    playwrightRefreshOrOpen(ComponentLegalOverviewPage.url(appPublicId, COMPONENT_HASH));
    playwrightLogin();

    noticePage = new NoticeFilesAccordionPage();
    assertions = new NoticeFilesAccordionPageAssertions(noticePage);
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

    HdsStubs.legalOverview(testCLMServer.getHdsServer());
  }

  @Test
  @Category(RegressionTest.class)
  public void testNoticeFilesAccordion_rendersAndModalOpensCloses() {

    assertions.shouldShowTile();
    assertions.shouldShowEditButton();
    assertions.shouldShowNoneFound();

    noticePage.openNoticesModal();
    assertions.shouldShowModal();
    assertions.shouldShowAddNoticeButton();

    noticePage.clickCancel();
    assertions.shouldHideModal();
  }

  @Test
  @Category(RegressionTest.class)
  public void testNoticeFiles_addIconWhenEmpty() {

    assertions.shouldShowTile();
    assertions.shouldShowEditButton();
    assertions.shouldShowAddIcon();

    noticePage.openNoticesModal();
    assertions.shouldShowModal();
    assertions.shouldShowAddNoticeButton();

    noticePage.clickCancel();
    assertions.shouldHideModal();
  }

  @Test
  @Category(RegressionTest.class)
  public void testNoticeFiles_editIconWhenNoticesExist() throws IOException {
    HdsStubs.legalOverview(testCLMServer.getHdsServer(), true);
    playwrightRefreshOrOpen(ComponentLegalOverviewPage.url(appPublicId, COMPONENT_HASH));

    assertions.shouldShowTile();
    assertions.shouldShowEditButton();
    assertions.shouldShowEditIcon();

    noticePage.openNoticesModal();
    assertions.shouldShowModal();

    noticePage.clickCancel();
    assertions.shouldHideModal();
  }

}
