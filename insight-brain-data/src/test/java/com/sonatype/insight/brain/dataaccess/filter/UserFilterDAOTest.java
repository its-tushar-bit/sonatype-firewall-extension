/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.filter;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static com.sonatype.insight.brain.model.filter.UserFilter.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserFilterDAOTest
    extends AbstractDbDAOTest
{
  private final UserFilterDAO userFilterDAO = new UserFilterDAO();

  @Test
  public void testCRUD() {
    // Add filter
    UserFilter userFilter = tempEntity.newUserFilter("testUsername", "testRealmId", "testFilterName",
        UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "testFilterString");

    // Retrieve filter and test
    UserFilter returnedFilter = userFilterDAO.getById(userFilter.getId());
    assertFilter(returnedFilter, userFilter);

    // Update filter
    userFilter.setFilter("testFilterString updated");
    userFilterDAO.update(userFilter);

    // Retrieve filter and test
    returnedFilter = userFilterDAO.getById(userFilter.getId());
    assertFilter(returnedFilter, userFilter);

    // Delete
    userFilterDAO.delete(userFilter);

    // Retrieve filter and test
    assertThat(userFilterDAO.getById(userFilter.getId())).isNull();
  }

  @Test
  public void testInsert_RealmIdNull() {
    assertThatThrownBy(() ->
      tempEntity.newUserFilter("testUsername", null /* realmId */, "testFilterName", ADVANCED_LEGAL_PACK_DASHBOARD)
    ).isInstanceOf(BadRequestException.class).hasMessage("The realm ID is required.");
  }

  @Test
  public void testUpdate_RealmIdNull() {
    UserFilter userFilter =
        tempEntity.newUserFilter("testUsername", "testRealmId", "testFilterName", ADVANCED_LEGAL_PACK_DASHBOARD);

    userFilter.setRealmId(null);
    assertThatThrownBy(() ->
      userFilterDAO.update(userFilter)
    ).isInstanceOf(BadRequestException.class).hasMessage("The realm ID is required.");
  }

  @Test
  public void testInsert_RealmIdWhitespace() {
    assertThatThrownBy(() ->
      tempEntity.newUserFilter("testUsername", " " /* realmId */, "testFilterName", ADVANCED_LEGAL_PACK_DASHBOARD)
    ).isInstanceOf(BadRequestException.class).hasMessage("The realm ID is required.");
  }

  @Test
  public void testUpdate_RealmIdWhitespace() {
    UserFilter userFilter =
        tempEntity.newUserFilter("testUsername", "testRealmId", "testFilterName", ADVANCED_LEGAL_PACK_DASHBOARD);

    userFilter.setRealmId(" ");
    assertThatThrownBy(() ->
      userFilterDAO.update(userFilter)
    ).isInstanceOf(BadRequestException.class).hasMessage("The realm ID is required.");
  }

  @Test
  public void testInsert_TypeNull() {
    assertThatThrownBy(() ->
      tempEntity.newUserFilter("testUsername", "testRealmId", "testFilterName", null)
    ).isInstanceOf(BadRequestException.class).hasMessage("The type is required.");
  }

  @Test
  public void testUpdate_TypeNull() {
    UserFilter userFilter =
        tempEntity.newUserFilter("testUsername", "testRealmId", "testFilterName", ADVANCED_LEGAL_PACK_DASHBOARD);

    userFilter.setType(null);
    assertThatThrownBy(() ->
      userFilterDAO.update(userFilter)
    ).isInstanceOf(BadRequestException.class).hasMessage("The type is required.");
  }

  @Test
  public void testValidateNullName_Insert() {
    UserFilter userFilter =
        new UserFilter("testUsername", "testRealmId", null /* name */, ADVANCED_LEGAL_PACK_DASHBOARD);
    assertThatThrownBy(() ->
      userFilterDAO.insert(userFilter)
    ).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateNullName_Update() {
    UserFilter userFilter =
        tempEntity.newUserFilter("testUsername", "testRealmId", "testFilterName", ADVANCED_LEGAL_PACK_DASHBOARD);
    userFilter.setName(null);
    assertThatThrownBy(() ->
      userFilterDAO.update(userFilter)
    ).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateEmptyName_Insert() {
    UserFilter actualFilter = tempEntity.newUserFilter("testUsername", "testRealmId", ACTIVE_FILTER_NAME,
        ADVANCED_LEGAL_PACK_DASHBOARD, "testFilterString");
    UserFilter expectedFilter = userFilterDAO.getById(actualFilter.getId());
    assertFilter(actualFilter, expectedFilter);
  }

  @Test
  public void testValidateEmptyName_Update() {
    UserFilter userFilter = tempEntity.newUserFilter("testUsername", "testRealmId", "testFilterName",
        ADVANCED_LEGAL_PACK_DASHBOARD, "testFilterString");
    userFilter.setName(ACTIVE_FILTER_NAME);
    userFilterDAO.update(userFilter);
    UserFilter expectedFilter = userFilterDAO.getById(userFilter.getId());
    assertFilter(userFilter, expectedFilter);
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      UserFilter userFilter = new UserFilter("testUsername", "testRealmId", name, ADVANCED_LEGAL_PACK_DASHBOARD);
      assertThatThrownBy(() ->
        userFilterDAO.insert(userFilter)
      ).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    UserFilter userFilter =
        tempEntity.newUserFilter("testUsername", "testRealmId", "testFilterName", ADVANCED_LEGAL_PACK_DASHBOARD);
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      userFilter.setName(name);
      assertThatThrownBy(() ->
        userFilterDAO.update(userFilter)
      ).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testValidateNameValidChars_Insert() {
    for (String name : NameHelperTest.VALID_NAMES) {
      tempEntity.newUserFilter("testUsername", "testRealmId", name, ADVANCED_LEGAL_PACK_DASHBOARD);
    }
  }

  @Test
  public void testValidateNameValidChars_Update() {
    UserFilter userFilter =
        tempEntity.newUserFilter("testUsername", "testRealmId", ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD);
    for (String name : NameHelperTest.VALID_NAMES) {
      userFilter.setName(name);
      userFilterDAO.update(userFilter);
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      assertThatThrownBy(() -> 
        tempEntity.newUserFilter("testUsername", "testRealmId", name, ADVANCED_LEGAL_PACK_DASHBOARD)
      ).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testValidateNameSpaces_Update() {
    UserFilter userFilter =
        tempEntity.newUserFilter("testUsername", "testRealmId", "testFilterName", ADVANCED_LEGAL_PACK_DASHBOARD);
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      userFilter.setName(name);
      assertThatThrownBy(() -> 
        userFilterDAO.update(userFilter)
      ).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testDuplicateName_Insert() {
    tempEntity.newUserFilter("testUsername", "testRealmId", "testFilterName", ADVANCED_LEGAL_PACK_DASHBOARD);
    assertThatThrownBy(() ->
      tempEntity.newUserFilter("testUsername", "testRealmId", "testFilterName", ADVANCED_LEGAL_PACK_DASHBOARD)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("testFilterName is already used as a name for type ADVANCED_LEGAL_PACK_DASHBOARD");
  }

  @Test
  public void testDuplicateName_Update() {
    tempEntity.newUserFilter("testUsername", "testRealmId", "test Filter Name 1", ADVANCED_LEGAL_PACK_DASHBOARD);
    UserFilter userFilter =
        tempEntity.newUserFilter("testUsername", "testRealmId", "testFilterName2", ADVANCED_LEGAL_PACK_DASHBOARD);
    userFilter.setName("test Filter Name 1");
    assertThatThrownBy(() ->
      userFilterDAO.update(userFilter)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("test Filter Name 1 is already used as a name for type ADVANCED_LEGAL_PACK_DASHBOARD");
  }

  @Test
  public void testValidateNameLength_Insert() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH + 1);
    assertThatThrownBy(() ->
      tempEntity.newUserFilter("testUsername", "testRealmId", name, ADVANCED_LEGAL_PACK_DASHBOARD)
    ).isInstanceOf(InvalidNameException.class).hasMessage("Name must be 60 characters or less.");
  }

  @Test
  public void testValidateNameLength_Update() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH + 1);
    UserFilter userFilter =
        tempEntity.newUserFilter("testUsername", "testRealmId", "testFilterName", ADVANCED_LEGAL_PACK_DASHBOARD);
    userFilter.setName(name);
    assertThatThrownBy(() ->
      userFilterDAO.update(userFilter)
    ).isInstanceOf(InvalidNameException.class).hasMessage("Name must be 60 characters or less.");
  }

  @Test
  public void testValidate_insertNamedFilterBasedOnAnother() {
    UserFilter userFilter = new UserFilter("testUsername", "testRealmId", "valid name", ADVANCED_LEGAL_PACK_DASHBOARD);
    userFilter.setBasedOnFilterName("any non-null value");
    assertThatThrownBy(() ->
      userFilterDAO.insert(userFilter)
    ).isInstanceOf(BadRequestException.class).hasMessage("Only the active filter can be based on another filter.");
  }

  @Test
  public void testValidate_updateNamedFilterBasedOnAnother() {
    UserFilter userFilter =
        tempEntity.newUserFilter("testUsername", "testRealmId", "original filter name", ADVANCED_LEGAL_PACK_DASHBOARD);
    userFilter.setBasedOnFilterName("any non-null value");
    assertThatThrownBy(() ->
      userFilterDAO.update(userFilter)
    ).isInstanceOf(BadRequestException.class).hasMessage("Only the active filter can be based on another filter.");
  }

  @Test
  public void testValidate_insertActiveFilterBasedOnMissingFilter() {
    UserFilter userFilter = tempEntity.newUserFilter("testUsername", "testRealmId", ACTIVE_FILTER_NAME,
        ADVANCED_LEGAL_PACK_DASHBOARD, "testFilter", "based on non existing filter");

    UserFilter activeFilter = userFilterDAO.getById(userFilter.getId());
    assertThat(activeFilter.getBasedOnFilterName()).isNull();
  }

  @Test
  public void testValidate_updateActiveFilterBasedOnMissingFilter() {
    UserFilter userFilter = tempEntity.newUserFilter("testUsername", "testRealmId", ACTIVE_FILTER_NAME,
        ADVANCED_LEGAL_PACK_DASHBOARD, "testFilter", "based on non existing filter");
    userFilter.setBasedOnFilterName("valid name of a filter that does not exist");
    userFilterDAO.update(userFilter);

    UserFilter activeFilter = userFilterDAO.getById(userFilter.getId());
    assertThat(activeFilter.getBasedOnFilterName()).isNull();
  }

  @Test
  public void testValidate_insertActiveFilterBasedOnExistingFilter() {
    String filterName = "test filter name";
    tempEntity.newUserFilter("testUsername", "testRealmId", filterName, ADVANCED_LEGAL_PACK_DASHBOARD);
    UserFilter newUserFilter = tempEntity.newUserFilter("testUsername", "testRealmId", ACTIVE_FILTER_NAME,
        ADVANCED_LEGAL_PACK_DASHBOARD, "testFilterValue", filterName);

    UserFilter activeFilter = userFilterDAO.getById(newUserFilter.getId());
    assertThat(activeFilter.getBasedOnFilterName()).isEqualTo(filterName);
  }

  @Test
  public void testValidate_updateActiveFilterBasedOnExistingFilter() {
    String filterName = "test filter name";
    tempEntity.newUserFilter("testUsername", "testRealmId", filterName, ADVANCED_LEGAL_PACK_DASHBOARD);
    UserFilter newUserFilter =
        tempEntity.newUserFilter("testUsername", "testRealmId", ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD);

    newUserFilter.setBasedOnFilterName(filterName);
    userFilterDAO.update(newUserFilter);

    UserFilter activeFilter = userFilterDAO.getById(newUserFilter.getId());
    assertThat(activeFilter.getBasedOnFilterName()).isEqualTo(filterName);
  }

  @Test
  public void testDeleteByRealmId() {
    UserFilter userFilter1 = tempEntity.newUserFilter("testUsername", "testRealmId1", "testFilterName",
        ADVANCED_LEGAL_PACK_DASHBOARD, "testFilterString");
    UserFilter userFilter2 = tempEntity.newUserFilter("testUsername", "testRealmId2", "testFilterName",
        ADVANCED_LEGAL_PACK_DASHBOARD, "testFilterString");

    try (TransactionContext tx = userFilterDAO.createTransactionContext()) {
      tx.begin();
      userFilterDAO.deleteByRealmId(tx, "testRealmId1");
      tx.commit();
    }

    assertThat(userFilterDAO.getById(userFilter1.getId())).isNull();
    assertFilter(userFilterDAO.getById(userFilter2.getId()), userFilter2);
  }

  @Test
  public void testGetByUsernameAndRealmIdAndNameAndType() {
    String username = "testUsername";
    String realmId = "testRealmId";
    String filterName = "test filter name";
    UserFilter userFilter = tempEntity.newUserFilter(username, realmId, filterName, ADVANCED_LEGAL_PACK_DASHBOARD);
    tempEntity.newUserFilter(username, realmId, "some other name", ADVANCED_LEGAL_PACK_DASHBOARD);

    UserFilter actual = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(username, realmId, filterName,
        ADVANCED_LEGAL_PACK_DASHBOARD);
    assertFilter(actual, userFilter);
  }

  @Test
  public void testGetByUsernameAndRealmIdAndNameAndType_UsernameCaseInsensitive() {
    String username = "testUsername";
    String realmId = "testRealmId";
    String filterName = "test filter name";
    UserFilter userFilter = tempEntity.newUserFilter(username, realmId, filterName, ADVANCED_LEGAL_PACK_DASHBOARD);

    UserFilter actual = userFilterDAO.getByUsernameAndRealmIdAndNameAndType("TESTuserNAME", realmId, filterName,
        ADVANCED_LEGAL_PACK_DASHBOARD);
    assertFilter(actual, userFilter);
  }

  @Test
  public void testGetNamedFiltersByUsernameAndRealmIdAndType() {
    String username = "testUsername";
    String realmId = "testRealmId";
    tempEntity.newUserFilter(username, realmId, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD);
    UserFilter namedFilter =
        tempEntity.newUserFilter(username, realmId, "some other name", ADVANCED_LEGAL_PACK_DASHBOARD);

    List<UserFilter> result =
        userFilterDAO.getNamedFiltersByUsernameAndRealmIdAndType(username, realmId, ADVANCED_LEGAL_PACK_DASHBOARD);
    assertThat(result).hasSize(1);
    assertFilter(result.get(0), namedFilter);
  }

  @Test
  public void testGetNamedFiltersByUsernameAndRealmIdAndType_UsernameCaseInsensitive() {
    String realmId = "testRealmId";
    UserFilter namedFilter = tempEntity.newUserFilter("testUsername", realmId, "name", ADVANCED_LEGAL_PACK_DASHBOARD);

    List<UserFilter> result = userFilterDAO.getNamedFiltersByUsernameAndRealmIdAndType("TESTuserNAME", realmId,
        ADVANCED_LEGAL_PACK_DASHBOARD);
    assertThat(result).hasSize(1);
    assertFilter(result.get(0), namedFilter);
  }

  @Test
  public void testDeleteByUsernameAndRealmId() {
    UserFilter userFilter1 =
        tempEntity.newUserFilter("testUsername1", "testRealmId1", "testFilterName",
            UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "testFilterString");
    UserFilter userFilter2 =
        tempEntity.newUserFilter("testUsername2", "testRealmId2", "testFilterName",
            UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "testFilterString");

    try (TransactionContext tx = userFilterDAO.createTransactionContext()) {
      tx.begin();
      userFilterDAO.deleteByUsernameAndRealmId(tx, "testUsername1", "testRealmId1");
      tx.commit();
    }

    assertThat(userFilterDAO.getById(userFilter1.getId())).isNull();
    assertFilter(userFilterDAO.getById(userFilter2.getId()), userFilter2);
  }

  private void assertFilter(UserFilter actualFilter, UserFilter expectedFilter) {
    assertThat(actualFilter).isNotNull();
    assertThat(actualFilter).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(expectedFilter);
  }
}
