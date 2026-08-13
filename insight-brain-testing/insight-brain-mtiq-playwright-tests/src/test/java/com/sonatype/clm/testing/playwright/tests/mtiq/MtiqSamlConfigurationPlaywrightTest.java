/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.SamlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.SamlConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Verifies the tenant-managed SAML configuration page in MTIQ mode.
 *
 * <p>
 * {@code SAML_ENABLED} is absent from {@code MTIQ_BANNED_FEATURES} and has
 * {@code enabledWhenAbsent = true}, so the page is available without feature-flag seeding.
 * All tests are read-only; {@link AbstractMtiqUiTest#afterTest()} handles tenant teardown.
 */
@Category(MtiqTest.class)
public class MtiqSamlConfigurationPlaywrightTest
    extends AbstractMtiqUiTest
{
  private SamlConfigurationPage samlPage;

  private SamlConfigurationPageAssertions samlAssertions;

  @Before
  public void loginAndCreatePageObjects() {
    playwrightLoginAdminAt(SamlConfigurationPage.url());
    samlPage = new SamlConfigurationPage();
    samlAssertions = new SamlConfigurationPageAssertions(samlPage);
  }

  @Test
  public void testMtiqSamlConfiguration_pageLoadsWithDefaultState() {
    assertThat(samlPage.saveButton()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    samlAssertions.shouldRenderPageLayout();
    samlAssertions.shouldShowSaveButtonDisabled();
    samlAssertions.shouldShowDeleteButtonDisabled();
  }
}
