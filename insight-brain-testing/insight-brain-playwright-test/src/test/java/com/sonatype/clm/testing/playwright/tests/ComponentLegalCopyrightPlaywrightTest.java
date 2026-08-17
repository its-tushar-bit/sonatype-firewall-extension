/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.playwright.pages.CopyrightOverrideFormPage;
import com.sonatype.clm.testing.playwright.pages.CopyrightOverrideFormPageAssertions;
import com.sonatype.clm.testing.playwright.utils.HdsStubs;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ComponentLegalCopyrightPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "pw-copyright-org";

  private static final String APP_NAME_PREFIX = "pw-copyright-app";

  private static final String APP_PUBLIC_ID_PREFIX = "pw-copyright-app";

  private static final String COMPONENT_GROUP_ID = "g";

  private static final String COMPONENT_ARTIFACT_ID = "a";

  private static final String COMPONENT_VERSION = "v";

  private static final String COMPONENT_HASH = "033e7a20b23ea284d474";

  private static final String COMPONENT_LICENSE_ID = "MIT";

  private static final String EDIT_MODAL_HEADER = "Edit Copyright Notices";

  private String appPublicId;

  private String rootOrgId;

  private CopyrightOverrideFormPage copyrightPage;

  private CopyrightOverrideFormPageAssertions assertions;

  @BeforeEach
  public void setUp() throws Exception {
    seedTestData();
    playwrightRefreshOrOpen(ComponentLegalOverviewPage.url(appPublicId, COMPONENT_HASH));
    playwrightLogin();

    copyrightPage = new CopyrightOverrideFormPage();
    assertions = new CopyrightOverrideFormPageAssertions(copyrightPage);
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
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());

    HdsStubs.legalOverview(testCLMServer.getHdsServer());

    Organization rootOrg = lookup(OrganizationDAO.class).getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrgId = rootOrg.getId();
  }

  @Test
  @Tag("regression")
  public void testCopyrightModal_rendersAndAddsNewRow() {

    assertions.shouldShowCopyrightTile();
    copyrightPage.openCopyrightModal();
    assertions.shouldShowModal();
    assertions.shouldShowModalHeaderText(EDIT_MODAL_HEADER);
    assertions.shouldShowAddCopyrightButton();
    assertions.shouldShowTextInputAt(0);
    assertions.shouldShowToggleAt(0);
    assertions.shouldHaveToggleLabelText(0, "Included");

    assertThat(copyrightPage.copyrightRows().first()).isVisible();
    int initialRowCount = copyrightPage.copyrightRows().count();
    assertions.shouldHaveCopyrightRowCount(initialRowCount);
    copyrightPage.clickAddCopyright();
    assertions.shouldHaveCopyrightRowCount(initialRowCount + 1);
  }

  @Test
  @Tag("regression")
  public void testCopyrightModal_noModificationsMessageAndCancelCloses() {

    copyrightPage.openCopyrightModal();
    assertions.shouldShowModal();
    assertions.shouldShowValidationAlert("No modifications");

    copyrightPage.clickCancel();
    assertions.shouldHideModal();
  }

  @Test
  @Tag("regression")
  public void testCopyrightModal_toggleIncludeExclude() {

    copyrightPage.openCopyrightModal();
    assertions.shouldShowModal();
    assertions.shouldShowToggleAt(0);
    assertions.shouldHaveToggleLabelText(0, "Included");

    copyrightPage.clickToggle(0);
    assertions.shouldHaveToggleLabelText(0, "Excluded");
    assertions.shouldHaveSaveButtonEnabled();
  }

  @Test
  @Tag("regression")
  public void testCopyrightModal_scopeDropdown() {

    copyrightPage.openCopyrightModal();
    assertions.shouldShowModal();
    assertions.shouldShowScopeDropdown();
    assertions.shouldHaveScopeValue(rootOrgId);
  }

}
