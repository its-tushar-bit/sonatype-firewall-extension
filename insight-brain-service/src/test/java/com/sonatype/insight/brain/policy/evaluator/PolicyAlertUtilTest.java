/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyAlertUtilTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Test
  public void testCreatePolicyAlerts_DeletedPolicy() {
    Application app = tempEntity.newApplicationWithParent("app-id");
    Policy policyDoesNotExist = tempEntity.newPolicy();
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "some-scan");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEval, policyDoesNotExist);
    new PolicyDAO().delete(policyDoesNotExist);
    List<PolicyAlert> alerts = PolicyAlertUtil.createPolicyAlerts(Arrays.asList(policyViolation),
        policyEval.getStageTypeId(), policyEval.isForMonitoring(), true);
    assertThat(alerts).hasSize(1);
    PolicyAlert alert = alerts.get(0);
    assertThat(alert.getTrigger().getPolicyId()).isEqualTo(policyDoesNotExist.getId());
    assertThat(alert.getTrigger().getPolicyName()).isEqualTo(policyDoesNotExist.getName());
    assertThat(alert.getActions()).isEmpty();
  }

  @Test
  public void testCreatePolicyAlerts_NoUnnecessaryData() {
    Application app = tempEntity.newApplicationWithParent("app-id");
    Policy policy = tempEntity.newPolicy(app);
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "some-scan");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEval, policy);
    ConditionFact conditionFact0 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "some summary", "some reason");
    conditionFact0.setTriggerJson("trigger 0");
    ConditionFact conditionFact1 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        1 /* conditionIndex */, "some summary", "some reason");
    conditionFact1.setTriggerJson("trigger 1");
    ConstraintFact constraintFact = new ConstraintFact("someConstraintId", "some constraint name", "and");
    constraintFact.addConditionFact(conditionFact0);
    constraintFact.addConditionFact(conditionFact1);
    policyViolation.setConstraintFacts(Collections.singletonList(constraintFact));

    List<PolicyAlert> alerts = PolicyAlertUtil.createPolicyAlerts(Arrays.asList(policyViolation),
        policyEval.getStageTypeId(), policyEval.isForMonitoring(), true);

    assertThat(alerts).hasSize(1);

    PolicyAlert alert = alerts.get(0);
    List<ComponentFact> componentFacts = alert.getTrigger().getComponentFacts();
    assertThat(componentFacts).hasSize(1);

    List<ConstraintFact> constraintFacts = componentFacts.get(0).getConstraintFacts();
    assertThat(constraintFacts).hasSize(1);
    constraintFact = constraintFacts.get(0);

    List<ConditionFact> conditionFacts = constraintFact.getConditionFacts();
    assertThat(conditionFacts).hasSize(2);
    // The condition index and triggers should not be populated in policy alerts.
    for (ConditionFact conditionFact : conditionFacts) {
      assertThat(conditionFact.getConditionIndex()).isEqualTo(0);
      assertThat(conditionFact.getTriggerJson()).isNull();
    }
  }

  @Test
  public void testCreatePolicyAlerts_OnePolicyAlertForEachPolicyViolation() {
    Application app = tempEntity.newApplicationWithParent("app-id");
    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "some-scan");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String hash = "hash";
    String reason1 = "test reason1";
    String reason2 = "test reason2";
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEval, policy, componentIdentifier, hash,
        reason1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEval, policy, componentIdentifier, hash,
        reason2);

    List<PolicyAlert> alerts = PolicyAlertUtil.createPolicyAlerts(Arrays.asList(policyViolation1, policyViolation2),
        policyEval.getStageTypeId(), policyEval.isForMonitoring(), true);

    assertThat(alerts).hasSize(2);

    PolicyAlert alert1 = alerts.get(0);
    assertThat(alert1.getTrigger().getPolicyId()).isEqualTo(policy.getId());
    assertThat(alert1.getTrigger().getPolicyName()).isEqualTo(policy.getName());
    assertThat(alert1.getTrigger().getPolicyViolationId()).isEqualTo(policyViolation1.getId());
    assertThat(alert1.getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0).getConditionFacts().get(0)
        .getReason()).isEqualTo(reason1);
    assertThat(alert1.getActions()).isEmpty();

    PolicyAlert alert2 = alerts.get(1);
    assertThat(alert2.getTrigger().getPolicyId()).isEqualTo(policy.getId());
    assertThat(alert2.getTrigger().getPolicyName()).isEqualTo(policy.getName());
    assertThat(alert2.getTrigger().getPolicyViolationId()).isEqualTo(policyViolation2.getId());
    assertThat(alert2.getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0).getConditionFacts().get(0)
        .getReason()).isEqualTo(reason2);
    assertThat(alert2.getActions()).isEmpty();
  }

  @Test
  public void testCreatePolicyAlerts_ActionsEnabled() {
    Application app = tempEntity.newApplicationWithParent("app-id");
    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    new PolicyDAO().update(policy);
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "some-scan");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEval, policy);
    ConditionFact conditionFact0 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "some summary", "some reason");
    conditionFact0.setTriggerJson("trigger 0");
    ConstraintFact constraintFact = new ConstraintFact("someConstraintId", "some constraint name", "and");
    constraintFact.addConditionFact(conditionFact0);
    policyViolation.setConstraintFacts(Collections.singletonList(constraintFact));

    List<PolicyAlert> alerts = PolicyAlertUtil.createPolicyAlerts(Arrays.asList(policyViolation),
        policyEval.getStageTypeId(), policyEval.isForMonitoring(), true);

    assertThat(alerts).hasSize(1);
    PolicyAlert alert = alerts.get(0);
    assertThat(alert.getActions()).hasSize(1);
    assertThat(alert.getActions().get(0).getActionTypeId()).isEqualTo(Action.ID_FAIL);
  }

  @Test
  public void testCreatePolicyAlerts_ActionsDisabled() {
    Application app = tempEntity.newApplicationWithParent("app-id");
    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    new PolicyDAO().update(policy);
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "some-scan");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEval, policy);
    ConditionFact conditionFact0 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "some summary", "some reason");
    conditionFact0.setTriggerJson("trigger 0");
    ConstraintFact constraintFact = new ConstraintFact("someConstraintId", "some constraint name", "and");
    constraintFact.addConditionFact(conditionFact0);
    policyViolation.setConstraintFacts(Collections.singletonList(constraintFact));

    List<PolicyAlert> alerts = PolicyAlertUtil.createPolicyAlerts(Arrays.asList(policyViolation),
        policyEval.getStageTypeId(), policyEval.isForMonitoring(), false);

    assertThat(alerts).hasSize(1);
    PolicyAlert alert = alerts.get(0);
    assertThat(alert.getActions()).isEmpty();
  }
}
