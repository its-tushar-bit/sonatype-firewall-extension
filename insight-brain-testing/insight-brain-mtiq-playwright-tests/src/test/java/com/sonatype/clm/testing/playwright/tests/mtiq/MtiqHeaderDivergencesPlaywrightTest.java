/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.MtiqHeaderDivergencesPage;
import com.sonatype.clm.testing.playwright.pages.MtiqHeaderDivergencesPageAssertions;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Verifies MTIQ-mode header/footer divergences: version text hidden
 * ({@code selectIsShowVersionEnabled=false}), notifications bell hidden
 * ({@code selectIsShowNotificationMenuEnabled=false}), and mail-config delete shows
 * Sonatype-fallback warning ({@code selectIsShowEmailStoppedEnabled=false}).
 */
@Tag("mtiq")
public class MtiqHeaderDivergencesPlaywrightTest
    extends AbstractMtiqUiTest
{
  private static final Data DATA = TestDataManager.load("mtiq-header-divergences", Data.class);

  private MtiqHeaderDivergencesPage divergencesPage;

  private MtiqHeaderDivergencesPageAssertions assertions;

  @BeforeEach
  public void loginAndCreatePageObjects() {
    playwrightLoginAdminAt(DashboardPage.url());
    divergencesPage = new MtiqHeaderDivergencesPage();
    assertions = new MtiqHeaderDivergencesPageAssertions(divergencesPage);
  }

  // Runs before afterTest() switches to GLOBAL_TENANT, so the singleton row is removed
  // from the correct tenant schema.
  @AfterEach
  public void cleanupMailConfig() {
    if (lookup(MailConfigurationDAO.class).getWithoutFallback() != null) {
      lookup(MailConfigurationDAO.class).delete();
    }
  }

  @Test
  public void testMtiqHeaderDivergences_versionText_hiddenInFooter() {
    assertions.footerVersionTextShouldBeHidden();
  }

  @Test
  public void testMtiqHeaderDivergences_notificationMenu_hiddenInHeader() {
    assertions.notificationsMenuButtonShouldBeHidden();
  }

  @Test
  public void testMtiqHeaderDivergences_emailStoppedBanner_showsSonatypeFallbackOnDelete() {
    MailConfiguration config = new MailConfiguration();
    config.setHostname(DATA.mailConfigHostname());
    config.setPort(DATA.mailConfigPort());
    config.setSystemEmail(DATA.mailConfigSystemEmail());
    lookup(MailConfigurationDAO.class).set(config);

    // SPA fetches features once at login; at that point no mail config exists so
    // EMAIL_CONFIGURATION is absent. Reload re-fetches with the seeded config, enabling DELETE.
    playwrightNavigateTo(MtiqHeaderDivergencesPage.mailConfigUrl());
    playwrightRefresh();
    assertions.deleteConfigButtonShouldBeEnabled();
    divergencesPage.deleteConfigButton().click();

    assertions.deleteModalShouldBeVisible();
    assertions.deleteModalWarningShouldContain(DATA.sonatypeMailFallbackWarning());

    divergencesPage.cancelDeleteButton().click();
  }

  private record Data(
      String sonatypeMailFallbackWarning,
      String mailConfigHostname,
      int mailConfigPort,
      String mailConfigSystemEmail)
  {
  }
}
