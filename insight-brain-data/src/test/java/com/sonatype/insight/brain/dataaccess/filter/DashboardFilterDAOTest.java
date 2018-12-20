/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.filter;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DashboardFilterDAOTest
    extends AbstractDbDAOTest
{
  private final DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
  
  @Test
  public void testCRUD() {
    String username = "test123";
    // Add filter
    String filterName = "TestFilterName";
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter(username, filterName, "testFilterString");

    // Retrieve filter and test
    DashboardFilter returnedFilter = dashboardFilterDAO.getByUsernameAndName(username, filterName);
    assertFilter(returnedFilter, dashboardFilter);

    // Update filter
    String username2 = "bob";
    dashboardFilter.setUsername(username2);
    dashboardFilterDAO.update(dashboardFilter);

    // Retrieve filter and test
    returnedFilter = dashboardFilterDAO.getByUsernameAndName(username2, filterName);
    assertFilter(returnedFilter, dashboardFilter);

    // Delete
    dashboardFilterDAO.delete(dashboardFilter);

    // Retrieve filter and test
    assertThat(dashboardFilterDAO.getByUsername(username2)).isEmpty();
  }

  @Test
  public void testGetByUsername() {
    String username = "test123";
    DashboardFilter dashboardFilter1 = tempEntity.newDashboardFilter(username, "filter 1", "testFilterString 1");
    DashboardFilter dashboardFilter2 = tempEntity.newDashboardFilter(username, "", "testFilterString 2");
    tempEntity.newDashboardFilter("admin123", "filter 2", "testFilterString 2");

    // Retrieve filters and test
    List<DashboardFilter> actual = dashboardFilterDAO.getByUsername(username);
    assertThat(actual).hasSize(2);
    assertFilter(actual.get(0), dashboardFilter2);
    assertFilter(actual.get(1), dashboardFilter1);
  }

  @Test
  public void testGetNamedFiltersByUsername() {
    String username = "test123";
    DashboardFilter dashboardFilter1 = tempEntity.newDashboardFilter(username, "filter 1", "testFilterString 1");
    DashboardFilter dashboardFilter2 = tempEntity.newDashboardFilter(username, "filter 2", "testFilterString 2");
    tempEntity.newDashboardFilter(username, "", "testFilterString 2");
    tempEntity.newDashboardFilter("admin123", "filter 2", "testFilterString 2");

    // Retrieve filters and test
    List<DashboardFilter> actual = dashboardFilterDAO.getNamedFiltersByUsername(username);
    assertThat(actual).hasSize(2);
    assertFilter(actual.get(0), dashboardFilter1);
    assertFilter(actual.get(1), dashboardFilter2);
  }

  @Test
  public void testGetByUsernameAndName() {
    String username = "test123";
    String filterName = "Abc filter";
    DashboardFilter dashboardFilter1 = tempEntity.newDashboardFilter(username, filterName, "testFilterString 1");
    tempEntity.newDashboardFilter(username, "Xyz Filter", "testFilterString 1");

    // Retrieve filter and test
    DashboardFilter actual = dashboardFilterDAO.getByUsernameAndName(username, filterName);
    assertFilter(actual, dashboardFilter1);
  }

  @Test
  public void testValidateNullName_Insert() {
    DashboardFilter dashboardFilter = new DashboardFilter(null);
    assertThatThrownBy(() -> {
      dashboardFilterDAO.insert(dashboardFilter);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateNullName_Update() {
    DashboardFilter dashboardFilter = new DashboardFilter(null);
    assertThatThrownBy(() -> {
      dashboardFilterDAO.update(dashboardFilter);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateEmptyName_Insert() {
    String username = "test123";
    // this should create a new filter
    DashboardFilter actualFilter = tempEntity.newDashboardFilter(username, "", "testFilterString 1");
    // search for the new filter
    DashboardFilter expectedFilter = dashboardFilterDAO.getByUsernameAndName(username, "");
    // verify
    assertFilter(actualFilter, expectedFilter);
  }

  @Test
  public void testValidateEmptyName_Update() {
    String username = "test123";
    // this should create a new filter
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter(username, "abc filter", "testFilterString 1");
    dashboardFilter.setName("");
    // update the new filter
    dashboardFilterDAO.update(dashboardFilter);
    // search for the updated filter
    DashboardFilter expectedFilter = dashboardFilterDAO.getByUsernameAndName(username, "");
    assertFilter(dashboardFilter, expectedFilter);
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      DashboardFilter dashboardFilter = new DashboardFilter(name);
      assertThatThrownBy(() -> {
        dashboardFilterDAO.insert(dashboardFilter);
      }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    String username = "test123";
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter(username, "test 1", "testFilterString 1");
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      dashboardFilter.setName(name);
      assertThatThrownBy(() -> {
        dashboardFilterDAO.update(dashboardFilter);
      }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testValidateNameValidChars_Insert() {
    String username = "test123";
    for (String name : NameHelperTest.VALID_NAMES) {
      tempEntity.newDashboardFilter(username, name, "testFilterString 1");
    }
  }

  @Test
  public void testValidateNameValidChars_Update() {
    String username = "test123";
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter(username, "", "testFilterString 1");
    for (String name : NameHelperTest.VALID_NAMES) {
      dashboardFilter.setName(name);
      dashboardFilterDAO.update(dashboardFilter);
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    String username = "test123";
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      assertThatThrownBy(() -> {
        tempEntity.newDashboardFilter(username, name, "testFilterString 1");
      }).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testValidateNameSpaces_Update() {
    String username = "test123";
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter(username, "sample filter", "testFilterString 1111");
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      dashboardFilter.setName(name);
      assertThatThrownBy(() -> {
        dashboardFilterDAO.update(dashboardFilter);
      }).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String username = "test123";
    String name = "test string With Case and Whitespace";
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter(username, name, "testFilterString 1111");
    assertThat(dashboardFilter.getName()).isEqualTo(name);
    assertThat(dashboardFilter.getNameLowercaseNoWhitespace()).isEqualTo("teststringwithcaseandwhitespace");

    String name1 = "TEST String      With    cASE and      whitespace";
    DashboardFilter actual = dashboardFilterDAO.getByUsernameAndName(username, name1);
    assertThat(actual).isNotNull();
    assertThat(actual.getId()).isEqualTo(dashboardFilter.getId());
  }
  
  @Test
  public void testDuplicateName_Insert() {
    String username = "test123";
    tempEntity.newDashboardFilter(username, "Filter12345", "testFilterString 1111");
    assertThatThrownBy(() -> {
      tempEntity.newDashboardFilter(username, "FILTER 12345", "testFilterString 1111");
    }).isInstanceOf(BadRequestException.class).hasMessage("FILTER 12345 is already used as a name.");
  }

  @Test
  public void testDuplicateName_Update() {
    String username = "test0123";
    tempEntity.newDashboardFilter(username, "Filter12345", "testFilterString 1111");
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter(username, "Filter 0123", "testFilterString 1111");
    dashboardFilter.setName("FILTER 12345");
    assertThatThrownBy(() -> {
      dashboardFilterDAO.update(dashboardFilter);
    }).isInstanceOf(BadRequestException.class).hasMessage("FILTER 12345 is already used as a name.");
  }

  @Test
  public void testValidateNameLength_Insert() {
    String username = "test123";
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    assertThatThrownBy(() -> {
      tempEntity.newDashboardFilter(username, name + "a", "testFilterString 1111");
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name must be 60 characters or less.");
    tempEntity.newDashboardFilter(username, name, "testFilterString 1111");
  }

  @Test
  public void testValidateNameLength_Update() {
    String username = "test123";
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter(username, "valid name", "testFilterString 1111");
    dashboardFilter.setName(name + "a");
    assertThatThrownBy(() -> {
      dashboardFilterDAO.update(dashboardFilter);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name must be 60 characters or less.");
    dashboardFilter.setName(name);
    dashboardFilterDAO.update(dashboardFilter);
  }

  @Test
  public void testValidate_insertNamedFilterBasedOnAnother() {
    DashboardFilter dashboardFilter = new DashboardFilter("valid name");
    dashboardFilter.setBasedOnFilterName("any non-null value");
    assertThatThrownBy(() -> {
      dashboardFilterDAO.insert(dashboardFilter);
    }).isInstanceOf(BadRequestException.class).hasMessage("Only the active filter can be based on another filter.");
  }

  @Test
  public void testValidate_insertActiveFilterBasedOnMissingFilter() {
    DashboardFilter dashboardFilter = new DashboardFilter("");
    dashboardFilter.setUsername("test user");
    dashboardFilter.setBasedOnFilterName("valid name of a filter that does not exist");
    dashboardFilter.setFilter("some filter string");
    dashboardFilterDAO.insert(dashboardFilter);

    DashboardFilter activeFilter = dashboardFilterDAO.getByUsernameAndName("test user", "");
    assertThat(activeFilter.getBasedOnFilterName()).isNull();
  }

  @Test
  public void testValidate_updateNamedFilterBasedOnAnother() {
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter("test user", "valid name", "originalFilter");
    dashboardFilter.setFilter("updatedFilter");
    dashboardFilter.setBasedOnFilterName("any non-null value");
    assertThatThrownBy(() -> {
      dashboardFilterDAO.update(dashboardFilter);
    }).isInstanceOf(BadRequestException.class).hasMessage("Only the active filter can be based on another filter.");
  }

  @Test
  public void testValidate_updateActiveFilterBasedOnMissingFilter() {
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter("another test user", "", "originalFilter");
    dashboardFilter.setFilter("updatedFilter");
    dashboardFilter.setBasedOnFilterName("valid name of a filter that does not exist");
    dashboardFilterDAO.update(dashboardFilter);

    DashboardFilter activeFilter = dashboardFilterDAO.getByUsernameAndName("test user", "");
    assertThat(activeFilter.getBasedOnFilterName()).isNull();
  }

  private void assertFilter(DashboardFilter actualFilter, DashboardFilter expectedFilter) {
    assertThat(actualFilter).isNotNull();
    assertThat(actualFilter.getId()).isEqualTo(expectedFilter.getId());
    assertThat(actualFilter.getUsername()).isEqualTo(expectedFilter.getUsername());
    assertThat(actualFilter.getFilter()).isEqualTo(expectedFilter.getFilter());
    assertThat(actualFilter.getName()).isEqualTo(expectedFilter.getName());
    assertThat(actualFilter.getNameLowercaseNoWhitespace()).isEqualTo(expectedFilter.getNameLowercaseNoWhitespace());
  }
}
