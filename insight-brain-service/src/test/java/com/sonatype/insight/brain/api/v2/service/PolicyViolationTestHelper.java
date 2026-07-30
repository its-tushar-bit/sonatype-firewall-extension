/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationTestHelper
{
  public static ProxyRepositoryPolicyViolation createPolicyViolationFail(
      Policy policy,
      ProxyRepositoryComponent component,
      final TemporaryEntity tempEntity)
  {
    ProxyRepositoryPolicyViolation policyViolation = new ProxyRepositoryPolicyViolation();
    policyViolation.setRepositoryId(component.getRepositoryId());
    policyViolation.setPathname(component.getPathname());
    policyViolation.setTime(new Date());
    policyViolation.setHash(component.getHash());
    policyViolation.setComponentIdentifier(component.getComponentIdentifier());
    policyViolation.setPolicyId(policy.getId());
    policyViolation.setPolicyName(policy.getName());
    policyViolation.setThreatLevel(policy.getThreatLevel());
    policyViolation.setThreatCategory(policy.getThreatCategory());
    policyViolation.setConstraintFacts(createConstraintFacts(policy));
    policyViolation.setActionTypeId(Action.ID_FAIL);
    return tempEntity.newRepositoryPolicyViolation(policyViolation);
  }

  public static ProxyRepositoryPolicyViolation createPolicyViolationWaived(
      Policy policy,
      ProxyRepositoryComponent component,
      final TemporaryEntity tempEntity)
  {
    ProxyRepositoryPolicyViolation policyViolation = new ProxyRepositoryPolicyViolation();
    policyViolation.setRepositoryId(component.getRepositoryId());
    policyViolation.setPathname(component.getPathname());
    policyViolation.setTime(new Date());
    policyViolation.setHash(component.getHash());
    policyViolation.setComponentIdentifier(component.getComponentIdentifier());
    policyViolation.setPolicyId(policy.getId());
    policyViolation.setPolicyName(policy.getName());
    policyViolation.setThreatLevel(policy.getThreatLevel());
    policyViolation.setThreatCategory(policy.getThreatCategory());
    policyViolation.setConstraintFacts(createConstraintFacts(policy));
    policyViolation.setActionTypeId(Action.ID_FAIL);
    policyViolation.setWaived(true);
    policyViolation.setPolicyWaiverId("waiver1");
    policyViolation.setWaiveTime(new Date());
    tempEntity.newRepositoryPolicyViolation(policyViolation);

    return policyViolation;
  }

  public static void createPolicyViolationWarn(
      Policy policy,
      ProxyRepositoryComponent component,
      final TemporaryEntity tempEntity)
  {
    ProxyRepositoryPolicyViolation policyViolation = new ProxyRepositoryPolicyViolation();
    policyViolation.setRepositoryId(component.getRepositoryId());
    policyViolation.setPathname(component.getPathname());
    policyViolation.setTime(new Date());
    policyViolation.setHash(component.getHash());
    policyViolation.setComponentIdentifier(component.getComponentIdentifier());
    policyViolation.setPolicyId(policy.getId());
    policyViolation.setPolicyName(policy.getName());
    policyViolation.setThreatLevel(policy.getThreatLevel());
    policyViolation.setThreatCategory(policy.getThreatCategory());
    policyViolation.setConstraintFacts(createConstraintFacts(policy));
    tempEntity.newRepositoryPolicyViolation(policyViolation);
  }

  private static List<ConstraintFact> createConstraintFacts(Policy policy) {
    Constraint constraint = policy.getConstraints().get(0);
    Condition condition = constraint.getConditions().get(0);
    TriggerReference triggerReference = new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "refId");
    ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(),
        constraint.getOperator().toString());
    constraintFact.addConditionFact(new ConditionFact("", 0, "", "random for condition "
        + condition.getConditionTypeId(), triggerReference));

    return Collections.singletonList(constraintFact);
  }

  public static void assertApiPolicyViolationDTOV2(
      ApiPolicyViolationDTOV2 policyViolationDTOV2,
      ProxyRepositoryPolicyViolation expectedPolicyViolation)
  {
    assertThat(policyViolationDTOV2.policyId).isEqualTo(expectedPolicyViolation.getPolicyId());
    assertThat(policyViolationDTOV2.policyName).isEqualTo(expectedPolicyViolation.getPolicyName());
    assertThat(policyViolationDTOV2.threatLevel).isEqualTo(expectedPolicyViolation.getThreatLevel());
    assertThat(policyViolationDTOV2.policyViolationId).isEqualTo(expectedPolicyViolation.getId());
    final ConstraintFact expectedConstraintFact = expectedPolicyViolation.getConstraintFacts().get(0);
    assertApiConstraintViolationDTO(policyViolationDTOV2.constraintViolations, expectedConstraintFact);
  }

  public static void assertApiConstraintViolationDTO(
      List<ApiConstraintViolationDTO> constraintViolationDTOs,
      ConstraintFact expectedConstraintFact)
  {
    assertThat(constraintViolationDTOs).hasSize(1);
    ApiConstraintViolationDTO constraintViolationDTO = constraintViolationDTOs.get(0);
    assertThat(constraintViolationDTO.constraintId).isEqualTo(expectedConstraintFact.getConstraintId());
    assertThat(constraintViolationDTO.constraintName).isEqualTo(expectedConstraintFact.getConstraintName());
    final ConditionFact conditionFact = expectedConstraintFact.getConditionFacts().get(0);
    assertThat(constraintViolationDTO.reasons.get(0).reason).isEqualTo(conditionFact.getReason());
    TriggerReference triggerReference = conditionFact.getReference();
    assertThat(constraintViolationDTO.reasons.get(0).reference.type).isEqualTo(triggerReference.getType().toString());
    assertThat(constraintViolationDTO.reasons.get(0).reference.value).isEqualTo(triggerReference.getValue());
  }
}
