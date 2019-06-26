/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.RootOrgMigrate;
import com.sonatype.clm.testing.functional.elements.RootOrgMigrateModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.model.Organization;

import com.codeborne.selenide.Condition;
import org.junit.Test;
import org.mockito.Mockito;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;

public class RootOrgMigrateTest
    extends AbstractFunctionalTest
{
  private Organization org;

  private static final String ORG_NAME = "testModal";

  @Test
  public void testEverything() {
    org = tempEntity.newOrganization(ORG_NAME);
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrationScheduled()).thenReturn(false);
    refreshOrOpen(DashboardPage.URL);
    loginAsAdmin();
    testBanner();
    testModal();
    testNoMigrate();
    testBannerChange();
  }

  private void testBanner() {
    RootOrgMigrate.migrateBanner().shouldBe(visible);
    RootOrgMigrate.migrateConfiguredBanner().shouldBe(hidden);
    refreshOrOpen(ReportListPage.URL);
    RootOrgMigrate.migrateBanner().shouldBe(visible);
    RootOrgMigrate.migrateConfiguredBanner().shouldBe(hidden);
    refreshOrOpen(OrganizationManagementPage.ROOT_ORG_URL);
    RootOrgMigrate.migrateBanner().shouldBe(visible);
    RootOrgMigrate.migrateConfiguredBanner().shouldBe(hidden);
  }

  private void testModal() {
    RootOrgMigrate.migrateBanner().shouldBe(visible);
    RootOrgMigrate.migrateConfiguredBanner().shouldBe(hidden);

    RootOrgMigrate.startButton().shouldBe(visible).click();

    RootOrgMigrateModal.root().shouldBe(visible);

    eyesWatcher.eyesCheck();

    RootOrgMigrateModal.selectOrgRadioButton().shouldBe(Condition.selected);
    RootOrgMigrateModal.continueButton().shouldBe(Condition.disabled);

    RootOrgMigrateModal.organizationSelect().shouldBe(Condition.enabled).selectOption(ORG_NAME);
    RootOrgMigrateModal.continueButton().shouldBe(Condition.enabled);

    RootOrgMigrateModal.blankRootRadioButton().click();
    RootOrgMigrateModal.organizationSelect().shouldBe(Condition.disabled);

    RootOrgMigrateModal.cancelButton().click();
    RootOrgMigrateModal.root().shouldBe(hidden);
  }

  private void testNoMigrate() {
    RootOrgMigrate.startButton().shouldBe(visible).click();

    RootOrgMigrateModal.root().shouldBe(visible);
    RootOrgMigrateModal.blankRootRadioButton().click();

    RootOrgMigrateModal.continueButton().shouldBe(Condition.enabled).click();
    RootOrgMigrateModal.reloadAppLink().shouldBe(visible);

    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrationScheduled()).thenReturn(false);

    refreshOrOpen(OrganizationManagementPage.ROOT_ORG_URL);

    RootOrgMigrateModal.root().shouldBe(hidden);
    RootOrgMigrate.migrateBanner().shouldBe(hidden);
    RootOrgMigrate.migrateConfiguredBanner().shouldBe(hidden);
  }

  private void testBannerChange() {
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrationScheduled()).thenReturn(false);

    refreshOrOpen(OrganizationManagementPage.ROOT_ORG_URL);
    RootOrgMigrate.startButton().shouldBe(visible).click();

    RootOrgMigrateModal.root().shouldBe(visible);

    RootOrgMigrateModal.selectOrgRadioButton().shouldBe(Condition.selected);
    RootOrgMigrateModal.continueButton().shouldBe(Condition.disabled);

    RootOrgMigrateModal.organizationSelect().shouldBe(visible).selectOption(ORG_NAME);
    RootOrgMigrateModal.continueButton().shouldBe(Condition.enabled).click();

    RootOrgMigrateModal.root().shouldBe(hidden);

    Mockito.verify(rootOrganizationConfigMigrationUtils).setSourceOrganizationId(org.getId());

    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrationScheduled()).thenReturn(true);

    refreshOrOpen(OrganizationManagementPage.ROOT_ORG_URL);
    RootOrgMigrate.migrateBanner().shouldBe(hidden);
    RootOrgMigrate.migrateConfiguredBanner().shouldBe(visible);
    refreshOrOpen(ReportListPage.URL);
    RootOrgMigrate.migrateBanner().shouldBe(hidden);
    RootOrgMigrate.migrateConfiguredBanner().shouldBe(visible);
    refreshOrOpen(DashboardPage.URL);
    RootOrgMigrate.migrateBanner().shouldBe(hidden);
    RootOrgMigrate.migrateConfiguredBanner().shouldBe(visible);
  }
}
