/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.dashboard.DashboardFilterService.ACTIVE_FILTER_NAME;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class DashboardFilterServiceTest
    extends AbstractComponentTest
{
  private static final String FILTER_WITHOUT_MAX_DAYS_OLD_PATH =
      "/DashboardFilterServiceTest/DashboardFilterWithoutMaxDaysOld.json";

  private static final String FILTER_WITHOUT_POLICY_VIOLATION_STATES =
      "/DashboardFilterServiceTest/DashboardFilterWithoutPolicyViolationStatesProperty.json";

  private final DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();

  @Inject
  private DashboardFilterService dashboardFilterService;

  @Inject
  private InsightConfig insightConfig;

  private Organization org;
  private Application app1;
  private Application app2;
  private Policy orgPolicy;
  private Policy app1Policy;
  private PolicyEvaluation app1PolicyEvaluation;
  private PolicyEvaluation app2PolicyEvaluation;
  private Tag tag1;
  private Tag tag2;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("app1", "app1", org.getId());
    app2 = tempEntity.newApplication("app2", "app2", org.getId());
    tempEntity.newPolicy(org.getParentOrganizationId(), "root org owned policy", 4);
    orgPolicy = tempEntity.newPolicy(org.getId(), "org owned policy", 3);
    app1Policy = tempEntity.newPolicy(app1.getId(), "app owned policy", 5);
    long time = System.currentTimeMillis() - 1000;
    app1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id",
        new Date(time));
    app2PolicyEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan app2 id",
        new Date(time + 1));
    tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy);
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-3", MatchState.SIMILAR, false);
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-4", MatchState.UNKNOWN, false);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"));
    tag1 = tempEntity.newTag(org.getId());
    tag2 = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app1.getId(), tag1.getId());
    tempEntity.newApplicationTag(app1.getId(), tag2.getId());
    tempEntity.newUser(USERNAME);
  }

  @Test
  public void testDashboardFilterDefaultFilter() throws Exception {
    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertThat(actual, notNullValue());
    assertThat(actual.filter.minPolicyThreatLevel, is(2));
    assertThat(actual.filter.maxPolicyThreatLevel, is(10));
    assertThat(actual.filter.applicationFilters, hasSize(0));
    assertThat(actual.filter.tagFilters, hasSize(0));
    assertThat(actual.filter.policyThreatCategoryFilters, hasSize(0));
    assertThat(actual.filter.stageTypeFilters, hasSize(0));
    assertThat(actual.filter.maxDaysOld, is(DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD));
    assertThat(actual.filter.policyViolationStates, hasSize(1));
    assertThat(actual.filter.policyViolationStates.get(0), is(DashboardFilterDTO.DEFAULT_POLICY_VIOLATION_STATE.name()));
    assertThat(actual.name, is(ACTIVE_FILTER_NAME));
    assertThat(actual.basedOnFilterName, is(nullValue()));
  }

  @Test
  public void testGetNamedDashboardFiltersForCurrentUser() throws IOException {
    String filterName1 = "Filter1";
    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO(filterName1, 2, 10);
    tempEntity.newDashboardFilter(USERNAME, filterName1, JsonUtils.format(dto1.filter));

    String filterName2 = "Filter2";
    NamedDashboardFilterDTO dto2 = createNamedDashboardFilterDTO(filterName2, 3, 9);
    tempEntity.newDashboardFilter(USERNAME, filterName2, JsonUtils.format(dto2.filter));

    String filterName3 = ACTIVE_FILTER_NAME;
    NamedDashboardFilterDTO dto3 = createNamedDashboardFilterDTO(filterName3, 5, 9);
    tempEntity.newDashboardFilter(USERNAME, filterName3, JsonUtils.format(dto3.filter));

    List<NamedDashboardFilterDTO> actual = dashboardFilterService.getNamedDashboardFiltersForCurrentUser();
    assertThat(actual, hasSize(2));
    assertNamedDashboardFilterDTO(actual.get(0), filterName1, null /* basedOnFilterName */,
        false /* needsAcknowledgement */);
    assertFilterEmptyState(actual.get(0).filter, 2, 10);

    assertNamedDashboardFilterDTO(actual.get(1), filterName2, null /* basedOnFilterName */,
        false /* needsAcknowledgement */);
    assertFilterEmptyState(actual.get(1).filter, 3, 9);
  }

  @Test
  public void testGetNamedDashboardFilterForCurrentUser_DefaultMaxDaysOld() throws Exception {
    String filterName = "Filter1";

    String filterJsonWithoutMaxDaysOld = IOUtils.toString(getClass().getResource(FILTER_WITHOUT_MAX_DAYS_OLD_PATH),
        "UTF-8");
    tempEntity.newDashboardFilter(USERNAME, filterName, filterJsonWithoutMaxDaysOld);

    List<NamedDashboardFilterDTO> actual = dashboardFilterService.getNamedDashboardFiltersForCurrentUser();
    assertThat(actual, hasSize(1));
    assertThat(actual.get(0).filter.maxDaysOld, is(DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD));
  }

  @Test
  public void testGetNamedDashboardFilterForCurrentUser_DefaultPolicyViolationState() throws Exception {
    String filterName = "Filter1";

    String filterJsonWithoutPolicyViolationStates = IOUtils
        .toString(getClass().getResource(FILTER_WITHOUT_POLICY_VIOLATION_STATES), "UTF-8");
    tempEntity.newDashboardFilter(USERNAME, filterName, filterJsonWithoutPolicyViolationStates);

    List<NamedDashboardFilterDTO> namedFilters = dashboardFilterService.getNamedDashboardFiltersForCurrentUser();
    assertThat(namedFilters, hasSize(1));

    NamedDashboardFilterDTO actual = namedFilters.get(0);

    assertThat(actual.filter.policyViolationStates, hasSize(1));
    assertThat(actual.filter.policyViolationStates.get(0),
        is(DashboardFilterDTO.DEFAULT_POLICY_VIOLATION_STATE.name()));
  }

  @Test
  public void testCreateOrUpdateDashboardFilterForCurrentUser_Update() throws Exception {
    testCreateOrUpdateDashboardFilterForCurrentUser_Update(false);
  }

  @Test
  public void testCreateOrUpdateDashboardFilterForCurrentUser_Update_NeedsAcknowledgementOfInitialDashboardFilter()
      throws Exception
  {
    testCreateOrUpdateDashboardFilterForCurrentUser_Update(true);
  }

  private void testCreateOrUpdateDashboardFilterForCurrentUser_Update(boolean needsAcknowledgementOfInitialDashboardFilter)
      throws Exception
  {
    String filterName1 = "Filter1";
    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO(filterName1, 2, 10);
    DashboardFilter filter1 = tempEntity.newDashboardFilter(USERNAME, filterName1, JsonUtils.format(dto1.filter));
    assertThat(filter1.isAcknowledged(), is(false));

    insightConfig.setNeedsAcknowledgementOfInitialDashboardFilter(needsAcknowledgementOfInitialDashboardFilter);

    NamedDashboardFilterDTO dto2 = createNamedDashboardFilterDTO(filterName1, 3, 9);
    //this should update the above filter
    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dto2);

    //verify that the filter above was updated successfully
    DashboardFilter actual = dashboardFilterDAO.getById(filter1.getId());
    DashboardFilterDTO actualDto = JsonUtils.parse(actual.getFilter(), DashboardFilterDTO.class);

    assertDashboardFilter(actual, filter1.getUsername(), filterName1, filter1.getNameLowercaseNoWhitespace(),
        null /* basedOnFilterName */, needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(actualDto, 3, 9);

    DashboardFilter activeFilter = dashboardFilterDAO.getByUsernameAndName(USERNAME, ACTIVE_FILTER_NAME);
    assertDashboardFilter(activeFilter, filter1.getUsername(), ACTIVE_FILTER_NAME /* name */,
        ACTIVE_FILTER_NAME /* nameLowercaseNoWhitespace */, "Filter1", needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(JsonUtils.parse(activeFilter.getFilter(), DashboardFilterDTO.class), 3, 9);

    // check that we can update the active filter
    NamedDashboardFilterDTO dto3 = createNamedDashboardFilterDTO(ACTIVE_FILTER_NAME, 7, 10);
    dto3.basedOnFilterName = filterName1;
    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dto3);

    activeFilter = dashboardFilterDAO.getByUsernameAndName(USERNAME, ACTIVE_FILTER_NAME);
    assertDashboardFilter(activeFilter, filter1.getUsername(), ACTIVE_FILTER_NAME /* name */,
        ACTIVE_FILTER_NAME /* nameLowercaseNoWhitespace */, "Filter1", needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(JsonUtils.parse(activeFilter.getFilter(), DashboardFilterDTO.class), 7, 10);
  }

  @Test
  public void testCreateOrUpdateDashboardFilterForCurrentUser_Insert() throws Exception {
    testCreateOrUpdateDashboardFilterForCurrentUser_Insert(false);
  }

  @Test
  public void testCreateOrUpdateDashboardFilterForCurrentUser_Insert_NeedsAcknowledgementOfInitialDashboardFilter()
      throws Exception
  {
    testCreateOrUpdateDashboardFilterForCurrentUser_Insert(true);
  }

  private void testCreateOrUpdateDashboardFilterForCurrentUser_Insert(boolean needsAcknowledgementOfInitialDashboardFilter)
      throws Exception
  {
    insightConfig.setNeedsAcknowledgementOfInitialDashboardFilter(needsAcknowledgementOfInitialDashboardFilter);

    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO("Filter1", 2, 10);
    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dto1);

    List<DashboardFilter> actual = dashboardFilterDAO.getNamedFiltersByUsername(USERNAME);
    assertThat(actual, hasSize(1));
    DashboardFilterDTO actualDto = JsonUtils.parse(actual.get(0).getFilter(), DashboardFilterDTO.class);
    assertDashboardFilter(actual.get(0), USERNAME, "Filter1", "filter1", null /* basedOnFilterName */,
        needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(actualDto, 2, 10);

    DashboardFilter activeFilter = dashboardFilterDAO.getByUsernameAndName(USERNAME, ACTIVE_FILTER_NAME);
    assertDashboardFilter(activeFilter, USERNAME, ACTIVE_FILTER_NAME, ACTIVE_FILTER_NAME, "Filter1",
        needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(JsonUtils.parse(activeFilter.getFilter(), DashboardFilterDTO.class), 2, 10);
  }

  private void assertFilterEmptyState(DashboardFilterDTO actualDto,
                                      int minPolicyThreatLevel,
                                      int maxPolicyThreatLevel)
  {
    assertThat(actualDto.minPolicyThreatLevel, is(minPolicyThreatLevel));
    assertThat(actualDto.maxPolicyThreatLevel, is(maxPolicyThreatLevel));
    assertThat(actualDto.applicationFilters, empty());
    assertThat(actualDto.organizationFilters, empty());
    assertThat(actualDto.tagFilters, empty());
    assertThat(actualDto.policyThreatCategoryFilters, empty());
    assertThat(actualDto.stageTypeFilters, empty());
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_NewFilter() throws Exception {
    testGetActiveDashboardFilterForCurrentUser_NewFilter(false);
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_NewFilter_NeedsAcknowledgementOfInitialDashboardFilter()
      throws Exception
  {
    testGetActiveDashboardFilterForCurrentUser_NewFilter(true);
  }

  private void testGetActiveDashboardFilterForCurrentUser_NewFilter(boolean needsAcknowledgementOfInitialDashboardFilter)
      throws Exception
  {
    insightConfig.setNeedsAcknowledgementOfInitialDashboardFilter(needsAcknowledgementOfInitialDashboardFilter);
    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertNamedDashboardFilterDTO(actual, ACTIVE_FILTER_NAME, null /* basedOnFilterName */,
        needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(actual.filter, 2, 10);

    assertThat(dashboardFilterDAO.getByUsernameAndName(USERNAME, ACTIVE_FILTER_NAME), is(nullValue()));
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_ExistingFilter() throws Exception {
    NamedDashboardFilterDTO namedDashboardFilterDTO = createNamedDashboardFilterDTO(ACTIVE_FILTER_NAME, 5, 7);
    tempEntity.newDashboardFilter(USERNAME, ACTIVE_FILTER_NAME, false /* acknowledged */, null,
        JsonUtils.format(namedDashboardFilterDTO.filter));

    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertNamedDashboardFilterDTO(actual, ACTIVE_FILTER_NAME, null /* basedOnFilterName */,
        false /* needsAcknowledgement */);
    assertFilterEmptyState(actual.filter, 5, 7);
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_ExistingFilter_NeedsAcknowledgementOfInitialDashboardFilter()
      throws Exception
  {
    // Create an unnamed active filter that was not acknowledged by the user.
    NamedDashboardFilterDTO namedDashboardFilterDTO = createNamedDashboardFilterDTO(ACTIVE_FILTER_NAME, 5, 7);
    DashboardFilter activeFilter = tempEntity.newDashboardFilter(USERNAME, ACTIVE_FILTER_NAME, false /* acknowledged */,
        null, JsonUtils.format(namedDashboardFilterDTO.filter));

    // Enable the needsAcknowledgementOfInitialDashboardFilter config option
    insightConfig.setNeedsAcknowledgementOfInitialDashboardFilter(true);

    // The existing filter must be marked as needing acknowledgement.
    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertNamedDashboardFilterDTO(actual, ACTIVE_FILTER_NAME, null /* basedOnFilterName */,
        true /* needsAcknowledgement */);
    assertFilterEmptyState(actual.filter, 5, 7);

    // Update the active filter to mark it as acknowledged.
    // It should not need acknowledgement again.
    activeFilter.setAcknowledged(true);
    dashboardFilterDAO.update(activeFilter);
    actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertNamedDashboardFilterDTO(actual, ACTIVE_FILTER_NAME, null /* basedOnFilterName */,
        false /* needsAcknowledgement */);

    // Update the active filter to mark it as not acknowledged, but based on a named filter.
    // It should not need acknowledgement.
    tempEntity.newDashboardFilter(USERNAME, "My Filter", JsonUtils.format(namedDashboardFilterDTO.filter));
    activeFilter.setAcknowledged(false);
    activeFilter.setBasedOnFilterName("My Filter");
    dashboardFilterDAO.update(activeFilter);
    actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertNamedDashboardFilterDTO(actual, ACTIVE_FILTER_NAME, "My Filter", false /* needsAcknowledgement */);
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_DefaultMaxDaysOld() throws Exception {
    String filterJsonWithoutMaxDaysOld = IOUtils.toString(getClass().getResource(FILTER_WITHOUT_MAX_DAYS_OLD_PATH),
        "UTF-8");
    tempEntity.newDashboardFilter(USERNAME, ACTIVE_FILTER_NAME, filterJsonWithoutMaxDaysOld);

    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertThat(actual.filter.maxDaysOld, is(DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD));
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_DefaultPolicyViolationState() throws Exception {
    String filterJsonWithoutPolicyViolationStates = IOUtils
        .toString(getClass().getResource(FILTER_WITHOUT_POLICY_VIOLATION_STATES), "UTF-8");
    tempEntity.newDashboardFilter(USERNAME, ACTIVE_FILTER_NAME, filterJsonWithoutPolicyViolationStates);

    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();

    assertThat(actual.filter.policyViolationStates, hasSize(1));
    assertThat(actual.filter.policyViolationStates.get(0),
        is(DashboardFilterDTO.DEFAULT_POLICY_VIOLATION_STATE.name()));
  }

  @Test
  public void testDeleteDashboardFiltersForCurrentUserByFilterName() {
    String filterName1 = "Filter 1";
    String filterName2 = "Filter 2";

    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO(filterName1, 5, 7);
    tempEntity.newDashboardFilter(USERNAME, dto1.name, JsonUtils.format(dto1.filter));

    NamedDashboardFilterDTO dto2 = createNamedDashboardFilterDTO(filterName2, 4, 8);
    tempEntity.newDashboardFilter(USERNAME, dto2.name, JsonUtils.format(dto2.filter));

    NamedDashboardFilterDTO activeDto = createNamedDashboardFilterDTO(ACTIVE_FILTER_NAME, 6, 7);
    tempEntity.newDashboardFilter(USERNAME, activeDto.name, false, filterName1, JsonUtils.format(activeDto.filter));

    List<String> filtersToDelete = Arrays.asList(filterName1, filterName2);

    dashboardFilterService.deleteDashboardFiltersForCurrentUserByFilterName(filtersToDelete);

    List<DashboardFilter> actual = dashboardFilterDAO.getNamedFiltersByUsername(USERNAME);
    assertThat(actual, hasSize(0));
    DashboardFilter activeFilter = dashboardFilterDAO.getByUsernameAndName(USERNAME, ACTIVE_FILTER_NAME);
    assertThat(activeFilter.getBasedOnFilterName(), nullValue());
  }

  @Test
  public void testDeleteDashboardFiltersForCurrentUserByFilterName_DeletesFilterWhenOneMissing() {
    String filterName1 = "Filter X";
    String filterName2 = "Filter Y";
    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO(filterName2, 5, 7);
    tempEntity.newDashboardFilter(USERNAME, dto1.name, JsonUtils.format(dto1.filter));

    List<String> filtersToDelete = Arrays.asList(filterName1, filterName2);

    List<DashboardFilterErrorResponseDTO> actualErrors = dashboardFilterService
        .deleteDashboardFiltersForCurrentUserByFilterName(filtersToDelete);
    // verify that Filter X failed
    assertThat(actualErrors, hasSize(1));
    assertThat(actualErrors.get(0).name, is(filterName1));
    assertThat(actualErrors.get(0).errorMessage,
        is("Cannot find a filter with name " + filterName1 + " for user " + USERNAME + "."));
    assertThat(actualErrors.get(0).status, is(404));

    // verify that Filter Y got deleted
    List<DashboardFilter> actualFilters = dashboardFilterDAO.getNamedFiltersByUsername(USERNAME);
    assertThat(actualFilters, hasSize(0));
  }

  @Test
  public void testDeleteDashboardFiltersForCurrentUserByFilterName_DeletesFilterWhenOneFails() {
    // creating filters
    String filterName1 = "Filter 1";
    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO(filterName1, 5, 7);
    DashboardFilter dashboardFilter1 = tempEntity
        .newDashboardFilter(USERNAME, dto1.name, JsonUtils.format(dto1.filter));

    String filterName2 = "Filter 2";
    NamedDashboardFilterDTO dto2 = createNamedDashboardFilterDTO(filterName2, 5, 9);
    tempEntity.newDashboardFilter(USERNAME, dto2.name, JsonUtils.format(dto2.filter));

    // spy
    DashboardFilterDAO dashboardFilterDao = dashboardFilterDAO;
    DashboardFilterDAO dashboardFilterDaoSpy = Mockito.spy(dashboardFilterDao);
    // mock
    CurrentUser currentUserMock = Mockito.mock(CurrentUser.class);
    DashboardUtils dashboardUtilsMock = Mockito.mock(DashboardUtils.class);

    DashboardFilterService dashboardFilterService = new DashboardFilterService(null, dashboardFilterDaoSpy,
        currentUserMock, dashboardUtilsMock, insightConfig);

    when(currentUserMock.getUsername()).thenReturn(USERNAME);
    when(dashboardFilterDaoSpy.getByUsernameAndName(USERNAME, filterName1)).thenReturn(dashboardFilter1);
    doThrow(new RuntimeException("Something went wrong.")).when(dashboardFilterDaoSpy).delete(dashboardFilter1);

    List<String> filtersToDelete = Arrays.asList(filterName1, filterName2);

    List<DashboardFilterErrorResponseDTO> actualErrors = dashboardFilterService
        .deleteDashboardFiltersForCurrentUserByFilterName(filtersToDelete);
    // verify that Filter 1 failed
    assertThat(actualErrors, hasSize(1));
    assertThat(actualErrors.get(0).name, is(filterName1));
    assertThat(actualErrors.get(0).errorMessage,
        is("An exception occurred while trying to find or delete filter name " + filterName1 + " for user " + USERNAME + "."));
    assertThat(actualErrors.get(0).status, is(500));

    // verify that Filter 1 is present and Filter 2 got deleted
    List<DashboardFilter> actualFilters = dashboardFilterDao.getNamedFiltersByUsername(USERNAME);
    assertThat(actualFilters, hasSize(1));
    assertDashboardFilter(actualFilters.get(0), USERNAME, "Filter 1", "filter1", null, false);
  }

  @Test
  public void testDeleteDashboardFiltersForCurrentUserByFilterName_ThrowsExceptionOnNullInput()
  {
    try {
      dashboardFilterService.deleteDashboardFiltersForCurrentUserByFilterName(null);
      fail("Expected exception to be thrown.");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("Filter names cannot be null or empty."));
    }
  }

  @Test
  public void testDeleteDashboardFiltersForCurrentUserByFilterName_ThrowsExceptionOnEmptyInput()
  {
    try {
      dashboardFilterService.deleteDashboardFiltersForCurrentUserByFilterName(new ArrayList<String>());
      fail("Expected exception to be thrown.");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("Filter names cannot be null or empty."));
    }
  }

  private NamedDashboardFilterDTO createNamedDashboardFilterDTO(String filterName,
                                                                int minPolicyThreatLevel,
                                                                int maxPolicyThreatLevel)
  {
    NamedDashboardFilterDTO dto = new NamedDashboardFilterDTO();
    DashboardFilterDTO filter = new DashboardFilterDTO();
    filter.applicationFilters = new ArrayList<>();
    filter.organizationFilters = new ArrayList<>();
    filter.minPolicyThreatLevel = minPolicyThreatLevel;
    filter.maxPolicyThreatLevel = maxPolicyThreatLevel;
    filter.stageTypeFilters = new ArrayList<>();
    filter.policyThreatCategoryFilters = new ArrayList<>();
    filter.tagFilters = new ArrayList<>();
    dto.name = filterName;
    dto.filter = filter;
    return dto;
  }

  private void assertNamedDashboardFilterDTO(NamedDashboardFilterDTO actual,
                                             String name,
                                             String basedOnFilterName,
                                             boolean needsAcknowledgement)
  {
    assertThat(actual.name, is(name));
    assertThat(actual.basedOnFilterName, is(basedOnFilterName));
    assertThat(actual.needsAcknowledgement, is(needsAcknowledgement));
  }

  private void assertDashboardFilter(DashboardFilter actual,
                                     String username,
                                     String name,
                                     String nameLowercaseNoWhitespace,
                                     String basedOnFilterName,
                                     boolean acknowledged)
  {
    assertThat(actual.getId(), is(notNullValue()));
    assertThat(actual.getUsername(), is(username));
    assertThat(actual.getName(), is(name));
    assertThat(actual.getNameLowercaseNoWhitespace(), is(nameLowercaseNoWhitespace));
    assertThat(actual.getBasedOnFilterName(), is(basedOnFilterName));
    assertThat(actual.isAcknowledged(), is(acknowledged));
  }
}
