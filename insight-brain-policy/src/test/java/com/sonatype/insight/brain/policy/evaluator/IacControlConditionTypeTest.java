/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.IacControl;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.IacControlConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class IacControlConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  @Test
  public void testInternalEvaluateCondition_Description_Null() {
    Constraint iacComplianceFamilyIsHippa =
        createConstraint("id", "name", IacControlConditionType.ID, "is", IacControl.HIPAA_v2013.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(iacComplianceFamilyIsHippa);

    Policy policy = new Policy("policy-id", "policy-name");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    SecurityVulnerability securityVulnerability = new SecurityVulnerability();
    securityVulnerability.setRefId("REF-01");

    Component component = new Component();
    component.setMatchState(MatchState.EXACT);
    component.setSecurityVulnerabilities(new ArrayList<>());
    component.getSecurityVulnerabilities().add(securityVulnerability);

    List<Component> components = new ArrayList<>();
    components.add(component);

    tempEntity.newThirdPartyVulnerability("REF-01", "Lorem Ipsum No Controls");

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testInternalEvaluateCondition_Description_NoControls() {
    Constraint iacComplianceFamilyIsHippa =
        createConstraint("id", "name", IacControlConditionType.ID, "is", IacControl.HIPAA_v2013.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(iacComplianceFamilyIsHippa);

    Policy policy = new Policy("policy-id", "policy-name");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    SecurityVulnerability securityVulnerability = new SecurityVulnerability();
    securityVulnerability.setRefId("REF-01");

    Component component = new Component();
    component.setMatchState(MatchState.EXACT);
    component.setSecurityVulnerabilities(new ArrayList<>());
    component.getSecurityVulnerabilities().add(securityVulnerability);

    List<Component> components = new ArrayList<>();
    components.add(component);

    tempEntity.newThirdPartyVulnerability("REF-01", null);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testInternalEvaluateCondition_Is() {
    Constraint iacComplianceFamilyIsHippa =
        createConstraint("id", "name", IacControlConditionType.ID, "is", IacControl.HIPAA_v2013.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(iacComplianceFamilyIsHippa);

    Policy policy = new Policy("policy-id", "policy-name");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    SecurityVulnerability securityVulnerability = new SecurityVulnerability();
    securityVulnerability.setRefId("REF-01");

    Component component = new Component();
    component.setMatchState(MatchState.EXACT);
    component.setSecurityVulnerabilities(new ArrayList<>());
    component.getSecurityVulnerabilities().add(securityVulnerability);

    List<Component> components = new ArrayList<>();
    components.add(component);

    tempEntity.newThirdPartyVulnerability("REF-01",
        "S3 bucket policies should only allow requests that use HTTPS.\n" +
            "#### Controls\n" +
            "`CIS-AWS_v1.3.0_2.1.2` `CIS-AWS_v1.4.0_2.1.2` `CIS-Controls_v7.1_14.4` `CSA-CCM_v3.0.1_DSI-04` " +
            "`CSA-CCM_v3.0.1_EKM-03` `CSA-CCM_v3.0.1_IPY-04` `GDPR_v2016_32(1)(a)` `HIPAA_v2013_164.312(e)(2)(ii)` " +
            "`ISO-27001_v2013_A.13.2.1` `NIST-800-53_vRev4_SC-8(1)` `PCI-DSS_v3.2.1_2.2.3` `PCI-DSS_v3.2.1_4.1` " +
            "`SOC-2_v2017_CC6.7.2`");

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);

    PolicyAlert policyAlert = policyAlerts.get(0);
    ConditionFact conditionFact =
        policyAlert.getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0).getConditionFacts().get(0);
    assertThat(conditionFact.getConditionTypeId()).isEqualTo(IacControlConditionType.ID);
  }

  @Test
  public void testInternalEvaluateCondition_Is_Negative() {
    Constraint iacComplianceFamilyIsHippa =
        createConstraint("id", "name", IacControlConditionType.ID, "is", IacControl.HIPAA_v2013.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(iacComplianceFamilyIsHippa);

    Policy policy = new Policy("policy-id", "policy-name");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    SecurityVulnerability securityVulnerability = new SecurityVulnerability();
    securityVulnerability.setRefId("REF-01");

    Component component = new Component();
    component.setMatchState(MatchState.EXACT);
    component.setSecurityVulnerabilities(new ArrayList<>());
    component.getSecurityVulnerabilities().add(securityVulnerability);

    List<Component> components = new ArrayList<>();
    components.add(component);

    tempEntity.newThirdPartyVulnerability("REF-01",
        "S3 bucket policies should only allow requests that use HTTPS.\n" +
            "#### Controls\n" +
            "`CIS-AWS_v1.3.0_2.1.2` `CIS-AWS_v1.4.0_2.1.2` `CIS-Controls_v7.1_14.4` `CSA-CCM_v3.0.1_DSI-04` " +
            "`CSA-CCM_v3.0.1_EKM-03` `CSA-CCM_v3.0.1_IPY-04` `GDPR_v2016_32(1)(a)` " +
            "`ISO-27001_v2013_A.13.2.1` `NIST-800-53_vRev4_SC-8(1)` `PCI-DSS_v3.2.1_2.2.3` `PCI-DSS_v3.2.1_4.1` " +
            "`SOC-2_v2017_CC6.7.2`");

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testInternalEvaluateCondition_IsNot() {
    Constraint iacComplianceFamilyIsHippa =
        createConstraint("id", "name", IacControlConditionType.ID, "is not", IacControl.HIPAA_v2013.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(iacComplianceFamilyIsHippa);

    Policy policy = new Policy("policy-id", "policy-name");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    SecurityVulnerability securityVulnerability = new SecurityVulnerability();
    securityVulnerability.setRefId("REF-01");

    Component component = new Component();
    component.setMatchState(MatchState.EXACT);
    component.setSecurityVulnerabilities(new ArrayList<>());
    component.getSecurityVulnerabilities().add(securityVulnerability);

    List<Component> components = new ArrayList<>();
    components.add(component);

    tempEntity.newThirdPartyVulnerability("REF-01",
        "S3 bucket policies should only allow requests that use HTTPS.\n" +
            "#### Controls\n" +
            "`CIS-AWS_v1.3.0_2.1.2` `CIS-AWS_v1.4.0_2.1.2` `CIS-Controls_v7.1_14.4` `CSA-CCM_v3.0.1_DSI-04` " +
            "`CSA-CCM_v3.0.1_EKM-03` `CSA-CCM_v3.0.1_IPY-04` `GDPR_v2016_32(1)(a)` " +
            "`ISO-27001_v2013_A.13.2.1` `NIST-800-53_vRev4_SC-8(1)` `PCI-DSS_v3.2.1_2.2.3` `PCI-DSS_v3.2.1_4.1` " +
            "`SOC-2_v2017_CC6.7.2`");

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);

    PolicyAlert policyAlert = policyAlerts.get(0);
    ConditionFact conditionFact =
        policyAlert.getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0).getConditionFacts().get(0);
    assertThat(conditionFact.getConditionTypeId()).isEqualTo(IacControlConditionType.ID);
  }

  @Test
  public void testInternalEvaluateCondition_IsNot_Negative() {
    Constraint iacComplianceFamilyIsHippa =
        createConstraint("id", "name", IacControlConditionType.ID, "is not", IacControl.HIPAA_v2013.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(iacComplianceFamilyIsHippa);

    Policy policy = new Policy("policy-id", "policy-name");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    SecurityVulnerability securityVulnerability = new SecurityVulnerability();
    securityVulnerability.setRefId("REF-01");

    Component component = new Component();
    component.setMatchState(MatchState.EXACT);
    component.setSecurityVulnerabilities(new ArrayList<>());
    component.getSecurityVulnerabilities().add(securityVulnerability);

    List<Component> components = new ArrayList<>();
    components.add(component);

    tempEntity.newThirdPartyVulnerability("REF-01",
        "S3 bucket policies should only allow requests that use HTTPS.\n" +
            "#### Controls\n" +
            "`CIS-AWS_v1.3.0_2.1.2` `CIS-AWS_v1.4.0_2.1.2` `CIS-Controls_v7.1_14.4` `CSA-CCM_v3.0.1_DSI-04` " +
            "`CSA-CCM_v3.0.1_EKM-03` `CSA-CCM_v3.0.1_IPY-04` `GDPR_v2016_32(1)(a)` " +
            "`ISO-27001_v2013_A.13.2.1` `NIST-800-53_vRev4_SC-8(1)` `PCI-DSS_v3.2.1_2.2.3` `PCI-DSS_v3.2.1_4.1` " +
            "`SOC-2_v2017_CC6.7.2` `HIPAA_v2013_164.312(e)(2)(ii)`");

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }
}
