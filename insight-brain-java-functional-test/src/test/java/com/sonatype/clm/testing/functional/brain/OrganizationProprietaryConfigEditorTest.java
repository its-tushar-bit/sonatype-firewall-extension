/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ProprietaryConfigEditorPage;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class OrganizationProprietaryConfigEditorTest
    extends AbstractProprietaryConfigEditorTest
{
  private Organization organization;

  @Before
  public void init() {
    organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
    super.init(organization);
  }

  @Test
  public void testEditProprietaryComponentMatchersRootOrg() {
    SidebarNavigation.policiesNavigationButton().click();
    OwnerSummaryPage.proprietaryComponentMatchers().shouldHave(text("2 local"));
  }

  @Test
  public void editProprietaryComponentMatchersVisualTest() {
    OwnerSummaryPage.proprietaryComponentMatchers().shouldBe(visible).click();
    waitUntilUrl(ProprietaryConfigEditorPage.url(organization));
    eyesWatcher.eyesCheck();
  }
}
