/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.service.PolicyViolationTestHelper;
import com.sonatype.insight.brain.dataaccess.policy.AutoUnquarantinePolicyConditionTypeDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.AutoReleaseQuarantineTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.api.v2.ApiFirewallService.AUTO_RELEASE_QUARANTINE_CONFIG_TELEMETRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ApiFirewallServiceTest
    extends AbstractComponentTest
{
  @Inject
  ApiFirewallService apiFirewallService;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private TelemetrySender telemetrySenderMock;

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  private final AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO =
      new AutoUnquarantinePolicyConditionTypeDAO();

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }

  @After
  public void cleanUp() {
    policyMonitoringDAO.getAll().forEach(policyMonitoringDAO::delete);
    autoUnquarantinePolicyConditionTypeDAO.getAll().forEach(autoUnquarantinePolicyConditionTypeDAO::delete);
  }

  @Test
  public void testGetFirewallReleaseQuarantineSummary() {
    Repository repository = tempEntity.newRepository();
    // create repository components in various state of quarantine
    final Date mtdDate = new Date();
    final Date ytdDate = DateUtils.setMonths(new Date(), 0);
    final Date previousYearDate = DateUtils.addYears(new Date(), -1);
    tempEntity.newRepositoryComponent(repository.getId(), "/audit", null, null);
    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined", mtdDate, null);
    tempEntity.newRepositoryComponent(repository.getId(), "/autoUnquarantinedMtd", mtdDate, mtdDate, true);
    tempEntity.newRepositoryComponent(repository.getId(), "/manualUnquarantinedMtd", mtdDate, mtdDate, false);
    tempEntity.newRepositoryComponent(repository.getId(), "/autoUnquarantinedYtd", ytdDate, ytdDate, true);
    tempEntity.newRepositoryComponent(repository.getId(), "/manualUnquarantinedYtd", ytdDate, ytdDate, false);
    tempEntity
        .newRepositoryComponent(repository.getId(), "/autoUnquarantinedPrevious", previousYearDate, previousYearDate,
            true);
    tempEntity
        .newRepositoryComponent(repository.getId(), "/manualUnquarantinedPrevious", previousYearDate, previousYearDate,
            false);

    //when: retrieving release quarantine summary
    ApiFirewallReleaseQuarantineSummaryDTO releaseQuarantineSummary = apiFirewallService.getReleaseQuarantineSummary();

    //then: expect to get a valid pojo back
    // if MTD and YTD dates are the same day it's January, and MTD counts will equal YTD counts
    if (DateUtils.isSameDay(mtdDate, ytdDate)) {
      assertThat(releaseQuarantineSummary.autoReleaseQuarantineCountMTD).isEqualTo(2);
    }
    else {
      assertThat(releaseQuarantineSummary.autoReleaseQuarantineCountMTD).isOne();
    }
    assertThat(releaseQuarantineSummary.autoReleaseQuarantineCountYTD).isEqualTo(2);
  }

  @Test
  public void testGetFirewallReleaseQuarantineSummary_NoFirewallAutoUnquarantineFeature() {
    //setup: remove firewall feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    //when: getting release quarantine summary
    //then: expect invalid license exception
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        apiFirewallService.getReleaseQuarantineSummary());
  }

  @Test
  public void testGetFirewallReleaseQuarantineSummary_NoReleaseIntegrityFeature() {
    //setup: remove release integrity feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    //when: getting release quarantine summary
    //then: expect invalid license exception
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        apiFirewallService.getReleaseQuarantineSummary());
  }

  @Test
  public void testGetFirewallReleaseQuarantineConfig() {
    tempEntity.newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);
    tempEntity.newAutoUnquarantinePolicyConditionType(LicenseConditionType.ID);

    final String[] autoUnquarantinedConditionTypes = ConditionTypes.getAllWithAutoUnquarantineSupported().stream()
        .map(ConditionType::getName)
        .toArray(String[]::new);

    //when: getting release quarantine config
    //then: expect the auto unquarantined enabled policy condition types with the enabled flag set
    Map<String, ApiFirewallReleaseQuarantineConfigDTO> releaseQuarantineConfig =
        apiFirewallService.getReleaseQuarantineConfig().stream()
            .collect(Collectors.toMap(dto -> dto.id, Function.identity()));
    long numTrue = releaseQuarantineConfig.values().stream().filter(dto -> dto.autoReleaseQuarantineEnabled).count();

    assertThat(releaseQuarantineConfig.values()).extracting("name")
        .containsOnly(autoUnquarantinedConditionTypes);
    // Ensures correct number of falses
    assertThat(numTrue).isEqualTo(2);
    assertThat(releaseQuarantineConfig.get(IntegrityRatingConditionType.ID).autoReleaseQuarantineEnabled).isTrue();
    assertThat(releaseQuarantineConfig.get(LicenseConditionType.ID).autoReleaseQuarantineEnabled).isTrue();
  }

  @Test
  public void testGetFirewallReleaseQuarantineConfig_NoFirewallAutoUnquarantineFeature() {
    //setup: remove firewall feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    //when: getting release quarantine config
    //then: expect invalid license exception
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        apiFirewallService.getReleaseQuarantineConfig());
  }

  @Test
  public void testGetFirewallReleaseQuarantineConfig_NoReleaseIntegrityFeature() {
    //setup: remove release integrity feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    //when: getting release quarantine config
    //then: expect invalid license exception
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        apiFirewallService.getReleaseQuarantineConfig());
  }

  @Test
  public void testSetFirewallReleaseQuarantineConfig_DoesNotChangeUnspecified() {
    tempEntity.newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);
    tempEntity.newAutoUnquarantinePolicyConditionType(LicenseConditionType.ID);

    List<ApiFirewallReleaseQuarantineConfigDTO> list = new ArrayList<>();
    ApiFirewallReleaseQuarantineConfigDTO licence = new ApiFirewallReleaseQuarantineConfigDTO();
    licence.id = LicenseConditionType.ID;
    licence.autoReleaseQuarantineEnabled = false;
    list.add(licence);
    ApiFirewallReleaseQuarantineConfigDTO security = new ApiFirewallReleaseQuarantineConfigDTO();
    security.id = SecurityVulnerabilityCategoryConditionType.ID;
    security.autoReleaseQuarantineEnabled = true;
    list.add(security);

    final String[] autoUnquarantinedConditionTypes = ConditionTypes.getAllWithAutoUnquarantineSupported().stream()
        .map(ConditionType::getName)
        .toArray(String[]::new);

    //when: setting release quarantine config

    Map<String, ApiFirewallReleaseQuarantineConfigDTO> releaseQuarantineConfig =
        apiFirewallService.setReleaseQuarantineConfig(list).stream()
            .collect(Collectors.toMap(dto -> dto.id, Function.identity()));

    //then: expect the auto unquarantined enabled policy condition types with the enabled flag set
    //condition types explicitly set to false should be false
    //condition types explicitly set to true should be true
    //condition types not explicitly set should remain as they were
    long numTrue = releaseQuarantineConfig.values().stream().filter(dto -> dto.autoReleaseQuarantineEnabled).count();

    assertThat(releaseQuarantineConfig.values()).extracting("name")
        .containsOnly(autoUnquarantinedConditionTypes);
    // Ensures correct number of falses
    assertThat(numTrue).isEqualTo(2);
    assertThat(releaseQuarantineConfig.get(SecurityVulnerabilityCategoryConditionType.ID).autoReleaseQuarantineEnabled)
        .isTrue();
    assertThat(releaseQuarantineConfig.get(IntegrityRatingConditionType.ID).autoReleaseQuarantineEnabled).isTrue();
  }

  @Test
  public void testSetFirewallReleaseQuarantineConfig() {
    tempEntity.newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);
    tempEntity.newAutoUnquarantinePolicyConditionType(LicenseConditionType.ID);

    List<ApiFirewallReleaseQuarantineConfigDTO> list = new ArrayList<>();
    ApiFirewallReleaseQuarantineConfigDTO integrity = new ApiFirewallReleaseQuarantineConfigDTO();
    integrity.id = IntegrityRatingConditionType.ID;
    integrity.autoReleaseQuarantineEnabled = true;
    list.add(integrity);
    ApiFirewallReleaseQuarantineConfigDTO licence = new ApiFirewallReleaseQuarantineConfigDTO();
    licence.id = LicenseConditionType.ID;
    licence.autoReleaseQuarantineEnabled = false;
    list.add(licence);
    ApiFirewallReleaseQuarantineConfigDTO security = new ApiFirewallReleaseQuarantineConfigDTO();
    security.id = SecurityVulnerabilityCategoryConditionType.ID;
    security.autoReleaseQuarantineEnabled = true;
    list.add(security);
    ApiFirewallReleaseQuarantineConfigDTO licenceThreatGroup = new ApiFirewallReleaseQuarantineConfigDTO();
    licenceThreatGroup.id = LicenseThreatGroupConditionType.ID;
    licenceThreatGroup.autoReleaseQuarantineEnabled = false;
    list.add(licenceThreatGroup);

    final String[] autoUnquarantinedConditionTypes = ConditionTypes.getAllWithAutoUnquarantineSupported().stream()
        .map(ConditionType::getName)
        .toArray(String[]::new);

    //when: setting release quarantine config

    Map<String, ApiFirewallReleaseQuarantineConfigDTO> releaseQuarantineConfig =
        apiFirewallService.setReleaseQuarantineConfig(list).stream()
            .collect(Collectors.toMap(dto -> dto.id, Function.identity()));

    //then: expect the auto unquarantined enabled policy condition types with the enabled flag set
    //condition types explicitly set to false should be false
    //condition types explicitly set to true should be true
    //false condition types set to false should remain false
    //true condition types set to true should remain true
    //condition types not explicitly set should remain as they were
    long numTrue = releaseQuarantineConfig.values().stream().filter(dto -> dto.autoReleaseQuarantineEnabled).count();

    assertThat(releaseQuarantineConfig.values()).extracting("name")
        .containsOnly(autoUnquarantinedConditionTypes);
    // Ensures correct number of falses
    assertThat(numTrue).isEqualTo(2);
    assertThat(releaseQuarantineConfig.get(SecurityVulnerabilityCategoryConditionType.ID).autoReleaseQuarantineEnabled)
        .isTrue();
    assertThat(releaseQuarantineConfig.get(IntegrityRatingConditionType.ID).autoReleaseQuarantineEnabled).isTrue();
  }

  @Test
  public void testSetFirewallReleaseQuarantineConfig_IdNotSpecified() {
    List<ApiFirewallReleaseQuarantineConfigDTO> list = new ArrayList<>();
    ApiFirewallReleaseQuarantineConfigDTO condition = new ApiFirewallReleaseQuarantineConfigDTO();
    condition.autoReleaseQuarantineEnabled = true;
    list.add(condition);

    //when: setting release quarantine config, expect exception
    assertThatThrownBy(() -> apiFirewallService.setReleaseQuarantineConfig(list))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Some Policy Condition Types do not have ID's specified.");
  }

  @Test
  public void testSetFirewallReleaseQuarantineConfig_FlagNotSpecified() {
    List<ApiFirewallReleaseQuarantineConfigDTO> list = new ArrayList<>();
    ApiFirewallReleaseQuarantineConfigDTO condition = new ApiFirewallReleaseQuarantineConfigDTO();
    condition.id = IntegrityRatingConditionType.ID;
    list.add(condition);

    //when: setting release quarantine config, expect exception
    assertThatThrownBy(() -> apiFirewallService.setReleaseQuarantineConfig(list))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Policy Condition Type with id 'IntegrityRating' does not have the enabled flag specified.");
  }

  @Test
  public void testSetFirewallReleaseQuarantineConfig_IdAndFlagNotSpecified() {
    List<ApiFirewallReleaseQuarantineConfigDTO> list = new ArrayList<>();
    ApiFirewallReleaseQuarantineConfigDTO condition = new ApiFirewallReleaseQuarantineConfigDTO();
    list.add(condition);

    //when: setting release quarantine config, expect exception
    assertThatThrownBy(() -> apiFirewallService.setReleaseQuarantineConfig(list))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Some Policy Condition Types do not have ID's specified.");
  }

  @Test
  public void testSetFirewallReleaseQuarantineConfig_NoFirewallAutoUnquarantineFeature() {
    //setup: remove firewall feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    //when: getting release quarantine config
    //then: expect invalid license exception
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        apiFirewallService.setReleaseQuarantineConfig(null));
  }

  @Test
  public void testSetFirewallReleaseQuarantineConfig_NoReleaseIntegrityFeature() {
    //setup: remove release integrity feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    //when: getting release quarantine config
    //then: expect invalid license exception
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        apiFirewallService.setReleaseQuarantineConfig(null));
  }

  @Test
  public void testSetFirewallReleaseQuarantine_Telemetry() {
    //setup
    ArgumentCaptor<TelemetryData> argCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    tempEntity.newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);
    List<ApiFirewallReleaseQuarantineConfigDTO> list = new ArrayList<>();
    ApiFirewallReleaseQuarantineConfigDTO integrity = new ApiFirewallReleaseQuarantineConfigDTO();
    integrity.id = IntegrityRatingConditionType.ID;
    integrity.autoReleaseQuarantineEnabled = true;
    list.add(integrity);

    //when: setting release quarantine config
    Map<String, ApiFirewallReleaseQuarantineConfigDTO> releaseQuarantineConfig =
        apiFirewallService.setReleaseQuarantineConfig(list).stream()
            .collect(Collectors.toMap(dto -> dto.id, Function.identity()));

    //then: expect telemetry to have been sent matching the current config
    assertThat(releaseQuarantineConfig.get(IntegrityRatingConditionType.ID).autoReleaseQuarantineEnabled).isTrue();
    assertThat(releaseQuarantineConfig.get(SecurityVulnerabilityCategoryConditionType.ID).autoReleaseQuarantineEnabled)
        .isFalse();
    assertThat(releaseQuarantineConfig.get(SecurityVulnerabilitySeverityConditionType.ID).autoReleaseQuarantineEnabled)
        .isFalse();
    assertThat(releaseQuarantineConfig.get(LicenseConditionType.ID).autoReleaseQuarantineEnabled)
        .isFalse();
    assertThat(releaseQuarantineConfig.get(LicenseThreatGroupConditionType.ID).autoReleaseQuarantineEnabled)
        .isFalse();

    verify(telemetrySenderMock, times(1)).send(argCaptor.capture());
    TelemetryData telemetryData = argCaptor.getValue();
    AutoReleaseQuarantineTelemetry telemetrySent =
        (AutoReleaseQuarantineTelemetry) telemetryData.getAttributes().get(AUTO_RELEASE_QUARANTINE_CONFIG_TELEMETRY);
    assertThat(telemetrySent.enabledConditionTypes)
        .hasSize(1)
        .containsOnly(IntegrityRatingConditionType.ID);
    assertThat(telemetrySent.disabledConditionTypes)
        .hasSize(5)
        .containsExactlyInAnyOrder(SecurityVulnerabilityCategoryConditionType.ID,
            SecurityVulnerabilitySeverityConditionType.ID, LicenseConditionType.ID, LicenseThreatGroupConditionType.ID,
                MatchStateConditionType.ID);
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.AUTO_RELEASE_FROM_QUARANTINE_CONFIGURATION);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
  }

  @Test
  public void testGetAutoUnquarantineEnabledPolicyConditionTypesIds() {
    //setup
    tempEntity.newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);
    tempEntity.newAutoUnquarantinePolicyConditionType(LicenseConditionType.ID);

    //when: getting all ids of condition types that are auto-unquarantine enabled
    final Set<String> actuals = apiFirewallService.getAutoUnquarantineEnabledPolicyConditionTypesIds();

    //then: expect to get a set of valid ids
    assertThat(actuals.size()).isEqualTo(2);
    assertThat(actuals).contains(IntegrityRatingConditionType.ID);
    assertThat(actuals).contains(LicenseConditionType.ID);
  }

  @Test
  public void testGetAutoUnquarantineEnabledPolicyConditionTypesIds_emptyResults() {
    //when: getting all ids of condition types that are auto-unquarantine enabled
    final Set<String> actuals = apiFirewallService.getAutoUnquarantineEnabledPolicyConditionTypesIds();

    //then: expect empty set
    assertThat(actuals).isEmpty();
  }

  @Test
  public void testGetQuarantineSummary() {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    tempEntity.newRepositoryComponent(repo, "hash");
    tempEntity.newRepositoryComponent(repo.getId(), "path", new Date(), null);
    tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo2", true, false);

    ApiFirewallQuarantineSummaryDTO summary = apiFirewallService.getQuarantineSummary();

    assertThat(summary).isNotNull();
    assertThat(summary.repositoryCount).isEqualTo(2);
    assertThat(summary.quarantineEnabled).isTrue();
    assertThat(summary.quarantineEnabledRepositoryCount).isEqualTo(1);
    assertThat(summary.totalComponentCount).isEqualTo(2);
    assertThat(summary.quarantinedComponentCount).isEqualTo(1);
  }

  @Test
  public void testGetQuarantineSummary_NoReleaseIntegrityFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      apiFirewallService.getQuarantineSummary();
    });
  }

  @Test
  public void testGetQuarantineSummary_NoFirewallAutoUnquarantineFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      apiFirewallService.getQuarantineSummary();
    });
  }

  @Test
  public void testGetComponents_withPolicyViolations() {
    // SETUP
    Date june1st2020 = Date.from(LocalDateTime.of(2020, 6, 1, 1, 0).toInstant(ZoneOffset.UTC));
    Date june2nd2020 = Date.from(LocalDateTime.of(2020, 6, 2, 1, 0).toInstant(ZoneOffset.UTC));
    Date june3rd2020 = Date.from(LocalDateTime.of(2020, 6, 3, 1, 0).toInstant(ZoneOffset.UTC));
    Date june4th2020 = Date.from(LocalDateTime.of(2020, 6, 4, 1, 0).toInstant(ZoneOffset.UTC));

    Repository repository = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    Condition condition = new Condition("RelativePopularity", "<=", "10");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy1 = tempEntity.newPolicy("policy1", constraint);

    constraint = new Constraint("c2", "constraint2", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy2 = tempEntity.newPolicy("policy2", constraint);

    // ADD COMPONENT
    final RepositoryComponent component1 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined1", june1st2020, june2nd2020, true);
    final RepositoryComponent component2 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined2", june2nd2020, june3rd2020, true);
    final RepositoryComponent component3 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined3", june3rd2020, june4th2020, true);
    final RepositoryComponent component4 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined4", june3rd2020, june4th2020, true);

    // CREATE POLICY VIOLATION
    RepositoryPolicyViolation policyViolation1 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, component1, tempEntity);

    RepositoryPolicyViolation policyViolation2 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy2, component2, tempEntity);

    // a non-failing violation should not be included in results
    PolicyViolationTestHelper.createPolicyViolationWarn(policy2, component2, tempEntity);

    // a waived violation should not be included in results
    PolicyViolationTestHelper.createPolicyViolationWaived(policy2, component3, tempEntity);

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, "/quarantined4", false, "policy_id_3", "policy_3",
        component4.getComponentIdentifier());

    final FirewallSortableField sortField = FirewallSortableField.RELEASE_QUARANTINE_TIME;
    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.UNQUARANTINE_AUTO, sortField, true,
            Collections.emptyList());

    // EXECUTE
    final ApiPageResult<ApiFirewallComponentDTO> unquarantineList = apiFirewallService.getComponents(filter);

    // VERIFY
    assertThat(unquarantineList.getTotal()).isEqualTo(4);
    assertThat(unquarantineList.getResults()).hasSize(4);

    final ApiFirewallComponentDTO componentDTO1 = unquarantineList.getResults().get(0);
    assertRepositoryComponentWithOnePolicyViolation(policyViolation1, componentDTO1, june1st2020, june2nd2020);

    final ApiFirewallComponentDTO componentDTO2 = unquarantineList.getResults().get(1);
    assertRepositoryComponentWithOnePolicyViolation(policyViolation2, componentDTO2, june2nd2020, june3rd2020);

    final ApiFirewallComponentDTO componentDTO3 = unquarantineList.getResults().get(2);
    assertRepositoryComponentZeroViolations(componentDTO3, june3rd2020, june4th2020);

    final ApiFirewallComponentDTO componentDTO4 = unquarantineList.getResults().get(3);
    assertRepositoryComponentZeroViolations(componentDTO4, june3rd2020, june4th2020);
  }

  @Test
  public void testGetComponents_withoutPolicyViolations() {
    // SETUP
    Date june1st2020 = Date.from(LocalDateTime.of(2020, 6, 1, 1, 0).toInstant(ZoneOffset.UTC));
    Date june2nd2020 = Date.from(LocalDateTime.of(2020, 6, 2, 1, 0).toInstant(ZoneOffset.UTC));
    Date june3rd2020 = Date.from(LocalDateTime.of(2020, 6, 3, 1, 0).toInstant(ZoneOffset.UTC));

    Repository repository = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    Condition condition = new Condition("RelativePopularity", "<=", "10");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy1 = tempEntity.newPolicy("policy1", constraint);

    // ADD COMPONENT
    final RepositoryComponent component1 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined1", june1st2020, june2nd2020, true);

    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined2", june2nd2020, june3rd2020, true);

    // CREATE POLICY VIOLATION
    RepositoryPolicyViolation policyViolation1 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, component1, tempEntity);

    final FirewallSortableField sortField = FirewallSortableField.RELEASE_QUARANTINE_TIME;
    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, sortField, true,
            Collections.emptyList());

    // EXECUTE
    final ApiPageResult<ApiFirewallComponentDTO> unquarantineList = apiFirewallService.getComponents(filter);

    // VERIFY
    assertThat(unquarantineList.getTotal()).isEqualTo(2);
    assertThat(unquarantineList.getResults()).hasSize(2);

    final ApiFirewallComponentDTO componentDTO1 = unquarantineList.getResults().get(0);
    assertRepositoryComponentWithOnePolicyViolation(policyViolation1, componentDTO1, june1st2020, june2nd2020);
    assertRepositoryComponentZeroViolations(unquarantineList.getResults().get(1), june2nd2020, june3rd2020);
  }

  @Test
  public void testGetComponents_NoSortField() {
    // SETUP
    Date june1st2020 = Date.from(LocalDateTime.of(2020, 6, 1, 1, 0).toInstant(ZoneOffset.UTC));
    Date june2nd2020 = Date.from(LocalDateTime.of(2020, 6, 2, 1, 0).toInstant(ZoneOffset.UTC));
    Date june3rd2020 = Date.from(LocalDateTime.of(2020, 6, 3, 1, 0).toInstant(ZoneOffset.UTC));
    Date june4th2020 = Date.from(LocalDateTime.of(2020, 6, 4, 1, 0).toInstant(ZoneOffset.UTC));

    Repository repository = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    Condition condition = new Condition("RelativePopularity", "<=", "10");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy1 = tempEntity.newPolicy("policy1", constraint);

    constraint = new Constraint("c2", "constraint2", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy2 = tempEntity.newPolicy("policy2", constraint);

    // ADD COMPONENT
    final RepositoryComponent component1 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined1", june1st2020, june2nd2020, true);
    final RepositoryComponent component2 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined2", june2nd2020, june3rd2020, true);
    final RepositoryComponent component3 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined3", june3rd2020, june4th2020, true);

    // CREATE POLICY VIOLATION
    RepositoryPolicyViolation policyViolation1 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, component1, tempEntity);

    RepositoryPolicyViolation policyViolation2 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy2, component2, tempEntity);

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, "/quarantined3", false, "policy_id_3", "policy_3",
        component3.getComponentIdentifier());

    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());

    // EXECUTE
    final ApiPageResult<ApiFirewallComponentDTO> unquarantineList = apiFirewallService.getComponents(filter);

    // VERIFY
    assertThat(unquarantineList.getTotal()).isEqualTo(3);
    assertThat(unquarantineList.getResults()).hasSize(2);

    final ApiFirewallComponentDTO componentDTO1 = unquarantineList.getResults().get(0);
    assertRepositoryComponentWithOnePolicyViolation(policyViolation1, componentDTO1, june1st2020, june2nd2020);

    final ApiFirewallComponentDTO componentDTO2 = unquarantineList.getResults().get(1);
    assertRepositoryComponentWithOnePolicyViolation(policyViolation2, componentDTO2, june2nd2020, june3rd2020);
  }

  @Test
  public void testGetComponents_noComponents() {
    // SETUP
    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());

    // EXECUTE
    final ApiPageResult<ApiFirewallComponentDTO> unquarantineList = apiFirewallService.getComponents(filter);

    // VERIFY
    assertThat(unquarantineList.getTotal()).isZero();
    assertThat(unquarantineList.getResults()).isEmpty();
  }

  @Test(expected = BadRequestException.class)
  public void testGetComponents_pageLessThanOne() {
    // SETUP
    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(0, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());

    // EXECUTE
    apiFirewallService.getComponents(filter);
  }

  @Test(expected = BadRequestException.class)
  public void testGetComponents_pageSizeLessThanOne() {
    // SETUP
    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 0, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());

    // EXECUTE
    apiFirewallService.getComponents(filter);
  }

  @Test
  public void testGetComponents_noStateSpecified() {
    // SETUP
    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 10, null, null, true,
            Collections.emptyList());

    // EXECUTE
    assertThatThrownBy(() -> apiFirewallService.getComponents(filter))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("firewallComponentFilterState is required and cannot be null.");
  }

  static void assertRepositoryComponentWithOnePolicyViolation(
      final RepositoryPolicyViolation expectedPolicyViolation,
      final ApiFirewallComponentDTO componentDTO,
      final Date quarantineDate,
      final Date dateCleared)
  {
    assertThat(componentDTO.displayName).isEqualTo("g : a : v");
    assertRepositoryComponent(componentDTO, quarantineDate, dateCleared);
    assertThat(componentDTO.repository).isEqualTo("repo1");
    assertThat(componentDTO.dateCleared).isEqualTo(dateCleared);
    assertThat(componentDTO.quarantineDate).isEqualTo(quarantineDate);
    assertThat(componentDTO.quarantinePolicyViolations).hasSize(1);
    PolicyViolationTestHelper
        .assertApiPolicyViolationDTOV2(componentDTO.quarantinePolicyViolations.get(0), expectedPolicyViolation);
  }

  static void assertRepositoryComponentZeroViolations(
      final ApiFirewallComponentDTO componentDTO,
      final Date quarantineDate,
      final Date dateCleared)
  {
    assertRepositoryComponent(componentDTO, quarantineDate, dateCleared);
    assertThat(componentDTO.quarantinePolicyViolations).isEmpty();
  }

  static void assertRepositoryComponent(
      final ApiFirewallComponentDTO componentDTO,
      final Date quarantineDate,
      final Date dateCleared)
  {
    assertThat(componentDTO.displayName).isEqualTo("g : a : v");
    assertThat(componentDTO.repository).isEqualTo("repo1");
    assertThat(componentDTO.dateCleared).isEqualTo(dateCleared);
    assertThat(componentDTO.quarantineDate).isEqualTo(quarantineDate);
  }
}
