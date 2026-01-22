/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiAgeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiAgeDTO.AgeUnit;
import com.sonatype.insight.brain.api.v2.dto.ApiDataRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPolicyDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSuccessMetricsRetentionPolicyDTO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiDataRetentionPolicyServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiDataRetentionPolicyService dataRetentionPolicyService;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private DataRetentionPolicyDAO dataRetentionPolicyDAO;

  @Test
  public void testGetDataRetentionPolicies_AppReports() {
    Organization org = tempEntity.newOrganization();
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD));
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_DEVELOP, true, 7, 5));

    ApiDataRetentionPoliciesDTO dto = dataRetentionPolicyService.getDataRetentionPolicies(org.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.applicationReports).isNotNull();
    assertThat(dto.applicationReports.stages).containsOnlyKeys(Stage.ID_DEVELOP, Stage.ID_SOURCE, Stage.ID_BUILD,
        Stage.ID_STAGE_RELEASE, Stage.ID_RELEASE, Stage.ID_OPERATE,
        DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING);
    assertThat(dto.applicationReports.stages.values()).allSatisfy(policyDTO -> assertThat(policyDTO).isNotNull());
    assertThat(dto.applicationReports.stages).allSatisfy((contextId, policyDTO) -> {
      if (!Stage.ID_BUILD.equals(contextId) && !Stage.ID_DEVELOP.equals(contextId)) {
        assertThat(policyDTO.inheritPolicy).isTrue();
      }
    });
    ApiReportRetentionPolicyDTO policyDTO = dto.applicationReports.stages.get(Stage.ID_BUILD);
    assertThat(policyDTO.inheritPolicy).isFalse();
    assertThat(policyDTO.enablePurging).isFalse();
    assertThat(policyDTO.maxCount).isNull();
    assertThat(policyDTO.maxAge).isNull();
    policyDTO = dto.applicationReports.stages.get(Stage.ID_DEVELOP);
    assertThat(policyDTO.inheritPolicy).isFalse();
    assertThat(policyDTO.enablePurging).isTrue();
    assertThat(policyDTO.maxCount).isEqualTo(7);
    assertThat(policyDTO.maxAge).usingRecursiveComparison().isEqualTo(new ApiAgeDTO(5, AgeUnit.DAY));
  }

  @Test
  public void testGetParentDataRetentionPolicies() {
    Organization parentOrg = tempEntity.newOrganization();
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(parentOrg.getId(), Stage.ID_BUILD));
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(parentOrg.getId(), Stage.ID_DEVELOP, true, 7, 5));

    Organization organization = tempEntity.newOrganization(parentOrg);

    ApiDataRetentionPoliciesDTO dto = dataRetentionPolicyService.getParentDataRetentionPolicies(organization.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.applicationReports).isNotNull();
    assertThat(dto.applicationReports.stages).containsOnlyKeys(Stage.ID_DEVELOP, Stage.ID_SOURCE, Stage.ID_BUILD,
        Stage.ID_STAGE_RELEASE, Stage.ID_RELEASE, Stage.ID_OPERATE,
        DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING);
    assertThat(dto.applicationReports.stages.values()).allSatisfy(policyDTO -> assertThat(policyDTO).isNotNull());

    ApiReportRetentionPolicyDTO policyDTO = dto.applicationReports.stages.get(Stage.ID_BUILD);
    assertThat(policyDTO.inheritPolicy).isFalse();
    assertThat(policyDTO.enablePurging).isFalse();
    assertThat(policyDTO.maxCount).isNull();
    assertThat(policyDTO.maxAge).isNull();

    policyDTO = dto.applicationReports.stages.get(Stage.ID_DEVELOP);
    assertThat(policyDTO.inheritPolicy).isFalse();
    assertThat(policyDTO.enablePurging).isTrue();
    assertThat(policyDTO.maxCount).isEqualTo(7);
    assertThat(policyDTO.maxAge).usingRecursiveComparison().isEqualTo(new ApiAgeDTO(5, AgeUnit.DAY));
  }

  @Test
  public void testGetDataRetentionPolicies_AppReports_LicensedStages() {
    Organization org = tempEntity.newOrganization();

    testProductLicense.setStageTypes(StageTypes.STAGE_RELEASE, StageTypes.RELEASE);

    ApiDataRetentionPoliciesDTO dto = dataRetentionPolicyService.getDataRetentionPolicies(org.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.applicationReports).isNotNull();
    assertThat(dto.applicationReports.stages).containsOnlyKeys(Stage.ID_STAGE_RELEASE, Stage.ID_RELEASE,
        DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING);

    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_MONITORING);

    dto = dataRetentionPolicyService.getDataRetentionPolicies(org.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.applicationReports).isNotNull();
    assertThat(dto.applicationReports.stages).containsOnlyKeys(Stage.ID_STAGE_RELEASE, Stage.ID_RELEASE);
  }

  @Test
  public void testGetDataRetentionPolicies_SuccessMetrics_LocalPolicy() {
    Organization org = tempEntity.newOrganization();
    dataRetentionPolicyDAO.insert(
        new DataRetentionPolicy(org.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, true, null, 365 * 5));

    ApiDataRetentionPoliciesDTO dto = dataRetentionPolicyService.getDataRetentionPolicies(org.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.successMetrics).isNotNull();
    assertThat(dto.successMetrics.inheritPolicy).isFalse();
    assertThat(dto.successMetrics.enablePurging).isTrue();
    assertThat(dto.successMetrics.maxAge).usingRecursiveComparison().isEqualTo(new ApiAgeDTO(5, AgeUnit.YEAR));
  }

  @Test
  public void testGetDataRetentionPolicies_SuccessMetrics_InheritedPolicy() {
    Organization org = tempEntity.newOrganization();
    DataRetentionPolicy policy = dataRetentionPolicyDAO.getByOwnerIdAndContextId(Organization.ROOT_ORGANIZATION_ID,
        DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);

    ApiDataRetentionPoliciesDTO dto = dataRetentionPolicyService.getDataRetentionPolicies(org.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.successMetrics).isNotNull();
    assertThat(dto.successMetrics.inheritPolicy).isTrue();
    assertThat(dto.successMetrics.enablePurging).isTrue();
    assertThat(dto.successMetrics.maxAge).isNotNull();
    assertThat(dto.successMetrics.maxAge.toDays()).isEqualTo(policy.getMaxAgeInDays());
  }

  @Test
  public void testSetDataRetentionPolicies_AppReports() {
    Organization org = tempEntity.newOrganization();
    // a policy to update
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD));
    // a policy to remove/inherit
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_DEVELOP));
    // a policy to leave as is
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_STAGE_RELEASE, true, 11, 3));

    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports = new ApiReportRetentionPoliciesDTO();
    dto.applicationReports.stages.put(Stage.ID_BUILD,
        new ApiReportRetentionPolicyDTO(false, true, 7, new ApiAgeDTO(2, AgeUnit.WEEK)));
    dto.applicationReports.stages.put(Stage.ID_DEVELOP, new ApiReportRetentionPolicyDTO(true, false, null, null));
    dto.applicationReports.stages.put(Stage.ID_RELEASE, new ApiReportRetentionPolicyDTO(true, false, null, null));
    dto.applicationReports.stages.put(Stage.ID_OPERATE, new ApiReportRetentionPolicyDTO(false, false, null, null));

    dataRetentionPolicyService.setDataRetentionPolicies(org.getId(), dto);

    Map<String, DataRetentionPolicy> policiesByContext = dataRetentionPolicyDAO.getByOwnerId(org.getId());
    assertThat(policiesByContext).containsOnlyKeys(Stage.ID_BUILD, Stage.ID_OPERATE, Stage.ID_STAGE_RELEASE);

    DataRetentionPolicy policy = policiesByContext.get(Stage.ID_BUILD);
    assertThat(policy.isPurgingEnabled()).isTrue();
    assertThat(policy.getMaxCount()).isEqualTo(7);
    assertThat(policy.getMaxAgeInDays()).isEqualTo(14);

    policy = policiesByContext.get(Stage.ID_OPERATE);
    assertThat(policy.isPurgingEnabled()).isFalse();
    assertThat(policy.getMaxCount()).isNull();
    assertThat(policy.getMaxAgeInDays()).isNull();

    policy = policiesByContext.get(Stage.ID_STAGE_RELEASE);
    assertThat(policy.isPurgingEnabled()).isTrue();
    assertThat(policy.getMaxCount()).isEqualTo(11);
    assertThat(policy.getMaxAgeInDays()).isEqualTo(3);
  }

  @Test
  public void testSetDataRetentionPolicies_AppReports_NoInheritanceForRootOrganization() {
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports = new ApiReportRetentionPoliciesDTO();
    dto.applicationReports.stages.put(Stage.ID_BUILD, new ApiReportRetentionPolicyDTO(true, false, null, null));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dataRetentionPolicyService.setDataRetentionPolicies(Organization.ROOT_ORGANIZATION_ID, dto))
        .withMessageContaining("root organization cannot inherit");
  }

  @Test
  public void testSetDataRetentionPolicies_AppReports_InvalidStage() {
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports = new ApiReportRetentionPoliciesDTO();
    dto.applicationReports.stages.put(Stage.ID_PROXY, new ApiReportRetentionPolicyDTO(false, false, null, null));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> dataRetentionPolicyService.setDataRetentionPolicies(tempEntity.newOrganization().getId(), dto))
        .withMessageContaining("Invalid stage id");
  }

  @Test
  public void testSetDataRetentionPolicies_SuccessMetrics_InheritedPolicy() {
    Organization org = tempEntity.newOrganization();
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS));

    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.successMetrics = new ApiSuccessMetricsRetentionPolicyDTO(true, false, null);

    dataRetentionPolicyService.setDataRetentionPolicies(org.getId(), dto);

    assertThat(dataRetentionPolicyDAO.getByOwnerId(org.getId())).isEmpty();

    dataRetentionPolicyService.setDataRetentionPolicies(org.getId(), dto);

    assertThat(dataRetentionPolicyDAO.getByOwnerId(org.getId())).isEmpty();
  }

  @Test
  public void testSetDataRetentionPolicies_SuccessMetrics_LocalPolicy() {
    Organization org = tempEntity.newOrganization();

    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports = new ApiReportRetentionPoliciesDTO();
    dto.applicationReports.stages = null;
    dto.successMetrics = new ApiSuccessMetricsRetentionPolicyDTO(false, false, null);

    dataRetentionPolicyService.setDataRetentionPolicies(org.getId(), dto);

    Map<String, DataRetentionPolicy> policiesByContext = dataRetentionPolicyDAO.getByOwnerId(org.getId());
    assertThat(policiesByContext).containsOnlyKeys(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);
    DataRetentionPolicy policy = policiesByContext.get(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);
    assertThat(policy.isPurgingEnabled()).isFalse();
    assertThat(policy.getMaxCount()).isNull();
    assertThat(policy.getMaxAgeInDays()).isNull();

    dto.successMetrics = new ApiSuccessMetricsRetentionPolicyDTO(false, true, new ApiAgeDTO(3, AgeUnit.YEAR));

    dataRetentionPolicyService.setDataRetentionPolicies(org.getId(), dto);

    policiesByContext = dataRetentionPolicyDAO.getByOwnerId(org.getId());
    assertThat(policiesByContext).containsOnlyKeys(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);
    policy = policiesByContext.get(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);
    assertThat(policy.isPurgingEnabled()).isTrue();
    assertThat(policy.getMaxCount()).isNull();
    assertThat(policy.getMaxAgeInDays()).isEqualTo(3 * 365);
  }

  @Test
  public void testSetDataRetentionPolicies_SuccessMetrics_NoInheritanceForRootOrganization() {
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.successMetrics = new ApiSuccessMetricsRetentionPolicyDTO(true, false, null);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dataRetentionPolicyService.setDataRetentionPolicies(Organization.ROOT_ORGANIZATION_ID, dto))
        .withMessageContaining("root organization cannot inherit");
  }

  @Test
  public void testSetDataRetentionPolicies_SuccessMetrics_InvalidTimeUnit() {
    Organization org = tempEntity.newOrganization();

    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.successMetrics = new ApiSuccessMetricsRetentionPolicyDTO(false, true, new ApiAgeDTO(30, AgeUnit.DAY));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dataRetentionPolicyService.setDataRetentionPolicies(org.getId(), dto))
        .withMessageContaining("expected unit 'year' for maximum age");
  }

  @Test
  public void testSetDataRetentionPolicies_NullRequestBody() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> dataRetentionPolicyService.setDataRetentionPolicies(tempEntity.newOrganization().getId(), null))
        .withMessageContaining("does not specify any retention policies");
  }

  @Test
  public void testSetDataRetentionPolicies_NullPolicies() {
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports = null;
    dto.successMetrics = null;

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> dataRetentionPolicyService.setDataRetentionPolicies(tempEntity.newOrganization().getId(), dto))
        .withMessageContaining("does not specify any retention policies");
  }

  @Test
  public void testSetDataRetentionPolicies_NullAppReportStages() {
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports = new ApiReportRetentionPoliciesDTO();
    dto.applicationReports.stages = null;

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> dataRetentionPolicyService.setDataRetentionPolicies(tempEntity.newOrganization().getId(), dto))
        .withMessageContaining("does not specify any retention policies");
  }

  @Test
  public void testSetDataRetentionPolicies_EmptyAppReportStages() {
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports = new ApiReportRetentionPoliciesDTO();
    dto.applicationReports.stages.clear();

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> dataRetentionPolicyService.setDataRetentionPolicies(tempEntity.newOrganization().getId(), dto))
        .withMessageContaining("does not specify any retention policies");
  }

  @Test
  public void testGetDataRetentionPolicies_nLevel() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);

    Organization parentOrg = organizations.get(1);
    List<DataRetentionPolicy> policies = Arrays.asList(
        new DataRetentionPolicy(parentOrg.getId(), Stage.ID_BUILD),
        new DataRetentionPolicy(parentOrg.getId(), Stage.ID_DEVELOP, true, 7, 5));
    policies.forEach( policy -> dataRetentionPolicyDAO.insert(policy));

    ApiDataRetentionPoliciesDTO parentOrgDTO = dataRetentionPolicyService.getDataRetentionPolicies(parentOrg.getId());
    verifyRetentionPolicies(parentOrgDTO, Optional.of(policies), Optional.empty());
    Organization childOrg = organizations.get(0);
    ApiDataRetentionPoliciesDTO childOrgDTO = dataRetentionPolicyService.getDataRetentionPolicies(childOrg.getId());
    verifyRetentionPolicies(childOrgDTO, Optional.empty(), Optional.of(policies));
  }

  @Test
  public void testGetDataRetentionPolicies_nLevel_multipleInheritance() {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 3, 0);

    Organization parentOrg1 = organizations.get(2);
    List<DataRetentionPolicy> parent1Policies = Arrays.asList(
        new DataRetentionPolicy(parentOrg1.getId(), Stage.ID_BUILD),
        new DataRetentionPolicy(parentOrg1.getId(), Stage.ID_DEVELOP, true, 7, 5));
    parent1Policies.forEach( policy -> dataRetentionPolicyDAO.insert(policy));

    Organization parentOrg2 = organizations.get(1);
    List<DataRetentionPolicy> parent2Policies = Arrays.asList(
        new DataRetentionPolicy(parentOrg2.getId(), Stage.ID_STAGE_RELEASE, false, 100, 365),
        new DataRetentionPolicy(parentOrg2.getId(), Stage.ID_SOURCE, true, null, 1024));
    parent2Policies.forEach( policy -> dataRetentionPolicyDAO.insert(policy));

    List<DataRetentionPolicy> inheritedPolicies = new LinkedList<>();
    ApiDataRetentionPoliciesDTO parentOrg1DTO = dataRetentionPolicyService.getDataRetentionPolicies(parentOrg1.getId());
    verifyRetentionPolicies(parentOrg1DTO, Optional.of(parent1Policies), Optional.of(inheritedPolicies));

    inheritedPolicies.addAll(parent1Policies);
    ApiDataRetentionPoliciesDTO parentOrg2DTO = dataRetentionPolicyService.getDataRetentionPolicies(parentOrg2.getId());
    verifyRetentionPolicies(parentOrg2DTO, Optional.of(parent2Policies), Optional.of(inheritedPolicies));

    Organization childOrg = organizations.get(0);
    inheritedPolicies.addAll(parent2Policies);
    ApiDataRetentionPoliciesDTO childOrgDTO = dataRetentionPolicyService.getDataRetentionPolicies(childOrg.getId());
    verifyRetentionPolicies(childOrgDTO, Optional.empty(), Optional.of(inheritedPolicies));
  }

  private void verifyRetentionPolicies(
      ApiDataRetentionPoliciesDTO dto,
      Optional<List<DataRetentionPolicy>> optionalPolicies,
      Optional<List<DataRetentionPolicy>> optionalInheritedPolicies)
  {
    List<DataRetentionPolicy> policies = optionalPolicies.isPresent()
        ? optionalPolicies.get()
        : Collections.EMPTY_LIST;
    List<String> definedStages = policies.stream().map(policy -> policy.getContextId()).collect(Collectors.toList());

    List<DataRetentionPolicy> inheritedPolicies = optionalInheritedPolicies.isPresent()
        ? optionalInheritedPolicies.get()
        : Collections.EMPTY_LIST;
    List<String> inheritedStages = inheritedPolicies.stream()
        .map(policy -> policy.getContextId()).collect(Collectors.toList());

    assertThat(dto).isNotNull();
    assertThat(dto.applicationReports).isNotNull();
    assertThat(dto.applicationReports.stages).containsOnlyKeys(Stage.ID_DEVELOP, Stage.ID_SOURCE, Stage.ID_BUILD,
        Stage.ID_STAGE_RELEASE, Stage.ID_RELEASE, Stage.ID_OPERATE,
        DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING);
    assertThat(dto.applicationReports.stages.values()).allSatisfy(policyDTO -> assertThat(policyDTO).isNotNull());
    assertThat(dto.applicationReports.stages).allSatisfy((contextId, policyDTO) -> {
      //Check first if stage is defined for org
      if (definedStages.contains(contextId)) {
        DataRetentionPolicy originalPolicy =
            policies.stream()
                .filter(dataRetentionPolicy -> dataRetentionPolicy.getContextId().equals(contextId))
                .findFirst()
                .get();
        assertThat(policyDTO.inheritPolicy).isFalse();
        verifyPolicy(originalPolicy, policyDTO);
      } //then check if stage is defined by any of parent org
      else if (inheritedStages.contains(contextId)) {
        DataRetentionPolicy inheritedPolicy =
            inheritedPolicies.stream()
                .filter(dataRetentionPolicy -> dataRetentionPolicy.getContextId().equals(contextId))
                .findFirst()
                .get();
        assertThat(policyDTO.inheritPolicy).isTrue();
        verifyPolicy(inheritedPolicy, policyDTO);
      } // finally check if stage is defined by root org - which is default
      else {
        assertThat(policyDTO.inheritPolicy).isTrue();
      }
    });
  }

  private void verifyPolicy(DataRetentionPolicy originalPolicy, ApiReportRetentionPolicyDTO policyDTO ) {
    assertThat(policyDTO.enablePurging).isEqualTo(originalPolicy.isPurgingEnabled());
    if (originalPolicy.getMaxCount() != null) {
      assertThat(originalPolicy.getMaxCount()).isEqualTo(policyDTO.maxCount);
    }
    else {
      assertThat(policyDTO.maxCount).isNull();
    }

    if (originalPolicy.getMaxAgeInDays() != null) {
      assertThat(policyDTO.maxAge.toDays()).isEqualTo(originalPolicy.getMaxAgeInDays());
    }
    else {
      assertThat(policyDTO.maxAge).isNull();
    }
  }
}
