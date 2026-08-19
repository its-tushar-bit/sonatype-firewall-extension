/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;

/**
 * @since 1.13.0
 */
public class PolicyViolationAdapter
{
  public static List<ApiConstraintViolationDTO> convert(final AbstractPolicyViolation policyViolation) {
    List<ApiConstraintViolationDTO> apiConstraintViolationsDTO = new ArrayList<>();
    for (ConstraintFact constraintFact : policyViolation.getConstraintFacts()) {
      ApiConstraintViolationDTO apiConstraintViolationDTO = new ApiConstraintViolationDTO();
      apiConstraintViolationsDTO.add(apiConstraintViolationDTO);
      apiConstraintViolationDTO.constraintId = constraintFact.getConstraintId();
      apiConstraintViolationDTO.constraintName = constraintFact.getConstraintName();
      apiConstraintViolationDTO.reasons = new ArrayList<>();
      for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
        ApiConstraintViolationReasonDTO apiConstraintReasonDTO = new ApiConstraintViolationReasonDTO();
        apiConstraintReasonDTO.reason = conditionFact.getReason();
        TriggerReference triggerReference = conditionFact.getReference();
        if (triggerReference != null) {
          apiConstraintReasonDTO.reference = new ApiConstraintViolationReasonDTO.TriggerReference();
          apiConstraintReasonDTO.reference.value = triggerReference.getValue();
          apiConstraintReasonDTO.reference.type = triggerReference.getType().toString();
        }
        apiConstraintViolationDTO.reasons.add(apiConstraintReasonDTO);
      }
    }
    return apiConstraintViolationsDTO;
  }
}
