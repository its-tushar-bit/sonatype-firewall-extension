/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.component.ApplicationComponentDetailsDTO.PolicyViolationSummaryDTO;
import com.sonatype.insight.brain.component.ApplicationComponentDetailsDTO.PolicyViolationSummaryDTO.ReasonDTO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

public class ComponentDetailServiceTest
    extends AbstractComponentTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Inject
  private ComponentDetailService componentDetailService;

  @Test
  public void testGetApplicationDetailsByHash() {
    String hash = "ababababab";

    // app1 has the component without any policy violations
    Application app1 = tempEntity.newApplicationWithParent("app1");
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash, "groupId", "artifactId", "version");

    // app2 has the component with policy violations
    Application app2 = tempEntity.newApplicationWithParent("app2");
    tempEntity.newApplicationComponent(app2.getId(), ReleaseStageType.ID, hash, "groupId", "artifactId", "version");
    // add two policy violations for a stage
    Policy policy1 = tempEntity.newPolicy(app2.getId(), "policy1", 1);
    Policy policy2 = tempEntity.newPolicy(app2.getId(), "policy2", 1);
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(policyEvaluation1.getId(), policyEvaluation1.getTime(), policy1, "groupId",
        "artifactId", "version", hash, "reason1");
    tempEntity.newPolicyViolation(policyEvaluation1.getId(), policyEvaluation1.getTime(), policy2, "groupId",
        "artifactId", "version", hash, "reason2");
    // add another policy violation for a different stage and with a different threat level
    policy1.setThreatLevel(2);
    new PolicyDAO().update(policy1);
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "scanId2");
    tempEntity.newPolicyViolation(policyEvaluation2.getId(), policyEvaluation2.getTime(), policy1, "groupId",
        "artifactId", "version", hash, "reason3");

    // app3 does not have the component
    tempEntity.newApplicationWithParent("app3");

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs, notNullValue());
    assertThat(appComponentDetailsDTOs, hasSize(2));
    ApplicationComponentDetailsDTO appComponentDetailsDTO = appComponentDetailsDTOs.get(0);
    assertThat(appComponentDetailsDTO.application.getId(), is(app1.getId()));
    assertThat(appComponentDetailsDTO.policyViolations, notNullValue());
    assertThat(appComponentDetailsDTO.policyViolations, hasSize(0));
    appComponentDetailsDTO = appComponentDetailsDTOs.get(1);
    assertThat(appComponentDetailsDTO.application.getId(), is(app2.getId()));
    assertThat(appComponentDetailsDTO.policyViolations, notNullValue());
    assertThat(appComponentDetailsDTO.policyViolations, hasSize(2));
    PolicyViolationSummaryDTO policyViolationSummaryDTO = getPolicyViolationSummaryDTO(policy1.getId(),
        appComponentDetailsDTO.policyViolations);
    assertThat(policyViolationSummaryDTO, notNullValue());
    assertThat(policyViolationSummaryDTO.policyName, is(policy1.getName()));
    assertThat(policyViolationSummaryDTO.threatLevel, is(2));
    assertThat(policyViolationSummaryDTO.stageTypeIds, containsInAnyOrder("build", "release"));
    assertThat(policyViolationSummaryDTO.reasons, hasSize(1));
    ReasonDTO reasonDTO = policyViolationSummaryDTO.reasons.get(0);
    assertThat(reasonDTO.constraintName, is("Test Constraint"));
    assertThat(reasonDTO.reasons, containsInAnyOrder("reason3"));
    policyViolationSummaryDTO = getPolicyViolationSummaryDTO(policy2.getId(), appComponentDetailsDTO.policyViolations);
    assertThat(policyViolationSummaryDTO, notNullValue());
    assertThat(policyViolationSummaryDTO.policyName, is(policy2.getName()));
    assertThat(policyViolationSummaryDTO.threatLevel, is(1));
    assertThat(policyViolationSummaryDTO.stageTypeIds, containsInAnyOrder("build"));
    assertThat(policyViolationSummaryDTO.reasons, hasSize(1));
    reasonDTO = policyViolationSummaryDTO.reasons.get(0);
    assertThat(reasonDTO.constraintName, is("Test Constraint"));
    assertThat(reasonDTO.reasons, containsInAnyOrder("reason2"));
  }

  @Test
  public void testGetApplicationDetailsByHash_MissingPolicy() {
    String hash = "ababababab";

    Application app = tempEntity.newApplicationWithParent("app");
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, hash, "groupId", "artifactId", "version");
    Policy policy = tempEntity.newPolicy(app.getId(), "policy", 1);
    String policyId = policy.getId();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    tempEntity.newPolicyViolation(policyEvaluation.getId(), policyEvaluation.getTime(), policy, "groupId", "artifactId",
        "version", hash, "reason");
    new PolicyDAO().delete(policy);

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs, notNullValue());
    assertThat(appComponentDetailsDTOs, hasSize(1));
    ApplicationComponentDetailsDTO appComponentDetailsDTO = appComponentDetailsDTOs.get(0);
    assertThat(appComponentDetailsDTO.application.getId(), is(app.getId()));
    assertThat(appComponentDetailsDTO.policyViolations, notNullValue());
    assertThat(appComponentDetailsDTO.policyViolations, hasSize(1));
    PolicyViolationSummaryDTO policyViolationSummaryDTO = getPolicyViolationSummaryDTO(policyId,
        appComponentDetailsDTO.policyViolations);
    assertThat(policyViolationSummaryDTO, notNullValue());
    assertThat(policyViolationSummaryDTO.policyName, is("policy"));
    assertThat(policyViolationSummaryDTO.threatLevel, is(1));
    assertThat(policyViolationSummaryDTO.stageTypeIds, containsInAnyOrder("build"));
    assertThat(policyViolationSummaryDTO.reasons, hasSize(1));
    ReasonDTO reasonDTO = policyViolationSummaryDTO.reasons.get(0);
    assertThat(reasonDTO.constraintName, is("Test Constraint"));
    assertThat(reasonDTO.reasons, containsInAnyOrder("reason"));
  }

  @Test
  public void testGetComponentNameByHash() throws Exception {
    String hash = "ababababab";
    Application app = tempEntity.newApplicationWithParent("app");
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, "groupId1", "artifactId1", "version1");
    // Force different times on the two ApplicationComponents
    Thread.sleep(1);
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, hash, "groupId2", "artifactId2", "version2");

    String name = componentDetailService.getComponentNameByHash(hash);
    assertThat(name, is("groupId2:artifactId2:version2"));
  }

  @Test
  public void testGetComponentNameByHash_UnnownHash() throws Exception {
    String hash = "ababababab";

    try {
      componentDetailService.getComponentNameByHash(hash);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("Unknown component with hash ababababab"));
    }
  }

  @Test
  public void testGetComponentNameByHash_NoGAV() throws Exception {
    String hash = "ababababab";
    Application app = tempEntity.newApplicationWithParent("app");
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, hash, null /* groupId */,
        null /* artifactId */, null /* version */, "somepath");

    String name = componentDetailService.getComponentNameByHash(hash);
    assertThat(name, is("somepath"));
  }

  @Test
  public void testGetComponentNameByHash_NoGAVOrPathnames() throws Exception {
    String hash = "ababababab";
    Application app = tempEntity.newApplicationWithParent("app");
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, hash, null /* groupId */,
        null /* artifactId */, null /* version */);

    String name = componentDetailService.getComponentNameByHash(hash);
    assertThat(name, nullValue());
  }

  private PolicyViolationSummaryDTO getPolicyViolationSummaryDTO(String policyId,
      List<PolicyViolationSummaryDTO> policyViolations)
  {
    for (PolicyViolationSummaryDTO policyViolation : policyViolations) {
      if (policyViolation.policyId.equals(policyId)) {
        return policyViolation;
      }
    }
    return null;
  }
}
