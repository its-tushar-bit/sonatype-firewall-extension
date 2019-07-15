/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.PackageUrlConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @since 1.69
 */
public class PackageUrlConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private static final String OPERATOR_MATCH = "matches";

  private static final String OPERATOR_DO_NOT_MATCH = "does not match";
  
  private static final String UNKNOWN_FORMAT = "unknown";

  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", PackageUrlConditionType.ID, operator, "pkg:" + value);
  }

  @Test
  public void testEvaluate_Maven_MatchExact() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_MAVEN + "/g2/a2@v2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_MAVEN, constraint);
  }

  @Test
  public void testEvaluate_Pypi_MatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_PYPI + "/a2@v2?qualifier=q2&extension=e2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_PYPI, constraint);
  }
  
  @Test
  public void testEvaluate_Aname_MatchExact() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_ANAME + "/a2@v2?qualifier=q2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_ANAME, constraint);
  }
  
  @Test
  public void testEvaluate_Rpm_MatchExact() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_RPM + "/a2@v2?arch=q2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_RPM, constraint);
  }
  
  @Test
  public void testEvaluate_Npm_MatchExact() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_NPM + "/a2@v2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_NPM, constraint);
  }
  
  @Test
  public void testEvaluate_Nuget_MatchExact() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_NUGET + "/a2@v2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_NUGET, constraint);
  }
  
  @Test
  public void testEvaluate_Golang_MatchExact() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_GOLANG + "/a2@v2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_GOLANG, constraint);
  }
  
  @Test
  public void testEvaluate_Rubygems_MatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_RUBYGEMS + "/a2@v2?platform=q2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_RUBYGEMS, constraint);
  }
  
  @Test
  public void testEvaluate_Unknown_MatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, UNKNOWN_FORMAT + "/g2/a2@v2?qualifier=q2");
    testEvaluate_MatchExact(UNKNOWN_FORMAT, constraint);
  }

  private void testEvaluate_MatchExact(String format, Constraint constraint) {
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 =
        ComponentFactory.forCoordinatesPackageUrl(format, "g1", "a1", "v1", "e1", "q1");
    components.add(component1);
    Component component2 =
        ComponentFactory.forCoordinatesPackageUrl(format, "g2", "a2", "v2", "e2", "q2");
    components.add(component2);
    Component component3 = new Component();
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, PackageUrlConditionType.ID,
        policyAlerts);
  }
  
  @Test
  public void testEvaluate_Maven_MatchGavecNotGavce() throws Exception {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + "/g/a@v?type=e&classifier=c");

    Component componentGavec = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");
    Component componentGavce = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "c", "e");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGavec, componentGavce));
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(componentGavec, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_Maven_MatchGaveNotGavc() throws Exception {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + "/g/a@v?type=e");

    Component componentGave = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavc = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "", "e");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGave, componentGavc));
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(componentGave, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_Maven_MatchGavAnyExtensionAnyClassifier() throws Exception {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + "/g/a@v");

    Component componentGav3 = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v");
    Component componentGav5 = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "", "");
    Component componentGave = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavc = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "", "c");
    Component componentGavec = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");

    List<PolicyAlert> policyAlerts = evaluate(policy,
        Arrays.asList(componentGav3, componentGav5, componentGave, componentGavc, componentGavec));
    assertThat(policyAlerts).hasSize(5);
    for (PolicyAlert policyAlert : policyAlerts) {
      assertFactCounts(1, 1, policyAlert);
    }
    assertContainsPolicyAlert(componentGav3, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGav5, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGave, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGavc, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGavec, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates() throws Exception {
    //Only Name
    testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates("maven/a");
    //Only name and namespace
    testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates("maven/g/a");
    // Name and version
    testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates("maven/a@v");
  }

  private void testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates(
      final String coordinatesValue) throws Exception
  {
    Policy policy = createPolicy(coordinatesValue);

    Component componentGav = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v");
    Component componentGave = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavec = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGav, componentGave, componentGavec));
    assertThat(policyAlerts).hasSize(3);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertFactCounts(1, 1, policyAlerts.get(1));
    assertFactCounts(1, 1, policyAlerts.get(2));
    assertContainsPolicyAlert(componentGav, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGave, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGavec, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates() throws Exception {
    testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates("a-name/n");
    testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates("a-name/n?qualifier=q");
    testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates("a-name/n@v?qualifier=q");
  }

  private void testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates(final String coordinatesValue) throws Exception {
    Policy policy = createPolicy(coordinatesValue);
    
    Component componentNqv = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_ANAME, "", "n", "v", "", "q");

    List<PolicyAlert> policyAlerts = evaluate(policy, Collections.singletonList(componentNqv));
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(componentNqv, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_Maven_EmptyClassifier_Matches_EmptyClassifierValue() throws Exception {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + "/g/a@v?type=e&classifier=");

    Component componentGave = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavec = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGave, componentGavec));
    assertThat(policyAlerts).hasSize(2);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(componentGave, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
  }
  
  @Test
  public void testEvaluate_Maven_WildcardClassifierCoordinate_Matches_AnyClassifierValue() throws Exception {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + "/g/a@v?type=e&classifier=*");

    Component componentGave = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavec = ComponentFactory
        .forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGave, componentGavec));
    assertThat(policyAlerts).hasSize(2);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertFactCounts(1, 1, policyAlerts.get(1));
    assertContainsPolicyAlert(componentGave, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGavec, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
  }

  private Policy createPolicy(final String constraintValue) {
    final Policy policy = new Policy("policyId", "policyName");
    policy.setConstraints(Collections.singletonList(createConstraint(OPERATOR_MATCH, constraintValue)));
    policy.setAction(BuildStageType.ID, FailActionType.ID);
    return policy;
  }

  @Test
  public void testEvaluate_Maven_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_MAVEN + "/g2/a*@v2");
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_MAVEN, constraint);
  }
 
  @Test
  public void testEvaluate_Aname_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_ANAME + "/a2@v*?qualifier=q2");
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_ANAME, constraint);
  }

  @Test
  public void testEvaluate_Pypi_MatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_PYPI + "/a2@v*?qualifier=q2&extension=e2");
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_PYPI, constraint);
  }
  
  @Test
  public void testEvaluate_Rpm_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_RPM + "/a2@v*?arch=q2");
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_RPM, constraint);
  }
  
  @Test
  public void testEvaluate_Npm_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_NPM + "/a2@v*");
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_NPM, constraint);
  }
  
  @Test
  public void testEvaluate_Nuget_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_NUGET + "/a2@v*");
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_NUGET, constraint);
  }
  
  @Test
  public void testEvaluate_Golang_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_GOLANG + "/a2@v*");
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_GOLANG, constraint);
  }
  
  @Test
  public void testEvaluate_Rubygems_MatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_RUBYGEMS + "/a2@v*?platform=q2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_RUBYGEMS, constraint);
  }
  
  @Test
  public void testEvaluate_Unknown_MatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, UNKNOWN_FORMAT + "/g2/a2@v*?qualifier=q2");
    testEvaluate_MatchExact(UNKNOWN_FORMAT, constraint);
  }

  private void testEvaluate_MatchWildcard(String format, Constraint constraint) {
    
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 =
        ComponentFactory.forCoordinatesPackageUrl(format, "g1", "a1", "v1", "e1", "q1");
    components.add(component1);
    Component component2 =
        ComponentFactory.forCoordinatesPackageUrl(format, "g2", "a2", "v2", "e2", "q2");
    components.add(component2);
    Component component3 = new Component();
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, PackageUrlConditionType.ID,
        policyAlerts);
  }

  @Test
  public void testEvaluate_Maven_DoNotMatchExact() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_MAVEN + "/g2/a2@v2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_MAVEN, constraint);
  }

  @Test
  public void testEvaluate_Aname_DoNotMatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_ANAME + "/a2@v2?qualifier=q2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_ANAME, constraint);
  }

  @Test
  public void testEvaluate_Pypi_DoNotMatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_PYPI + "/a2@v2?qualifier=q2&extension=e2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_PYPI, constraint);
  }
  
  @Test
  public void testEvaluate_Rpm_DoNotMatchExact() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_RPM + "/a2@v2?arch=q2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_RPM, constraint);
  }
  
  @Test
  public void testEvaluate_Npm_DoNotMatchExact() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_NPM + "/a2@v2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_NPM, constraint);
  }
  
  @Test
  public void testEvaluate_Nuget_DoNotMatchExact() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_NUGET + "/a2@v2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_NUGET, constraint);
  }
  
  @Test
  public void testEvaluate_Golang_DoNotMatchExact() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_GOLANG + "/a2@v2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_GOLANG, constraint);
  }
  
  @Test
  public void testEvaluate_Rubygems_DoNotMatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_RUBYGEMS + "/a2@v2?platform=q2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_RUBYGEMS, constraint);
  }
  
  @Test
  public void testEvaluate_Unknown_DoNotMatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, UNKNOWN_FORMAT + "/g2/a2@v2?qualifier=q2");
    testEvaluate_DoNotMatchExact(UNKNOWN_FORMAT, constraint);
  }

  private void testEvaluate_DoNotMatchExact(String format, Constraint constraint) {
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 =
        ComponentFactory.forCoordinatesPackageUrl(format, "g1", "a1", "v1", "e1", "q1");
    components.add(component1);
    Component component2 =
        ComponentFactory.forCoordinatesPackageUrl(format, "g2", "a2", "v2", "e2", "q2");
    components.add(component2);
    Component component3 = new Component();
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, PackageUrlConditionType.ID,
        policyAlerts);
  }

  @Test
  public void testEvaluate_Maven_DoNotMatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_MAVEN + "/g2/a*@v2");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_MAVEN, constraint);
  }

  @Test
  public void testEvaluate_Aname_DoNotMatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_ANAME + "/a2@v*?qualifier=q2");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_ANAME, constraint);
  }

  @Test
  public void testEvaluate_Pypi_DoNotMatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_PYPI + "/a2@v*?qualifier=q2&extension=e2");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_PYPI, constraint);
  }
  
  @Test
  public void testEvaluate_Rpm_DoNotMatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_RPM + "/a2@v*?arch=q2");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_RPM, constraint);
  }
  
  @Test
  public void testEvaluate_Npm_DoNotMatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_NPM + "/a2@v*");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_NPM, constraint);
  }
  
  @Test
  public void testEvaluate_Nuget_DoNotMatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_NUGET + "/a2@v*");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_NUGET, constraint);
  }
  
  @Test
  public void testEvaluate_Golang_DoNotMatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_GOLANG + "/a2@v*");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_GOLANG, constraint);
  }
  
  @Test
  public void testEvaluate_Rubygems_DoNotMatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_RUBYGEMS + "/a2@v*?platform=q2");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_RUBYGEMS, constraint);
  }
  
  @Test
  public void testEvaluate_Unknown_DoNotMatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, UNKNOWN_FORMAT + "/g2/a2@v*?qualifier=q2");
    testEvaluate_DoNotMatchWildcard(UNKNOWN_FORMAT, constraint);
  }

  private void testEvaluate_DoNotMatchWildcard(String format, Constraint constraint) {
   
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 =
        ComponentFactory.forCoordinatesPackageUrl(format, "g1", "a1", "v1", "e1", "q1");
    components.add(component1);
    Component component2 =
        ComponentFactory.forCoordinatesPackageUrl(format, "g2", "a2", "v2", "e2", "q2");
    components.add(component2);
    Component component3 = new Component();
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, PackageUrlConditionType.ID,
        policyAlerts);
  }

  @Test
  public void testEvaluate_EscapeUnsafeCharacter() {
    String artifactId = "test%40test%23";
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    Constraint constraint = createConstraint(OPERATOR_MATCH, "maven/g1/" + artifactId);
    policy.addConstraint(constraint);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 =
        ComponentFactory.forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN,
        "g1", "test@test#", "v1");
    components.add(component1);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, PackageUrlConditionType.ID,
        policyAlerts);
  }

  @Test
  public void testValidateCondition_NullPackageUrl() {
    Condition condition = new Condition(PackageUrlConditionType.ID, OPERATOR_MATCH, null);
    assertThatThrownBy(() -> {
      new PackageUrlConditionType().validateCondition(null, condition, null);
    }).isInstanceOf(InvalidConditionException.class).hasMessageEndingWith("missing package URL");
  }

  @Test
  public void testValidateCondition_EmptyPackageUrl() {
    Condition condition = new Condition(PackageUrlConditionType.ID, OPERATOR_MATCH, " ");
    assertThatThrownBy(() -> {
      new PackageUrlConditionType().validateCondition(null, condition, null);
    }).isInstanceOf(InvalidConditionException.class).hasMessageEndingWith("missing package URL");
  }
  
  @Test
  public void testValidateCondition_InvalidPackageUrl() {
    Condition condition = new Condition(PackageUrlConditionType.ID, OPERATOR_MATCH, "invalid");
    assertThatThrownBy(() -> {
      new PackageUrlConditionType().validateCondition(null, condition, null);
    }).isInstanceOf(InvalidConditionException.class).hasMessageEndingWith("invalid package URL");
  }

  @Test
  public void testConvertIfNeeded_UnsupportedCoordinateFormat_DoesNotThrowNullPointerException() throws Exception {
    new Condition(PackageUrlConditionType.ID, OPERATOR_MATCH, "pkg:unknown/g1/a1@v1").getValue();
  }

  @Test
  public void testConvertIfNeeded() throws Exception {
    convertIfNeededMaven();
    convertIfNeededAname();
    convertIfNeededPypi();
    convertIfNeededGolang();
    convertIfNeededNpm();
    convertIfNeededRpm();
    convertIfNeededRubygems();
    convertIfNeededNuget();
    convertIfNeededUnknown();
  }
  
  private void convertIfNeededMaven() {
    assertConvertIfNeeded("pkg:maven/a", "pkg:maven/*/a@*?classifier=*&type=*");
    assertConvertIfNeeded("pkg:maven/g/a", "pkg:maven/g/a@*?classifier=*&type=*");
    assertConvertIfNeeded("pkg:maven/g/a@v", "pkg:maven/g/a@v?classifier=*&type=*");
    assertConvertIfNeeded("pkg:maven/g/a@v?type=e&classifier=", "pkg:maven/g/a@v?classifier=*&type=e");
    assertConvertIfNeeded("pkg:maven/g/a@v?type=", "pkg:maven/g/a@v?classifier=*&type=*");
    assertConvertIfNeeded("pkg:maven/g/a@v?classifier=", "pkg:maven/g/a@v?classifier=*&type=*");
    assertConvertIfNeeded("pkg:maven/g/a@v?type=&classifier=c", "pkg:maven/g/a@v?classifier=c&type=*");
    assertConvertIfNeeded("pkg:maven/g/a@v?type=&classifier=", "pkg:maven/g/a@v?classifier=*&type=*");
    assertConvertIfNeeded("pkg:maven/a@v?type=&classifier=", "pkg:maven/*/a@v?classifier=*&type=*");
    assertConvertIfNeeded("pkg:maven/g/a@v?type=e&classifier=c", "pkg:maven/g/a@v?classifier=c&type=e");
  }
  
  private void convertIfNeededAname() {
    assertConvertIfNeeded("pkg:a-name/n", "pkg:a-name/n@*?qualifier=*");
    assertConvertIfNeeded("pkg:a-name/n?qualifier=q", "pkg:a-name/n@*?qualifier=q");
    assertConvertIfNeeded("pkg:a-name/n@v?qualifier=q", "pkg:a-name/n@v?qualifier=q");
    assertConvertIfNeeded("pkg:a-name/n@v", "pkg:a-name/n@v?qualifier=*");
    assertConvertIfNeeded("pkg:a-name/n@v?qualifier=", "pkg:a-name/n@v?qualifier=*");
  }
  
  private void convertIfNeededPypi() {
    assertConvertIfNeeded("pkg:pypi/n", "pkg:pypi/n@*?extension=*&qualifier=*");
    assertConvertIfNeeded("pkg:pypi/n@v", "pkg:pypi/n@v?extension=*&qualifier=*");
    assertConvertIfNeeded("pkg:pypi/n@v?qualifier=q", "pkg:pypi/n@v?extension=*&qualifier=q");
    assertConvertIfNeeded("pkg:pypi/n@v?extension=e", "pkg:pypi/n@v?extension=e&qualifier=*");
    assertConvertIfNeeded("pkg:pypi/n@v?qualifier=", "pkg:pypi/n@v?extension=*&qualifier=*");
    assertConvertIfNeeded("pkg:pypi/n@v?extension=", "pkg:pypi/n@v?extension=*&qualifier=*");
    assertConvertIfNeeded("pkg:pypi/n@v?extension=e&qualifier=q", "pkg:pypi/n@v?extension=e&qualifier=q");
    assertConvertIfNeeded("pkg:pypi/n@v?extension=e&qualifier=", "pkg:pypi/n@v?extension=e&qualifier=*");
    assertConvertIfNeeded("pkg:pypi/n@v?extension=&qualifier=q", "pkg:pypi/n@v?extension=*&qualifier=q");
    assertConvertIfNeeded("pkg:pypi/n?extension=e&qualifier=q", "pkg:pypi/n@*?extension=e&qualifier=q");
  }

  private void convertIfNeededNuget() {
    assertConvertIfNeeded("pkg:nuget/n", "pkg:nuget/n@*");
    assertConvertIfNeeded("pkg:nuget/n@v", "pkg:nuget/n@v");
    assertConvertIfNeeded("pkg:nuget/n/n@v", "pkg:nuget/n/n@v");
  }

  private void convertIfNeededNpm() {
    assertConvertIfNeeded("pkg:npm/n", "pkg:npm/n@*");
    assertConvertIfNeeded("pkg:npm/n@v", "pkg:npm/n@v");
    assertConvertIfNeeded("pkg:npm/a/n@v", "pkg:npm/a/n@v");
  }

  private void convertIfNeededRpm() {
    assertConvertIfNeeded("pkg:rpm/n", "pkg:rpm/n@*?arch=*");
    assertConvertIfNeeded("pkg:rpm/n?arch=q", "pkg:rpm/n@*?arch=q");
    assertConvertIfNeeded("pkg:rpm/n@v?arch=q", "pkg:rpm/n@v?arch=q");
    assertConvertIfNeeded("pkg:rpm/n@v?arch=", "pkg:rpm/n@v?arch=*");
    assertConvertIfNeeded("pkg:rpm/n@v", "pkg:rpm/n@v?arch=*");
    assertConvertIfNeeded("pkg:rpm/a/n@v", "pkg:rpm/a/n@v?arch=*");
  }

  private void convertIfNeededGolang() {
    assertConvertIfNeeded("pkg:golang/n", "pkg:golang/n@*");
    assertConvertIfNeeded("pkg:golang/n@v", "pkg:golang/n@v");
    assertConvertIfNeeded("pkg:golang/a/n@v", "pkg:golang/a/n@v");
    assertConvertIfNeeded("pkg:golang/a/n", "pkg:golang/a/n@*");
  }

  private void convertIfNeededRubygems() {
    assertConvertIfNeeded("pkg:gem/n", "pkg:gem/n@*?platform=*");
    assertConvertIfNeeded("pkg:gem/n?platform=q", "pkg:gem/n@*?platform=q");
    assertConvertIfNeeded("pkg:gem/n@v?platform=q", "pkg:gem/n@v?platform=q");
    assertConvertIfNeeded("pkg:gem/n@v?platform=", "pkg:gem/n@v?platform=*");
    assertConvertIfNeeded("pkg:gem/n@v", "pkg:gem/n@v?platform=*");
    assertConvertIfNeeded("pkg:gem/a/n@v", "pkg:gem/a/n@v?platform=*");
  }
  
  private void convertIfNeededUnknown() {
    assertConvertIfNeeded("pkg:unknown/name", "pkg:unknown/name@*");
    assertConvertIfNeeded("pkg:unknown/name?qualifier=q", "pkg:unknown/name@*?qualifier=q");
    assertConvertIfNeeded("pkg:unknown/name@v?qualifier=q", "pkg:unknown/name@v?qualifier=q");
    assertConvertIfNeeded("pkg:unknown/name@v", "pkg:unknown/name@v");
    assertConvertIfNeeded("pkg:unknown/name@v?qualifier=", "pkg:unknown/name@v");
    assertConvertIfNeeded("pkg:unknown/namespace/name@v?qualifier=q", "pkg:unknown/namespace/name@v?qualifier=q");
    assertConvertIfNeeded("pkg:unknown/namespace/name@v?qualifier=q&type=t&arch=a",
        "pkg:unknown/namespace/name@v?arch=a&qualifier=q&type=t");
  }
  
  private void assertConvertIfNeeded(final String value, final String expectedConvertedValue) {
    assertThat(createCoordinateCondition(value).getValue()).isEqualTo(expectedConvertedValue);
  }
  
  private Condition createCoordinateCondition(final String value) {
    return new Condition(PackageUrlConditionType.ID, OPERATOR_MATCH, value);
  }
}
