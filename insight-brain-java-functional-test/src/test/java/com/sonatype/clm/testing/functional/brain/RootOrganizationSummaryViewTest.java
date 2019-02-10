/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;

public class RootOrganizationSummaryViewTest extends AbstractFunctionalTest
{
  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.URL);
    loginAsAdmin();
  }

  @Before
  public void init() {
    Organization rootOrg = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);
    refreshOrOpen(OwnerSummaryPage.url(rootOrg));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(rootOrg.getName()));
  }

  @Test
  public void testDeleteRootOrg() {
    // Delete action should not be available to the root org
    ActionDropDown.actionButton().click();
    ActionDropDown.deleteOwnerButton().shouldNot(exist);
  }
}
