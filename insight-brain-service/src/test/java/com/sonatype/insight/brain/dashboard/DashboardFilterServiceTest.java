/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import com.google.common.collect.Lists;

import static com.sonatype.insight.brain.dashboard.DashboardFilterService.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class DashboardFilterServiceTest
    extends AbstractComponentTest
{
  private static final String FILTER_WITHOUT_MAX_DAYS_OLD_PATH =
      "/DashboardFilterServiceTest/DashboardFilterWithoutMaxDaysOld.json";

  private static final String FILTER_WITHOUT_POLICY_VIOLATION_STATES =
      "/DashboardFilterServiceTest/DashboardFilterWithoutPolicyViolationStatesProperty.json";

  @Inject
  private DashboardFilterDAO dashboardFilterDAO;
  
  @Inject
  private DashboardFilterService dashboardFilterService;

  @Inject
  private Configuration configuration;

  @Inject
  private ApiConfigurationService configurationService;

  @Inject
  private TestProductLicense testProductLicense;

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
  public void testCreateOrUpdateDashboardFilterForCurrentUser_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD, LicensedFeature.WAIVERS_DASHBOARD);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(null));
  }

  @Test
  public void testCreateOrUpdateDashboardFilterForCurrentUser_persistsPolicyWaiverReason() throws IOException {
    final var dashboardFilterDTO = new DashboardFilterDTO();
    dashboardFilterDTO.applicationFilters = Lists.newArrayList();
    dashboardFilterDTO.policyWaiverReasonIds = Lists.newArrayList("some-reason-id-1", "some-reason-id-2");

    final var namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    namedDashboardFilterDTO.name = "";
    namedDashboardFilterDTO.basedOnFilterName = "Filter 1";
    namedDashboardFilterDTO.filter = dashboardFilterDTO;

    final var result = dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(namedDashboardFilterDTO);

    assertThat(result.filter.policyWaiverReasonIds)
        .containsExactlyInAnyOrder("some-reason-id-1", "some-reason-id-2");

    final var fetchedResult = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertThat(fetchedResult.filter.policyWaiverReasonIds)
        .containsExactlyInAnyOrder("some-reason-id-1", "some-reason-id-2");
  }

  @Test
  public void testDeleteDashboardFiltersForCurrentUserByFilterName_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD, LicensedFeature.WAIVERS_DASHBOARD);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> dashboardFilterService.deleteDashboardFilterForCurrentUserByFilterName(null));
  }

  @Test
  public void testGetNamedDashboardFiltersForCurrentUser_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD, LicensedFeature.WAIVERS_DASHBOARD);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> dashboardFilterService.getNamedDashboardFiltersForCurrentUser());
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD, LicensedFeature.WAIVERS_DASHBOARD);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> dashboardFilterService.getActiveDashboardFilterForCurrentUser());
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUserWorksWhenMissingDashboardFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD);

    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertThat(actual).isNotNull();
    assertThat(actual.filter.minPolicyThreatLevel).isEqualTo(2);
    assertThat(actual.filter.maxPolicyThreatLevel).isEqualTo(10);
    assertThat(actual.filter.applicationFilters).isEmpty();
    assertThat(actual.filter.repositoryFilters).isEmpty();
    assertThat(actual.filter.tagFilters).isEmpty();
    assertThat(actual.filter.policyThreatCategoryFilters).isEmpty();
    assertThat(actual.filter.stageTypeFilters).isEmpty();
    assertThat(actual.filter.maxDaysOld).isEqualTo(DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD);
    assertThat(actual.filter.policyViolationStates).hasSize(1);
    assertThat(actual.filter.policyViolationStates.get(0))
        .isEqualTo(DashboardFilterDTO.DEFAULT_POLICY_VIOLATION_STATE.name());
    assertThat(actual.name).isEqualTo(ACTIVE_FILTER_NAME);
    assertThat(actual.basedOnFilterName).isNull();
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUserWorksWhenMissingWaiversDashboardFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.WAIVERS_DASHBOARD);

    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertThat(actual).isNotNull();
    assertThat(actual.filter.minPolicyThreatLevel).isEqualTo(2);
    assertThat(actual.filter.maxPolicyThreatLevel).isEqualTo(10);
    assertThat(actual.filter.applicationFilters).isEmpty();
    assertThat(actual.filter.repositoryFilters).isEmpty();
    assertThat(actual.filter.tagFilters).isEmpty();
    assertThat(actual.filter.policyThreatCategoryFilters).isEmpty();
    assertThat(actual.filter.stageTypeFilters).isEmpty();
    assertThat(actual.filter.maxDaysOld).isEqualTo(DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD);
    assertThat(actual.filter.policyViolationStates).hasSize(1);
    assertThat(actual.filter.policyViolationStates.get(0))
        .isEqualTo(DashboardFilterDTO.DEFAULT_POLICY_VIOLATION_STATE.name());
    assertThat(actual.name).isEqualTo(ACTIVE_FILTER_NAME);
    assertThat(actual.basedOnFilterName).isNull();
  }

  @Test
  public void testDashboardFilterDefaultFilter() throws Exception {
    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertThat(actual).isNotNull();
    assertThat(actual.filter.minPolicyThreatLevel).isEqualTo(2);
    assertThat(actual.filter.maxPolicyThreatLevel).isEqualTo(10);
    assertThat(actual.filter.applicationFilters).isEmpty();
    assertThat(actual.filter.repositoryFilters).isEmpty();
    assertThat(actual.filter.tagFilters).isEmpty();
    assertThat(actual.filter.policyThreatCategoryFilters).isEmpty();
    assertThat(actual.filter.stageTypeFilters).isEmpty();
    assertThat(actual.filter.maxDaysOld).isEqualTo(DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD);
    assertThat(actual.filter.policyViolationStates).hasSize(1);
    assertThat(actual.filter.policyViolationStates.get(0))
        .isEqualTo(DashboardFilterDTO.DEFAULT_POLICY_VIOLATION_STATE.name());
    assertThat(actual.name).isEqualTo(ACTIVE_FILTER_NAME);
    assertThat(actual.basedOnFilterName).isNull();
  }

  @Test
  public void testGetNamedDashboardFiltersForCurrentUser() throws IOException {
    String filterName1 = "Filter1";
    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO(filterName1, 2, 10);
    tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, filterName1, JsonUtils.format(dto1.filter));

    String filterName2 = "Filter2";
    NamedDashboardFilterDTO dto2 = createNamedDashboardFilterDTO(filterName2, 3, 9);
    tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, filterName2, JsonUtils.format(dto2.filter));

    String filterName3 = "Filter3";
    NamedDashboardFilterDTO dto3 = createNamedDashboardFilterDTO(filterName3, 1, 5);
    tempEntity.newDashboardFilterLegacy(USERNAME, filterName3, JsonUtils.format(dto3.filter));

    String filterName4 = ACTIVE_FILTER_NAME;
    NamedDashboardFilterDTO dto4 = createNamedDashboardFilterDTO(filterName4, 5, 9);
    tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, filterName4, JsonUtils.format(dto4.filter));

    List<NamedDashboardFilterDTO> actual = dashboardFilterService.getNamedDashboardFiltersForCurrentUser();
    assertThat(actual).hasSize(3);
    assertNamedDashboardFilterDTO(actual.get(0), filterName1, null /* basedOnFilterName */,
        false /* needsAcknowledgement */);
    assertFilterEmptyState(actual.get(0).filter, 2, 10);

    assertNamedDashboardFilterDTO(actual.get(1), filterName2, null /* basedOnFilterName */,
        false /* needsAcknowledgement */);
    assertFilterEmptyState(actual.get(1).filter, 3, 9);

    assertNamedDashboardFilterDTO(actual.get(2), filterName3, null /* basedOnFilterName */,
        false /* needsAcknowledgement */);
    assertFilterEmptyState(actual.get(2).filter, 1, 5);
  }

  @Test
  public void testGetNamedDashboardFilterForCurrentUser_DefaultMaxDaysOld() throws Exception {
    String filterName = "Filter1";

    String filterJsonWithoutMaxDaysOld = IOUtils.toString(getClass().getResource(FILTER_WITHOUT_MAX_DAYS_OLD_PATH),
        StandardCharsets.UTF_8);
    tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, filterName, filterJsonWithoutMaxDaysOld);

    List<NamedDashboardFilterDTO> actual = dashboardFilterService.getNamedDashboardFiltersForCurrentUser();
    assertThat(actual).hasSize(1);
    assertThat(actual.get(0).filter.maxDaysOld).isEqualTo(DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD);
  }

  @Test
  public void testGetNamedDashboardFilterForCurrentUser_DefaultPolicyViolationState() throws Exception {
    String filterName = "Filter1";

    String filterJsonWithoutPolicyViolationStates = IOUtils
        .toString(getClass().getResource(FILTER_WITHOUT_POLICY_VIOLATION_STATES), StandardCharsets.UTF_8);
    tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, filterName, filterJsonWithoutPolicyViolationStates);

    List<NamedDashboardFilterDTO> namedFilters = dashboardFilterService.getNamedDashboardFiltersForCurrentUser();
    assertThat(namedFilters).hasSize(1);

    NamedDashboardFilterDTO actual = namedFilters.get(0);

    assertThat(actual.filter.policyViolationStates).hasSize(1);
    assertThat(actual.filter.policyViolationStates.get(0))
        .isEqualTo(DashboardFilterDTO.DEFAULT_POLICY_VIOLATION_STATE.name());
  }

  @Test
  public void testGetNamedDashboardFilterForCurrentUser_DashboardFeatureDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> dashboardFilterService.getNamedDashboardFiltersForCurrentUser())
        .withMessage("The dashboard feature has been disabled.");
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

  private void testCreateOrUpdateDashboardFilterForCurrentUser_Update(
      boolean needsAcknowledgementOfInitialDashboardFilter) throws Exception
  {
    String filterName1 = "Filter1";
    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO(filterName1, 2, 10);
    DashboardFilter filter1 =
        tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, filterName1, JsonUtils.format(dto1.filter));
    assertThat(filter1.isAcknowledged()).isFalse();

    setNeedsAcknowledgementOfInitialDashboardFilter(needsAcknowledgementOfInitialDashboardFilter);

    NamedDashboardFilterDTO dto2 = createNamedDashboardFilterDTO(filterName1, 3, 9);
    //this should update the above filter
    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dto2);

    //verify that the filter above was updated successfully
    DashboardFilter actual = dashboardFilterDAO.getById(filter1.getId());
    DashboardFilterDTO actualDto = JsonUtils.parse(actual.getFilter(), DashboardFilterDTO.class);

    assertDashboardFilter(actual, filter1.getUsername(), InternalRealm.ID, filterName1,
        filter1.getNameLowercaseNoWhitespace(), null /* basedOnFilterName */,
        needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(actualDto, 3, 9);

    DashboardFilter activeFilter =
        dashboardFilterDAO.getByUsernameAndRealmIdAndName(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME);
    assertDashboardFilter(activeFilter, filter1.getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME /* name */,
        ACTIVE_FILTER_NAME /* nameLowercaseNoWhitespace */, "Filter1", needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(JsonUtils.parse(activeFilter.getFilter(), DashboardFilterDTO.class), 3, 9);

    // check that we can update the active filter
    NamedDashboardFilterDTO dto3 = createNamedDashboardFilterDTO(ACTIVE_FILTER_NAME, 7, 10);
    dto3.basedOnFilterName = filterName1;
    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dto3);

    activeFilter = dashboardFilterDAO.getByUsernameAndRealmIdAndName(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME);
    assertDashboardFilter(activeFilter, filter1.getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME /* name */,
        ACTIVE_FILTER_NAME /* nameLowercaseNoWhitespace */, "Filter1", needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(JsonUtils.parse(activeFilter.getFilter(), DashboardFilterDTO.class), 7, 10);
  }

  @Test
  public void testCreateOrUpdateDashboardFilterForCurrentUser_UpdateLegacyFilter() throws Exception {
    boolean needsAcknowledgementOfInitialDashboardFilter = false;
    String filterName = "Filter";
    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO(filterName, 2, 10);
    DashboardFilter filter = tempEntity.newDashboardFilterLegacy(USERNAME, filterName, JsonUtils.format(dto1.filter));
    assertThat(filter.getRealmId()).isNull();
    assertThat(filter.isAcknowledged()).isFalse();

    setNeedsAcknowledgementOfInitialDashboardFilter(needsAcknowledgementOfInitialDashboardFilter);

    NamedDashboardFilterDTO dto2 = createNamedDashboardFilterDTO(filterName, 3, 9);
    // this should update the above filter
    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dto2);

    // verify that the filter above was updated successfully
    DashboardFilter actual = dashboardFilterDAO.getById(filter.getId());
    DashboardFilterDTO actualDto = JsonUtils.parse(actual.getFilter(), DashboardFilterDTO.class);

    assertDashboardFilter(actual, filter.getUsername(), InternalRealm.ID, filterName,
        filter.getNameLowercaseNoWhitespace(), null /* basedOnFilterName */,
        needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(actualDto, 3, 9);

    DashboardFilter activeFilter =
        dashboardFilterDAO.getByUsernameAndRealmIdAndName(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME);
    assertDashboardFilter(activeFilter, filter.getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME /* name */,
        ACTIVE_FILTER_NAME /* nameLowercaseNoWhitespace */, filterName, needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(JsonUtils.parse(activeFilter.getFilter(), DashboardFilterDTO.class), 3, 9);

    // check that we can update the active filter
    NamedDashboardFilterDTO dto3 = createNamedDashboardFilterDTO(ACTIVE_FILTER_NAME, 7, 10);
    dto3.basedOnFilterName = filterName;
    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dto3);

    activeFilter = dashboardFilterDAO.getByUsernameAndRealmIdAndName(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME);
    assertDashboardFilter(activeFilter, filter.getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME /* name */,
        ACTIVE_FILTER_NAME /* nameLowercaseNoWhitespace */, filterName, needsAcknowledgementOfInitialDashboardFilter);
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

  private void testCreateOrUpdateDashboardFilterForCurrentUser_Insert(
      boolean needsAcknowledgementOfInitialDashboardFilter) throws Exception
  {
    setNeedsAcknowledgementOfInitialDashboardFilter(needsAcknowledgementOfInitialDashboardFilter);

    NamedDashboardFilterDTO dto1 = createNamedDashboardFilterDTO("Filter1", 2, 10);
    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dto1);

    List<DashboardFilter> actual = dashboardFilterDAO.getNamedFiltersByUsernameAndRealmId(USERNAME, InternalRealm.ID);
    assertThat(actual).hasSize(1);
    DashboardFilterDTO actualDto = JsonUtils.parse(actual.get(0).getFilter(), DashboardFilterDTO.class);
    assertDashboardFilter(actual.get(0), USERNAME, InternalRealm.ID, "Filter1", "filter1", null /* basedOnFilterName */,
        needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(actualDto, 2, 10);

    DashboardFilter activeFilter =
        dashboardFilterDAO.getByUsernameAndRealmIdAndName(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME);
    assertDashboardFilter(activeFilter, USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME, ACTIVE_FILTER_NAME, "Filter1",
        needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(JsonUtils.parse(activeFilter.getFilter(), DashboardFilterDTO.class), 2, 10);
  }

  @Test
  public void testCreateOrUpdateDashboardFilterForCurrentUser_DashboardFeatureDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");
    NamedDashboardFilterDTO namedDashboardFilterDTO = createNamedDashboardFilterDTO("Filter1", 2, 10);

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(namedDashboardFilterDTO))
        .withMessage("The dashboard feature has been disabled.");
  }

  private void assertFilterEmptyState(DashboardFilterDTO actualDto,
                                      int minPolicyThreatLevel,
                                      int maxPolicyThreatLevel)
  {
    assertThat(actualDto.minPolicyThreatLevel).isEqualTo(minPolicyThreatLevel);
    assertThat(actualDto.maxPolicyThreatLevel).isEqualTo(maxPolicyThreatLevel);
    assertThat(actualDto.applicationFilters).isEmpty();
    assertThat(actualDto.organizationFilters).isEmpty();
    assertThat(actualDto.repositoryFilters).isEmpty();
    assertThat(actualDto.tagFilters).isEmpty();
    assertThat(actualDto.policyThreatCategoryFilters).isEmpty();
    assertThat(actualDto.stageTypeFilters).isEmpty();
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

  private void testGetActiveDashboardFilterForCurrentUser_NewFilter(
      boolean needsAcknowledgementOfInitialDashboardFilter) throws Exception
  {
    setNeedsAcknowledgementOfInitialDashboardFilter(needsAcknowledgementOfInitialDashboardFilter);
    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertNamedDashboardFilterDTO(actual, ACTIVE_FILTER_NAME, null /* basedOnFilterName */,
        needsAcknowledgementOfInitialDashboardFilter);
    assertFilterEmptyState(actual.filter, 2, 10);

    assertThat(dashboardFilterDAO.getByUsernameAndRealmIdAndName(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME))
        .isNull();
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_ExistingFilter() throws Exception {
    NamedDashboardFilterDTO namedDashboardFilterDTO = createNamedDashboardFilterDTO(ACTIVE_FILTER_NAME, 5, 7);
    tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME, false /* acknowledged */, null,
        JsonUtils.format(namedDashboardFilterDTO.filter));

    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertNamedDashboardFilterDTO(actual, ACTIVE_FILTER_NAME, null /* basedOnFilterName */,
        false /* needsAcknowledgement */);
    assertFilterEmptyState(actual.filter, 5, 7);
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_ExistingLegacyFilter() throws Exception {
    NamedDashboardFilterDTO namedDashboardFilterDTO = createNamedDashboardFilterDTO(ACTIVE_FILTER_NAME, 5, 7);
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilterLegacy(USERNAME, ACTIVE_FILTER_NAME,
        JsonUtils.format(namedDashboardFilterDTO.filter));
    assertThat(dashboardFilter.getRealmId()).isNull();

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
    DashboardFilter activeFilter = tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME,
        false /* acknowledged */, null, JsonUtils.format(namedDashboardFilterDTO.filter));

    // Enable the needsAcknowledgementOfInitialDashboardFilter config option
    setNeedsAcknowledgementOfInitialDashboardFilter(true);

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
    tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, "My Filter",
        JsonUtils.format(namedDashboardFilterDTO.filter));
    activeFilter.setAcknowledged(false);
    activeFilter.setBasedOnFilterName("My Filter");
    dashboardFilterDAO.update(activeFilter);
    actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertNamedDashboardFilterDTO(actual, ACTIVE_FILTER_NAME, "My Filter", false /* needsAcknowledgement */);
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_DefaultMaxDaysOld() throws Exception {
    String filterJsonWithoutMaxDaysOld = IOUtils.toString(getClass().getResource(FILTER_WITHOUT_MAX_DAYS_OLD_PATH),
        StandardCharsets.UTF_8);
    tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME, filterJsonWithoutMaxDaysOld);

    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    assertThat(actual.filter.maxDaysOld).isEqualTo(DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD);
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_DefaultPolicyViolationState() throws Exception {
    String filterJsonWithoutPolicyViolationStates = IOUtils
        .toString(getClass().getResource(FILTER_WITHOUT_POLICY_VIOLATION_STATES), StandardCharsets.UTF_8);
    tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, ACTIVE_FILTER_NAME,
        filterJsonWithoutPolicyViolationStates);

    NamedDashboardFilterDTO actual = dashboardFilterService.getActiveDashboardFilterForCurrentUser();

    assertThat(actual.filter.policyViolationStates).hasSize(1);
    assertThat(actual.filter.policyViolationStates.get(0))
        .isEqualTo(DashboardFilterDTO.DEFAULT_POLICY_VIOLATION_STATE.name());
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_DashboardFeatureDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> dashboardFilterService.getActiveDashboardFilterForCurrentUser())
        .withMessage("The dashboard feature has been disabled.");
  }

  @Test
  public void testDeleteDashboardFilterForCurrentUserByFilterName() {
    String filterName = "Filter 1";

    NamedDashboardFilterDTO dto = createNamedDashboardFilterDTO(filterName, 5, 7);
    DashboardFilter dashboardFilter =
        tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, dto.name, JsonUtils.format(dto.filter));

    NamedDashboardFilterDTO activeDto = createNamedDashboardFilterDTO(ACTIVE_FILTER_NAME, 6, 7);
    DashboardFilter dashboardFilterActive = tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, activeDto.name,
        false, filterName, JsonUtils.format(activeDto.filter));

    dashboardFilterService.deleteDashboardFilterForCurrentUserByFilterName(filterName);

    assertThat(dashboardFilterDAO.getById(dashboardFilter.getId())).isNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilterActive.getId())).isNotNull();
  }

  @Test
  public void testDeleteDashboardFilterForCurrentUserByFilterName_LegacyFilter() {
    String filterName = "Filter 1";

    NamedDashboardFilterDTO dto = createNamedDashboardFilterDTO(filterName, 1, 5);
    DashboardFilter dashboardFilter =
        tempEntity.newDashboardFilterLegacy(USERNAME, dto.name, JsonUtils.format(dto.filter));

    NamedDashboardFilterDTO activeDto = createNamedDashboardFilterDTO(ACTIVE_FILTER_NAME, 6, 7);
    DashboardFilter dashboardFilterActive = tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, activeDto.name,
        false, filterName, JsonUtils.format(activeDto.filter));

    dashboardFilterService.deleteDashboardFilterForCurrentUserByFilterName(filterName);

    assertThat(dashboardFilterDAO.getById(dashboardFilter.getId())).isNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilterActive.getId())).isNotNull();
  }

  @Test
  public void testDeleteDashboardFilterForCurrentUserByFilterName_MissingFilter() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> dashboardFilterService.deleteDashboardFilterForCurrentUserByFilterName("Filter X"))
        .withMessage("Cannot find a filter with name Filter X for user testuser.");
  }

  @Test
  public void testDeleteDashboardFilterForCurrentUserByFilterName_HandlesDeleteFailure() {

    // creating filters
    String filterName = "Filter 1";
    NamedDashboardFilterDTO dto = createNamedDashboardFilterDTO(filterName, 5, 7);
    DashboardFilter dashboardFilter =
        tempEntity.newDashboardFilter(USERNAME, InternalRealm.ID, dto.name, JsonUtils.format(dto.filter));

    // spy
    DashboardFilterDAO dashboardFilterDaoSpy = Mockito.spy(dashboardFilterDAO);
    // mock
    CurrentUser currentUserMock = Mockito.mock(CurrentUser.class);
    DashboardUtils dashboardUtilsMock = Mockito.mock(DashboardUtils.class);

    DashboardFilterService dashboardFilterService = new DashboardFilterService(null, dashboardFilterDaoSpy,
        currentUserMock, dashboardUtilsMock, configuration);

    when(currentUserMock.getUsername()).thenReturn(USERNAME);
    when(currentUserMock.getRealmId()).thenReturn(InternalRealm.ID);
    doReturn(null).when(dashboardFilterDaoSpy).getByUsernameAndRealmIdAndName(USERNAME, InternalRealm.ID,
        ACTIVE_FILTER_NAME);
    doReturn(dashboardFilter).when(dashboardFilterDaoSpy).getByUsernameAndRealmIdAndName(USERNAME,
        InternalRealm.ID, filterName);
    doThrow(new RuntimeException("Something went wrong.")).when(dashboardFilterDaoSpy).delete(dashboardFilter);

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> dashboardFilterService.deleteDashboardFilterForCurrentUserByFilterName(filterName))
        .withMessage("Something went wrong.");

    // verify that Filter 1 is present
    assertThat(dashboardFilterDAO.getById(dashboardFilter.getId())).isNotNull();
  }

  @Test
  public void testDeleteDashboardFilterForCurrentUserByFilterName_ThrowsExceptionOnNullInput() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dashboardFilterService.deleteDashboardFilterForCurrentUserByFilterName(null))
        .withMessage("Filter name cannot be null.");
  }

  @Test
  public void testDeleteDashboardFilterForCurrentUserByFilterName_DashboardFeatureDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> dashboardFilterService.deleteDashboardFilterForCurrentUserByFilterName(null))
        .withMessage("The dashboard feature has been disabled.");
  }

  private NamedDashboardFilterDTO createNamedDashboardFilterDTO(String filterName,
                                                                int minPolicyThreatLevel,
                                                                int maxPolicyThreatLevel)
  {
    NamedDashboardFilterDTO dto = new NamedDashboardFilterDTO();
    DashboardFilterDTO filter = new DashboardFilterDTO();
    filter.applicationFilters = new ArrayList<>();
    filter.organizationFilters = new ArrayList<>();
    filter.repositoryFilters = new ArrayList<>();
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
    assertThat(actual.name).isEqualTo(name);
    assertThat(actual.basedOnFilterName).isEqualTo(basedOnFilterName);
    assertThat(actual.needsAcknowledgement).isEqualTo(needsAcknowledgement);
  }

  private void assertDashboardFilter(
      DashboardFilter actual,
      String username,
      String realmId,
      String name,
      String nameLowercaseNoWhitespace,
      String basedOnFilterName,
      boolean acknowledged)
  {
    assertThat(actual.getId()).isNotNull();
    assertThat(actual.getUsername()).isEqualTo(username);
    assertThat(actual.getRealmId()).isEqualTo(realmId);
    assertThat(actual.getName()).isEqualTo(name);
    assertThat(actual.getNameLowercaseNoWhitespace()).isEqualTo(nameLowercaseNoWhitespace);
    assertThat(actual.getBasedOnFilterName()).isEqualTo(basedOnFilterName);
    assertThat(actual.isAcknowledged()).isEqualTo(acknowledged);
  }

  private void setNeedsAcknowledgementOfInitialDashboardFilter(Boolean needsAcknowledgementOfInitialDashboardFilter) {
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER,
        needsAcknowledgementOfInitialDashboardFilter);
    configurationService.applyConfigurationToClients(
        SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER);
  }
}
