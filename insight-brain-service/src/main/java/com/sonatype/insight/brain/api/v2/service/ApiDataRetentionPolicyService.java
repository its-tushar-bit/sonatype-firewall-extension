/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiAgeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiDataRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPolicyDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSuccessMetricsRetentionPolicyDTO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @since 1.63
 */
@Named
public class ApiDataRetentionPolicyService
{
  private static final Set<String> VALID_REPORT_CONTEXT_IDS =
      Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList( //
          Stage.ID_DEVELOP, //
          Stage.ID_BUILD, //
          Stage.ID_STAGE_RELEASE, //
          Stage.ID_RELEASE, //
          Stage.ID_OPERATE, //
          DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING)));

  private final DataRetentionPolicyDAO dataRetentionPolicyDAO;

  private final ProductLicense productLicense;

  @Inject
  public ApiDataRetentionPolicyService(
      DataRetentionPolicyDAO dataRetentionPolicyDAO,
      ProductLicense productLicense)
  {
    this.dataRetentionPolicyDAO = dataRetentionPolicyDAO;
    this.productLicense = productLicense;
  }

  private List<String> getLicensedReportContextIds() {
    Set<String> licensed = Stream.concat( //
        productLicense.getStageTypes().stream().map(StageType::getId),
        productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)
            ? Stream.of(DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING)
            : Stream.empty())
        .collect(toSet());
    return VALID_REPORT_CONTEXT_IDS.stream().filter(licensed::contains).collect(toList());
  }

  @Authorize(permission = Permission.READ)
  public ApiDataRetentionPoliciesDTO getDataRetentionPolicies(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId)
  {
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports = new ApiReportRetentionPoliciesDTO();
    dto.successMetrics = new ApiSuccessMetricsRetentionPolicyDTO();
    Map<String, DataRetentionPolicy> policiesByContext = dataRetentionPolicyDAO.getByOwnerId(organizationId);
    for (String contextId : getLicensedReportContextIds()) {
      ApiReportRetentionPolicyDTO policyDTO = new ApiReportRetentionPolicyDTO();
      DataRetentionPolicy policy = policiesByContext.get(contextId);
      policyDTO.inheritPolicy = policy == null;
      if (policy == null) {
        policy = dataRetentionPolicyDAO.getByOwnerIdAndContextId(Organization.ROOT_ORGANIZATION_ID, contextId);
      }
      policyDTO.enablePurging = policy.isPurgingEnabled();
      policyDTO.maxCount = policy.getMaxCount();
      policyDTO.maxAge = ApiAgeDTO.fromDays(policy.getMaxAgeInDays());
      dto.applicationReports.stages.put(contextId, policyDTO);
    }
    DataRetentionPolicy policy = policiesByContext.get(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);
    ApiSuccessMetricsRetentionPolicyDTO policyDTO = dto.successMetrics;
    policyDTO.inheritPolicy = policy == null;
    if (policy == null) {
      policy = dataRetentionPolicyDAO.getByOwnerIdAndContextId(Organization.ROOT_ORGANIZATION_ID,
          DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);
    }
    policyDTO.enablePurging = policy.isPurgingEnabled();
    policyDTO.maxAge = ApiAgeDTO.fromDays(policy.getMaxAgeInDays());
    return dto;
  }

  @Authorize(permission = Permission.WRITE)
  public void setDataRetentionPolicies(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId,
      ApiDataRetentionPoliciesDTO dto)
  {
    if (dto == null || ((dto.applicationReports == null || dto.applicationReports.stages == null
        || dto.applicationReports.stages.isEmpty()) && dto.successMetrics == null)) {
      throw new BadRequestException("The request does not specify any retention policies to configure");
    }
    try (TransactionContext tx = dataRetentionPolicyDAO.createTransactionContext()) {
      tx.begin();
      Map<String, DataRetentionPolicy> policiesByContext = dataRetentionPolicyDAO.getByOwnerId(tx, organizationId);
      if (dto.applicationReports != null && dto.applicationReports.stages != null) {
        for (Map.Entry<String, ApiReportRetentionPolicyDTO> entry : dto.applicationReports.stages.entrySet()) {
          String contextId = entry.getKey();
          if (!VALID_REPORT_CONTEXT_IDS.contains(contextId)) {
            throw new BadRequestException(
                "Invalid stage id '" + contextId + "', expected one of " + VALID_REPORT_CONTEXT_IDS);
          }
          ApiReportRetentionPolicyDTO policyDTO = entry.getValue();
          if (policyDTO != null) {
            DataRetentionPolicy policy = policiesByContext.get(contextId);
            updateRetentionPolicy(tx, organizationId, contextId, policy, policyDTO.inheritPolicy,
                policyDTO.enablePurging, policyDTO.maxCount, policyDTO.maxAge);
          }
        }
      }
      if (dto.successMetrics != null) {
        if (dto.successMetrics.maxAge != null && !ApiAgeDTO.AgeUnit.YEAR.equals(dto.successMetrics.maxAge.unitOfTime)) {
          throw new BadRequestException("Invalid time unit '" + dto.successMetrics.maxAge.unitOfTime
              + "', expected unit 'year' for maximum age of Success Metrics");
        }
        String contextId = DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS;
        DataRetentionPolicy policy = policiesByContext.get(contextId);
        updateRetentionPolicy(tx, organizationId, contextId, policy, dto.successMetrics.inheritPolicy,
            dto.successMetrics.enablePurging, null, dto.successMetrics.maxAge);
      }
      tx.commit();
    }
  }

  private void updateRetentionPolicy(
      TransactionContext tx,
      String organizationId,
      String contextId,
      DataRetentionPolicy policy,
      boolean inheritPolicy,
      boolean enablePurging,
      Integer maxCount,
      ApiAgeDTO maxAge)
  {
    if (inheritPolicy) {
      if (Organization.ROOT_ORGANIZATION_ID.equals(organizationId)) {
        String msg = "The root organization cannot inherit a retention policy for ";
        if (DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS.equals(contextId)) {
          msg += "Success Metrics";
        }
        else {
          msg += "stage '" + contextId + "'";
        }
        throw new BadRequestException(msg);
      }
      if (policy != null) {
        dataRetentionPolicyDAO.delete(tx, policy);
      }
    }
    else {
      if (policy == null) {
        policy = new DataRetentionPolicy(organizationId, contextId);
      }
      policy.setPurgingEnabled(enablePurging);
      policy.setMaxCount(maxCount);
      policy.setMaxAgeInDays(maxAge != null ? maxAge.toDays() : null);
      if (policy.getId() != null) {
        dataRetentionPolicyDAO.update(tx, policy);
      }
      else {
        dataRetentionPolicyDAO.insert(tx, policy);
      }
    }
  }
}
