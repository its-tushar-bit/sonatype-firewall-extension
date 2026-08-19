/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqFipsUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.MtiqAdministratorsEditPage;
import com.sonatype.clm.testing.playwright.pages.MtiqAdministratorsEditPageAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.security.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Administrators page works under a FIPS-enabled tenant profile.
 * {@link AbstractMtiqFipsUiTest} inserts the BouncyCastle JCE provider and sets
 * {@code FIPS_MODE_ENABLED=true} before tenant provisioning. No UI assertions differ
 * from the non-FIPS path — the test validates FIPS mode does not break the page.
 */
@Tag("mtiq")
public class MtiqAdministratorsFIPSPlaywrightTest
    extends AbstractMtiqFipsUiTest
{
  private MtiqAdministratorsEditPage adminEditPage;

  private MtiqAdministratorsEditPageAssertions adminEditAssertions;

  @BeforeEach
  public void loginAndCreatePageObjects() {
    playwrightLoginAdminAt(DashboardPage.url());
    adminEditPage = new MtiqAdministratorsEditPage();
    adminEditAssertions = new MtiqAdministratorsEditPageAssertions(adminEditPage);
  }

  @Test
  public void testMtiqAdministratorsFips_pageLoadsWithoutCryptoErrors() {
    playwrightNavigateTo(MtiqAdministratorsEditPage.url(Role.POLICY_ADMIN_ROLE_ID));
    assertThat(adminEditPage.addMembersForm()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    adminEditAssertions.ldapGroupSearchAlertShouldBeHidden();
  }
}
