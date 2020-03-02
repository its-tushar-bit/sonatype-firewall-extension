/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.AdvancedSearchPage;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.FULL_TEXT_SEARCH_ENABLED;

public class AdvancedSearchPageTest
    extends AbstractFunctionalTest
{
  private final AdvancedSearchPage page = new AdvancedSearchPage();

  private final SystemConfigurationPropertyDAO dao = new SystemConfigurationPropertyDAO();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(AdvancedSearchPage.url());
    loginAsAdmin();
  }

  @Test
  public void testOptedOut_ShowsDisabledError() {
    refreshOrOpen(AdvancedSearchPage.url());
    page.advancedSearchDisabledError().shouldBe(visible);
    page.advancedSearchEnabledContent().shouldBe(hidden);
  }

  @Test
  public void testOptedIn_ShowsEnabledContent() {
    dao.update(new SystemConfigurationProperty(FULL_TEXT_SEARCH_ENABLED, "true"));
    refreshOrOpen(AdvancedSearchPage.url());
    page.advancedSearchDisabledError().shouldBe(hidden);
    page.advancedSearchEnabledContent().shouldBe(visible);
  }
}
