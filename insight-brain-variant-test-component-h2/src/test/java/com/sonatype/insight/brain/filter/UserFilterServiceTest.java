/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.filter.UserFilter.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ComponentH2Test
public class UserFilterServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private UserFilterService userFilterService;

  @Inject
  private UserFilterDAO userFilterDAO;

  @Test
  public void testCreateOrUpdateUserFilterForCurrentUser_Insert() {
    UserFilterDTO userFilterDTO = newALPDashboardUserFilter("test filter name");
    UserFilter namedFilter = null;
    UserFilter activeFilter = null;

    try {
      userFilterService.createOrUpdateUserFilterForCurrentUser(userFilterDTO);

      // Get named filter
      namedFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(USERNAME, InternalRealm.ID,
          userFilterDTO.getName(), ADVANCED_LEGAL_PACK_DASHBOARD);
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
    AdvancedLegalPackDashboardFilter advancedLegalPackDashboardFilter = new AdvancedLegalPackDashboardFilter();
    advancedLegalPackDashboardFilter.getApplicationFilters().add("appId1");
    advancedLegalPackDashboardFilter.getOrganizationFilters().add("orgId1");

    tempEntity.newUserFilter(USERNAME, InternalRealm.ID, filterName, ADVANCED_LEGAL_PACK_DASHBOARD, "");
    tempEntity.newUserFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD,
        JsonUtils.format(advancedLegalPackDashboardFilter), filterName);

    UserFilterDTO userFilterDTO = newALPDashboardUserFilter(filterName);

    userFilterService.createOrUpdateUserFilterForCurrentUser(userFilterDTO);

    UserFilter namedFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(USERNAME, InternalRealm.ID,
        userFilterDTO.getName(), ADVANCED_LEGAL_PACK_DASHBOARD);
    assertFilter(namedFilter, userFilterDTO, false);

    UserFilter activeFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(USERNAME, InternalRealm.ID,
        ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD);
    assertFilter(activeFilter, userFilterDTO, true);
  }

  @Test
  public void testCreateOrUpdateUserFilterForCurrentUser_UpdateActiveFilter() {
    String baseFilterName = "base";
    tempEntity.newUserFilter(USERNAME, InternalRealm.ID, baseFilterName, ADVANCED_LEGAL_PACK_DASHBOARD, "");
    tempEntity.newUserFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD, "");
    UserFilterDTO userFilterDTO = newALPDashboardUserFilter(ACTIVE_FILTER_NAME);
    userFilterDTO.setBasedOnFilterName(baseFilterName);

    userFilterService.createOrUpdateUserFilterForCurrentUser(userFilterDTO);

    UserFilter userFilter = userFilterDAO.getByUsernameAndRealmIdAndNameAndType(USERNAME, InternalRealm.ID,
        userFilterDTO.getName(), ADVANCED_LEGAL_PACK_DASHBOARD);
    assertFilter(userFilter, userFilterDTO, false);
  }

  @Test
  public void testGetActiveUserFilterForCurrentUser() {
    UserFilter userFilter = tempEntity.newUserFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME,
        ADVANCED_LEGAL_PACK_DASHBOARD, JsonUtils.format(new AdvancedLegalPackDashboardFilter()));

    UserFilterDTO result = userFilterService.getActiveUserFilterForCurrentUser(ADVANCED_LEGAL_PACK_DASHBOARD);
    assertFilter(result, userFilter);
  }

  @Test
  public void testGetActiveUserFilterForCurrentUser_NonExisting() {
    UserFilter userFilter =
        new UserFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD);

    UserFilterDTO result = userFilterService.getActiveUserFilterForCurrentUser(ADVANCED_LEGAL_PACK_DASHBOARD);
    assertFilter(result, userFilter);
  }

  @Test
  public void testGetNamedFiltersForCurrentUser() {
    String filterString = JsonUtils.format(new AdvancedLegalPackDashboardFilter());
    UserFilter filter1 = tempEntity.newUserFilter(
        USERNAME, InternalRealm.ID, "filter1", ADVANCED_LEGAL_PACK_DASHBOARD, filterString);
    UserFilter filter2 =
        tempEntity.newUserFilter(USERNAME, InternalRealm.ID, "filter2", ADVANCED_LEGAL_PACK_DASHBOARD, filterString);
    tempEntity
        .newUserFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD, filterString);

    List<UserFilterDTO> result = userFilterService.getNamedFiltersForCurrentUser(ADVANCED_LEGAL_PACK_DASHBOARD);
    assertThat(result).hasSize(2);
    assertFilter(result.get(0), filter1);
    assertFilter(result.get(1), filter2);
  }

  @Test
  public void deleteFilterForCurrentUserByNameAndType() {
    UserFilter filter1 =
        tempEntity.newUserFilter(USERNAME, InternalRealm.ID, "filter1", ADVANCED_LEGAL_PACK_DASHBOARD, "");
    UserFilter filter2 =
        tempEntity.newUserFilter(USERNAME, InternalRealm.ID, "filter2", ADVANCED_LEGAL_PACK_DASHBOARD, "");
    UserFilter activeFilter = tempEntity.newUserFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME,
        ADVANCED_LEGAL_PACK_DASHBOARD, filter1.getFilter(), filter1.getName());

    userFilterService.deleteFilterForCurrentUserByNameAndType(filter1.getName(), ADVANCED_LEGAL_PACK_DASHBOARD);
    assertThat(userFilterDAO.getById(filter1.getId())).isNull();
    assertThat(userFilterDAO.getById(filter2.getId())).isNotNull();
    assertThat(userFilterDAO.getById(activeFilter.getId()).getBasedOnFilterName()).isNull();
  }

  @Test
  public void deleteFilterForCurrentUserByNameAndType_NonExisting() {
    assertThatThrownBy(
        () -> userFilterService.deleteFilterForCurrentUserByNameAndType("fake", ADVANCED_LEGAL_PACK_DASHBOARD))
            .isInstanceOf(NotFoundException.class)
            .hasMessage(
                "Cannot find a filter with name fake and type ADVANCED_LEGAL_PACK_DASHBOARD for user " + USERNAME
                    + ".");
  }

  @Test
  public void pruneDeletedApplication() {
    AdvancedLegalPackDashboardFilter advancedLegalPackDashboardFilter = new AdvancedLegalPackDashboardFilter();
    advancedLegalPackDashboardFilter.getApplicationFilters().add("appId1");
    advancedLegalPackDashboardFilter.getApplicationFilters().add("appId2");
    advancedLegalPackDashboardFilter.getOrganizationFilters().add("orgId");

    tempEntity.newUserFilter(USERNAME, InternalRealm.ID, "filter", ADVANCED_LEGAL_PACK_DASHBOARD,
        JsonUtils.format(advancedLegalPackDashboardFilter));

    tempEntity.newOrganizationWithSpecificId("orgId");
    tempEntity.newApplicationWithSpecificId("appId1", "appId1", "appId1", "orgId");

    List<UserFilterDTO> result = userFilterService.getNamedFiltersForCurrentUser(ADVANCED_LEGAL_PACK_DASHBOARD);

    assertThat(result).hasSize(1);
    UserFilterDTO userFilterDTO = result.get(0);
    AdvancedLegalPackDashboardFilter userFilterResult = (AdvancedLegalPackDashboardFilter) userFilterDTO.getFilter();
    assertThat(userFilterResult.getApplicationFilters()).hasSize(1);
    assertThat(userFilterResult.getApplicationFilters().get(0)).isEqualTo("appId1");
    assertThat(userFilterResult.getOrganizationFilters().get(0)).isEqualTo("orgId");
  }

  private UserFilterDTO newALPDashboardUserFilter(String filterName) {
    AdvancedLegalPackDashboardFilter filter = new AdvancedLegalPackDashboardFilter();
    filter.getApplicationFilters().addAll(Lists.newArrayList("app1", "app2"));
    filter.getOrganizationFilters().add("org1");
    filter.getCategoryFilters().add("cat1");
    filter.getCategoryFilters().add("build");
    filter.getProgressOptionsFilters().add("complete");
    UserFilter userFilter = new UserFilter(null, null, filterName, ADVANCED_LEGAL_PACK_DASHBOARD);
    userFilter.setFilter(JsonUtils.format(filter));
    UserFilterDTO userFilterDTO = new UserFilterDTO(userFilter);
    return userFilterDTO;
  }

  private void assertFilter(UserFilter actualFilter, UserFilterDTO expectedFilter, boolean isActiveFilter) {
    assertThat(actualFilter).isNotNull();
    assertThat(actualFilter.getRealmId()).isEqualTo(InternalRealm.ID);
    // AdvancedLegalPackDashboardFilter expectedALPDashboardFilter =
    assertThat(actualFilter.getType()).isEqualTo(expectedFilter.getType());
    assertThat(actualFilter.getUsername()).isEqualTo(USERNAME);
    assertThat(actualFilter.getFilter()).isEqualTo(JsonUtils.format(expectedFilter.getFilter()));
    if (isActiveFilter) {
      assertThat(actualFilter.getName()).isEqualTo(ACTIVE_FILTER_NAME);
      assertThat(actualFilter.getBasedOnFilterName()).isEqualTo(expectedFilter.getName());
    }
    else {
      assertThat(actualFilter.getName()).isEqualTo(expectedFilter.getName());
      assertThat(actualFilter.getBasedOnFilterName()).isEqualTo(expectedFilter.getBasedOnFilterName());
    }
  }

  private void assertFilter(UserFilterDTO actualFilter, UserFilter expectedFilter) {
    assertThat(actualFilter).isNotNull();
    assertThat(actualFilter.getType()).isEqualTo(expectedFilter.getType());
    if (expectedFilter.getFilter() == null) {
      assertThat(actualFilter.getFilter()).isNull();
    }
    else {
      assertThat(actualFilter.getFilter()).isEqualTo(new UserFilterDTO(expectedFilter).getFilter());
    }
    assertThat(actualFilter.getName()).isEqualTo(expectedFilter.getName());
    assertThat(actualFilter.getBasedOnFilterName()).isEqualTo(expectedFilter.getBasedOnFilterName());
  }
}
