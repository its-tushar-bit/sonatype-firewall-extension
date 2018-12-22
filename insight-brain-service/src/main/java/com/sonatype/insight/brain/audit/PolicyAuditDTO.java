/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.policy.Policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class PolicyAuditDTO
{
  public String policyId;

  public String policyName;

  public PolicyAuditDTO() {
    //for jackson
  }

  public PolicyAuditDTO(final String policyId, final Policy policy) {
    this.policyId = policyId;
    if (policy != null) {
      policyName = policy.getName();
    }
  }

  public static List<PolicyAuditDTO> transcribe(final Set<String> policyIds) {
    List<PolicyAuditDTO> policyAuditDTOs = new ArrayList<>();
    PolicyDAO policyDAO = new PolicyDAO();
    for (String policyId : policyIds) {
      policyAuditDTOs.add(new PolicyAuditDTO(policyId, policyDAO.getById(policyId)));
    }
    return policyAuditDTOs;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicyAuditDTO that = (PolicyAuditDTO) o;
    return Objects.equals(policyId, that.policyId) &&
        Objects.equals(policyName, that.policyName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(policyId, policyName);
  }
}
