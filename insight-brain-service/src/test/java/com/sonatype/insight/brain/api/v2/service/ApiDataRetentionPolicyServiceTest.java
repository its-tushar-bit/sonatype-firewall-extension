/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiAgeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiAgeDTO.AgeUnit;
import com.sonatype.insight.brain.api.v2.dto.ApiDataRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPolicyDTO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiDataRetentionPolicyServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiDataRetentionPolicyService dataRetentionPolicyService;

  private DataRetentionPolicyDAO dataRetentionPolicyDAO = new DataRetentionPolicyDAO();

  @Test
  public void testGetDataRetentionPolicies() {
    Organization org = tempEntity.newOrganization();
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD));
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_DEVELOP, true, 7, 5));

    ApiDataRetentionPoliciesDTO dto = dataRetentionPolicyService.getDataRetentionPolicies(org.getId());

    assertThat(dto).isNotNull();
    assertThat(dto.applicationReports).isNotNull();
    assertThat(dto.applicationReports.stages).containsOnlyKeys(Stage.ID_DEVELOP, Stage.ID_BUILD, Stage.ID_STAGE_RELEASE,
        Stage.ID_RELEASE, Stage.ID_OPERATE, ApiDataRetentionPolicyService.CONTEXT_ID_CONTINUOUS_MONITORING);
    assertThat(dto.applicationReports.stages.values()).allSatisfy(policyDTO -> {
      assertThat(policyDTO).isNotNull();
    });
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
  public void testSetDataRetentionPolicies() {
    Organization org = tempEntity.newOrganization();
    // a policy to update
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD));
    // a policy to remove/inherit
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_DEVELOP));
    // a policy to leave as is
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_STAGE_RELEASE, true, 11, 3));

    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports.stages.put(Stage.ID_BUILD,
        new ApiReportRetentionPolicyDTO(false, true, 7, new ApiAgeDTO(2, AgeUnit.WEEK)));
    dto.applicationReports.stages.put(Stage.ID_DEVELOP, new ApiReportRetentionPolicyDTO(true, false, null, null));
    dto.applicationReports.stages.put(Stage.ID_RELEASE, new ApiReportRetentionPolicyDTO(true, false, null, null));
    dto.applicationReports.stages.put(Stage.ID_OPERATE, new ApiReportRetentionPolicyDTO(false, false, null, null));

    dataRetentionPolicyService.setDataRetentionPolicies(org.getId(), dto);

    Map<String, DataRetentionPolicy> policiesByContext = dataRetentionPolicyDAO.getByOwnerId(org.getId()).stream()
        .collect(toMap(DataRetentionPolicy::getContextId, Function.identity()));
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
  public void testSetDataRetentionPolicies_NoInheritanceForRootOrganization() {
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports.stages.put(Stage.ID_BUILD, new ApiReportRetentionPolicyDTO(true, false, null, null));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dataRetentionPolicyService.setDataRetentionPolicies(Organization.ROOT_ORGANIZATION_ID, dto);
    }).withMessageContaining("root organization cannot inherit");
  }

  @Test
  public void testSetDataRetentionPolicies_InvalidStage() {
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports.stages.put(Stage.ID_PROXY, new ApiReportRetentionPolicyDTO(false, false, null, null));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dataRetentionPolicyService.setDataRetentionPolicies(tempEntity.newOrganization().getId(), dto);
    }).withMessageContaining("Invalid stage id");
  }

  @Test
  public void testSetDataRetentionPolicies_NullRequestBody() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dataRetentionPolicyService.setDataRetentionPolicies(tempEntity.newOrganization().getId(), null);
    }).withMessageContaining("does not specify any retention policies");
  }

  @Test
  public void testSetDataRetentionPolicies_NullAppReportConfig() {
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports = null;

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dataRetentionPolicyService.setDataRetentionPolicies(tempEntity.newOrganization().getId(), dto);
    }).withMessageContaining("does not specify any retention policies");
  }

  @Test
  public void testSetDataRetentionPolicies_NullAppReportStages() {
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports.stages = null;

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dataRetentionPolicyService.setDataRetentionPolicies(tempEntity.newOrganization().getId(), dto);
    }).withMessageContaining("does not specify any retention policies");
  }

  @Test
  public void testSetDataRetentionPolicies_EmptyAppReportStages() {
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports.stages.clear();

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dataRetentionPolicyService.setDataRetentionPolicies(tempEntity.newOrganization().getId(), dto);
    }).withMessageContaining("does not specify any retention policies");
  }
}
