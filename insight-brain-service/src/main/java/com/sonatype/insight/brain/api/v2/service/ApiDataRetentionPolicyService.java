/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiAgeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiDataRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPolicyDTO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import static java.util.stream.Collectors.toMap;

/**
 * @since version.next
 */
@Named
public class ApiDataRetentionPolicyService
{
  private static final Set<String> VALID_CONTEXT_IDS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList( //
      Stage.ID_DEVELOP, //
      Stage.ID_BUILD, //
      Stage.ID_STAGE_RELEASE, //
      Stage.ID_RELEASE, //
      Stage.ID_OPERATE, //
      DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING)));

  private final DataRetentionPolicyDAO dataRetentionPolicyDAO;

  @Inject
  public ApiDataRetentionPolicyService(DataRetentionPolicyDAO dataRetentionPolicyDAO) {
    this.dataRetentionPolicyDAO = dataRetentionPolicyDAO;
  }

  @Authorize(permission = Permission.READ)
  public ApiDataRetentionPoliciesDTO getDataRetentionPolicies(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId)
  {
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    Map<String, DataRetentionPolicy> policiesByContext = dataRetentionPolicyDAO.getByOwnerId(organizationId).stream()
        .collect(toMap(DataRetentionPolicy::getContextId, Function.identity()));
    for (String contextId : VALID_CONTEXT_IDS) {
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
    return dto;
  }

  @Authorize(permission = Permission.WRITE)
  public void setDataRetentionPolicies(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId,
      ApiDataRetentionPoliciesDTO dto)
  {
    if (dto == null || dto.applicationReports == null || dto.applicationReports.stages == null
        || dto.applicationReports.stages.isEmpty()) {
      throw new BadRequestException("The request does not specify any retention policies to configure");
    }
    try (TransactionContext tx = dataRetentionPolicyDAO.createTransactionContext()) {
      tx.begin();
      Map<String, DataRetentionPolicy> policiesByContext = dataRetentionPolicyDAO.getByOwnerId(tx, organizationId)
          .stream().collect(toMap(DataRetentionPolicy::getContextId, Function.identity()));
      for (Map.Entry<String, ApiReportRetentionPolicyDTO> entry : dto.applicationReports.stages.entrySet()) {
        String contextId = entry.getKey();
        if (!VALID_CONTEXT_IDS.contains(contextId)) {
          throw new BadRequestException("Invalid stage id '" + contextId + "', expected one of " + VALID_CONTEXT_IDS);
        }
        ApiReportRetentionPolicyDTO policyDTO = entry.getValue();
        if (policyDTO != null) {
          DataRetentionPolicy policy = policiesByContext.get(contextId);
          if (policyDTO.inheritPolicy) {
            if (Organization.ROOT_ORGANIZATION_ID.equals(organizationId)) {
              throw new BadRequestException(
                  "The root organization cannot inherit a retention policy for stage '" + contextId + "'");
            }
            if (policy != null) {
              dataRetentionPolicyDAO.delete(tx, policy);
            }
          }
          else {
            if (policy == null) {
              policy = new DataRetentionPolicy(organizationId, contextId);
            }
            policy.setPurgingEnabled(policyDTO.enablePurging);
            policy.setMaxCount(policyDTO.maxCount);
            policy.setMaxAgeInDays(policyDTO.maxAge != null ? policyDTO.maxAge.toDays() : null);
            if (policy.getId() != null) {
              dataRetentionPolicyDAO.update(tx, policy);
            }
            else {
              dataRetentionPolicyDAO.insert(tx, policy);
            }
          }
        }
      }
      tx.commit();
    }
  }
}
