/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.successmetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class PolicyViolationResolutionStateTest
{
  @Test
  public void testSetStageTypeById() {
    PolicyViolationResolutionState resolutionState = new PolicyViolationResolutionState();

    // sanity check
    assertThat(resolutionState.getDevelopStageType(), is(false));

    // test one-arg method
    resolutionState.setStageTypeById(DevelopStageType.ID);
    assertThat(resolutionState.getDevelopStageType(), is(true));
    resolutionState.setStageTypeById(BuildStageType.ID);
    assertThat(resolutionState.getBuildStageType(), is(true));
    resolutionState.setStageTypeById(StageReleaseStageType.ID);
    assertThat(resolutionState.getStageReleaseStageType(), is(true));
    resolutionState.setStageTypeById(ReleaseStageType.ID);
    assertThat(resolutionState.getReleaseStageType(), is(true));
    resolutionState.setStageTypeById(OperateStageType.ID);
    assertThat(resolutionState.getOperateStageType(), is(true));
    resolutionState.setStageTypeById(ProxyStageType.ID);
    assertThat(resolutionState.getProxyStageType(), is(true));
    try {
      resolutionState.setStageTypeById("asdf");
      fail();
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Unknown stageType"));
    }

    // test 2-arg method setting back to false
    resolutionState.setStageTypeById(DevelopStageType.ID, false);
    assertThat(resolutionState.getDevelopStageType(), is(false));
    resolutionState.setStageTypeById(BuildStageType.ID, false);
    assertThat(resolutionState.getBuildStageType(), is(false));
    resolutionState.setStageTypeById(StageReleaseStageType.ID, false);
    assertThat(resolutionState.getStageReleaseStageType(), is(false));
    resolutionState.setStageTypeById(ReleaseStageType.ID, false);
    assertThat(resolutionState.getReleaseStageType(), is(false));
    resolutionState.setStageTypeById(OperateStageType.ID, false);
    assertThat(resolutionState.getOperateStageType(), is(false));
    resolutionState.setStageTypeById(ProxyStageType.ID, false);
    assertThat(resolutionState.getProxyStageType(), is(false));
    try {
      resolutionState.setStageTypeById("asdf", false);
      fail();
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Unknown stageType"));
    }
  }

  @Test
  public void testIsClearedInAllStages() {
    PolicyViolationResolutionState resolutionState = new PolicyViolationResolutionState();

    assertThat(resolutionState.isClearedInAllStages(), is(true));

    resolutionState.setDevelopStageType(true);
    assertThat(resolutionState.isClearedInAllStages(), is(false));

    resolutionState.setBuildStageType(true);
    resolutionState.setDevelopStageType(false);
    assertThat(resolutionState.isClearedInAllStages(), is(false));

    resolutionState.setStageReleaseStageType(true);
    resolutionState.setBuildStageType(false);
    assertThat(resolutionState.isClearedInAllStages(), is(false));

    resolutionState.setReleaseStageType(true);
    resolutionState.setStageReleaseStageType(false);
    assertThat(resolutionState.isClearedInAllStages(), is(false));

    resolutionState.setOperateStageType(true);
    resolutionState.setReleaseStageType(false);
    assertThat(resolutionState.isClearedInAllStages(), is(false));

    resolutionState.setProxyStageType(true);
    resolutionState.setOperateStageType(false);
    assertThat(resolutionState.isClearedInAllStages(), is(false));

    resolutionState.setProxyStageType(false);
    assertThat(resolutionState.isClearedInAllStages(), is(true));

    resolutionState.setDevelopStageType(true);
    resolutionState.setBuildStageType(true);
    resolutionState.setStageReleaseStageType(true);
    resolutionState.setReleaseStageType(true);
    resolutionState.setOperateStageType(true);
    resolutionState.setProxyStageType(true);
    assertThat(resolutionState.isClearedInAllStages(), is(false));
  }

  @Test
  public void testSetConstraintFactsJson() throws Exception {
    PolicyViolationResolutionState policyViolationResolutionState = new PolicyViolationResolutionState();

    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.format(constraintFacts);
    policyViolationResolutionState.setConstraintFactsJson(constraintFactsJson);
    assertThat(policyViolationResolutionState.getConstraintFactsJson(), is(constraintFactsJson));
    assertConstraintFacts(policyViolationResolutionState.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFactsJson_Null() throws Exception {
    PolicyViolationResolutionState policyViolationResolutionState = new PolicyViolationResolutionState();

    try {
      policyViolationResolutionState.setConstraintFactsJson(null);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage(), is("ConstraintFactsJson cannot be null or empty."));
    }
  }

  @Test
  public void testSetConstraintFactsJson_Empty() throws Exception {
    PolicyViolationResolutionState policyViolationResolutionState = new PolicyViolationResolutionState();

    try {
      policyViolationResolutionState.setConstraintFactsJson(" ");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage(), is("ConstraintFactsJson cannot be null or empty."));
    }
  }

  private List<ConstraintFact> createConstraintFacts(int count) {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ConstraintFact constraintFact = new ConstraintFact(UUID.randomUUID().toString(), "constraintName " + i, "and");
      ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
          0 /* conditionIndex */, "some summary", "some reason");
      conditionFact.setTriggerJson("some trigger");
      constraintFact.addConditionFact(conditionFact);
      constraintFacts.add(constraintFact);
    }
    return constraintFacts;
  }

  private void assertConstraintFacts(List<ConstraintFact> actual, List<ConstraintFact> expected) {
    assertThat(actual, hasSize(expected.size()));
    for (int constraintFactIndex = 0; constraintFactIndex < expected.size(); constraintFactIndex++) {
      ConstraintFact expectedConstraintFact = expected.get(constraintFactIndex);
      ConstraintFact actualConstraintFact = actual.get(constraintFactIndex);
      assertThat(actualConstraintFact.getConstraintId(), is(expectedConstraintFact.getConstraintId()));
      assertThat(actualConstraintFact.getConstraintName(), is(expectedConstraintFact.getConstraintName()));
      assertThat(actualConstraintFact.getOperatorName(), is(expectedConstraintFact.getOperatorName()));
      for (int conditionFactIndex = 0; conditionFactIndex < expectedConstraintFact.getConditionFacts()
          .size(); conditionFactIndex++) {
        ConditionFact expectedConditionFact = expectedConstraintFact.getConditionFacts().get(conditionFactIndex);
        ConditionFact actualConditionFact = actualConstraintFact.getConditionFacts().get(conditionFactIndex);
        assertThat(actualConditionFact.getConditionTypeId(), is(expectedConditionFact.getConditionTypeId()));
        assertThat(actualConditionFact.getConditionIndex(), is(expectedConditionFact.getConditionIndex()));
        assertThat(actualConditionFact.getSummary(), is(expectedConditionFact.getSummary()));
        assertThat(actualConditionFact.getReason(), is(expectedConditionFact.getReason()));
        assertThat(actualConditionFact.getTriggerJson(), is(expectedConditionFact.getTriggerJson()));
      }
    }
  }
}
