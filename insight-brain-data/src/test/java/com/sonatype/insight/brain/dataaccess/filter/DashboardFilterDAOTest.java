/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.filter;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.NameableDAOTest;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DashboardFilterDAOTest
    extends NameableDAOTest<DashboardFilter>
{
  private DashboardFilterDAO dashboardFilterDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dashboardFilterDAO = daoFactory.createDashboardFilterDAO();
  }

  @Override
  protected DashboardFilter createNameable(String a) {
    DashboardFilter dashboardFilter =
        tempEntity.newDashboardFilter("test123", "testRealmId", a, "testFilterString 1111");

    return dashboardFilter;
  }

  @Override
  protected AbstractOperationalSqlDAO<DashboardFilter> getDao() {
    return dashboardFilterDAO;
  }

  @Override
  protected int getMaxNameLength() {
    return NameHelper.MAX_NAME_LENGTH;
  }

  @Override
  protected DashboardFilter getEntityByName(String name) {
    return dashboardFilterDAO.getByUsernameAndRealmIdAndName("test123", "testRealmId", name);
  }

  @Test
  public void testCRUD() {
    // Add filter
    DashboardFilter dashboardFilter =
        tempEntity.newDashboardFilter("testUsername", "testRealmId", "testFilterName", "testFilterString");

    // Retrieve filter and test
    DashboardFilter returnedFilter = dashboardFilterDAO.getById(dashboardFilter.getId());
    assertFilter(returnedFilter, dashboardFilter);

    // Update filter
    dashboardFilter.setFilter("testFilterString updated");
    dashboardFilterDAO.update(dashboardFilter);

    // Retrieve filter and test
    returnedFilter = dashboardFilterDAO.getById(dashboardFilter.getId());
    assertFilter(returnedFilter, dashboardFilter);

    // Delete
    dashboardFilterDAO.delete(dashboardFilter);

    // Retrieve filter and test
    assertThat(dashboardFilterDAO.getById(dashboardFilter.getId())).isNull();
  }

  @Test
  public void testGetByUsernameAndRealmId() {
    String username = "test123";
    String realmId = "realm123";
    DashboardFilter dashboardFilter1 =
        tempEntity.newDashboardFilter(username, realmId, "filter 1", "testFilterString 1");
    DashboardFilter dashboardFilter2 = tempEntity.newDashboardFilter(username, realmId, "", "testFilterString 2");
    tempEntity.newDashboardFilter("admin123", realmId, "filter 2", "testFilterString 2");

    // Retrieve filters and test
    List<DashboardFilter> actual = dashboardFilterDAO.getByUsernameAndRealmId(username, realmId);
    assertThat(actual).hasSize(2);
    assertFilter(actual.get(0), dashboardFilter2);
    assertFilter(actual.get(1), dashboardFilter1);
  }

  @Test
  public void testGetByUsernameAndRealmId_UsernameCaseInsensitive() {
    String realmId = "realm123";
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter("Test123", realmId, "filter", "testFilterString");

    // Retrieve filters and test
    List<DashboardFilter> actual = dashboardFilterDAO.getByUsernameAndRealmId("tEst123", realmId);
    assertThat(actual).hasSize(1);
    assertFilter(actual.get(0), dashboardFilter);
  }

  @Test
  public void testGetNamedFiltersByUsernameAndRealmId() {
    String username = "test123";
    String realmId = "realm123";
    DashboardFilter dashboardFilter1 =
        tempEntity.newDashboardFilter(username, realmId, "filter 1", "testFilterString 1");
    DashboardFilter dashboardFilter2 =
        tempEntity.newDashboardFilter(username, realmId, "filter 2", "testFilterString 2");
    tempEntity.newDashboardFilter(username, realmId, "", "testFilterString 2");
    tempEntity.newDashboardFilter("admin123", realmId, "filter 2", "testFilterString 2");

    // Retrieve filters and test
    List<DashboardFilter> actual = dashboardFilterDAO.getNamedFiltersByUsernameAndRealmId(username, realmId);
    assertThat(actual).hasSize(2);
    assertFilter(actual.get(0), dashboardFilter1);
    assertFilter(actual.get(1), dashboardFilter2);
  }

  @Test
  public void testGetNamedFiltersByUsernameAndRealmId_UsernameCaseInsensitive() {
    String realmId = "realm123";
    DashboardFilter dashboardFilter =
        tempEntity.newDashboardFilter("Test123", realmId, "filter 1", "testFilterString 1");

    // Retrieve filters and test
    List<DashboardFilter> actual = dashboardFilterDAO.getNamedFiltersByUsernameAndRealmId("tEst123", realmId);
    assertThat(actual).hasSize(1);
    assertFilter(actual.get(0), dashboardFilter);
  }

  @Test
  public void testGetByUsernameAndRealmIdAndName() {
    String username = "test123";
    String realmId = "realm123";
    String filterName = "Abc filter";
    DashboardFilter dashboardFilter =
        tempEntity.newDashboardFilter(username, realmId, filterName, "testFilterString 1");
    tempEntity.newDashboardFilter(username, realmId, "Xyz Filter", "testFilterString 1");

    // Retrieve filter and test
    DashboardFilter actual = dashboardFilterDAO.getByUsernameAndRealmIdAndName(username, realmId, filterName);
    assertFilter(actual, dashboardFilter);
  }

  @Test
  public void testGetByUsernameAndRealmIdAndName_UsernameCaseInsensitive() {
    String realmId = "realm123";
    String filterName = "Abc filter";
    DashboardFilter dashboardFilter =
        tempEntity.newDashboardFilter("Test123", realmId, filterName, "testFilterString 1");

    // Retrieve filter and test
    DashboardFilter actual = dashboardFilterDAO.getByUsernameAndRealmIdAndName("tEst123", realmId, filterName);
    assertFilter(actual, dashboardFilter);
  }

  @Test
  public void testDuplicateName_Insert_LegacyFilter() {
    String username = "test123";
    tempEntity.newDashboardFilterLegacy(username, "Filter12345", "testFilterString 1111");
    assertThatThrownBy(() -> tempEntity.newDashboardFilter(username, "testRealmId", "FILTER 12345",
        "testFilterString 1111")).isInstanceOf(InvalidNameException.class)
            .hasMessage("FILTER 12345 is already used as a name.");
  }

  @Test
  public void testDuplicateName_Update_LegacyFilter() {
    String username = "test0123";
    tempEntity.newDashboardFilterLegacy(username, "Filter12345", "testFilterString 1111");
    DashboardFilter dashboardFilter =
        tempEntity.newDashboardFilter(username, "testRealmId", "Filter 0123", "testFilterString 1111");
    dashboardFilter.setName("FILTER 12345");
    assertThatThrownBy(() -> dashboardFilterDAO.update(dashboardFilter)).isInstanceOf(InvalidNameException.class)
        .hasMessage("FILTER 12345 is already used as a name.");
  }

  @Test
  public void testValidate_insertNamedFilterBasedOnAnother() {
    DashboardFilter dashboardFilter = new DashboardFilter("testUsername", "testRealmId", "valid name");
    dashboardFilter.setBasedOnFilterName("any non-null value");
    assertThatThrownBy(() -> dashboardFilterDAO.insert(dashboardFilter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Only the active filter can be based on another filter.");
  }

  @Test
  public void testValidate_insertActiveFilterBasedOnMissingFilter() {
    String username = "test user";
    String realmId = "testRealmId";
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter(username, realmId, "" /* filterName */,
        true /* acknowledged */, "valid name of a filter that does not exist" /* basedOn */, "some filter string");

    DashboardFilter activeFilter = dashboardFilterDAO.getById(dashboardFilter.getId());
    assertThat(activeFilter.getBasedOnFilterName()).isNull();
  }

  @Test
  public void testValidate_updateNamedFilterBasedOnAnother() {
    DashboardFilter dashboardFilter =
        tempEntity.newDashboardFilter("test user", "testRealmId", "valid name", "originalFilter");
    dashboardFilter.setFilter("updatedFilter");
    dashboardFilter.setBasedOnFilterName("any non-null value");
    assertThatThrownBy(() -> dashboardFilterDAO.update(dashboardFilter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Only the active filter can be based on another filter.");
  }

  @Test
  public void testValidate_updateActiveFilterBasedOnMissingFilter() {
    String username = "test user";
    String realmId = "testRealmId";
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter(username, realmId, "", "originalFilter");
    dashboardFilter.setFilter("updatedFilter");
    dashboardFilter.setBasedOnFilterName("valid name of a filter that does not exist");
    dashboardFilterDAO.update(dashboardFilter);

    DashboardFilter activeFilter = dashboardFilterDAO.getById(dashboardFilter.getId());
    assertThat(activeFilter.getBasedOnFilterName()).isNull();
  }

  @Test
  public void testGetLegacyByUsernameAndName() {
    String username = "testUsername";
    String realmId = "testRealmId";
    String filterName = "testFilterName";
    tempEntity.newDashboardFilter(username, realmId, filterName, "filter");
    DashboardFilter dashboardFilterLegacy = tempEntity.newDashboardFilterLegacy(username, filterName, "filter");

    assertFilter(dashboardFilterDAO.getLegacyByUsernameAndName(username, filterName), dashboardFilterLegacy);
  }

  @Test
  public void testGetLegacyByUsernameAndName_UsernameCaseInsensitive() {
    String realmId = "testRealmId";
    String filterName = "testFilterName";
    tempEntity.newDashboardFilter("testUsername", realmId, filterName, "filter");
    DashboardFilter dashboardFilterLegacy = tempEntity.newDashboardFilterLegacy("testUsername", filterName, "filter");

    assertFilter(dashboardFilterDAO.getLegacyByUsernameAndName("TestUsername", filterName), dashboardFilterLegacy);
  }

  @Test
  public void testGetLegacyByUsernameAndName_MultipleFilters() {
    String realmId = "testRealmId";
    String filterName = "testFilterName";
    tempEntity.newDashboardFilter("testUsername", realmId, filterName, "filter");
    DashboardFilter dashboardFilterLegacy1 = tempEntity.newDashboardFilterLegacy("testUsername", filterName, "filter");
    DashboardFilter dashboardFilterLegacy2 = tempEntity.newDashboardFilterLegacy("TestUsername", filterName, "filter");

    assertFilter(dashboardFilterDAO.getLegacyByUsernameAndName("testUsername", filterName), dashboardFilterLegacy1);
    assertFilter(dashboardFilterDAO.getLegacyByUsernameAndName("TestUsername", filterName), dashboardFilterLegacy2);
    assertThat(dashboardFilterDAO.getLegacyByUsernameAndName("TESTUSERNAME", filterName).getRealmId()).isNull();
  }

  @Test
  public void testGetLegacyNamedFiltersByUsername() {
    String username = "test123";
    DashboardFilter dashboardFilter1 = tempEntity.newDashboardFilterLegacy(username, "filter 1", "testFilterString 1");
    DashboardFilter dashboardFilter2 = tempEntity.newDashboardFilterLegacy(username, "filter 2", "testFilterString 2");
    tempEntity.newDashboardFilterLegacy(username, "", "testFilterString 2");
    tempEntity.newDashboardFilterLegacy("admin123", "filter 2", "testFilterString 2");
    tempEntity.newDashboardFilter(username, "testRealmId", "filter 3", "testFilterString 3");

    // Retrieve filters and test
    List<DashboardFilter> actual = dashboardFilterDAO.getLegacyNamedFiltersByUsername(username);
    assertThat(actual).hasSize(2);
    assertFilter(actual.get(0), dashboardFilter1);
    assertFilter(actual.get(1), dashboardFilter2);
  }

  @Test
  public void testGetLegacyNamedFiltersByUsername_UsernameCaseInsensitive() {
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilterLegacy("Test123", "filter 1", "testFilterString 1");

    // Retrieve filters and test
    List<DashboardFilter> actual = dashboardFilterDAO.getLegacyNamedFiltersByUsername("tEst123");
    assertThat(actual).hasSize(1);
    assertFilter(actual.get(0), dashboardFilter);
  }

  @Test
  public void testInsert_BasedOnLegacyFilter() {
    String username = "testUsername";
    String realmId = "testRealmId";
    DashboardFilter basedOnFilter = tempEntity.newDashboardFilterLegacy(username, "filter 1", "testFilterString 1");
    assertThat(basedOnFilter.getRealmId()).isNull();
    tempEntity.newDashboardFilter(username, realmId, "", true /* acknowledged */, basedOnFilter.getName(),
        "testFilterString 2");

    basedOnFilter = dashboardFilterDAO.getById(basedOnFilter.getId());
    assertThat(basedOnFilter.getRealmId()).isEqualTo(realmId);
  }

  @Test
  public void testUpdate_BasedOnLegacyFilter() {
    String username = "testUsername";
    String realmId = "testRealmId";
    DashboardFilter basedOnFilter = tempEntity.newDashboardFilterLegacy(username, "filter 1", "testFilterString 1");
    assertThat(basedOnFilter.getRealmId()).isNull();
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter(username, realmId, "", "testFilterString 2");
    dashboardFilter.setBasedOnFilterName(basedOnFilter.getName());
    dashboardFilterDAO.update(dashboardFilter);

    basedOnFilter = dashboardFilterDAO.getById(basedOnFilter.getId());
    assertThat(basedOnFilter.getRealmId()).isEqualTo(realmId);
  }

  @Test
  public void testUpdate_FromLegacyFilter() {
    String username = "testUsername";
    String realmId = "testRealmId";
    String filterName = "testFilterName";
    DashboardFilter legacyDashboardFilter =
        tempEntity.newDashboardFilterLegacy(username, filterName, "testFilterString");
    assertThat(legacyDashboardFilter.getRealmId()).isNull();

    String newFilterContent = "new testFilterString";
    DashboardFilter dashboardFilter = new DashboardFilter(username, realmId, filterName);
    dashboardFilter.setId(legacyDashboardFilter.getId());
    dashboardFilter.setRealmId(realmId);
    dashboardFilter.setFilter(newFilterContent);
    dashboardFilterDAO.update(dashboardFilter);

    dashboardFilter = dashboardFilterDAO.getById(legacyDashboardFilter.getId());
    assertThat(dashboardFilter.getRealmId()).isEqualTo(realmId);
    assertThat(dashboardFilter.getFilter()).isEqualTo(newFilterContent);
  }

  @Test
  public void testInsert_RealmIdNull() {
    assertThatThrownBy(() -> tempEntity.newDashboardFilter("testUsername", null /* realmId */, "testFilterName",
        "testFilterString")).isInstanceOf(BadRequestException.class).hasMessage("The realm ID is required.");
  }

  @Test
  public void testInsert_RealmIdWhitespace() {
    assertThatThrownBy(() -> tempEntity.newDashboardFilter("testUsername", " " /* realmId */, "testFilterName",
        "testFilterString")).isInstanceOf(BadRequestException.class).hasMessage("The realm ID is required.");
  }

  @Test
  public void testUpdate_RealmIdNull() {
    DashboardFilter dashboardFilter =
        tempEntity.newDashboardFilter("testUsername", "testRealmId", "testFilterName", "testFilterString");

    dashboardFilter.setRealmId(null);
    assertThatThrownBy(() -> dashboardFilterDAO.update(dashboardFilter)).isInstanceOf(BadRequestException.class)
        .hasMessage("The realm ID is required.");
  }

  @Test
  public void testUpdate_RealmIdWhitespace() {
    DashboardFilter dashboardFilter =
        tempEntity.newDashboardFilter("testUsername", "testRealmId", "testFilterName", "testFilterString");

    dashboardFilter.setRealmId(" ");
    assertThatThrownBy(() -> dashboardFilterDAO.update(dashboardFilter)).isInstanceOf(BadRequestException.class)
        .hasMessage("The realm ID is required.");
  }

  private void assertFilter(DashboardFilter actualFilter, DashboardFilter expectedFilter) {
    assertThat(actualFilter).isNotNull();
    assertThat(actualFilter.getId()).isEqualTo(expectedFilter.getId());
    assertThat(actualFilter.getUsername()).isEqualTo(expectedFilter.getUsername());
    assertThat(actualFilter.getUsernameLowercase()).isEqualTo(expectedFilter.getUsernameLowercase());
    assertThat(actualFilter.getRealmId()).isEqualTo(expectedFilter.getRealmId());
    assertThat(actualFilter.getFilter()).isEqualTo(expectedFilter.getFilter());
    assertThat(actualFilter.getName()).isEqualTo(expectedFilter.getName());
    assertThat(actualFilter.getNameLowercaseNoWhitespace()).isEqualTo(expectedFilter.getNameLowercaseNoWhitespace());
  }
}
