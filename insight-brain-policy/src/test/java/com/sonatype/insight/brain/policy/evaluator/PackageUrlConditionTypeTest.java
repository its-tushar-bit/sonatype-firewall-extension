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
import com.sonatype.insight.lqa.LqaFormat;

import com.github.packageurl.PackageURL.StandardTypes;
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
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_MAVEN, constraint,
        "(matches package URL pkg:maven/g2/a2@v2?classifier=*&type=*)");
  }

  @Test
  public void testEvaluate_Pypi_MatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_PYPI + "/a2@v2?qualifier=q2&extension=e2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_PYPI, constraint,
        "(matches package URL pkg:pypi/a2@v2?extension=e2&qualifier=q2)");
  }

  @Test
  public void testEvaluate_Aname_MatchExact() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_ANAME + "/a2@v2?qualifier=q2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_ANAME, constraint,
        "(matches package URL pkg:a-name/a2@v2?qualifier=q2)");
  }

  @Test
  public void testEvaluate_Rpm_MatchExact() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_RPM + "/a2@v2?arch=q2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_RPM, constraint, "(matches package URL pkg:rpm/a2@v2?arch=q2)");
  }

  @Test
  public void testEvaluate_Npm_MatchExact() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_NPM + "/a2@v2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_NPM, constraint, "(matches package URL pkg:npm/a2@v2)");
  }

  @Test
  public void testEvaluate_Nuget_MatchExact() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_NUGET + "/a2@v2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_NUGET, constraint, "(matches package URL pkg:nuget/a2@v2)");
  }

  @Test
  public void testEvaluate_Nuget_MatchExact_IgnoreCase() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_NUGET + "/A2@v2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_NUGET, constraint, "(matches package URL pkg:nuget/A2@v2)");
  }

  @Test
  public void testEvaluate_Golang_MatchExact() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_GOLANG + "/a2@v2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_GOLANG, constraint, "(matches package URL pkg:golang/a2@v2)");
  }

  @Test
  public void testEvaluate_Rubygems_MatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_RUBYGEMS + "/a2@v2?platform=q2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_RUBYGEMS, constraint,
        "(matches package URL pkg:gem/a2@v2?platform=q2)");
  }

  @Test
  public void testEvaluate_Unknown_MatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, UNKNOWN_FORMAT + "/g2/a2@v2?qualifier=q2");
    testEvaluate_MatchExact(UNKNOWN_FORMAT, constraint, "(matches package URL pkg:unknown/g2/a2@v2?qualifier=q2)");
  }

  private void testEvaluate_MatchExact(String format, Constraint constraint, String expectedConditionMessage) {
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = forCoordinatesPackageUrl(format, "g1", "a1", "v1", "e1", "q1");
    components.add(component1);
    Component component2 = forCoordinatesPackageUrl(format, "g2", "a2", "v2", "e2", "q2");
    components.add(component2);
    Component component3 = new Component();
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, PackageUrlConditionType.ID,
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
    assertThat(actualReason)
        .isEqualTo("Coordinates were " + component2.getDisplayNameFromIdentifier() + " " + expectedConditionMessage);
  }

  @Test
  public void testEvaluate_Maven_MatchGavecNotGavce() {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + "/g/a@v?type=e&classifier=c");

    Component componentGavec = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");
    Component componentGavce = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "c", "e");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGavec, componentGavce));
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(componentGavec, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason)
        .isEqualTo("Coordinates were g : a : e : c : v (matches package URL pkg:maven/g/a@v?classifier=c&type=e)");
  }

  @Test
  public void testEvaluate_Maven_MatchGaveNotGavc() {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + "/g/a@v?type=e");

    Component componentGave = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavc = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "", "e");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGave, componentGavc));
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(componentGave, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason)
        .isEqualTo("Coordinates were g : a : e : v (matches package URL pkg:maven/g/a@v?classifier=*&type=e)");
  }

  @Test
  public void testEvaluate_Maven_MatchGavAnyExtensionAnyClassifier() {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + "/g/a@v");

    Component componentGav3 = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v");
    Component componentGav5 = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "", "");
    Component componentGave = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavc = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "", "c");
    Component componentGavec = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");

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
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason)
        .isEqualTo("Coordinates were g : a :  : c : v (matches package URL pkg:maven/g/a@v?classifier=*&type=*)");
    actualReason = policyAlerts.get(1)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason)
        .isEqualTo("Coordinates were g : a : e : c : v (matches package URL pkg:maven/g/a@v?classifier=*&type=*)");
    actualReason = policyAlerts.get(2)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason)
        .isEqualTo("Coordinates were g : a : e : v (matches package URL pkg:maven/g/a@v?classifier=*&type=*)");
    actualReason = policyAlerts.get(3)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason)
        .isEqualTo("Coordinates were g : a : v (matches package URL pkg:maven/g/a@v?classifier=*&type=*)");
    actualReason = policyAlerts.get(4)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason)
        .isEqualTo("Coordinates were g : a : v (matches package URL pkg:maven/g/a@v?classifier=*&type=*)");
  }

  @Test
  public void testEvaluate_Maven_LegacyConditionsWithEmptyVCoordinates() throws Exception {
    // Only name and namespace
    testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates("maven/g/a",
        "(matches package URL pkg:maven/g/a@*?classifier=*&type=*)");
  }

  private void testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates(
      final String coordinatesValue,
      final String expectedConditionMessage)
  {
    Policy policy = createPolicy(coordinatesValue);

    Component componentGav = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v");
    Component componentGave = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavec = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");

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
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : e : c : v " + expectedConditionMessage);
    actualReason = policyAlerts.get(1)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : e : v " + expectedConditionMessage);
    actualReason = policyAlerts.get(2)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : v " + expectedConditionMessage);
  }

  @Test
  public void testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates() throws Exception {
    testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates("a-name/n",
        "(matches package URL pkg:a-name/n@*?qualifier=*)");
    testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates("a-name/n?qualifier=q",
        "(matches package URL pkg:a-name/n@*?qualifier=q)");
    testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates("a-name/n@v?qualifier=q",
        "(matches package URL pkg:a-name/n@v?qualifier=q)");
  }

  private void testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates(
      final String coordinatesValue,
      final String expectedConditionMessage)
  {
    Policy policy = createPolicy(coordinatesValue);

    Component componentNqv = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_ANAME, "", "n", "v", "", "q");

    List<PolicyAlert> policyAlerts = evaluate(policy, Collections.singletonList(componentNqv));
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(componentNqv, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Coordinates were n (q) v " + expectedConditionMessage);
  }

  @Test
  public void testEvaluate_Maven_EmptyClassifier_Matches_EmptyClassifierValue() {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + "/g/a@v?type=e&classifier=");

    Component componentGave = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavec = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGave, componentGavec));
    assertThat(policyAlerts).hasSize(2);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(componentGave, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason)
        .isEqualTo("Coordinates were g : a : e : c : v (matches package URL pkg:maven/g/a@v?classifier=*&type=e)");
  }

  @Test
  public void testEvaluate_Maven_WildcardClassifierCoordinate_Matches_AnyClassifierValue() {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + "/g/a@v?type=e&classifier=*");

    Component componentGave = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavec = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGave, componentGavec));
    assertThat(policyAlerts).hasSize(2);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertFactCounts(1, 1, policyAlerts.get(1));
    assertContainsPolicyAlert(componentGave, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGavec, policy, policy.getConstraints().get(0), FailActionType.ID,
        PackageUrlConditionType.ID, policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason)
        .isEqualTo("Coordinates were g : a : e : c : v (matches package URL pkg:maven/g/a@v?classifier=*&type=e)");
    actualReason = policyAlerts.get(1)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason)
        .isEqualTo("Coordinates were g : a : e : v (matches package URL pkg:maven/g/a@v?classifier=*&type=e)");
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
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_MAVEN, constraint,
        "(matches package URL pkg:maven/g2/a*@v2?classifier=*&type=*)");
  }

  @Test
  public void testEvaluate_Aname_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_ANAME + "/a2@v*?qualifier=q2");
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_ANAME, constraint,
        "(matches package URL pkg:a-name/a2@v*?qualifier=q2)");
  }

  @Test
  public void testEvaluate_Pypi_MatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_PYPI + "/a2@v*?qualifier=q2&extension=e2");
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_PYPI, constraint,
        "(matches package URL pkg:pypi/a2@v*?extension=e2&qualifier=q2)");
  }

  @Test
  public void testEvaluate_Rpm_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_RPM + "/a2@v*?arch=q2");
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_RPM, constraint,
        "(matches package URL pkg:rpm/a2@v*?arch=q2)");
  }

  @Test
  public void testEvaluate_Npm_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_NPM + "/a2@v*");
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_NPM, constraint, "(matches package URL pkg:npm/a2@v*)");
  }

  @Test
  public void testEvaluate_Nuget_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_NUGET + "/a2@v*");
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_NUGET, constraint, "(matches package URL pkg:nuget/a2@v*)");
  }

  @Test
  public void testEvaluate_Golang_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_GOLANG + "/a2@v*");
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_GOLANG, constraint, "(matches package URL pkg:golang/a2@v*)");
  }

  @Test
  public void testEvaluate_Rubygems_MatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_RUBYGEMS + "/a2@v*?platform=q2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_RUBYGEMS, constraint,
        "(matches package URL pkg:gem/a2@v*?platform=q2)");
  }

  @Test
  public void testEvaluate_Swift_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_SWIFT + "/a2@v*");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_SWIFT, constraint, "(matches package URL pkg:swift/a2@v*)");
  }

  @Test
  public void testEvaluate_Cocoapods_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_COCOAPODS + "/a2@v*");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_COCOAPODS, constraint,
        "(matches package URL pkg:cocoapods/a2@v*)");
  }

  @Test
  public void testEvaluate_Pecoff_MatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, StandardTypes.GENERIC + "/a2@v*?nexustype=pecoff");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_PECOFF, constraint,
        "(matches package URL pkg:generic/a2@v*?nexustype=pecoff)");
  }

  @Test
  public void testEvaluate_Pecoff_Namespace_MatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, StandardTypes.GENERIC + "/a2@v*?nexusnamespace=g2&nexustype=pecoff");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_PECOFF, constraint,
        "(matches package URL pkg:generic/a2@v*?nexusnamespace=g2&nexustype=pecoff)");
  }

  @Test
  public void testEvaluate_Terraform_MatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_TERRAFORM + "/g2/a2@*");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_TERRAFORM, constraint,
        "(matches package URL pkg:terraform/g2/a2@*)");
  }

  @Test
  public void testEvaluate_Container_MatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, StandardTypes.GENERIC + "/g2/a2@*?nexustype=container");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_CONTAINER, constraint,
        "(matches package URL pkg:generic/g2/a2@*?nexustype=container)");
  }

  @Test
  public void testEvaluate_Conan_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_CONAN + "/g2/a2@v*?channel=e2");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_CONAN, constraint,
        "(matches package URL pkg:conan/g2/a2@v*?channel=e2)");
  }

  @Test
  public void testEvaluate_Composer_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_COMPOSER + "/g2/a2@v*");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_COMPOSER, constraint,
        "(matches package URL pkg:composer/g2/a2@v*)");
  }

  @Test
  public void testEvaluate_Conda_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_CONDA + "/a2@v*");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_CONDA, constraint,
        "(matches package URL pkg:conda/a2@v*?build=*&channel=*&subdir=*&type=*)");
  }

  @Test
  public void testEvaluate_Cran_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_CRAN + "/a2@v*");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_CRAN, constraint, "(matches package URL pkg:cran/a2@v*?type=*)");
  }

  @Test
  public void testEvaluate_Cargo_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, ComponentIdentifier.FORMAT_CARGO + "/a2@v*");
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_CARGO, constraint,
        "(matches package URL pkg:cargo/a2@v*?type=*)");
  }

  @Test
  public void testEvaluate_Debian_MatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_MATCH, LqaFormat.DEBIAN.format + "/g2/a2@v*");
    testEvaluate_MatchExact(LqaFormat.DEBIAN.format, constraint, "(matches package URL pkg:deb/g2/a2@v*)");
  }

  @Test
  public void testEvaluate_Unknown_MatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_MATCH, UNKNOWN_FORMAT + "/g2/a2@v*?qualifier=q2");
    testEvaluate_MatchExact(UNKNOWN_FORMAT, constraint, "(matches package URL pkg:unknown/g2/a2@v*?qualifier=q2)");
  }

  private void testEvaluate_MatchWildcard(String format, Constraint constraint, String expectedConditionMessage) {

    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = forCoordinatesPackageUrl(format, "g1", "a1", "v1", "e1", "q1");
    components.add(component1);
    Component component2 = forCoordinatesPackageUrl(format, "g2", "a2", "v2", "e2", "q2");
    components.add(component2);
    Component component3 = new Component();
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, PackageUrlConditionType.ID,
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
    assertThat(actualReason)
        .isEqualTo("Coordinates were " + component2.getDisplayNameFromIdentifier() + " " + expectedConditionMessage);
  }

  @Test
  public void testEvaluate_Maven_DoNotMatchExact() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_MAVEN + "/g2/a2@v2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_MAVEN, constraint,
        "(does not match package URL pkg:maven/g2/a2@v2?classifier=*&type=*)");
  }

  @Test
  public void testEvaluate_Aname_DoNotMatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_ANAME + "/a2@v2?qualifier=q2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_ANAME, constraint,
        "(does not match package URL pkg:a-name/a2@v2?qualifier=q2)");
  }

  @Test
  public void testEvaluate_Pypi_DoNotMatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_PYPI + "/a2@v2?qualifier=q2&extension=e2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_PYPI, constraint,
        "(does not match package URL pkg:pypi/a2@v2?extension=e2&qualifier=q2)");
  }

  @Test
  public void testEvaluate_Rpm_DoNotMatchExact() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_RPM + "/a2@v2?arch=q2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_RPM, constraint,
        "(does not match package URL pkg:rpm/a2@v2?arch=q2)");
  }

  @Test
  public void testEvaluate_Npm_DoNotMatchExact() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_NPM + "/a2@v2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_NPM, constraint,
        "(does not match package URL pkg:npm/a2@v2)");
  }

  @Test
  public void testEvaluate_Nuget_DoNotMatchExact() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_NUGET + "/a2@v2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_NUGET, constraint,
        "(does not match package URL pkg:nuget/a2@v2)");
  }

  @Test
  public void testEvaluate_Golang_DoNotMatchExact() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_GOLANG + "/a2@v2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_GOLANG, constraint,
        "(does not match package URL pkg:golang/a2@v2)");
  }

  @Test
  public void testEvaluate_Rubygems_DoNotMatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_RUBYGEMS + "/a2@v2?platform=q2");
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_RUBYGEMS, constraint,
        "(does not match package URL pkg:gem/a2@v2?platform=q2)");
  }

  @Test
  public void testEvaluate_Unknown_DoNotMatchExact() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, UNKNOWN_FORMAT + "/g2/a2@v2?qualifier=q2");
    testEvaluate_DoNotMatchExact(UNKNOWN_FORMAT, constraint,
        "(does not match package URL pkg:unknown/g2/a2@v2?qualifier=q2)");
  }

  private void testEvaluate_DoNotMatchExact(String format, Constraint constraint, String expectedConditionMessage) {
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = forCoordinatesPackageUrl(format, "g1", "a1", "v1", "e1", "q1");
    components.add(component1);
    Component component2 = forCoordinatesPackageUrl(format, "g2", "a2", "v2", "e2", "q2");
    components.add(component2);
    Component component3 = new Component();
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, PackageUrlConditionType.ID,
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
    assertThat(actualReason)
        .isEqualTo("Coordinates were " + component1.getDisplayNameFromIdentifier() + " " + expectedConditionMessage);
  }

  @Test
  public void testEvaluate_Maven_DoNotMatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_MAVEN + "/g2/a*@v2");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_MAVEN, constraint,
        "(does not match package URL pkg:maven/g2/a*@v2?classifier=*&type=*)");
  }

  @Test
  public void testEvaluate_Aname_DoNotMatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_ANAME + "/a2@v*?qualifier=q2");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_ANAME, constraint,
        "(does not match package URL pkg:a-name/a2@v*?qualifier=q2)");
  }

  @Test
  public void testEvaluate_Pypi_DoNotMatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_PYPI + "/a2@v*?qualifier=q2&extension=e2");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_PYPI, constraint,
        "(does not match package URL pkg:pypi/a2@v*?extension=e2&qualifier=q2)");
  }

  @Test
  public void testEvaluate_Rpm_DoNotMatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_RPM + "/a2@v*?arch=q2");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_RPM, constraint,
        "(does not match package URL pkg:rpm/a2@v*?arch=q2)");
  }

  @Test
  public void testEvaluate_Npm_DoNotMatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_NPM + "/a2@v*");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_NPM, constraint,
        "(does not match package URL pkg:npm/a2@v*)");
  }

  @Test
  public void testEvaluate_Nuget_DoNotMatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_NUGET + "/a2@v*");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_NUGET, constraint,
        "(does not match package URL pkg:nuget/a2@v*)");
  }

  @Test
  public void testEvaluate_Golang_DoNotMatchWildcard() {
    Constraint constraint = createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_GOLANG + "/a2@v*");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_GOLANG, constraint,
        "(does not match package URL pkg:golang/a2@v*)");
  }

  @Test
  public void testEvaluate_Rubygems_DoNotMatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, ComponentIdentifier.FORMAT_RUBYGEMS + "/a2@v*?platform=q2");
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_RUBYGEMS, constraint,
        "(does not match package URL pkg:gem/a2@v*?platform=q2)");
  }

  @Test
  public void testEvaluate_Unknown_DoNotMatchWildcard() {
    Constraint constraint =
        createConstraint(OPERATOR_DO_NOT_MATCH, UNKNOWN_FORMAT + "/g2/a2@v*?qualifier=q2");
    testEvaluate_DoNotMatchWildcard(UNKNOWN_FORMAT, constraint,
        "(does not match package URL pkg:unknown/g2/a2@v*?qualifier=q2)");
  }

  private void testEvaluate_DoNotMatchWildcard(String format, Constraint constraint, String expectedConditionMessage) {

    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = forCoordinatesPackageUrl(format, "g1", "a1", "v1", "e1", "q1");
    components.add(component1);
    Component component2 = forCoordinatesPackageUrl(format, "g2", "a2", "v2", "e2", "q2");
    components.add(component2);
    Component component3 = new Component();
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, PackageUrlConditionType.ID,
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
    assertThat(actualReason)
        .isEqualTo("Coordinates were " + component1.getDisplayNameFromIdentifier() + " " + expectedConditionMessage);
  }

  @Test
  public void testEvaluate_EscapeUnsafeCharacter() {
    String artifactId = "test%40test%23";
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    Constraint constraint = createConstraint(OPERATOR_MATCH, "maven/g1/" + artifactId);
    policy.addConstraint(constraint);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = forCoordinatesPackageUrl(ComponentIdentifier.FORMAT_MAVEN, "g1", "test@test#", "v1");
    components.add(component1);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, PackageUrlConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Coordinates were g1 : test@test# : v1 " +
        "(matches package URL pkg:maven/g1/test%40test%23@*?classifier=*&type=*)");
  }

  @Test
  public void testValidateCondition_NullPackageUrl() {
    Condition condition = new Condition(PackageUrlConditionType.ID, OPERATOR_MATCH, null);
    assertThatThrownBy(() -> new PackageUrlConditionType().validateCondition(null, condition, null))
        .isInstanceOf(InvalidConditionException.class)
        .hasMessageEndingWith("missing package URL");
  }

  @Test
  public void testValidateCondition_EmptyPackageUrl() {
    Condition condition = new Condition(PackageUrlConditionType.ID, OPERATOR_MATCH, " ");
    assertThatThrownBy(() -> new PackageUrlConditionType().validateCondition(null, condition, null))
        .isInstanceOf(InvalidConditionException.class)
        .hasMessageEndingWith("missing package URL");
  }

  @Test
  public void testValidateCondition_InvalidPackageUrl() {
    Condition condition = new Condition(PackageUrlConditionType.ID, OPERATOR_MATCH, "invalid");
    assertThatThrownBy(() -> new PackageUrlConditionType().validateCondition(null, condition, null))
        .isInstanceOf(InvalidConditionException.class)
        .hasMessageEndingWith("invalid package URL");
  }

  @Test
  public void testConvertIfNeeded_UnsupportedCoordinateFormat_DoesNotThrowNullPointerException() {
    new Condition(PackageUrlConditionType.ID, OPERATOR_MATCH, "pkg:unknown/g1/a1@v1").getValue();
  }

  @Test
  public void testConvertIfNeeded() {
    convertIfNeededMaven();
    convertIfNeededAname();
    convertIfNeededPypi();
    convertIfNeededGolang();
    convertIfNeededNpm();
    convertIfNeededRpm();
    convertIfNeededRubygems();
    convertIfNeededNuget();
    convertIfNeededUnknown();
    convertIfNeededCocoapods();
    convertIfNeededSwift();
    convertIfNeededPecoff();
    convertIfNeededTerraform();
    convertIfNeededContainer();
    convertIfNeededConan();
    convertIfNeededCargo();
    convertIfNeededCran();
    convertIfNeededConda();
  }

  private void convertIfNeededMaven() {
    assertConvertIfNeeded("pkg:maven/g/a", "pkg:maven/g/a@*?classifier=*&type=*");
    assertConvertIfNeeded("pkg:maven/g/a@v", "pkg:maven/g/a@v?classifier=*&type=*");
    assertConvertIfNeeded("pkg:maven/g/a@v?type=e&classifier=", "pkg:maven/g/a@v?classifier=*&type=e");
    assertConvertIfNeeded("pkg:maven/g/a@v?type=", "pkg:maven/g/a@v?classifier=*&type=*");
    assertConvertIfNeeded("pkg:maven/g/a@v?classifier=", "pkg:maven/g/a@v?classifier=*&type=*");
    assertConvertIfNeeded("pkg:maven/g/a@v?type=&classifier=c", "pkg:maven/g/a@v?classifier=c&type=*");
    assertConvertIfNeeded("pkg:maven/g/a@v?type=&classifier=", "pkg:maven/g/a@v?classifier=*&type=*");
    assertConvertIfNeeded("pkg:maven/g/a@v?type=e&classifier=c", "pkg:maven/g/a@v?classifier=c&type=e");
    assertConvertIfNeeded("pkg:maven/G/A@v?Type=e&Classifier=c", "pkg:maven/G/A@v?classifier=c&type=e");
    assertConvertIfNeeded("pkg:maven/G/A@v?type=e&classifier=c", "pkg:maven/G/A@v?classifier=c&type=e");
    assertConvertIfNeeded("pkg:maven/G/A@v?type=E&classifier=C", "pkg:maven/G/A@v?classifier=C&type=E");
  }

  private void convertIfNeededAname() {
    assertConvertIfNeeded("pkg:a-name/n", "pkg:a-name/n@*?qualifier=*");
    assertConvertIfNeeded("pkg:a-name/n?qualifier=q", "pkg:a-name/n@*?qualifier=q");
    assertConvertIfNeeded("pkg:a-name/n@v?qualifier=q", "pkg:a-name/n@v?qualifier=q");
    assertConvertIfNeeded("pkg:a-name/n@v", "pkg:a-name/n@v?qualifier=*");
    assertConvertIfNeeded("pkg:a-name/n@v?qualifier=", "pkg:a-name/n@v?qualifier=*");
    assertConvertIfNeeded("pkg:a-name/N@V?qualifier=", "pkg:a-name/N@V?qualifier=*");
    assertConvertIfNeeded("pkg:a-name/N@V?qualifier=Q", "pkg:a-name/N@V?qualifier=Q");
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
    assertConvertIfNeeded("pkg:pypi/N?extension=e&qualifier=q", "pkg:pypi/N@*?extension=e&qualifier=q");
    assertConvertIfNeeded("pkg:pypi/N/name/*?extension=E&qualifier=q", "pkg:pypi/N/name/*@*?extension=E&qualifier=q");
  }

  private void convertIfNeededNuget() {
    assertConvertIfNeeded("pkg:nuget/n", "pkg:nuget/n@*");
    assertConvertIfNeeded("pkg:nuget/n@v", "pkg:nuget/n@v");
    assertConvertIfNeeded("pkg:nuget/n/n@v", "pkg:nuget/n/n@v");
    assertConvertIfNeeded("pkg:nuget/N/n@V", "pkg:nuget/N/n@V");
  }

  private void convertIfNeededNpm() {
    assertConvertIfNeeded("pkg:npm/n", "pkg:npm/n@*");
    assertConvertIfNeeded("pkg:npm/n@v", "pkg:npm/n@v");
    assertConvertIfNeeded("pkg:npm/a/n@v", "pkg:npm/a/n@v");
    assertConvertIfNeeded("pkg:npm/A/n@V", "pkg:npm/A/n@V");
  }

  private void convertIfNeededRpm() {
    assertConvertIfNeeded("pkg:rpm/n", "pkg:rpm/n@*?arch=*");
    assertConvertIfNeeded("pkg:rpm/n?arch=q", "pkg:rpm/n@*?arch=q");
    assertConvertIfNeeded("pkg:rpm/n@v?arch=q", "pkg:rpm/n@v?arch=q");
    assertConvertIfNeeded("pkg:rpm/n@v?arch=", "pkg:rpm/n@v?arch=*");
    assertConvertIfNeeded("pkg:rpm/n@v", "pkg:rpm/n@v?arch=*");
    assertConvertIfNeeded("pkg:rpm/a/n@v", "pkg:rpm/a/n@v?arch=*");
    assertConvertIfNeeded("pkg:rpm/A/N@v", "pkg:rpm/A/N@v?arch=*");
  }

  private void convertIfNeededGolang() {
    assertConvertIfNeeded("pkg:golang/n", "pkg:golang/n@*");
    assertConvertIfNeeded("pkg:golang/n@v", "pkg:golang/n@v");
    assertConvertIfNeeded("pkg:golang/a/n@v", "pkg:golang/a/n@v");
    assertConvertIfNeeded("pkg:golang/a/n", "pkg:golang/a/n@*");
    assertConvertIfNeeded("pkg:golang/A/n", "pkg:golang/A/n@*");
    assertConvertIfNeeded("pkg:golang/A/N/*", "pkg:golang/A/N/*@*");
    assertConvertIfNeeded("pkg:golang/A/N/*@V", "pkg:golang/A/N/*@V");
  }

  private void convertIfNeededRubygems() {
    assertConvertIfNeeded("pkg:gem/n", "pkg:gem/n@*?platform=*");
    assertConvertIfNeeded("pkg:gem/n?platform=q", "pkg:gem/n@*?platform=q");
    assertConvertIfNeeded("pkg:gem/n@v?platform=q", "pkg:gem/n@v?platform=q");
    assertConvertIfNeeded("pkg:gem/n@v?platform=", "pkg:gem/n@v?platform=*");
    assertConvertIfNeeded("pkg:gem/n@v", "pkg:gem/n@v?platform=*");
    assertConvertIfNeeded("pkg:gem/a/n@v", "pkg:gem/a/n@v?platform=*");
    assertConvertIfNeeded("pkg:gem/A/n@V", "pkg:gem/A/n@V?platform=*");
    assertConvertIfNeeded("pkg:gem/A/N@V", "pkg:gem/A/N@V?platform=*");
  }

  private void convertIfNeededCocoapods() {
    assertConvertIfNeeded("pkg:cocoapods/n", "pkg:cocoapods/n@*");
    assertConvertIfNeeded("pkg:cocoapods/n@v", "pkg:cocoapods/n@v");
    assertConvertIfNeeded("pkg:cocoapods/n@V", "pkg:cocoapods/n@V");
    assertConvertIfNeeded("pkg:cocoapods/N@V", "pkg:cocoapods/N@V");
  }

  private void convertIfNeededConan() {
    assertConvertIfNeeded("pkg:conan/n", "pkg:conan/*/n@*?channel=*");
    assertConvertIfNeeded("pkg:conan/o/n?channel=q", "pkg:conan/o/n@*?channel=q");
    assertConvertIfNeeded("pkg:conan/o/n@v?channel=q", "pkg:conan/o/n@v?channel=q");
    assertConvertIfNeeded("pkg:conan/o/n@v?channel=", "pkg:conan/o/n@v?channel=*");
    assertConvertIfNeeded("pkg:conan/o/n@v", "pkg:conan/o/n@v?channel=*");
    assertConvertIfNeeded("pkg:conan/o/N@v", "pkg:conan/o/N@v?channel=*");
    assertConvertIfNeeded("pkg:conan/O/n@V", "pkg:conan/O/n@V?channel=*");
    assertConvertIfNeeded("pkg:conan/O/N@V", "pkg:conan/O/N@V?channel=*");
  }

  private void convertIfNeededCargo() {
    assertConvertIfNeeded("pkg:cargo/n", "pkg:cargo/n@*?type=*");
    assertConvertIfNeeded("pkg:cargo/n?type=t", "pkg:cargo/n@*?type=t");
    assertConvertIfNeeded("pkg:cargo/n@v?type=e", "pkg:cargo/n@v?type=e");
    assertConvertIfNeeded("pkg:cargo/n@v?type=", "pkg:cargo/n@v?type=*");
    assertConvertIfNeeded("pkg:cargo/n@v", "pkg:cargo/n@v?type=*");
    assertConvertIfNeeded("pkg:cargo/N@v", "pkg:cargo/N@v?type=*");
    assertConvertIfNeeded("pkg:cargo/n@V", "pkg:cargo/n@V?type=*");
    assertConvertIfNeeded("pkg:cargo/N@V", "pkg:cargo/N@V?type=*");
  }

  private void convertIfNeededCran() {
    assertConvertIfNeeded("pkg:cran/n", "pkg:cran/n@*?type=*");
    assertConvertIfNeeded("pkg:cran/n?type=t", "pkg:cran/n@*?type=t");
    assertConvertIfNeeded("pkg:cran/n@v?type=e", "pkg:cran/n@v?type=e");
    assertConvertIfNeeded("pkg:cran/n@v?type=", "pkg:cran/n@v?type=*");
    assertConvertIfNeeded("pkg:cran/n@v", "pkg:cran/n@v?type=*");
    assertConvertIfNeeded("pkg:cran/N@v", "pkg:cran/N@v?type=*");
    assertConvertIfNeeded("pkg:cran/n@V", "pkg:cran/n@V?type=*");
    assertConvertIfNeeded("pkg:cran/N@V", "pkg:cran/N@V?type=*");
  }

  private void convertIfNeededSwift() {
    assertConvertIfNeeded("pkg:swift/n", "pkg:swift/n@*");
    assertConvertIfNeeded("pkg:swift/n@v", "pkg:swift/n@v");
    assertConvertIfNeeded("pkg:swift/n@V", "pkg:swift/n@V");
    assertConvertIfNeeded("pkg:swift/N@V", "pkg:swift/N@V");
  }

  private void convertIfNeededConda() {
    assertConvertIfNeeded("pkg:conda/n", "pkg:conda/n@*?build=*&channel=*&subdir=*&type=*");
    assertConvertIfNeeded("pkg:conda/n?channel=q", "pkg:conda/n@*?build=*&channel=q&subdir=*&type=*");
    assertConvertIfNeeded("pkg:conda/n@v?channel=q", "pkg:conda/n@v?build=*&channel=q&subdir=*&type=*");
    assertConvertIfNeeded("pkg:conda/n@v?channel=", "pkg:conda/n@v?build=*&channel=*&subdir=*&type=*");
    assertConvertIfNeeded("pkg:conda/n@v", "pkg:conda/n@v?build=*&channel=*&subdir=*&type=*");
    assertConvertIfNeeded("pkg:conda/N@v?subdir=s", "pkg:conda/N@v?build=*&channel=*&subdir=s&type=*");
    assertConvertIfNeeded("pkg:conda/n@V?type=conda", "pkg:conda/n@V?build=*&channel=*&subdir=*&type=conda");
    assertConvertIfNeeded("pkg:conda/N@V?build=b", "pkg:conda/N@V?build=b&channel=*&subdir=*&type=*");
    assertConvertIfNeeded("pkg:conda/N@V?build=b&channel=c&subdir=s&type=conda",
        "pkg:conda/N@V?build=b&channel=c&subdir=s&type=conda");
  }

  private void convertIfNeededPecoff() {
    assertConvertIfNeeded("pkg:generic/n?nexustype=pecoff", "pkg:generic/n@*?nexustype=pecoff");
    assertConvertIfNeeded("pkg:generic/n?nexusnamespace=a&nexustype=pecoff",
        "pkg:generic/n@*?nexusnamespace=a&nexustype=pecoff");
  }

  private void convertIfNeededTerraform() {
    assertConvertIfNeeded("pkg:terraform/n", "pkg:terraform/n@*");
    assertConvertIfNeeded("pkg:terraform/n@v", "pkg:terraform/n@v");
    assertConvertIfNeeded("pkg:terraform/a/n@v", "pkg:terraform/a/n@v");
    assertConvertIfNeeded("pkg:terraform/A/n@V", "pkg:terraform/A/n@V");
  }

  private void convertIfNeededContainer() {
    assertConvertIfNeeded("pkg:generic/n?nexustype=container", "pkg:generic/*/n@*?nexustype=container");
    assertConvertIfNeeded("pkg:generic/n/n?nexustype=container", "pkg:generic/n/n@*?nexustype=container");
    assertConvertIfNeeded("pkg:generic/n/n@v?nexustype=container", "pkg:generic/n/n@v?nexustype=container");
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
    assertConvertIfNeeded("pkg:unknown/NAMEspace/name@v?qualifier=q&type=t&Arch=A",
        "pkg:unknown/NAMEspace/name@v?arch=A&qualifier=q&type=t");
    assertConvertIfNeeded("pkg:unknown/NAME@v?qualifier=", "pkg:unknown/NAME@v");
  }

  private void assertConvertIfNeeded(final String value, final String expectedConvertedValue) {
    assertThat(createCoordinateCondition(value).getValue()).isEqualTo(expectedConvertedValue);
  }

  private Condition createCoordinateCondition(final String value) {
    return new Condition(PackageUrlConditionType.ID, OPERATOR_MATCH, value);
  }
}
