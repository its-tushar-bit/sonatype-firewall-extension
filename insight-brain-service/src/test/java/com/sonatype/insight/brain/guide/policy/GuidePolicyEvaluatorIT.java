/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.testing.BrainInjectedTest;
import jakarta.inject.Inject;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * <b>Intentionally retained as an isolated, per-class-configuration test</b> (not migrated to the reused-context
 * component-h2 module). It installs a {@code @Primary} mock of the {@code ApiComponentDetailsServiceV2} HDS boundary
 * and asserts against a clean root-organization policy/evaluation state; both couple it to a dedicated context and
 * database, so it cannot share the reused component-test context. See CLM-45581.
 */
@ContextConfiguration(classes = GuidePolicyEvaluatorIT.GuidePolicyEvaluatorITConfiguration.class)
public class GuidePolicyEvaluatorIT
    extends BrainInjectedTest
{
  @TestConfiguration
  static class GuidePolicyEvaluatorITConfiguration
  {
    @Bean
    @Primary
    ApiComponentDetailsServiceV2 mockApiComponentDetailsServiceV2() {
      ApiComponentDetailsServiceV2 mock = Mockito.mock(ApiComponentDetailsServiceV2.class);
      // Answer dynamically: for each requested PURL in the batch, build a ComponentEvaluationData
      // using EvaluatorFixtures so the evaluator can produce a Component fact and run Drools.
      // No real HDS call is made — this is the IT's substitute for the network boundary.
      when(mock.getComponentDetailsListFromHds(
          any(ApiComponentDetailsRequestDTOV2.class), anyString()))
              .thenAnswer(inv -> {
                ApiComponentDetailsRequestDTOV2 req = inv.getArgument(0);
                List<ComponentEvaluationData> out = new ArrayList<>();
                int i = 0;
                for (ApiComponentDTOV2 comp : req.components) {
                  ComponentEvaluationData data = new ComponentEvaluationData();
                  data.requestIndex = i++;
                  data.componentIdentifier = EvaluatorFixtures.identifierFor(comp.packageUrl);
                  // Initialize sets to empty — null licenses cause NPEs in augment/Drools paths.
                  data.declaredLicenses = Collections.emptySet();
                  data.observedLicenses = Collections.emptySet();
                  data.securityVulnerabilities = Collections.emptyList();
                  out.add(data);
                }
                return out;
              });
      return mock;
    }
  }

  @Inject
  private GuidePolicyEvaluator underTest;

  @Test
  public void noPolicies_returnsEmptyButPresent_perPurl() {
    // No policies persisted under root org → the evaluator produces a compliant result
    // with no violations for every requested PURL.
    Map<String, GuidePolicyCompliance> result = underTest.evaluate(List.of(
        "pkg:maven/org.example/no-policy@1.0"));

    GuidePolicyCompliance compliance = result.get("pkg:maven/org.example/no-policy@1.0");
    assertThat(compliance).isNotNull();
    assertThat(compliance.compliant()).isTrue();
    assertThat(compliance.stage()).isEqualTo("release");
    assertThat(compliance.ownerId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(compliance.violations()).isEmpty();
  }

  @Test
  public void violatingPolicy_fires_coordMatchConstraint() {
    // Persist a policy at ROOT_ORGANIZATION_ID that fires on any maven:org.example coordinate.
    // Using CoordinatesConditionType avoids the need for fake security or license data in the
    // mock ComponentEvaluationData — the constraint fires purely on the component's identifier.
    Policy policy = buildCoordPolicy();
    tempEntity.newPolicy(policy);

    Map<String, GuidePolicyCompliance> result = underTest.evaluate(List.of(COORD_PURL));

    GuidePolicyCompliance compliance = result.get(COORD_PURL);
    assertThat(compliance).isNotNull();
    assertThat(compliance.compliant()).isFalse();
    assertThat(compliance.violations()).hasSize(1);
    assertThat(compliance.violations().get(0).policyId()).isEqualTo(policy.getId());
    assertThat(compliance.violations().get(0).waived()).isFalse();
    assertThat(compliance.violations().get(0).waiver()).isNull();
    assertThat(compliance.summary().activeViolationCount()).isEqualTo(1);
    assertThat(compliance.summary().waivedViolationCount()).isEqualTo(0);
  }

  @Test
  public void matchingWaiver_makesCompliant_andPopulatesWaiverField() {
    // Same policy as above, but with a legacy waiver (hash=null, constraintFacts=null) scoped
    // to ROOT_ORGANIZATION_ID. A legacy waiver matches the policy for any component — it is the
    // simplest waiver that will flip the violation to "waived" without needing to reproduce the
    // exact ConstraintFact JSON that Drools produces at runtime.
    Policy policy = buildCoordPolicy();
    tempEntity.newPolicy(policy);
    tempEntity.newWaiver(null, policy.getId(), Organization.ROOT_ORGANIZATION_ID);

    Map<String, GuidePolicyCompliance> result = underTest.evaluate(List.of(COORD_PURL));

    GuidePolicyCompliance compliance = result.get(COORD_PURL);
    assertThat(compliance).isNotNull();
    assertThat(compliance.compliant()).isTrue();
    assertThat(compliance.violations()).hasSize(1);
    assertThat(compliance.violations().get(0).waived()).isTrue();
    assertThat(compliance.violations().get(0).waiver()).isNotNull();
    assertThat(compliance.violations().get(0).waiver().comment()).isEqualTo("testing");
    assertThat(compliance.summary().activeViolationCount()).isEqualTo(0);
    assertThat(compliance.summary().waivedViolationCount()).isEqualTo(1);
  }

  // ── helpers ─────────────────────────────────────────────────────────────────

  /**
   * Canonical PURL whose maven groupId starts with "org.example" — triggers the coord policy.
   * Intentionally the bare form (no {@code ?type=jar}): real REST/MCP callers go through
   * {@code GuidePurlAssembler.buildPurlForPolicyEval}, which adds {@code type=jar}, but this IT
   * passes the bare PURL straight to the evaluator to exercise its own
   * {@code ensureCompleteIdentifier} normalization. If that normalization ever stopped accepting
   * the bare maven form, the PURL would be skipped and the {@code assertThat(compliance).isNotNull()}
   * assertions below would fail — so this test doubles as the validator for that path.
   */
  private static final String COORD_PURL = "pkg:maven/org.example/lib@1.0";

  /**
   * Builds (but does not persist) a Policy scoped to ROOT_ORGANIZATION_ID that fires at the
   * release stage on any component whose maven groupId starts with "org.example". The
   * CoordinatesConditionType condition requires no HDS-sourced data (no CVEs, no licenses),
   * so the minimal ComponentEvaluationData produced by the @TestConfiguration mock is enough.
   */
  private static Policy buildCoordPolicy() {
    Policy policy = new Policy(null, "Coord Block Policy");
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    policy.setThreatLevel(5);
    policy.setAction(Stage.ID_RELEASE, FailActionType.ID);
    Constraint constraint = new Constraint(null, "Coord Constraint", LogicalOperator.AND);
    // "maven:org.example" auto-expands to "maven:org.example:*:*:*:*" — matches our fixture PURL.
    constraint.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:org.example"));
    policy.addConstraint(constraint);
    return policy;
  }
}
