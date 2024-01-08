/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq;

import com.sonatype.clm.testing.functional.elements.OrgsAndPoliciesSidebar;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.utils.NameSupplierDictionary;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;

public class MtiqOrgsAndPoliciesSidebarTest
    extends AbstractMtiqFunctionalTest
{
  @Before
  public void init() {
    tempEntity.newRelatedOrganizationsAsMap(null, 2, 3, 3, new NameSupplierDictionary());
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_importApplications() {
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = new OrgsAndPoliciesSidebar();

    orgsAndPoliciesSidebar.getOrganizationLink(0).click();
    orgsAndPoliciesSidebar.getApplicationPlusIcon().click();

    orgsAndPoliciesSidebar.getImportApplicationsButton().shouldBe(visible);
  }
}
