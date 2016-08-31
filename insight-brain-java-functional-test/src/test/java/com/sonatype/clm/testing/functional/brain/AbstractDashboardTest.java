/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.filter.DashboardFilter;

import org.junit.Before;
import org.junit.BeforeClass;

public abstract class AbstractDashboardTest extends AbstractFunctionalTest
{
  @BeforeClass
  public static void beforeClass() throws Exception {
    refreshOrOpen(DashboardPage.VIOLATIONS_URL);
    loginAsAdmin();
  }

  @Before
  public void before() {
    clearFilters();
    refreshOrOpen(DashboardPage.VIOLATIONS_URL);
  }

  protected void clearFilters() {
    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
    DashboardFilter filter = dashboardFilterDAO.getByUsername("admin");
    dashboardFilterDAO.delete(filter);
  }

}
