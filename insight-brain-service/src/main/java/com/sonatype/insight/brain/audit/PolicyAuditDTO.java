/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.Objects;

import com.sonatype.insight.brain.model.policy.Policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class PolicyAuditDTO
{
  public String policyId;

  public String policyName;

  public PolicyAuditDTO() {
    // for jackson
  }

  public PolicyAuditDTO(final String policyId, final Policy policy) {
    this.policyId = policyId;
    if (policy != null) {
      policyName = policy.getName();
    }
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
