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
import com.sonatype.clm.testing.playwright.pages.OriginalSourcesFormPage;
import com.sonatype.clm.testing.playwright.pages.OriginalSourcesFormPageAssertions;
import com.sonatype.clm.testing.playwright.utils.HdsStubs;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ComponentLegalOriginalSourcesPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "pw-orig-sources-org";

  private static final String APP_NAME_PREFIX = "pw-orig-sources-app";

  private static final String APP_PUBLIC_ID_PREFIX = "pw-orig-sources";

  private static final String COMPONENT_GROUP_ID = "g";

  private static final String COMPONENT_ARTIFACT_ID = "a";

  private static final String COMPONENT_VERSION = "v";

  private static final String COMPONENT_HASH = "033e7a20b23ea284d474";

  private static final String COMPONENT_LICENSE_ID = "MIT";

  private static final String ADD_SOURCES_HEADER = "Add Original Sources";

  private String appPublicId;

  private OriginalSourcesFormPage sourcesPage;

  private OriginalSourcesFormPageAssertions assertions;

  @Before
  public void setUp() throws Exception {
    seedTestData();
    playwrightRefreshOrOpen(ComponentLegalOverviewPage.url(appPublicId, COMPONENT_HASH));
    playwrightLogin();

    sourcesPage = new OriginalSourcesFormPage();
    assertions = new OriginalSourcesFormPageAssertions(sourcesPage);
  }

  private void seedTestData() throws IOException {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);

    String orgName = ORG_NAME_PREFIX + TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(orgName);
    appPublicId = APP_PUBLIC_ID_PREFIX + TemporaryEntity.uuid();
    Application app = tempEntity.newApplication(APP_NAME_PREFIX + TemporaryEntity.uuid(), appPublicId, org.getId());

    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates(
        COMPONENT_GROUP_ID, COMPONENT_ARTIFACT_ID, COMPONENT_VERSION, "", "jar");
    OwnerComponent appComponent = tempEntity.newApplicationComponent(
        app.getId(), BuildStageType.ID, COMPONENT_HASH, componentId);
    tempEntity.newApplicationComponentLicense(appComponent.getId(), COMPONENT_LICENSE_ID);

    HdsStubs.legalOverview(testCLMServer.getHdsServer());
  }

  @Test
  @Category(RegressionTest.class)
  public void testOriginalSourcesForm_renders() {

    assertions.shouldShowTile();
    sourcesPage.openOriginalSourcesModal();
    assertions.shouldShowModal();
    assertions.shouldShowModalHeaderText(ADD_SOURCES_HEADER);
    assertions.shouldShowAddLinkButton();
  }

  @Test
  @Category(RegressionTest.class)
  public void testOriginalSources_addLinkAddsNewRow() {

    sourcesPage.openOriginalSourcesModal();
    assertions.shouldShowModal();

    assertThat(sourcesPage.sourceRows().first()).isVisible();
    int initialRowCount = sourcesPage.sourceRows().count();
    assertions.shouldHaveSourceRowCount(initialRowCount);
    sourcesPage.clickAddLink();
    assertions.shouldHaveSourceRowCount(initialRowCount + 1);
    assertions.shouldShowSourceInputAt(0);
    assertions.shouldShowToggleAt(0);
    assertions.shouldHaveToggleLabelText(0, "Included");
  }

  @Test
  @Category(RegressionTest.class)
  public void testOriginalSources_urlFieldConstraints() {

    sourcesPage.openOriginalSourcesModal();
    assertions.shouldShowModal();

    sourcesPage.clickAddLink();
    assertions.shouldHaveSourceInputEnabled(0);
    assertions.shouldHaveSourceInputMaxLength(0, "1000");
    assertions.shouldHaveToggleLabelText(0, "Included");

    sourcesPage.clickToggle(0);
    assertions.shouldHaveSourceInputDisabled(0);
    assertions.shouldHaveToggleLabelText(0, "Excluded");

    sourcesPage.clickToggle(0);
    assertions.shouldHaveSourceInputEnabled(0);
    assertions.shouldHaveToggleLabelText(0, "Included");
  }

  @Test
  @Category(RegressionTest.class)
  public void testOriginalSources_noModificationsAndSaveBecomesActive() {

    sourcesPage.openOriginalSourcesModal();
    assertions.shouldShowModal();
    assertions.shouldShowValidationAlert("No modifications");

    sourcesPage.clickAddLink();
    sourcesPage.fillSourceUrl(0, "https://example.com/source");
    assertions.shouldNotShowValidationAlert();

    sourcesPage.clickCancel();
    assertions.shouldHideModal();
  }

  @Test
  @Category(RegressionTest.class)
  public void testOriginalSources_scopeDropdownVisibleAndCancelCloses() {

    sourcesPage.openOriginalSourcesModal();
    assertions.shouldShowModal();
    assertions.shouldShowScopeDropdown();

    sourcesPage.clickCancel();
    assertions.shouldHideModal();
  }

}
