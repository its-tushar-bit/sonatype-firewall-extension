/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.RootOrgMigrate;
import com.sonatype.clm.testing.functional.elements.RootOrgMigrateModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ManagementPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.model.Organization;

import com.codeborne.selenide.Condition;
import org.junit.Test;
import org.mockito.Mockito;

import static com.codeborne.selenide.Selenide.open;

public class RootOrgMigrateTest
    extends AbstractFunctionalTest
{
  private Organization org;

  private static final String ORG_NAME = "testModal";

  @Test
  public void testEverything() throws IOException {
    org = tempEntity.newOrganization(ORG_NAME);
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrationScheduled()).thenReturn(false);
    open(DashboardPage.URL);
    loginAsAdmin();
    testBanner();
    testModal();
    testNoMigrate();
    testBannerChange();
  }

  private void testBanner() {
    RootOrgMigrate.migrateBanner().shouldBe(Condition.visible);
    RootOrgMigrate.migrateConfiguredBanner().shouldNotBe(Condition.visible);
    refreshOrOpen(ReportListPage.URL);
    RootOrgMigrate.migrateBanner().shouldBe(Condition.visible);
    RootOrgMigrate.migrateConfiguredBanner().shouldNotBe(Condition.visible);
    refreshOrOpen(ManagementPage.URL);
    RootOrgMigrate.migrateBanner().shouldBe(Condition.visible);
    RootOrgMigrate.migrateConfiguredBanner().shouldNotBe(Condition.visible);
  }

  private void testModal() {
    RootOrgMigrate.migrateBanner().shouldBe(Condition.visible);
    RootOrgMigrate.migrateConfiguredBanner().shouldNotBe(Condition.visible);

    RootOrgMigrate.startButton().shouldBe(Condition.visible).click();

    RootOrgMigrateModal.root().isDisplayed();

    RootOrgMigrateModal.selectOrgRadioButton().shouldBe(Condition.selected);
    RootOrgMigrateModal.continueButton().shouldBe(Condition.disabled);

    RootOrgMigrateModal.organizationSelect().shouldBe(Condition.enabled).selectOption(ORG_NAME);
    RootOrgMigrateModal.continueButton().shouldBe(Condition.enabled);

    RootOrgMigrateModal.blankRootRadioButton().setSelected(true);
    RootOrgMigrateModal.organizationSelect().shouldBe(Condition.disabled);

    RootOrgMigrateModal.cancelButton().click();
    RootOrgMigrateModal.root().shouldNotBe(Condition.visible);
  }

  private void testNoMigrate() {
    RootOrgMigrate.startButton().shouldBe(Condition.visible).click();

    RootOrgMigrateModal.root().isDisplayed();
    RootOrgMigrateModal.blankRootRadioButton().setSelected(true);

    RootOrgMigrateModal.continueButton().shouldBe(Condition.enabled).click();
    RootOrgMigrateModal.reloadAppLink().shouldBe(Condition.visible);

    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrationScheduled()).thenReturn(false);

    refreshOrOpen(ManagementPage.URL);

    RootOrgMigrateModal.root().shouldNotBe(Condition.visible);
    RootOrgMigrate.migrateBanner().shouldNotBe(Condition.visible);
    RootOrgMigrate.migrateConfiguredBanner().shouldNotBe(Condition.visible);
  }

  private void testBannerChange() throws IOException {
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrationScheduled()).thenReturn(false);

    refreshOrOpen(ManagementPage.URL);
    RootOrgMigrate.startButton().shouldBe(Condition.visible).click();

    RootOrgMigrateModal.root().isDisplayed();

    RootOrgMigrateModal.selectOrgRadioButton().shouldBe(Condition.selected);
    RootOrgMigrateModal.continueButton().shouldBe(Condition.disabled);

    RootOrgMigrateModal.organizationSelect().shouldBe(Condition.visible).selectOption(ORG_NAME);
    RootOrgMigrateModal.continueButton().shouldBe(Condition.enabled).click();

    RootOrgMigrateModal.root().shouldNotBe(Condition.visible);

    Mockito.verify(rootOrganizationConfigMigrationUtils).setSourceOrganizationId(org.getId());

    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrationScheduled()).thenReturn(true);

    refreshOrOpen(ManagementPage.URL);
    RootOrgMigrate.migrateBanner().shouldNotBe(Condition.visible);
    RootOrgMigrate.migrateConfiguredBanner().shouldBe(Condition.visible);
    refreshOrOpen(ReportListPage.URL);
    RootOrgMigrate.migrateBanner().shouldNotBe(Condition.visible);
    RootOrgMigrate.migrateConfiguredBanner().shouldBe(Condition.visible);
    refreshOrOpen(DashboardPage.URL);
    RootOrgMigrate.migrateBanner().shouldNotBe(Condition.visible);
    RootOrgMigrate.migrateConfiguredBanner().shouldBe(Condition.visible);
  }
}
