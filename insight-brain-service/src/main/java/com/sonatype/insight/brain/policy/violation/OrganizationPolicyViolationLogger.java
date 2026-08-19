/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.Date;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.security.CurrentUser;

/**
 * @since 1.60
 */
public class OrganizationPolicyViolationLogger
    extends AbstractPolicyViolationLogger<PolicyViolation>
{
  private Organization organization;

  public OrganizationPolicyViolationLogger(
      boolean licensed,
      Date logTimestamp,
      Organization organization,
      CurrentUser currentUser)
  {
    super(licensed, logTimestamp, currentUser);
    this.organization = organization;
  }

  @Override
  protected PolicyViolationLogDTO createPolicyViolationLogDTO(
      PolicyViolationData<PolicyViolation> policyViolationData)
  {
    PolicyViolationLogDTO policyViolationLogDTO = super.createPolicyViolationLogDTO(policyViolationData);
    policyViolationLogDTO.organizationId = organization.getId();
    policyViolationLogDTO.organizationName = organization.getName();
    return policyViolationLogDTO;
  }
}
