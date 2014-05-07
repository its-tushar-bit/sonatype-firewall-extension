/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.filter;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.filter.DashboardFilter;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

public class DashboardFilterDAOTest
    extends AbstractDbDAOTest
{
  private final DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();


  @Test
  public void testCRUD() {
    // Add filter
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter("admin", "testFilterString");

    // Retrieve filter and test
    DashboardFilter returnedFilter = dashboardFilterDAO.getByUsername(dashboardFilter.getUsername());
    assertFilter(returnedFilter, dashboardFilter);

    // Update filter
    dashboardFilter.setUsername("bob");
    dashboardFilterDAO.update(dashboardFilter);

    // Retrieve filter and test
    returnedFilter = dashboardFilterDAO.getByUsername(dashboardFilter.getUsername());
    assertFilter(returnedFilter, dashboardFilter);

    // Delete
    dashboardFilterDAO.delete(dashboardFilter);

    // Retrieve filter and test
    assertThat(dashboardFilterDAO.getByUsername(dashboardFilter.getUsername()), nullValue());
  }

  private void assertFilter(DashboardFilter actualFilter, DashboardFilter expectedFilter) {
    assertThat(actualFilter, notNullValue());
    assertThat(actualFilter.getId(), is(expectedFilter.getId()));
    assertThat(actualFilter.getUsername(), is(expectedFilter.getUsername()));
    assertThat(actualFilter.getFilter(), is(expectedFilter.getFilter()));
  }
}
