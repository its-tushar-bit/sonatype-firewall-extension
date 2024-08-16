/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.api.v2.service;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverReasonDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

@Named
public class ApiPolicyWaiverReasonService
{
  private final PolicyWaiverReasonDAO policyWaiverReasonDAO;

  @Inject
  public ApiPolicyWaiverReasonService(
      final PolicyWaiverReasonDAO policyWaiverReasonDAO
  )
  {
    this.policyWaiverReasonDAO = policyWaiverReasonDAO;
  }

  @Authorize(permission = Permission.READ)
  public List<ApiPolicyWaiverReasonDTO> getAllPolicyWaiverReasonsWithAuthzCheck() {
    return this.getAllPolicyWaiverReasons();
  }

  private List<ApiPolicyWaiverReasonDTO> getAllPolicyWaiverReasons() {
    return this.policyWaiverReasonDAO.getAll()
        .stream()
        .map(ApiPolicyWaiverReasonDTO::fromPolicyWaiverReason)
        .collect(Collectors.toList());
  }
}
