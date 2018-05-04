/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PolicyAlertUtilTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Test
  public void testCreatePolicyAlerts_DeletedPolicy() {
    Application app = tempEntity.newApplicationWithParent("app-id");
    Policy policy = new Policy("id", "Deleted Policy");
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "some-scan");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEval, policy);
    List<PolicyAlert> alerts = PolicyAlertUtil.createPolicyAlerts(Arrays.asList(policyViolation),
        policyEval.getStageTypeId(), policyEval.isForMonitoring());
    assertThat(alerts, hasSize(1));
    PolicyAlert alert = alerts.get(0);
    assertThat(alert.getTrigger().getPolicyId(), is(policy.getId()));
    assertThat(alert.getTrigger().getPolicyName(), is(policy.getName()));
    assertThat(alert.getActions(), empty());
  }

  @Test
  public void testCreatePolicyAlerts_NoUnnecessaryData() {
    Application app = tempEntity.newApplicationWithParent("app-id");
    Policy policy = tempEntity.newPolicy(app.getId(), "Test Policy");
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
        policyEval.getStageTypeId(), policyEval.isForMonitoring());

    assertThat(alerts, hasSize(1));

    PolicyAlert alert = alerts.get(0);
    List<ComponentFact> componentFacts = alert.getTrigger().getComponentFacts();
    assertThat(componentFacts, hasSize(1));

    List<ConstraintFact> constraintFacts = componentFacts.get(0).getConstraintFacts();
    assertThat(constraintFacts, hasSize(1));
    constraintFact = constraintFacts.get(0);

    List<ConditionFact> conditionFacts = constraintFact.getConditionFacts();
    assertThat(conditionFacts, hasSize(2));
    // The condition index and triggers should not be populated in policy alerts.
    for (ConditionFact conditionFact : conditionFacts) {
      assertThat(conditionFact.getConditionIndex(), is(0));
      assertThat(conditionFact.getTriggerJson(), is(nullValue()));
    }
  }
}
