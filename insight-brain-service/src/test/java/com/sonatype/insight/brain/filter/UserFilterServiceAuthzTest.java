/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class UserFilterServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private UserFilterService userFilterService;

  @Test
  public void testGetNamedFiltersForCurrentUser_UnauthorizedApps() throws Exception {
    Application app2 = tempEntity.newApplication(org.getId());

    UserFilterDTO userFilterDTO = createNamedFilterDTO("abcd", app2.getId(), app.getId());
    login();

    userFilterService.createOrUpdateUserFilterForCurrentUser(userFilterDTO);

    grantReadPermission(app.getId());

    List<UserFilterDTO> actual =
        userFilterService.getNamedFiltersForCurrentUser(UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD);

    assertAdvancedLegalApplicationFilters(actual.get(0), app.getId());

    grantReadPermission(app2.getId());
    actual = userFilterService.getNamedFiltersForCurrentUser(UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD);
    assertAdvancedLegalApplicationFilters(actual.get(0), app.getId(), app2.getId());
  }

  @Test
  public void testGetActiveFilterForCurrentUser_UnauthorizedApps() throws Exception {
    Application app2 = tempEntity.newApplication(org.getId());
    UserFilterDTO userFilterDTO = createNamedFilterDTO("", app2.getId(), app.getId());

    login();

    userFilterService.createOrUpdateUserFilterForCurrentUser(userFilterDTO);

    grantReadPermission(app.getId());
    UserFilterDTO actual =
        userFilterService.getActiveUserFilterForCurrentUser(UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD);
    assertAdvancedLegalApplicationFilters(actual, app.getId());

    grantReadPermission(app2.getId());
    actual = userFilterService.getActiveUserFilterForCurrentUser(UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD);
    assertAdvancedLegalApplicationFilters(actual, app.getId(), app2.getId());
  }

  private UserFilterDTO createNamedFilterDTO(final String filterName, final String... applicationIds) {
    AdvancedLegalPackDashboardFilter advancedLegalPackDashboardFilter = new AdvancedLegalPackDashboardFilter();
    advancedLegalPackDashboardFilter.getApplicationFilters().addAll(Arrays.asList(applicationIds));
    UserFilterDTO userFilterDTO =
        new UserFilterDTO(filterName, null, ADVANCED_LEGAL_PACK_DASHBOARD, advancedLegalPackDashboardFilter);
    return userFilterDTO;
  }

  private void assertAdvancedLegalApplicationFilters(
      final UserFilterDTO userFilterDTO, final String... applicationIds)
  {
    AdvancedLegalPackDashboardFilter userALPDashboardFilter =
        (AdvancedLegalPackDashboardFilter) userFilterDTO.getFilter();
    assertThat(userALPDashboardFilter.getApplicationFilters()).containsExactlyInAnyOrder(applicationIds);
  }
}
