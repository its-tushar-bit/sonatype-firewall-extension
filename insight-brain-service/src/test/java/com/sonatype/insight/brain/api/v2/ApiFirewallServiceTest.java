/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallQuarantinedComponentDto;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList.ApiRepositoryComponentEvaluationRequest;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationResultList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryContainerDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerListDTO;
import com.sonatype.insight.brain.api.v2.service.PolicyViolationTestHelper;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField;
import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField.FirewallFilterableField;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField;
import com.sonatype.insight.brain.dataaccess.repository.InvalidRepositoryException;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.integration.repository.RepositoryService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCustomCVSSVectorStringConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCustomRemediationConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityDetectionConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityEpssScoreConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityResearchConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.AutoReleaseQuarantineTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.RepositoryPathnameSerializer;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.matcher.Matchers;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.api.v2.ApiFirewallService.AUTO_RELEASE_QUARANTINE_CONFIG_TELEMETRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiFirewallServiceTest
    extends AbstractComponentTest
{
  @Inject
  ApiFirewallService apiFirewallService;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private RepositoryComponentDAO repositoryComponentDAO;

  @Inject
  private QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Mock
  private RepositoryService repositoryServiceMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    binder.bindInterceptor(Matchers.subclassesOf(RepositoryService.class), Matchers.any(), invocation -> {
      if (invocation.getMethod().getName().equals("evaluateComponents")) {
        return invocation.getMethod().invoke(repositoryServiceMock, invocation.getArguments());
      }
      return invocation.proceed();
    });
    super.configure(binder);
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

    final List<String> autoUnquarantinedConditionTypes = ConditionTypes.getAllWithAutoUnquarantineSupported().stream()
        .map(ConditionType::getName)
        .toList();

    //when: getting release quarantine config
    //then: expect the auto unquarantined enabled policy condition types with the enabled flag set
    Map<String, ApiFirewallReleaseQuarantineConfigDTO> releaseQuarantineConfig =
        apiFirewallService.getReleaseQuarantineConfig().stream()
            .collect(Collectors.toMap(dto -> dto.id, Function.identity()));
    long numTrue = releaseQuarantineConfig.values().stream().filter(dto -> dto.autoReleaseQuarantineEnabled).count();

    assertThat(releaseQuarantineConfig.values()).extracting("name")
        .containsExactlyInAnyOrderElementsOf(autoUnquarantinedConditionTypes);
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

    final List<String> autoUnquarantinedConditionTypes = ConditionTypes.getAllWithAutoUnquarantineSupported().stream()
        .map(ConditionType::getName)
        .toList();

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
        .containsExactlyInAnyOrderElementsOf(autoUnquarantinedConditionTypes);
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

    final List<String> autoUnquarantinedConditionTypes = ConditionTypes.getAllWithAutoUnquarantineSupported().stream()
        .map(ConditionType::getName)
        .toList();

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
        .containsExactlyInAnyOrderElementsOf(autoUnquarantinedConditionTypes);
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
    assertThat(releaseQuarantineConfig.get(SecurityVulnerabilityResearchConditionType.ID).autoReleaseQuarantineEnabled)
        .isFalse();
    assertThat(releaseQuarantineConfig.get(SecurityVulnerabilityCustomCVSSVectorStringConditionType.ID)
        .autoReleaseQuarantineEnabled)
        .isFalse();
    assertThat(releaseQuarantineConfig
        .get(SecurityVulnerabilityCustomRemediationConditionType.ID).autoReleaseQuarantineEnabled).isFalse();

    verify(telemetrySenderMock, times(1)).send(argCaptor.capture());
    TelemetryData telemetryData = argCaptor.getValue();
    AutoReleaseQuarantineTelemetry telemetrySent =
        (AutoReleaseQuarantineTelemetry) telemetryData.getAttributes().get(AUTO_RELEASE_QUARANTINE_CONFIG_TELEMETRY);
    assertThat(telemetrySent.enabledConditionTypes)
        .hasSize(1)
        .containsOnly(IntegrityRatingConditionType.ID);
    assertThat(telemetrySent.disabledConditionTypes)
        .hasSize(11)
        .containsExactlyInAnyOrder(
            AgeInDaysConditionType.ID, SecurityVulnerabilityCategoryConditionType.ID,
            SecurityVulnerabilitySeverityConditionType.ID, LicenseConditionType.ID, LicenseThreatGroupConditionType.ID,
            MatchStateConditionType.ID, SecurityVulnerabilityResearchConditionType.ID,
            SecurityVulnerabilityCustomCVSSVectorStringConditionType.ID,
            SecurityVulnerabilityCustomRemediationConditionType.ID,
            SecurityVulnerabilityDetectionConditionType.ID, SecurityVulnerabilityEpssScoreConditionType.ID);
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
    tempEntity.newHostedRepository(tempEntity.newRepositoryManager(), "hostedRepo1", "npm", true);
    tempEntity.newHostedRepository(tempEntity.newRepositoryManager(), "hostedRepo2", "maven", false);

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

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> apiFirewallService.getQuarantineSummary());
  }

  @Test
  public void testGetQuarantineSummary_NoFirewallAutoUnquarantineFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> apiFirewallService.getQuarantineSummary());
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
  public void testGetComponents_UnknownComponent() {
    Date date = new Date();
    Repository repository = tempEntity.newRepository(tempEntity.newRepositoryManager(), "testRepo", true, true);
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN,
        "testPathName", "testHash", null /* componentIdentifier */, date, date);
    Policy policy = tempEntity.newPolicy();
    RepositoryPolicyViolation policyViolation =
        PolicyViolationTestHelper.createPolicyViolationFail(policy, component, tempEntity);

    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.QUARANTINE,
            FirewallSortableField.QUARANTINE_TIME, true /* asc */, Collections.emptyList());

    ApiPageResult<ApiFirewallComponentDTO> result = apiFirewallService.getComponents(filter);

    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getResults()).hasSize(1);

    ApiFirewallComponentDTO apiFirewallComponentDTO = result.getResults().get(0);
    assertThat(apiFirewallComponentDTO.displayName).isEqualTo("testPathName (testPathName)");
    assertThat(apiFirewallComponentDTO.repository).isEqualTo(repository.getPublicId());
    assertThat(apiFirewallComponentDTO.dateCleared).isNull();
    assertThat(apiFirewallComponentDTO.quarantineDate).isEqualTo(date);
    assertThat(apiFirewallComponentDTO.componentIdentifier).isNull();
    assertThat(apiFirewallComponentDTO.pathname).isEqualTo("testPathName");
    assertThat(apiFirewallComponentDTO.hash).isEqualTo("testHash");
    assertThat(apiFirewallComponentDTO.matchState).isEqualTo(MatchState.UNKNOWN.getId());
    assertThat(apiFirewallComponentDTO.repositoryId).isEqualTo(repository.getId());
    assertThat(apiFirewallComponentDTO.quarantined).isTrue();
    assertThat(apiFirewallComponentDTO.quarantinePolicyViolations).hasSize(1);
    PolicyViolationTestHelper.assertApiPolicyViolationDTOV2(apiFirewallComponentDTO.quarantinePolicyViolations.get(0),
        policyViolation);
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

  @Test
  public void testGetRepositoryManagers() {
    //given
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("instanceId1", "repoName1",
        "repoProductName1", "repoProductVersion1");
    String repositoryManagerId1 = repositoryManager.getId();
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager("instanceId2", "repoName2",
        "repoProductName2", "repoProductVersion2");
    String repositoryManagerId2 = repositoryManager2.getId();

    //when
    ApiRepositoryManagerListDTO repositoryManagers = apiFirewallService.getRepositoryManagers();

    //then
    assertThat(repositoryManagers.repositoryManagers.size()).isEqualTo(2);
    assertThat(repositoryManagers.repositoryManagers.get(0).id).isEqualTo(repositoryManagerId1);
    assertThat(repositoryManagers.repositoryManagers.get(0).instanceId).isEqualTo("instanceId1");
    assertThat(repositoryManagers.repositoryManagers.get(0).name).isEqualTo("repoName1");
    assertThat(repositoryManagers.repositoryManagers.get(0).productName).isEqualTo("repoProductName1");
    assertThat(repositoryManagers.repositoryManagers.get(0).productVersion).isEqualTo("repoProductVersion1");
    assertThat(repositoryManagers.repositoryManagers.get(1).id).isEqualTo(repositoryManagerId2);
    assertThat(repositoryManagers.repositoryManagers.get(1).instanceId).isEqualTo("instanceId2");
    assertThat(repositoryManagers.repositoryManagers.get(1).name).isEqualTo("repoName2");
    assertThat(repositoryManagers.repositoryManagers.get(1).productName).isEqualTo("repoProductName2");
    assertThat(repositoryManagers.repositoryManagers.get(1).productVersion).isEqualTo("repoProductVersion2");
  }

  @Test
  public void testGetRepositoryManagers_Empty() {
    //when
    ApiRepositoryManagerListDTO repositoryManagers = apiFirewallService.getRepositoryManagers();

    //then
    assertThat(repositoryManagers.repositoryManagers).isEmpty();
  }

  @Test
  public void testSetQuarantinedComponentViewAnonymousAccess() {
    assertThat(quarantinedComponentAccessDAO.isAnonymousAccessEnabled()).isTrue();

    apiFirewallService.setQuarantinedComponentViewAnonymousAccess(false);
    assertThat(quarantinedComponentAccessDAO.isAnonymousAccessEnabled()).isFalse();

    apiFirewallService.setQuarantinedComponentViewAnonymousAccess(true);
    assertThat(quarantinedComponentAccessDAO.isAnonymousAccessEnabled()).isTrue();
  }

  @Test
  public void testGetConfiguredRepositories() {
    Date may5th20239AM = Date.from(LocalDateTime.of(2023, 5, 1, 9, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
    Date may5th202310AM = Date.from(LocalDateTime.of(2023, 5, 1, 10, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
    Date may5th202311AM = Date.from(LocalDateTime.of(2023, 5, 1, 11, 0, 0).atZone(ZoneId.systemDefault()).toInstant());

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    tempEntity.newRepository(repositoryManager, "testRepoNpm", RepositoryType.proxy, "npm",
        may5th20239AM);
    Repository repository =
        tempEntity.newRepository(repositoryManager, "testRepoMaven", RepositoryType.proxy, "maven", may5th202311AM);

    ApiRepositoryListDTO apiRepositoryListDTO = apiFirewallService.getConfiguredRepositories(repositoryManager.getId(),
        may5th202310AM.getTime());

    assertThat(apiRepositoryListDTO.repositories).hasSize(1);
    ApiRepositoryDTO apiRepositoryDTO = apiRepositoryListDTO.repositories.get(0);
    assertThat(apiRepositoryDTO.repositoryId).isEqualTo(repository.getId());
    assertThat(apiRepositoryDTO.publicId).isEqualTo(repository.getName());
    assertThat(apiRepositoryDTO.format).isEqualTo(repository.getFormat());
    assertThat(apiRepositoryDTO.type).isEqualTo(repository.getRepositoryType().name());
    assertThat(apiRepositoryDTO.auditEnabled).isEqualTo(repository.isAuditEnabled());
    assertThat(apiRepositoryDTO.quarantineEnabled).isEqualTo(repository.isQuarantineEnabled());
    assertThat(apiRepositoryDTO.policyCompliantComponentSelectionEnabled).isEqualTo(
        repository.isPolicyCompliantComponentSelectionEnabled());
    assertThat(apiRepositoryDTO.namespaceConfusionProtectionEnabled).isEqualTo(
        repository.isNamespaceConfusionProtectionEnabled());
  }

  @Test
  public void testGetConfiguredRepositories_NotExistingRepositoryManager() {
    final String repositoryMangerId = "invalidId";
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      apiFirewallService.getConfiguredRepositories(repositoryMangerId, 0L);
    }).withMessage("RepositoryManager with ID " + repositoryMangerId + " does not exist.");
  }

  @Test
  public void testGetConfiguredRepositories_NullTimestamp() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    Repository repository1 = tempEntity.newRepository(repositoryManager, "testRepoNpm1", RepositoryType.proxy, "npm",
        new Date(0));

    Repository repository2 = tempEntity.newRepository(repositoryManager, "testRepoNpm2", RepositoryType.proxy, "npm",
        new Date(1));

    Repository repository3 = tempEntity.newRepository(repositoryManager, TemporaryEntity.uuid());

    ApiRepositoryListDTO apiRepositoryListDTO =
        apiFirewallService.getConfiguredRepositories(repositoryManager.getId(), null);

    assertThat(apiRepositoryListDTO.repositories).extracting(r -> r.publicId)
        .containsExactlyInAnyOrder(repository1.getName(), repository2.getName(), repository3.getName());
  }

  @Test
  public void testGetConfiguredRepositories_NoFirewallFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        apiFirewallService
            .getConfiguredRepositories("repositoryManagerId", null));
  }

  @Test
  public void testEvaluateComponents_NoFirewallFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        apiFirewallService
            .evaluateComponents("repositoryManagerId", "repositoryId",
                new ApiRepositoryComponentEvaluationRequestList()));
  }

  @Test
  public void testEvaluateComponents_RepositoryManagerNotFound() {
    // given
    Repository repository = tempEntity.newRepository();
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    String someOtherRepoManagerId = "someOtherRepoManagerId";

    // then
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
            () -> apiFirewallService.evaluateComponents(someOtherRepoManagerId, repository.getId(), requestList))
        .withMessage("RepositoryManager with ID " + someOtherRepoManagerId + " does not exist.");
  }

  @Test
  public void testEvaluateComponents_RepositoryNotFound() {
    // given
    Repository repository = tempEntity.newRepository();
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    String someOtherRepoId = "someOtherRepoManagerId";

    // then
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(), someOtherRepoId,
            requestList))
        .withMessage("Repository with ID " + someOtherRepoId + " does not exist.");
  }

  @Test
  public void testEvaluateComponents_RepositoryDoesNotBelongToRepoManager() {
    // given
    Repository repository = tempEntity.newRepository();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();

    // then
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
            () -> apiFirewallService.evaluateComponents(repositoryManager.getId(), repository.getId(), requestList))
        .withMessage(
            "Repository '" + repository.getId() + "' not found in repository manager '" + repositoryManager.getId() +
                "'.");
  }

  @Test
  public void testEvaluateComponents_NullRequestList() {
    // given
    Repository repository = tempEntity.newRepository();

    // then
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
            () -> apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(), repository.getId(), null))
        .withMessage("There should be at least 1 component to evaluate.");
  }

  @Test
  public void testEvaluateComponents_NullComponents() {
    // given
    Repository repository = tempEntity.newRepository();
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    requestList.components = null;

    // then
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
            () -> apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(), repository.getId(),
                requestList))
        .withMessage("There should be at least 1 component to evaluate.");
  }

  @Test
  public void testEvaluateComponents_EmptyComponents() {
    // given
    Repository repository = tempEntity.newRepository();
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();

    // then
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
            () -> apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(), repository.getId(),
                requestList))
        .withMessage("There should be at least 1 component to evaluate.");
  }

  @Test
  public void testEvaluateComponents_TooManyComponents() {
    // given
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "/audit", null, null);
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = ComponentIdentifier.FORMAT_MAVEN;
    for (int i = 0; i < 101; i++) {
      requestList.components.add(
          new ApiRepositoryComponentEvaluationRequest(repositoryComponent.getPathname(),
              repositoryComponent.getHash()));
    }

    // then
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
            () -> apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(), repository.getId(),
                requestList))
        .withMessage("Max amount of components to evaluate is '100'.");
  }

  @Test
  public void testEvaluateComponents_RequestWithoutFormat() {
    // given
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "/audit", null, null);
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    requestList.components.add(
        new ApiRepositoryComponentEvaluationRequest(repositoryComponent.getPathname(), repositoryComponent.getHash()));
    // then
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
            () -> apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(), repository.getId(),
                requestList))
        .withMessage("The format must be specified.");
  }

  @Test
  public void testEvaluateComponents_NoHash() {
    Repository repository = tempEntity.newRepository();
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = "npm";
    requestList.components.add(new ApiRepositoryComponentEvaluationRequest("somePathname", null, null));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(),
            repository.getId(), requestList))
        .withMessage("The hash must be specified.");
  }

  @Test
  public void testEvaluateComponents_NoPathnameOrPackageUrl() {
    Repository repository = tempEntity.newRepository();
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = "npm";
    requestList.components.add(new ApiRepositoryComponentEvaluationRequest(null, "someHash", null));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(),
            repository.getId(), requestList))
        .withMessage("One of pathname or packageUrl must be specified.");
  }

  @Test
  public void testEvaluateComponents_InvalidPackageUrl() {
    Repository repository = tempEntity.newRepository();
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = "npm";
    requestList.components.add(new ApiRepositoryComponentEvaluationRequest(null, "someHash", "invalidPackageUrl"));

    assertThatExceptionOfType(InvalidPackageURLException.class)
        .isThrownBy(() -> apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(),
            repository.getId(), requestList))
        .withMessage("Invalid package url");
  }

  @Test
  public void testEvaluateComponents_UnsupportedFormat() {
    Repository repository = tempEntity.newRepository();
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = "f";
    requestList.components.add(new ApiRepositoryComponentEvaluationRequest(null, "someHash", "pkg:f/n@v"));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(),
            repository.getId(), requestList))
        .withMessage("Unsupported format f.");
  }

  @Test
  public void testEvaluateComponents_PackageUrlFormatMismatch() {
    Repository repository = tempEntity.newRepository();
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = "npm";
    requestList.components.add(new ApiRepositoryComponentEvaluationRequest(null, "someHash",
        "pkg:maven/commons-fileupload/commons-fileupload@1.3.2?type=jar"));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(),
            repository.getId(), requestList))
        .withMessage("Component format must match that of the request.");
  }

  @Test
  public void testEvaluateComponents_PathnameAndHash() {
    // given
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "/audit", new Date(), null);
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = ComponentIdentifier.FORMAT_MAVEN;
    requestList.components.add(
        new ApiRepositoryComponentEvaluationRequest(repositoryComponent.getPathname(), repositoryComponent.getHash()));
    RepositoryComponentEvaluationDataList repositoryServiceEvaluateResult = new RepositoryComponentEvaluationDataList();
    RepositoryComponentEvaluationData rced = new RepositoryComponentEvaluationData();
    rced.quarantine = true;
    rced.catalogDate = new Date();
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy Name", null);
    repositoryServiceEvaluateResult.componentEvalResults.add(rced);
    when(repositoryServiceMock.evaluateComponents(any(Repository.class), anyString(), any(), eq(false), eq(true),
        isNull())).thenReturn(repositoryServiceEvaluateResult);

    // when
    ApiRepositoryComponentEvaluationResultList result =
        apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(), repository.getId(),
            requestList);

    // then
    assertThat(result).isNotNull();
    assertThat(result.repositoryManagerId).isEqualTo(repository.getRepositoryManagerId());
    assertThat(result.repositoryId).isEqualTo(repository.getId());
    assertThat(result.repositoryPublicId).isEqualTo(repository.getPublicId());
    assertThat(result.repositoryType).isEqualTo(repository.getRepositoryType().name());
    assertThat(result.results.size()).isEqualTo(1);
    assertThat(result.results.get(0).quarantined).isEqualTo(rced.quarantine);
    assertThat(result.results.get(0).quarantineDate).isEqualTo(repositoryComponent.getQuarantineTime());
    assertThat(result.results.get(0).component).isEqualTo(requestList.components.get(0));
    assertThat(result.results.get(0).catalogDate).isAfterOrEqualTo(rced.catalogDate);
    assertThat(result.results.get(0).policyViolations.size()).isEqualTo(1);
    ApiPolicyViolationDTOV2 policyViolationDTOV2 = result.results.get(0).policyViolations.get(0);
    assertThat(policyViolationDTOV2.policyId).isEqualTo(repositoryPolicyViolation.getPolicyId());
    assertThat(policyViolationDTOV2.threatLevel).isEqualTo(repositoryPolicyViolation.getThreatLevel());
  }

  @Test
  public void testEvaluateComponents_PurlAndHash() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String fakePathname = RepositoryPathnameSerializer.toPathname(componentIdentifier);
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), fakePathname, new Date(), null);
    repositoryComponent.setComponentIdentifier(componentIdentifier);
    repositoryComponentDAO.update(repositoryComponent);
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = ComponentIdentifier.FORMAT_MAVEN;
    requestList.components.add(new ApiRepositoryComponentEvaluationRequest(
        null,
        repositoryComponent.getHash(),
        PackageUrlIdentifier.fromComponentIdentifier(repositoryComponent.getComponentIdentifier()).getPackageUrl())
    );
    RepositoryComponentEvaluationDataList repositoryServiceEvaluateResult = new RepositoryComponentEvaluationDataList();
    RepositoryComponentEvaluationData rced = new RepositoryComponentEvaluationData();
    rced.quarantine = true;
    rced.catalogDate = new Date();
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy Name", null);
    repositoryServiceEvaluateResult.componentEvalResults.add(rced);
    when(repositoryServiceMock.evaluateComponents(any(Repository.class), anyString(), any(), eq(false), eq(true),
        isNull())).thenReturn(repositoryServiceEvaluateResult);

    ApiRepositoryComponentEvaluationResultList result =
        apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(), repository.getId(),
            requestList);

    assertThat(result).isNotNull();
    assertThat(result.repositoryManagerId).isEqualTo(repository.getRepositoryManagerId());
    assertThat(result.repositoryId).isEqualTo(repository.getId());
    assertThat(result.repositoryPublicId).isEqualTo(repository.getPublicId());
    assertThat(result.repositoryType).isEqualTo(repository.getRepositoryType().name());
    assertThat(result.results.size()).isEqualTo(1);
    assertThat(result.results.get(0).quarantined).isEqualTo(rced.quarantine);
    assertThat(result.results.get(0).quarantineDate).isEqualTo(repositoryComponent.getQuarantineTime());
    assertThat(result.results.get(0).component).isEqualTo(requestList.components.get(0));
    assertThat(result.results.get(0).catalogDate).isAfterOrEqualTo(rced.catalogDate);
    assertThat(result.results.get(0).policyViolations.size()).isEqualTo(1);
    ApiPolicyViolationDTOV2 policyViolationDTOV2 = result.results.get(0).policyViolations.get(0);
    assertThat(policyViolationDTOV2.policyId).isEqualTo(repositoryPolicyViolation.getPolicyId());
    assertThat(policyViolationDTOV2.threatLevel).isEqualTo(repositoryPolicyViolation.getThreatLevel());
    ArgumentCaptor<RepositoryComponentEvaluationDataRequestList> captor = ArgumentCaptor.forClass(
        RepositoryComponentEvaluationDataRequestList.class);
    verify(repositoryServiceMock).evaluateComponents(any(Repository.class), anyString(), captor.capture(), eq(false),
        eq(true), isNull());
    RepositoryComponentEvaluationDataRequestList request = captor.getValue();
    assertThat(request.components.get(0).pathname).isEqualTo(fakePathname);
  }

  @Test
  public void testEvaluateComponents_WithoutPolicyAlerts() {
    // given
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "/audit", null, null);
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = ComponentIdentifier.FORMAT_MAVEN;
    requestList.components.add(
        new ApiRepositoryComponentEvaluationRequest(repositoryComponent.getPathname(), repositoryComponent.getHash()));
    RepositoryComponentEvaluationDataList repositoryServiceEvaluateResult = new RepositoryComponentEvaluationDataList();
    repositoryServiceEvaluateResult.componentEvalResults.add(new RepositoryComponentEvaluationData());
    when(repositoryServiceMock.evaluateComponents(any(Repository.class), anyString(), any(), eq(false), eq(true),
        isNull())).thenReturn(repositoryServiceEvaluateResult);

    // when
    ApiRepositoryComponentEvaluationResultList result =
        apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(), repository.getId(), requestList);

    // then
    assertThat(result).isNotNull();
    assertThat(result.repositoryManagerId).isEqualTo(repository.getRepositoryManagerId());
    assertThat(result.repositoryId).isEqualTo(repository.getId());
    assertThat(result.repositoryPublicId).isEqualTo(repository.getPublicId());
    assertThat(result.repositoryType).isEqualTo(repository.getRepositoryType().name());
    assertThat(result.results.size()).isEqualTo(1);
    assertThat(result.results.get(0).quarantined).isFalse();
    assertThat(result.results.get(0).quarantineDate).isNull();
    assertThat(result.results.get(0).component).isEqualTo(requestList.components.get(0));
    assertThat(result.results.get(0).catalogDate).isNull();
    assertThat(result.results.get(0).policyViolations).isEmpty();
  }

  @Test
  public void testEvaluateComponents_WithQuarantine() {
    Repository repository = tempEntity.newRepository();
    repository.setQuarantineEnabled(true);
    repositoryDAO.update(repository);
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "/audit", null, null);
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = ComponentIdentifier.FORMAT_MAVEN;
    requestList.components.add(
        new ApiRepositoryComponentEvaluationRequest(repositoryComponent.getPathname(), repositoryComponent.getHash()));
    RepositoryComponentEvaluationDataList repositoryServiceEvaluateResult = new RepositoryComponentEvaluationDataList();
    repositoryServiceEvaluateResult.componentEvalResults.add(new RepositoryComponentEvaluationData());
    when(repositoryServiceMock.evaluateComponents(any(Repository.class), anyString(), any(), eq(true), eq(true),
        isNull())).thenReturn(repositoryServiceEvaluateResult);

    ApiRepositoryComponentEvaluationResultList result =
        apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(), repository.getId(), requestList);

    assertThat(result).isNotNull();
    verify(repositoryServiceMock).evaluateComponents(any(Repository.class), anyString(), any(), eq(true), eq(true),
        isNull());
  }

  @Test
  public void testEvaluateComponents_includesOpenWaiveTimes() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "/audit", new Date(), null);
    ApiRepositoryComponentEvaluationRequestList requestList = new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = ComponentIdentifier.FORMAT_MAVEN;
    requestList.components.add(
        new ApiRepositoryComponentEvaluationRequest(repositoryComponent.getPathname(), repositoryComponent.getHash()));
    RepositoryComponentEvaluationDataList repositoryServiceEvaluateResult = new RepositoryComponentEvaluationDataList();
    RepositoryComponentEvaluationData rced = new RepositoryComponentEvaluationData();
    rced.catalogDate = new Date();
    RepositoryPolicyViolation v1 =
        tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy Name", null);
    RepositoryPolicyViolation v2 =
        tempEntity.newRepositoryPolicyViolation(repositoryComponent, 9, true, "Policy Name", null);
    v2.setWaiveTime(DateUtils.addDays(rced.catalogDate, 1));
    repositoryPolicyViolationDAO.update(v2);
    repositoryServiceEvaluateResult.componentEvalResults.add(rced);
    when(repositoryServiceMock.evaluateComponents(any(Repository.class), anyString(), any(), eq(false), eq(true),
        isNull())).thenReturn(repositoryServiceEvaluateResult);

    ApiRepositoryComponentEvaluationResultList result =
        apiFirewallService.evaluateComponents(repository.getRepositoryManagerId(), repository.getId(),
            requestList);

    assertThat(result).isNotNull();
    assertThat(result.results).hasSize(1);
    assertThat(result.results.get(0).policyViolations).hasSize(2);
    ApiPolicyViolationDTOV2 result1 = result.results.get(0).policyViolations.get(0);
    assertThat(result1.openTime).isNotNull().isEqualTo(v1.getOpenTime());
    assertThat(result1.waiveTime).isNull();
    ApiPolicyViolationDTOV2 result2 = result.results.get(0).policyViolations.get(1);
    assertThat(result2.openTime).isNotNull().isEqualTo(v2.getOpenTime());
    assertThat(result2.waiveTime).isNotNull().isEqualTo(v2.getWaiveTime());
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

  static void assertFirewallQuarantinedDetails(
      final Repository expectedRepository,
      final RepositoryComponent expectedComponent,
      final RepositoryPolicyViolation expectedViolation,
      final ApiFirewallQuarantinedComponentDto componentDTO)
  {
    assertThat(componentDTO.threatLevel).isEqualTo(expectedViolation.getThreatLevel());
    assertThat(componentDTO.policyName).isEqualTo(expectedViolation.getPolicyName());
    assertThat(componentDTO.quarantineDate).isEqualTo(expectedComponent.getQuarantineTime());
    assertThat(componentDTO.displayName).isEqualTo(expectedComponent.getDisplayName());
    assertThat(componentDTO.repositoryName).isEqualTo(expectedRepository.getName());
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

  @Test
  public void testGetRepositoryManagers_NoFirewallFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> apiFirewallService.getRepositoryManagers());
  }

  @Test
  public void testConfigureRepositories_MissingFirewallFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> apiFirewallService.configureRepositories(null, null));
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_Null() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), null))
        .withMessageContaining("No repository configurations specified.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_NullList() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("No repository configurations specified.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_EmptyList() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    dto.repositories = Collections.emptyList();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("No repository configurations specified.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_NullPublicId() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    ApiRepositoryDTO repoDto = new ApiRepositoryDTO();
    repoDto.publicId = null;
    repoDto.type = "proxy";
    dto.repositories = Collections.singletonList(repoDto);

    assertThatExceptionOfType(InvalidRepositoryException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("The repository public ID cannot be null or empty.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_EmptyPublicId() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    ApiRepositoryDTO repoDto = new ApiRepositoryDTO();
    repoDto.publicId = "";
    repoDto.type = "proxy";
    dto.repositories = Collections.singletonList(repoDto);

    assertThatExceptionOfType(InvalidRepositoryException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("The repository public ID cannot be null or empty.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_NullType() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    ApiRepositoryDTO repoDto = new ApiRepositoryDTO();
    repoDto.publicId = "publicId";
    repoDto.type = null;
    dto.repositories = Collections.singletonList(repoDto);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("The repository type must be proxy or hosted.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_EmptyType() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    ApiRepositoryDTO repoDto = new ApiRepositoryDTO();
    repoDto.publicId = "publicId";
    repoDto.type = "";
    dto.repositories = Collections.singletonList(repoDto);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("The repository type must be proxy or hosted.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_BadFormat() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    ApiRepositoryDTO repoDto = new ApiRepositoryDTO();
    repoDto.publicId = "publicId";
    repoDto.type = "proxy";
    repoDto.format = "bob";
    dto.repositories = Collections.singletonList(repoDto);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("Unrecognized format 'bob'.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_ProxyNamespace() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    ApiRepositoryDTO repoDto = new ApiRepositoryDTO();
    repoDto.publicId = "publicId";
    repoDto.type = "proxy";
    repoDto.namespaceConfusionProtectionEnabled = true;
    dto.repositories = Collections.singletonList(repoDto);

    assertThatExceptionOfType(InvalidRepositoryException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("Namespace Confusion Protection can be enabled only for hosted repositories.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_Quarantine() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    ApiRepositoryDTO repoDto = new ApiRepositoryDTO();
    repoDto.publicId = "publicId";
    repoDto.type = "proxy";
    repoDto.quarantineEnabled = true;
    dto.repositories = Collections.singletonList(repoDto);

    assertThatExceptionOfType(InvalidRepositoryException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("Quarantine requires Audit to be enabled.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_ProxyPolicyCompliantComponentSelection() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    ApiRepositoryDTO repoDto = new ApiRepositoryDTO();
    repoDto.publicId = "publicId";
    repoDto.type = "proxy";
    repoDto.policyCompliantComponentSelectionEnabled = true;
    dto.repositories = Collections.singletonList(repoDto);

    assertThatExceptionOfType(InvalidRepositoryException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("Policy Compliant Component Selection requires Audit and Quarantine to be enabled.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_ChangeType() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "proxy");
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    ApiRepositoryDTO repoDto = ApiRepositoryDTO.fromRepository(repository);
    repoDto.type = "hosted";
    dto.repositories = Collections.singletonList(repoDto);

    assertThatExceptionOfType(InvalidRepositoryException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("Cannot change the repository type.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_ChangeFormat() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "proxy");
    repository.setFormat("npm");
    repositoryDAO.update(repository);
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    ApiRepositoryDTO repoDto = ApiRepositoryDTO.fromRepository(repository);
    repoDto.format = "conan";
    dto.repositories = Collections.singletonList(repoDto);

    assertThatExceptionOfType(InvalidRepositoryException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("Cannot change the repository format.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_HostedAudit() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    ApiRepositoryDTO repoDto = new ApiRepositoryDTO();
    repoDto.publicId = "publicId";
    repoDto.type = "hosted";
    repoDto.auditEnabled = true;
    dto.repositories = Collections.singletonList(repoDto);

    assertThatExceptionOfType(InvalidRepositoryException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("Audit can be enabled only for proxy repositories.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_HostedQuarantine() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    ApiRepositoryDTO repoDto = new ApiRepositoryDTO();
    repoDto.publicId = "publicId";
    repoDto.type = "hosted";
    repoDto.quarantineEnabled = true;
    dto.repositories = Collections.singletonList(repoDto);

    assertThatExceptionOfType(InvalidRepositoryException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("Quarantine can be enabled only for proxy repositories.");
  }

  @Test
  public void testConfigureRepositories_ApiRepositoryListDTO_HostedPolicyCompliantComponentSelection() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    ApiRepositoryDTO repoDto = new ApiRepositoryDTO();
    repoDto.publicId = "publicId";
    repoDto.type = "hosted";
    repoDto.policyCompliantComponentSelectionEnabled = true;
    dto.repositories = Collections.singletonList(repoDto);

    assertThatExceptionOfType(InvalidRepositoryException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories(repositoryManager.getId(), dto))
        .withMessageContaining("Policy Compliant Component Selection can be enabled only for proxy repositories.");
  }

  @Test
  public void testConfigureRepositories_RepositoryManagerIdNotFound() {
    Repository repository = tempEntity.newRepository();
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    dto.repositories = Collections.singletonList(ApiRepositoryDTO.fromRepository(repository));
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiFirewallService.configureRepositories("doesNotExist", dto))
        .withMessageContaining("RepositoryManager with ID doesNotExist does not exist.");
  }

  @Test
  public void testConfigureRepositories() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository proxyRepository = tempEntity.newRepository(repositoryManager, "r1");
    proxyRepository.setFormat("maven2");
    proxyRepository.setAuditEnabled(false);
    repositoryDAO.update(proxyRepository);

    Repository hostedRepository =
        tempEntity.newHostedRepository(repositoryManager, "r2", "maven2", false);

    // No changes
    configureAndAssertRepositories(proxyRepository, null, hostedRepository, null);

    // Update audit
    proxyRepository.setAuditEnabled(true);
    configureAndAssertRepositories(proxyRepository, new Date(), hostedRepository, null);

    // Update quarantine
    proxyRepository.setQuarantineEnabled(true);
    configureAndAssertRepositories(proxyRepository, new Date(), hostedRepository, null);

    // Update policy compliant component selection
    proxyRepository.setPolicyCompliantComponentSelectionEnabled(true);
    configureAndAssertRepositories(proxyRepository, new Date(), hostedRepository, null);

    // Update namespace confusion protection
    hostedRepository.setNamespaceConfusionProtectionEnabled(true);
    configureAndAssertRepositories(proxyRepository, null, hostedRepository, new Date());
  }

  @Test
  public void testGetRepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    ApiRepositoryManagerDTO apiRepositoryManagerDTO =
        apiFirewallService.getRepositoryManager(repositoryManager.getId());

    assertThat(apiRepositoryManagerDTO.id).isEqualTo(repositoryManager.getId());
    assertThat(apiRepositoryManagerDTO.instanceId).isEqualTo(repositoryManager.getInstanceId());
    assertThat(apiRepositoryManagerDTO.name).isEqualTo(repositoryManager.getName());
    assertThat(apiRepositoryManagerDTO.productName).isEqualTo(repositoryManager.getProductName());
    assertThat(apiRepositoryManagerDTO.productVersion).isEqualTo(repositoryManager.getProductVersion());
  }

  @Test
  public void testDeleteRepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    apiFirewallService.deleteRepositoryManager(repositoryManager.getId());

    assertThat(repositoryManagerDAO.getById(repositoryManager.getId())).isNull();
  }

  @Test
  public void testDeleteRepositoryManager_WithRelatedOrgAndApp() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "repo");

    Organization repoManOrg = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(repoManOrg.getId());

    Organization repoOrg = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(repoOrg.getId());

    repositoryManager.setRelatedOrganizationId(repoManOrg.getId());
    repositoryManagerDAO.update(repositoryManager);

    repository.setRelatedOrganizationId(repoOrg.getId());
    repositoryDAO.update(repository);

    // Sanity check
    assertThat(repositoryManager.getRelatedOrganizationId()).isNotNull();
    assertThat(repository.getRelatedOrganizationId()).isNotNull();

    apiFirewallService.deleteRepositoryManager(repositoryManager.getId());

    assertThat(repositoryManagerDAO.getById(repositoryManager.getId())).isNull();
    assertThat(repositoryDAO.getById(repository.getId())).isNull();
    assertThat(organizationDAO.getById(repoManOrg.getId())).isNull();
    assertThat(organizationDAO.getById(repoOrg.getId())).isNull();
    assertThat(applicationDAO.getById(app1.getId())).isNull();
    assertThat(applicationDAO.getById(app2.getId())).isNull();
  }

  @Test
  public void testAddRepositoryManager() throws Exception {
    ApiRepositoryManagerDTO apiRepositoryManagerDTO = new ApiRepositoryManagerDTO();
    apiRepositoryManagerDTO.instanceId = "testInstanceId";
    apiRepositoryManagerDTO.name = "testName";
    apiRepositoryManagerDTO.productName = "testProductName";
    apiRepositoryManagerDTO.productVersion = "testProductVersion";

    apiRepositoryManagerDTO = apiFirewallService.addRepositoryManager(apiRepositoryManagerDTO);

    // Assert the repository manager data in the response
    assertThat(apiRepositoryManagerDTO.id).isNotNull();
    assertThat(apiRepositoryManagerDTO.instanceId).isEqualTo("testInstanceId");
    assertThat(apiRepositoryManagerDTO.name).isEqualTo("testName");
    assertThat(apiRepositoryManagerDTO.productName).isEqualTo("testProductName");
    assertThat(apiRepositoryManagerDTO.productVersion).isEqualTo("testProductVersion");

    // Assert the repository manager data in the db
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(apiRepositoryManagerDTO.id);
    assertThat(repositoryManager.getInstanceId()).isEqualTo("testInstanceId");
    assertThat(repositoryManager.getName()).isEqualTo("testName");
    assertThat(repositoryManager.getProductName()).isEqualTo("testProductName");
    assertThat(repositoryManager.getProductVersion()).isEqualTo("testProductVersion");
  }

  @Test
  public void testAddRepositoryManager_CannotSpecifyRepositoryManagerId() throws Exception {
    ApiRepositoryManagerDTO apiRepositoryManagerDTO = new ApiRepositoryManagerDTO();
    apiRepositoryManagerDTO.id = "testId";
    apiRepositoryManagerDTO.instanceId = "testInstanceId";
    apiRepositoryManagerDTO.name = "testName";
    apiRepositoryManagerDTO.productName = "testProductName";
    apiRepositoryManagerDTO.productVersion = "testProductVersion";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> {
          apiFirewallService.addRepositoryManager(apiRepositoryManagerDTO);
        }).withMessageContaining("The repository manager ID must be null.");
  }

  @Test
  public void testAddRepositoryManager_NoFirewallFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    ApiRepositoryManagerDTO apiRepositoryManagerDTO = new ApiRepositoryManagerDTO();
    apiRepositoryManagerDTO.id = "testId";
    apiRepositoryManagerDTO.instanceId = "testInstanceId";
    apiRepositoryManagerDTO.name = "testName";
    apiRepositoryManagerDTO.productName = "testProductName";
    apiRepositoryManagerDTO.productVersion = "testProductVersion";

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      apiFirewallService.addRepositoryManager(apiRepositoryManagerDTO);
    });
  }

  @Test
  public void testGetRepositoryContainer() {
    ApiRepositoryContainerDTO apiRepositoryContainerDTO = apiFirewallService.getRepositoryContainer();

    assertThat(apiRepositoryContainerDTO.id).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(apiRepositoryContainerDTO.name).isEqualTo(RepositoryContainer.SINGLETON.getName());
  }

  @Test
  public void testGetQuarantinedComponents() {
    /*
    This test data covers all below scenarios.
    1. sort by quarantine time (default desc)
    2. sort by threat level for same quarantine times
    3. sort by component name for same threat level & quarantine times
    4. sort by quarantine time asc
    5. excluding waived violations, 'warn' action policies and unquarantined components
    6. selecting the valid highest threat level and policy name combination for a quarantined component
    7. policy id and component name filters
    */
    Date jan1st2024hour12 = Date.from(LocalDateTime.of(2024, 1, 1, 12, 0).toInstant(ZoneOffset.UTC));
    Date jan1st2024hour14 = Date.from(LocalDateTime.of(2024, 1, 1, 14, 0).toInstant(ZoneOffset.UTC));
    Date jan2nd2024hour12 = Date.from(LocalDateTime.of(2024, 1, 2, 12, 0).toInstant(ZoneOffset.UTC));
    Date jan2nd2024hour14 = Date.from(LocalDateTime.of(2024, 1, 2, 14, 0).toInstant(ZoneOffset.UTC));

    Policy policy1 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy1", 10, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Policy policy2 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy2", 10, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Policy policy3 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy3", 7, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Policy policy4 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy4", 10, Action.ID_WARN,
        Stage.ID_PROXY, null);
    RepositoryManager rm1 = tempEntity.newRepositoryManager();
    RepositoryManager rm2 = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository(rm1, "repo1", true, true);
    Repository repo2 = tempEntity.newRepository(rm2, "repo2", true, true);
    final RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repo1.getId(), "pathname1", jan2nd2024hour14, null);
    final RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repo2.getId(), "pathname2", jan2nd2024hour14, null);
    final RepositoryComponent c3 =
        tempEntity.newRepositoryComponent(repo1.getId(), "pathname3", jan2nd2024hour12, null);
    final RepositoryComponent c4 =
        tempEntity.newRepositoryComponent(repo1.getId(), "pathname4", jan1st2024hour12, null);
    final RepositoryComponent c5 =
        tempEntity.newRepositoryComponent(repo2.getId(), MatchState.EXACT, "pathname5", "hash",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), jan1st2024hour12, jan1st2024hour12);
    tempEntity.newRepositoryComponent(repo2.getId(), "pathname6", jan1st2024hour14, jan1st2024hour12);
    PolicyViolationTestHelper.createPolicyViolationFail(policy1, c1, tempEntity);
    RepositoryPolicyViolation violation1 = PolicyViolationTestHelper.createPolicyViolationFail(policy2, c1, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationFail(policy3, c1, tempEntity);
    RepositoryPolicyViolation violation2 = PolicyViolationTestHelper.createPolicyViolationFail(policy3, c2, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationWarn(policy4, c2, tempEntity);
    RepositoryPolicyViolation violation3 = PolicyViolationTestHelper.createPolicyViolationFail(policy1, c3, tempEntity);
    RepositoryPolicyViolation violation4 = PolicyViolationTestHelper.createPolicyViolationFail(policy3, c4, tempEntity);
    PolicyViolationTestHelper.createPolicyViolationWaived(policy2, c5, tempEntity);
    RepositoryPolicyViolation violation5 = PolicyViolationTestHelper.createPolicyViolationFail(policy3, c5, tempEntity);

    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.QUARANTINE,
            FirewallSortableField.QUARANTINE_TIME, false,
            Collections.emptyList());

    // EXECUTE
    ApiPageResult<ApiFirewallQuarantinedComponentDto> details =
        apiFirewallService.getQuarantinedComponents(filter);

    // VERIFY
    assertThat(details.getTotal()).isEqualTo(5);
    assertThat(details.getResults()).hasSize(5);
    assertFirewallQuarantinedDetails(repo1, c1, violation1, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo2, c2, violation2, details.getResults().get(1));
    assertFirewallQuarantinedDetails(repo1, c3, violation3, details.getResults().get(2));
    assertFirewallQuarantinedDetails(repo2, c5, violation4, details.getResults().get(3));
    assertFirewallQuarantinedDetails(repo1, c4, violation5, details.getResults().get(4));

    filter.asc = true;

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    // VERIFY
    assertThat(details.getTotal()).isEqualTo(5);
    assertThat(details.getResults()).hasSize(5);

    assertFirewallQuarantinedDetails(repo2, c5, violation4, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo1, c4, violation5, details.getResults().get(1));
    assertFirewallQuarantinedDetails(repo1, c3, violation3, details.getResults().get(2));
    assertFirewallQuarantinedDetails(repo1, c1, violation1, details.getResults().get(3));
    assertFirewallQuarantinedDetails(repo2, c2, violation2, details.getResults().get(4));

    List<FirewallFilterField> filterFields =
        Arrays.asList(new FirewallFilterField(FirewallFilterableField.POLICY_ID, policy3.getId()),
            new FirewallFilterField(FirewallFilterableField.COMPONENT_NAME, c5.getDisplayName()));
    filter.filterFields = filterFields;

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    // VERIFY
    assertThat(details.getTotal()).isEqualTo(1);
    assertThat(details.getResults()).hasSize(1);
    assertFirewallQuarantinedDetails(repo2, c5, violation5, details.getResults().get(0));
  }

  @Test
  public void testGetQuarantinedComponents_filterByPolicyIdAndComponentName() {
    Date june1st2022 = Date.from(LocalDateTime.of(2022, 6, 1, 1, 0).toInstant(ZoneOffset.UTC));
    Date june2nd2022 = Date.from(LocalDateTime.of(2022, 6, 2, 1, 0).toInstant(ZoneOffset.UTC));
    Date june3rd2022 = Date.from(LocalDateTime.of(2022, 6, 3, 1, 0).toInstant(ZoneOffset.UTC));
    Date june4th2022 = Date.from(LocalDateTime.of(2022, 6, 4, 1, 0).toInstant(ZoneOffset.UTC));
    Date june5th2022 = Date.from(LocalDateTime.of(2022, 6, 5, 1, 0).toInstant(ZoneOffset.UTC));
    Date june6th2022 = Date.from(LocalDateTime.of(2022, 6, 6, 1, 0).toInstant(ZoneOffset.UTC));

    Policy policy1 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy1", 10, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Policy policy2 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy2", 9, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Policy policy3 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy3", 8, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Policy policy4 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy4", 7, Action.ID_FAIL,
        Stage.ID_PROXY, null);

    RepositoryManager rm1 = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository(rm1, "repo1", true, true);

    final RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repo1.getId(), MatchState.EXACT, "pathname1", "hash",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), june1st2022, june1st2022);
    final RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repo1.getId(), MatchState.EXACT, "pathname2", "hash",
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v1"), june2nd2022, june2nd2022);
    final RepositoryComponent c3 =
        tempEntity.newRepositoryComponent(repo1.getId(), MatchState.EXACT, "pathname3", "hash",
            ComponentIdentifier.createMavenCoordinates("g1", "a3", "v1"), june3rd2022, june3rd2022);
    final RepositoryComponent c4 =
        tempEntity.newRepositoryComponent(repo1.getId(), MatchState.EXACT, "pathname4", "hash",
            ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"), june4th2022, june4th2022);
    final RepositoryComponent c5 =
        tempEntity.newRepositoryComponent(repo1.getId(), MatchState.EXACT, "pathname5", "hash",
            ComponentIdentifier.createMavenCoordinates("g5", "a5", "v5"), june5th2022, june5th2022);
    final RepositoryComponent c6 =
        tempEntity.newRepositoryComponent(repo1.getId(), MatchState.EXACT, "pathname6", "hash",
            ComponentIdentifier.createMavenCoordinates("g6", "a6", "v6"), june6th2022, june6th2022);

    RepositoryPolicyViolation violation1 = PolicyViolationTestHelper.createPolicyViolationFail(policy1, c1, tempEntity);
    RepositoryPolicyViolation violation2 = PolicyViolationTestHelper.createPolicyViolationFail(policy2, c2, tempEntity);
    RepositoryPolicyViolation violation3 = PolicyViolationTestHelper.createPolicyViolationFail(policy3, c3, tempEntity);
    RepositoryPolicyViolation violation4 = PolicyViolationTestHelper.createPolicyViolationFail(policy4, c4, tempEntity);
    RepositoryPolicyViolation violation5 = PolicyViolationTestHelper.createPolicyViolationFail(policy4, c5, tempEntity);
    RepositoryPolicyViolation violation6 = PolicyViolationTestHelper.createPolicyViolationFail(policy4, c6, tempEntity);

    // FILTER BY MULTIPLE POLICY IDS
    List<FirewallFilterField> filterFields = new ArrayList<>();
    filterFields.add(new FirewallFilterField(FirewallFilterableField.POLICY_ID,
        Sets.newHashSet(policy1.getId(), policy3.getId())));

    FirewallRepositoryComponentFilter filter = new FirewallRepositoryComponentFilter(1, 10,
        FirewallComponentFilterState.QUARANTINE, FirewallSortableField.QUARANTINE_TIME, false, filterFields);

    // EXECUTE
    ApiPageResult<ApiFirewallQuarantinedComponentDto> details = apiFirewallService.getQuarantinedComponents(filter);

    assertThat(details.getTotal()).isEqualTo(2);
    assertThat(details.getResults()).hasSize(2);
    assertFirewallQuarantinedDetails(repo1, c3, violation3, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo1, c1, violation1, details.getResults().get(1));

    // SORT BY QUARANTINE TIME ASC
    filter.asc = true;

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    assertThat(details.getTotal()).isEqualTo(2);
    assertThat(details.getResults()).hasSize(2);
    assertFirewallQuarantinedDetails(repo1, c1, violation1, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo1, c3, violation3, details.getResults().get(1));

    // FILTER BY COMPONENT NAME
    filterFields.remove(0);
    filterFields.add(new FirewallFilterField(FirewallFilterableField.COMPONENT_NAME, "g2"));
    filter.filterFields = filterFields;
    filter.asc = false;

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    assertThat(details.getTotal()).isEqualTo(1);
    assertThat(details.getResults()).hasSize(1);
    assertFirewallQuarantinedDetails(repo1, c2, violation2, details.getResults().get(0));

    // FILTER BY MULTIPLE POLICY IDS AND COMPONENT NAME
    filterFields.remove(0);
    filterFields.add(new FirewallFilterField(FirewallFilterableField.POLICY_ID,
        Sets.newHashSet(policy1.getId(), policy2.getId())));
    filterFields.add(new FirewallFilterField(FirewallFilterableField.COMPONENT_NAME, "v1"));
    filter.filterFields = filterFields;
    filter.asc = false;

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    assertThat(details.getTotal()).isEqualTo(2);
    assertThat(details.getResults()).hasSize(2);
    assertFirewallQuarantinedDetails(repo1, c2, violation2, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo1, c1, violation1, details.getResults().get(1));

    // SORT BY QUARANTINE TIME ASC
    filter.asc = true;

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    assertThat(details.getTotal()).isEqualTo(2);
    assertThat(details.getResults()).hasSize(2);
    assertFirewallQuarantinedDetails(repo1, c1, violation1, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo1, c2, violation2, details.getResults().get(1));

    // FILTER BY MULTIPLE POLICY IDS AND SET PAGE SIZE TO 3 TO TEST PAGED RESULTS
    filterFields.clear();
    filterFields.add(new FirewallFilterField(FirewallFilterableField.POLICY_ID,
        Sets.newHashSet(policy1.getId(), policy2.getId(), policy4.getId())));
    filter.filterFields = filterFields;
    filter.asc = false;
    filter.pageSize = 3;

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    assertThat(details.getTotal()).isEqualTo(5);
    assertThat(details.getResults()).hasSize(3);
    assertFirewallQuarantinedDetails(repo1, c6, violation6, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo1, c5, violation5, details.getResults().get(1));
    assertFirewallQuarantinedDetails(repo1, c4, violation4, details.getResults().get(2));

    // GET PAGE 2
    filter.page = 2;

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    assertThat(details.getTotal()).isEqualTo(5);
    assertThat(details.getResults()).hasSize(2);
    assertFirewallQuarantinedDetails(repo1, c2, violation2, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo1, c1, violation1, details.getResults().get(1));

    // SORT BY QUARANTINE TIME ASC
    filter.asc = true;

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    assertThat(details.getTotal()).isEqualTo(5);
    assertThat(details.getResults()).hasSize(2);
    assertFirewallQuarantinedDetails(repo1, c5, violation5, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo1, c6, violation6, details.getResults().get(1));
  }

  @Test
  public void testGetQuarantinedComponents_sortAndFilterByRepoPublicId() {
    Date jan1st2024hour12 = Date.from(LocalDateTime.of(2024, 1, 1, 12, 0).toInstant(ZoneOffset.UTC));
    Date jan2nd2024hour12 = Date.from(LocalDateTime.of(2024, 1, 2, 12, 0).toInstant(ZoneOffset.UTC));
    Policy policy1 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy1", 10, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    RepositoryManager rm1 = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository(rm1, "repo1", true, true);
    Repository repo2 = tempEntity.newRepository(rm1, "repo2", true, true);

    final RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repo1.getId(), MatchState.EXACT, "pathname1", "hash1",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), jan1st2024hour12, jan1st2024hour12);
    final RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repo2.getId(), MatchState.EXACT, "pathname2", "hash2",
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v1"), jan1st2024hour12, jan1st2024hour12);
    final RepositoryComponent c3 =
        tempEntity.newRepositoryComponent(repo2.getId(), MatchState.EXACT, "pathname3", "hash3",
            ComponentIdentifier.createMavenCoordinates("g1", "a3", "v1"), jan2nd2024hour12, jan2nd2024hour12);

    RepositoryPolicyViolation violation1 = PolicyViolationTestHelper.createPolicyViolationFail(policy1, c1, tempEntity);
    RepositoryPolicyViolation violation2 = PolicyViolationTestHelper.createPolicyViolationFail(policy1, c2, tempEntity);
    RepositoryPolicyViolation violation3 = PolicyViolationTestHelper.createPolicyViolationFail(policy1, c3, tempEntity);

    // SORT BY REPOSITORY PUBLIC ID DESC
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.QUARANTINE,
            FirewallSortableField.REPOSITORY_PUBLIC_ID, false,
            Collections.emptyList());

    // EXECUTE
    ApiPageResult<ApiFirewallQuarantinedComponentDto> details =
        apiFirewallService.getQuarantinedComponents(filter);

    // VERIFY
    assertThat(details.getTotal()).isEqualTo(3);
    assertThat(details.getResults()).hasSize(3);
    assertFirewallQuarantinedDetails(repo2, c3, violation3, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo2, c2, violation2, details.getResults().get(1));
    assertFirewallQuarantinedDetails(repo1, c1, violation1, details.getResults().get(2));

    // SORT BY REPOSITORY PUBLIC ID ASC
    filter.asc = true;

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    // VERIFY
    assertThat(details.getTotal()).isEqualTo(3);
    assertThat(details.getResults()).hasSize(3);
    assertFirewallQuarantinedDetails(repo1, c1, violation1, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo2, c3, violation3, details.getResults().get(1));
    assertFirewallQuarantinedDetails(repo2, c2, violation2, details.getResults().get(2));

    // FILTER BY REPOSITORY PUBLIC ID
    filter.filterFields =
        Arrays.asList(new FirewallFilterField(FirewallFilterableField.REPOSITORY_PUBLIC_ID, repo1.getPublicId()));

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    // VERIFY
    assertThat(details.getTotal()).isEqualTo(1);
    assertThat(details.getResults()).hasSize(1);
    assertFirewallQuarantinedDetails(repo1, c1, violation1, details.getResults().get(0));

    // FILTER BY REPOSITORY PUBLIC ID, POLICY ID AND COMPONENT NAME
    filter.filterFields =
        Arrays.asList(new FirewallFilterField(FirewallFilterableField.REPOSITORY_PUBLIC_ID, repo2.getPublicId()),
            new FirewallFilterField(FirewallFilterableField.POLICY_ID, policy1.getId()),
            new FirewallFilterField(FirewallFilterableField.COMPONENT_NAME, c2.getDisplayName()));

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    // VERIFY
    assertThat(details.getTotal()).isEqualTo(1);
    assertThat(details.getResults()).hasSize(1);
    assertFirewallQuarantinedDetails(repo2, c2, violation2, details.getResults().get(0));
  }

  @Test
  public void testGetQuarantinedComponents_sortByPolicyName() {
    Date jan1st2024hour12 = Date.from(LocalDateTime.of(2024, 1, 1, 12, 0).toInstant(ZoneOffset.UTC));
    Date jan2nd2024hour12 = Date.from(LocalDateTime.of(2024, 1, 2, 12, 0).toInstant(ZoneOffset.UTC));
    Policy policy1 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy1", 8, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Policy policy2 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy2", 10, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Policy policy3 = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy3", 5, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    final RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT, "pathname1", "hash1",
            ComponentIdentifier.createMavenCoordinates("g10", "a11", "v12"), jan1st2024hour12, jan1st2024hour12);
    final RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT, "pathname2", "hash2",
            ComponentIdentifier.createMavenCoordinates("g20", "a21", "v22"), jan1st2024hour12, jan1st2024hour12);
    final RepositoryComponent c3 =
        tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT, "pathname3", "hash3",
            ComponentIdentifier.createMavenCoordinates("g30", "a31", "v32"), jan2nd2024hour12, jan2nd2024hour12);
    RepositoryPolicyViolation violation1 = PolicyViolationTestHelper.createPolicyViolationFail(policy3, c1, tempEntity);
    RepositoryPolicyViolation violation2 = PolicyViolationTestHelper.createPolicyViolationFail(policy1, c2, tempEntity);
    RepositoryPolicyViolation violation3 = PolicyViolationTestHelper.createPolicyViolationFail(policy2, c3, tempEntity);

    // SORT BY POLICY NAME DESC
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.QUARANTINE,
            FirewallSortableField.POLICY_NAME, false,
            Collections.emptyList());

    // EXECUTE
    ApiPageResult<ApiFirewallQuarantinedComponentDto> details =
        apiFirewallService.getQuarantinedComponents(filter);

    // VERIFY
    assertThat(details.getTotal()).isEqualTo(3);
    assertThat(details.getResults()).hasSize(3);
    assertFirewallQuarantinedDetails(repo, c1, violation1, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo, c3, violation3, details.getResults().get(1));
    assertFirewallQuarantinedDetails(repo, c2, violation2, details.getResults().get(2));

    // SORT BY POLICY NAME ASC
    filter.asc = true;

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    // VERIFY
    assertThat(details.getTotal()).isEqualTo(3);
    assertThat(details.getResults()).hasSize(3);
    assertFirewallQuarantinedDetails(repo, c2, violation2, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo, c3, violation3, details.getResults().get(1));
    assertFirewallQuarantinedDetails(repo, c1, violation1, details.getResults().get(2));

    // DEFAULT: SORT BY QUARANTINE TIME DESC
    filter = new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.QUARANTINE,
        FirewallSortableField.QUARANTINE_TIME, false, Collections.emptyList());

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    // VERIFY
    assertThat(details.getTotal()).isEqualTo(3);
    assertThat(details.getResults()).hasSize(3);
    assertFirewallQuarantinedDetails(repo, c3, violation3, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo, c2, violation2, details.getResults().get(1));
    assertFirewallQuarantinedDetails(repo, c1, violation1, details.getResults().get(2));
  }

  @Test
  public void testGetQuarantinedComponents_sortByComponentDisplayName() {
    Date jan1st2024hour12 = Date.from(LocalDateTime.of(2024, 1, 1, 12, 0).toInstant(ZoneOffset.UTC));
    Policy policy = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy1", 10, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    final RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT, "pathname1", "hash1",
            ComponentIdentifier.createMavenCoordinates("g01", "a01", "v01"), jan1st2024hour12, jan1st2024hour12);
    final RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT, "pathname2", "hash2",
            ComponentIdentifier.createMavenCoordinates("g02", "a02", "v02"), jan1st2024hour12, jan1st2024hour12);
    final RepositoryComponent c3 =
        tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT, "pathname3", "hash3",
            ComponentIdentifier.createMavenCoordinates("g03", "a03", "v03"), new Date(), new Date());
    RepositoryPolicyViolation violation1 = PolicyViolationTestHelper.createPolicyViolationFail(policy, c1, tempEntity);
    RepositoryPolicyViolation violation2 = PolicyViolationTestHelper.createPolicyViolationFail(policy, c2, tempEntity);
    RepositoryPolicyViolation violation3 = PolicyViolationTestHelper.createPolicyViolationFail(policy, c3, tempEntity);

    // SORT BY COMPONENT DISPLAY NAME DESC
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.QUARANTINE,
            FirewallSortableField.COMPONENT_DISPLAY_NAME, false,
            Collections.emptyList());

    // EXECUTE
    ApiPageResult<ApiFirewallQuarantinedComponentDto> details =
        apiFirewallService.getQuarantinedComponents(filter);

    // VERIFY
    assertThat(details.getTotal()).isEqualTo(3);
    assertThat(details.getResults()).hasSize(3);
    assertFirewallQuarantinedDetails(repo, c3, violation3, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo, c2, violation2, details.getResults().get(1));
    assertFirewallQuarantinedDetails(repo, c1, violation1, details.getResults().get(2));

    // SORT BY COMPONENT DISPLAY NAME ASC
    filter.asc = true;

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    // VERIFY
    assertThat(details.getTotal()).isEqualTo(3);
    assertThat(details.getResults()).hasSize(3);
    assertFirewallQuarantinedDetails(repo, c1, violation1, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo, c2, violation2, details.getResults().get(1));
    assertFirewallQuarantinedDetails(repo, c3, violation3, details.getResults().get(2));

    // DEFAULT: SORT BY QUARANTINE TIME DESC COMPONENT DISPLAY NAME DESC
    filter = new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.QUARANTINE,
        FirewallSortableField.QUARANTINE_TIME, false, Collections.emptyList());

    // EXECUTE
    details = apiFirewallService.getQuarantinedComponents(filter);

    // VERIFY
    assertThat(details.getTotal()).isEqualTo(3);
    assertThat(details.getResults()).hasSize(3);
    assertFirewallQuarantinedDetails(repo, c3, violation3, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo, c2, violation2, details.getResults().get(1));
    assertFirewallQuarantinedDetails(repo, c1, violation1, details.getResults().get(2));
  }

  @Test
  public void testGetQuarantinedComponents_sortAndFilterByQuarantineTime() {
    // Test #1 - No filters, sort by QUARANTINE_TIME desc
    // Arrange
    Instant past1Days = Instant.now().minus(1, ChronoUnit.DAYS);
    Instant past7Days = Instant.now().minus(7, ChronoUnit.DAYS);

    Date past1DaysQuarantineTime = Date.from(past1Days);
    Date past7DaysQuarantineTime = Date.from(past7Days);

    RepositoryManager rm1 = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository(rm1, "repo1", true, true);
    Repository repo2 = tempEntity.newRepository(rm1, "repo2", true, true);

    final RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repo1.getId(), MatchState.EXACT, "pathname1", "hash1",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), past7DaysQuarantineTime,
            past7DaysQuarantineTime);
    final RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repo2.getId(), MatchState.EXACT, "pathname2", "hash2",
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v1"), past7DaysQuarantineTime,
            past7DaysQuarantineTime);
    final RepositoryComponent c3 =
        tempEntity.newRepositoryComponent(repo2.getId(), MatchState.EXACT, "pathname3", "hash3",
            ComponentIdentifier.createMavenCoordinates("g1", "a3", "v1"), past1DaysQuarantineTime,
            past1DaysQuarantineTime);

    Policy policy = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policy", 10, Action.ID_FAIL,
        Stage.ID_PROXY, null);
    RepositoryPolicyViolation violation1 = PolicyViolationTestHelper.createPolicyViolationFail(policy, c1, tempEntity);
    RepositoryPolicyViolation violation2 = PolicyViolationTestHelper.createPolicyViolationFail(policy, c2, tempEntity);
    RepositoryPolicyViolation violation3 = PolicyViolationTestHelper.createPolicyViolationFail(policy, c3, tempEntity);

    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.QUARANTINE,
            FirewallSortableField.QUARANTINE_TIME, false, List.of());

    // Act
    ApiPageResult<ApiFirewallQuarantinedComponentDto> details =
        apiFirewallService.getQuarantinedComponents(filter);

    assertThat(details.getTotal()).isEqualTo(3);
    assertThat(details.getResults()).hasSize(3);
    assertFirewallQuarantinedDetails(repo2, c3, violation3, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo2, c2, violation2, details.getResults().get(1));
    assertFirewallQuarantinedDetails(repo1, c1, violation1, details.getResults().get(2));

    // Test #2 - No filters, sort by QUARANTINE_TIME asc
    // Arrange
    filter.asc = true;

    // Act
    details = apiFirewallService.getQuarantinedComponents(filter);

    assertThat(details.getTotal()).isEqualTo(3);
    assertThat(details.getResults()).hasSize(3);
    assertFirewallQuarantinedDetails(repo2, c2, violation2, details.getResults().get(0));
    assertFirewallQuarantinedDetails(repo1, c1, violation1, details.getResults().get(1));
    assertFirewallQuarantinedDetails(repo2, c3, violation3, details.getResults().get(2));

    // Test #3 - Filter and sort by QUARANTINE_TIME asc
    // Arrange
    String quarantineTimeFilter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .format(LocalDateTime.ofInstant(past1Days, ZoneId.systemDefault()));
    filter.filterFields =
        List.of(new FirewallFilterField(FirewallFilterableField.QUARANTINE_TIME, quarantineTimeFilter));

    // Act
    details = apiFirewallService.getQuarantinedComponents(filter);

    assertThat(details.getTotal()).isEqualTo(1);
    assertThat(details.getResults()).hasSize(1);
    assertFirewallQuarantinedDetails(repo2, c3, violation3, details.getResults().get(0));

    // Test #4 - Filter by all fields and sort by QUARANTINE_TIME asc
    // Arrange
    quarantineTimeFilter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .format(LocalDateTime.ofInstant(past7Days, ZoneId.systemDefault()));
    filter.filterFields =
        List.of(new FirewallFilterField(FirewallFilterableField.REPOSITORY_PUBLIC_ID, repo2.getPublicId()),
            new FirewallFilterField(FirewallFilterableField.POLICY_ID, policy.getId()),
            new FirewallFilterField(FirewallFilterableField.COMPONENT_NAME, c2.getDisplayName()),
            new FirewallFilterField(FirewallFilterableField.QUARANTINE_TIME, quarantineTimeFilter));

    // Act
    details = apiFirewallService.getQuarantinedComponents(filter);

    assertThat(details.getTotal()).isEqualTo(1);
    assertThat(details.getResults()).hasSize(1);
    assertFirewallQuarantinedDetails(repo2, c2, violation2, details.getResults().get(0));
  }

  @Test
  public void testGetQuarantinedComponents_invalid() {
    // null firewallComponentFilterState
    final FirewallRepositoryComponentFilter filter1 =
        new FirewallRepositoryComponentFilter(1, 10, null, FirewallSortableField.QUARANTINE_TIME, true,
            Collections.emptyList());

    // EXECUTE
    assertThatThrownBy(() -> apiFirewallService.getQuarantinedComponents(filter1))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("firewallComponentFilterState is required and cannot be null.");

    // invalid firewallComponentFilterState
    final FirewallRepositoryComponentFilter filter2 =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.UNQUARANTINE_AUTO,
            FirewallSortableField.QUARANTINE_TIME, true,
            Collections.emptyList());

    // EXECUTE
    assertThatThrownBy(() -> apiFirewallService.getQuarantinedComponents(filter2))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("FilterState UNQUARANTINE_AUTO is not applicable to get Firewall Quarantined components.");

    // invalid sortableField
    final FirewallRepositoryComponentFilter filter3 =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.QUARANTINE,
            FirewallSortableField.RELEASE_QUARANTINE_TIME, true, Collections.emptyList());

    // EXECUTE
    assertThatThrownBy(() -> apiFirewallService.getQuarantinedComponents(filter3))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("SortableField releaseQuarantineTime is not applicable to get Firewall Quarantined components.");

    // invalid page
    final FirewallRepositoryComponentFilter filter4 =
        new FirewallRepositoryComponentFilter(0, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO,
            FirewallSortableField.QUARANTINE_TIME, true,
            Collections.emptyList());

    // EXECUTE
    assertThatThrownBy(() -> apiFirewallService.getQuarantinedComponents(filter4))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid page: 0. Page shouldn't be lower than 1");

    // invalid pageSize
    final FirewallRepositoryComponentFilter filter5 =
        new FirewallRepositoryComponentFilter(1, 0, FirewallComponentFilterState.UNQUARANTINE_AUTO,
            FirewallSortableField.QUARANTINE_TIME, true,
            Collections.emptyList());

    // EXECUTE
    assertThatThrownBy(() -> apiFirewallService.getQuarantinedComponents(filter5))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid page size: 0. Page size should be between 1 and 10000");
  }

  private ApiRepositoryListDTO createApiRepositoryListDTO(Repository... repositories) {
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    dto.repositories = Arrays.stream(repositories).map(ApiRepositoryDTO::fromRepository).collect(Collectors.toList());
    return dto;
  }

  private void configureAndAssertRepositories(Object... repositoriesAndUpdatedAfterDate) {
    apiFirewallService.configureRepositories(
        ((Repository) repositoriesAndUpdatedAfterDate[0]).getRepositoryManagerId(),
        createApiRepositoryListDTO(
            Arrays.stream(repositoriesAndUpdatedAfterDate)
                .filter(o -> o instanceof Repository)
                .toArray(Repository[]::new)
        )
    );
    for (int i = 0; i < repositoriesAndUpdatedAfterDate.length; i += 2) {
      Repository repository = (Repository) repositoriesAndUpdatedAfterDate[i];
      Date updatedAfterDate = (Date) repositoriesAndUpdatedAfterDate[i + 1];
      Repository storedRepository = repositoryDAO.getById(repository.getId());
      assertThat(repository)
          .usingRecursiveComparison()
          .ignoringFields(ArrayUtils.add(JPA.IGNORE_FIELDS, "lastManualConfigureTime"))
          .isEqualTo(storedRepository);
      if (updatedAfterDate != null) {
        assertThat(storedRepository.getLastManualConfigureTime()).isAfterOrEqualTo(updatedAfterDate);
      }
    }
  }
}
