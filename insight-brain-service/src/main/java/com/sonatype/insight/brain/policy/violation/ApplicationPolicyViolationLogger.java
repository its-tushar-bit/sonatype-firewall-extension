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

/**
 * @since 1.60
 */
public class ApplicationPolicyViolationLogger
    extends AbstractPolicyViolationLogger<PolicyViolation>
{
  private Organization organization;

  private Application application;

  public ApplicationPolicyViolationLogger(boolean licensed, Date logTimestamp, Application application) {
    super(licensed, logTimestamp);

    if (isEnabled()) {
      this.application = application;
      organization = new OrganizationDAO().getById(application.getOrganizationId());
    }
  }

  @Override
  protected PolicyViolationLogDTO createPolicyViolationLogDTO(
      PolicyViolationData<PolicyViolation> policyViolationData)
  {
    PolicyViolationLogDTO policyViolationLogDTO = super.createPolicyViolationLogDTO(policyViolationData);

    policyViolationLogDTO.stageTypeId =
        policyViolationData.policyViolation == null ? null : policyViolationData.policyViolation.getStageTypeId();
    policyViolationLogDTO.applicationId = application.getId();
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
}
