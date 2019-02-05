/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

public class ApplicationPolicyViolationLogger
    extends AbstractPolicyViolationLogger<PolicyViolation>
{
  private Organization organization;

  private Application application;

  public ApplicationPolicyViolationLogger(boolean licensed, Application application) {
    super(licensed);

    if (isEnabled()) {
      this.application = application;
      organization = new OrganizationDAO().getById(application.getOrganizationId());
    }
  }

  @Override
  protected PolicyViolationLogDTO createPolicyViolationLogDTO(PolicyViolationLogEvent policyViolationLogEvent,
                                                              PolicyViolation policyViolation)
  {
    PolicyViolationLogDTO policyViolationLogDTO =
        super.createPolicyViolationLogDTO(policyViolationLogEvent, policyViolation);

    policyViolationLogDTO.eventTimestamp = formatTimestamp(getDate(policyViolationLogEvent, policyViolation));
    policyViolationLogDTO.stageTypeId = policyViolation.getStageTypeId();
    policyViolationLogDTO.applicationId = policyViolation.getApplicationId();
    policyViolationLogDTO.applicationPublicId = application.getPublicId();
    policyViolationLogDTO.applicationName = application.getName();
    policyViolationLogDTO.organizationId = application.getOrganizationId();
    policyViolationLogDTO.organizationName = organization.getName();
    return policyViolationLogDTO;
  }

  @Override
  protected boolean shouldIncludeStagePolicyAction(PolicyViolationLogEvent policyViolationLogEvent,
                                                   PolicyViolation policyViolation)
  {
    return super.shouldIncludeStagePolicyAction(policyViolationLogEvent, policyViolation) &&
        !policyViolation.isGrandfathered() && !policyViolation.isWaived();
  }

  private Date getDate(PolicyViolationLogEvent policyViolationLogEvent,
                       PolicyViolation policyViolation)
  {
    switch (policyViolationLogEvent) {
      case CREATE:
        return policyViolation.getOpenTime();
      case FIX:
        return policyViolation.getFixTime();
    }
    throw new RuntimeException();
  }
}
