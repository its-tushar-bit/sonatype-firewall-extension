/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.DataSourceConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.component.ComponentDataSource.IDENTITY;
import static com.sonatype.insight.brain.model.component.ComponentDataSource.LICENSE;
import static com.sonatype.insight.brain.model.policy.conditions.DataSourceConditionType.HAS_NO_SUPPORT_FOR;
import static com.sonatype.insight.brain.model.policy.conditions.DataSourceConditionType.HAS_SUPPORT_FOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DataSourceConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", DataSourceConditionType.ID, operator, value);
  }

  @Test
  public void testEvaluatehasSupportForIdentity_HDS() {
    Constraint constraint = createConstraint(HAS_SUPPORT_FOR, IDENTITY.getId());
    testEvaluateDataSource(ComponentIdentifier.FORMAT_MAVEN, constraint, fromHds(),
        fromThirdParty(), "Data Source has support for Identity");
  }

  @Test
  public void testEvaluatehasNoSupportForIdentity_LQA() {
    Constraint constraint = createConstraint(HAS_NO_SUPPORT_FOR, IDENTITY.getId());
    testEvaluateDataSource("composer", constraint, fromLqa(),
        fromHds(), "Data Source has no support for Identity");
  }

  @Test
  public void testEvaluatehasNoSupportForIdentity_ThirdParty() {
    Constraint constraint = createConstraint(HAS_NO_SUPPORT_FOR, IDENTITY.getId());
    testEvaluateDataSource("composer", constraint, fromThirdParty(),
        fromHds(), "Data Source has no support for Identity");
  }

  @Test
  public void testEvaluatehasSupportForLicense_HDS() {
    Constraint constraint = createConstraint(HAS_SUPPORT_FOR, LICENSE.getId());
    testEvaluateDataSource(ComponentIdentifier.FORMAT_MAVEN, constraint, fromHds(), fromThirdParty(),
        "Data Source has support for License");
  }

  @Test
  public void testEvaluatehasNoSupportForLicense_LQA() {
    Constraint constraint = createConstraint(HAS_NO_SUPPORT_FOR, LICENSE.getId());
    testEvaluateDataSource(ComponentIdentifier.FORMAT_MAVEN, constraint, fromLqa(),
        fromHds(), "Data Source has no support for License");
  }

  @Test
  public void testEvaluatehasNoSupportForLicense_ThirdParty() {
    Constraint constraint = createConstraint(HAS_NO_SUPPORT_FOR, LICENSE.getId());
    testEvaluateDataSource(ComponentIdentifier.FORMAT_MAVEN, constraint, fromThirdParty(), fromHds(),
        "Data Source has no support for License");
  }

  @Test
  public void testValidateCondition_InvalidValue() {
    Condition condition = new Condition(DataSourceConditionType.ID, HAS_SUPPORT_FOR, "abc");
    assertThatThrownBy(() -> new DataSourceConditionType().validateCondition(null, condition, null /* applicationId */))
        .isInstanceOf(InvalidConditionException.class)
        .hasMessageEndingWith("Value not supported: abc");
  }

  @Test
  public void testEvaluate_unknownComponent_HDS() {
    Constraint constraint = createConstraint(HAS_SUPPORT_FOR, IDENTITY.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId", "Policy Name");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component = new Component();
    component.setAnalyzerFeatures(fromHds());
    component.setMatchState(MatchState.UNKNOWN);
    component.setProprietary(false);
    components.add(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID, DataSourceConditionType.ID,
        policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Data Source has support for Identity");
  }

  @Test
  public void testEvaluate_unknownComponent_LQA() {
    Constraint constraint = createConstraint(HAS_SUPPORT_FOR, IDENTITY.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId", "Policy Name");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component = new Component();
    component.setAnalyzerFeatures(fromLqa());
    component.setMatchState(MatchState.UNKNOWN);
    component.setProprietary(false);
    components.add(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(0);
  }

  @Test
  public void testEvaluate_unknownComponent_ThirdParty() {
    Constraint constraint = createConstraint(HAS_SUPPORT_FOR, IDENTITY.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId", "Policy Name");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component = new Component();
    component.setAnalyzerFeatures(fromThirdParty());
    component.setMatchState(MatchState.UNKNOWN);
    component.setProprietary(false);
    components.add(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(0);
  }

  @Test
  public void testEvaluatehasNoSupportForLicense_NoMetadata_License_Maven() {
    Constraint constraint = createConstraint(HAS_NO_SUPPORT_FOR, LICENSE.getId());
    assertNoViolations(ComponentIdentifier.FORMAT_MAVEN, constraint, null);
  }

  private void testEvaluateDataSource(
      String format,
      Constraint constraint,
      AnalyzerFeatures analyzerFeatures1,
      AnalyzerFeatures analyzerFeatures2,
      String expectedConditionMessage)
  {
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId", "Policy Name");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = forCoordinatesPackageUrl(format, "g", "a", "v", "q", "t");
    component1.setAnalyzerFeatures(analyzerFeatures1);
    components.add(component1);
    Component component2 = forCoordinatesPackageUrl(format, "g", "a", "v", "q", "t");
    component2.setAnalyzerFeatures(analyzerFeatures2);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, DataSourceConditionType.ID,
        policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo(expectedConditionMessage);
  }

  private void assertNoViolations(
      final String format,
      final Constraint constraint,
      final AnalyzerFeatures analyzerFeatures)
  {
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId", "Policy Name");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = forCoordinatesPackageUrl(format, "g", "a", "v", "q", "t");
    component1.setAnalyzerFeatures(analyzerFeatures);
    components.add(component1);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }

  private AnalyzerFeatures fromHds() {
    return new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.COORDINATE, "CLI", true, true, true);
  }

  private AnalyzerFeatures fromLqa() {
    return new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.COORDINATE, "CLI", false, false, true);
  }

  private AnalyzerFeatures fromThirdParty() {
    return new AnalyzerFeatures(AnalysisSource.THIRD_PARTY, AnalysisType.COORDINATE, "CLI", null);
  }
}
