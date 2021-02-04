/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static com.sonatype.insight.brain.model.filter.UserFilter.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserFilterServiceTest
    extends AbstractComponentTest
{
  @Inject
  private UserFilterService userFilterService;

  @Inject
  private UserFilterDAO userFilterDAO;

  @Test
  public void testCreateOrUpdateUserFilterForCurrentUser_Insert() {
    UserFilterDTO userFilterDTO = newUserFilterDTO("test filter name");
    UserFilter namedFilter = null;
    UserFilter activeFilter = null;

    try {
      userFilterService.createOrUpdateUserFilterForCurrentUser(userFilterDTO);

      // Get named filter
      namedFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(USERNAME, InternalRealm.ID,
          userFilterDTO.name, ADVANCED_LEGAL_PACK_DASHBOARD);
      assertFilter(namedFilter, userFilterDTO, false);

      // Get active filter
      activeFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME,
          ADVANCED_LEGAL_PACK_DASHBOARD);
      assertFilter(activeFilter, userFilterDTO, true);
    }
    finally {
      if (namedFilter != null) {
        userFilterDAO.delete(namedFilter);
      }
      if (activeFilter != null) {
        userFilterDAO.delete(activeFilter);
      }
    }
  }

  @Test
  public void testCreateOrUpdateUserFilterForCurrentUser_UpdateNamedFilter() {
    String filterName = "test filter";
    tempEntity.newUserFilter(USERNAME, InternalRealm.ID, filterName, ADVANCED_LEGAL_PACK_DASHBOARD);
    tempEntity.newUserFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD,
        "testFilter", filterName);
    UserFilterDTO userFilterDTO = newUserFilterDTO(filterName);

    userFilterService.createOrUpdateUserFilterForCurrentUser(userFilterDTO);

    UserFilter namedFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(USERNAME, InternalRealm.ID,
        userFilterDTO.name, ADVANCED_LEGAL_PACK_DASHBOARD);
    assertFilter(namedFilter, userFilterDTO, false);

    UserFilter activeFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(USERNAME, InternalRealm.ID,
        ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD);
    assertFilter(activeFilter, userFilterDTO, true);
  }

  @Test
  public void testCreateOrUpdateUserFilterForCurrentUser_UpdateActiveFilter() {
    String baseFilterName = "base";
    tempEntity.newUserFilter(USERNAME, InternalRealm.ID, baseFilterName, ADVANCED_LEGAL_PACK_DASHBOARD);
    tempEntity.newUserFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD);
    UserFilterDTO userFilterDTO = newUserFilterDTO(ACTIVE_FILTER_NAME);
    userFilterDTO.basedOnFilterName = baseFilterName;

    userFilterService.createOrUpdateUserFilterForCurrentUser(userFilterDTO);

    UserFilter userFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(USERNAME, InternalRealm.ID,
        userFilterDTO.name, ADVANCED_LEGAL_PACK_DASHBOARD);
    assertFilter(userFilter, userFilterDTO, false);
  }

  @Test
  public void testGetActiveUserFilterForCurrentUser() throws IOException {
    UserFilter userFilter = tempEntity.newUserFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME,
        ADVANCED_LEGAL_PACK_DASHBOARD, JsonUtils.format(new HashMap<>()));

    UserFilterDTO result = userFilterService.getActiveUserFilterForCurrentUser(ADVANCED_LEGAL_PACK_DASHBOARD);
    assertFilter(result, userFilter);
  }

  @Test
  public void testGetActiveUserFilterForCurrentUser_NonExisting() throws IOException {
    UserFilter userFilter =
        new UserFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD);

    UserFilterDTO result = userFilterService.getActiveUserFilterForCurrentUser(ADVANCED_LEGAL_PACK_DASHBOARD);
    assertFilter(result, userFilter);
  }

  @Test
  public void testGetNamedFiltersForCurrentUser() throws IOException {
    UserFilter filter1 = tempEntity.newUserFilter(USERNAME, InternalRealm.ID, "filter1", ADVANCED_LEGAL_PACK_DASHBOARD);
    UserFilter filter2 = tempEntity.newUserFilter(USERNAME, InternalRealm.ID, "filter2", ADVANCED_LEGAL_PACK_DASHBOARD);
    tempEntity.newUserFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD);

    List<UserFilterDTO> result = userFilterService.getNamedFiltersForCurrentUser(ADVANCED_LEGAL_PACK_DASHBOARD);
    assertThat(result).hasSize(2);
    assertFilter(result.get(0), filter1);
    assertFilter(result.get(1), filter2);
  }

  @Test
  public void deleteFilterForCurrentUserByNameAndType() {
    UserFilter filter1 = tempEntity.newUserFilter(USERNAME, InternalRealm.ID, "filter1", ADVANCED_LEGAL_PACK_DASHBOARD);
    UserFilter filter2 = tempEntity.newUserFilter(USERNAME, InternalRealm.ID, "filter2", ADVANCED_LEGAL_PACK_DASHBOARD);
    UserFilter activeFilter = tempEntity.newUserFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME,
        ADVANCED_LEGAL_PACK_DASHBOARD, filter1.getFilter(), filter1.getName());

    userFilterService.deleteFilterForCurrentUserByNameAndType(filter1.getName(), ADVANCED_LEGAL_PACK_DASHBOARD);
    assertThat(userFilterDAO.getById(filter1.getId())).isNull();
    assertThat(userFilterDAO.getById(filter2.getId())).isNotNull();
    assertThat(userFilterDAO.getById(activeFilter.getId()).getBasedOnFilterName()).isNull();
  }

  @Test
  public void deleteFilterForCurrentUserByNameAndType_NonExisting() {
    assertThatThrownBy(() ->
        userFilterService.deleteFilterForCurrentUserByNameAndType("fake", ADVANCED_LEGAL_PACK_DASHBOARD)
    ).isInstanceOf(NotFoundException.class).hasMessage(
        "Cannot find a filter with name fake and type ADVANCED_LEGAL_PACK_DASHBOARD for user " + USERNAME + ".");
  }

  private UserFilterDTO newUserFilterDTO(String filterName) {
    UserFilterDTO userFilterDTO = new UserFilterDTO();
    userFilterDTO.name = filterName;
    userFilterDTO.type = ADVANCED_LEGAL_PACK_DASHBOARD;
    userFilterDTO.filter = ImmutableMap.of("key1", "value 1", "key2", true, "key3", ImmutableMap.of("subKey1", 1));
    return userFilterDTO;
  }

  private void assertFilter(UserFilter actualFilter, UserFilterDTO expectedFilter, boolean isActiveFilter) {
    assertThat(actualFilter).isNotNull();
    assertThat(actualFilter.getRealmId()).isEqualTo(InternalRealm.ID);
    assertThat(actualFilter.getType()).isEqualTo(expectedFilter.type);
    assertThat(actualFilter.getUsername()).isEqualTo(USERNAME);
    assertThat(actualFilter.getFilter()).isEqualTo(JsonUtils.format(expectedFilter.filter));
    if (isActiveFilter) {
      assertThat(actualFilter.getName()).isEqualTo(ACTIVE_FILTER_NAME);
      assertThat(actualFilter.getBasedOnFilterName()).isEqualTo(expectedFilter.name);
    }
    else {
      assertThat(actualFilter.getName()).isEqualTo(expectedFilter.name);
      assertThat(actualFilter.getBasedOnFilterName()).isEqualTo(expectedFilter.basedOnFilterName);
    }
  }

  private void assertFilter(UserFilterDTO actualFilter, UserFilter expectedFilter) throws IOException {
    assertThat(actualFilter).isNotNull();
    assertThat(actualFilter.type).isEqualTo(expectedFilter.getType());
    if (expectedFilter.getFilter() == null) {
      assertThat(actualFilter.filter).isNull();
    }
    else {
      assertThat(actualFilter.filter).isEqualTo(JsonUtils.parse(expectedFilter.getFilter(), Map.class));
    }
    assertThat(actualFilter.name).isEqualTo(expectedFilter.getName());
    assertThat(actualFilter.basedOnFilterName).isEqualTo(expectedFilter.getBasedOnFilterName());
  }
}
