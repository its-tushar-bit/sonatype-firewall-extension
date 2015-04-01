/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collection;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.api.v1.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentPolicyViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;

/**
 * @since 1.13.0
 */
@Named
public class ApiComponentDetailsAdapter
{
  private final ApiLicenseDataAdapter licenseDataAdapter;

  private final ApiSecurityDataAdapter securityDataAdapter;


  @Inject
  public ApiComponentDetailsAdapter(final ApiLicenseDataAdapter licenseDataAdapter,
      final ApiSecurityDataAdapter securityDataAdapter)
  {
    this.licenseDataAdapter = licenseDataAdapter;
    this.securityDataAdapter = securityDataAdapter;
  }

  public ApiComponentDetailsDTOV2 convertToDTO(final Component component, final Collection<PolicyAlert> policyAlerts) {
    ApiComponentDetailsDTOV2 componentDetailsDTO = new ApiComponentDetailsDTOV2();
    componentDetailsDTO.component = new ApiComponentDTOV2();
    componentDetailsDTO.component.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(component.getComponentIdentifier());
    componentDetailsDTO.component.hash = component.getHash();
    componentDetailsDTO.component.proprietary = component.isProprietary();
    componentDetailsDTO.matchState =
        component.getMatchState() == null ? MatchState.UNKNOWN.getId() : component.getMatchState().getId();

    if (component.getCatalogDate() != null) {
      componentDetailsDTO.catalogDate = new Date(component.getCatalogDate());
    }

    componentDetailsDTO.licenseData = licenseDataAdapter.convertToDTO(component);
    componentDetailsDTO.securityData = securityDataAdapter.convertToDTO(component);

    componentDetailsDTO.policyData = new ApiComponentPolicyViolationListDTOV2();
    for (PolicyAlert policyAlert : policyAlerts) {
      componentDetailsDTO.policyData.policyViolations.add(convert(policyAlert));
    }
    return componentDetailsDTO;
  }

  private ApiPolicyViolationDTOV2 convert(final PolicyAlert policyAlert) {
    ApiPolicyViolationDTOV2 componentPolicyViolationDTO = new ApiPolicyViolationDTOV2();
    PolicyFact policyFact = policyAlert.getTrigger();
    componentPolicyViolationDTO.policyId = policyFact.getPolicyId();
    componentPolicyViolationDTO.policyName = policyFact.getPolicyName();
    componentPolicyViolationDTO.threatLevel = policyFact.getThreatLevel();

    for (ComponentFact componentFact : policyFact.getComponentFacts()) {
      for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
        componentPolicyViolationDTO.constraintViolations.add(convert(constraintFact));
      }
    }
    return componentPolicyViolationDTO;
  }

  private ApiConstraintViolationDTO convert(final ConstraintFact constraintFact) {
    ApiConstraintViolationDTO constraintViolationDTO = new ApiConstraintViolationDTO();
    constraintViolationDTO.constraintId = constraintFact.getConstraintId();
    constraintViolationDTO.constraintName = constraintFact.getConstraintName();
    for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
      ApiConstraintViolationReasonDTO constraintViolationReasonDTO = new ApiConstraintViolationReasonDTO();
      constraintViolationReasonDTO.reason = conditionFact.getReason();
      constraintViolationDTO.reasons.add(constraintViolationReasonDTO);
    }
    return constraintViolationDTO;
  }
}
