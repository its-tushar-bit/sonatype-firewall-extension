/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
    tempEntity.newPolicyViolation(policyEval.getId(), policyEval.getTime(), policy);
    List<PolicyAlert> alerts = PolicyAlertUtil.createPolicyAlerts(policyEval);
    assertThat(alerts, hasSize(1));
    PolicyAlert alert = alerts.get(0);
    assertThat(alert.getTrigger().getPolicyId(), is(policy.getId()));
    assertThat(alert.getTrigger().getPolicyName(), is(policy.getName()));
    assertThat(alert.getActions(), empty());
  }
}
