/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PullRequestLineFeedbackTest
    extends AbstractComponentTest
{
  @Inject
  private InsightConfig config;

  @Before
  public void before() {
    config.setBaseUrl("http://localhost:1122");
  }

  private String readResource(String resourceName) throws Exception {
    final Path path = Paths.get(getClass().getResource("/PullRequestLineFeedbackTest/" + resourceName).toURI());
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }

  @Test
  public void testPullRequestFeedback_multipleNoSuggestion() throws Exception {
    //when
    final PullRequestLineFeedback details =
        new PullRequestLineFeedback(defaultPolicyViolations(10), "Test Component",
            lookup(BaseUrl.class).getConfigured(), null);

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestLineFeedback_multipleNoSuggestions.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(removeDateFromOutput(contents.get())).isEqualTo(removeDateFromOutput(expectedContent));
  }

  @Test
  public void testPullRequestFeedback_multipleWithSuggestion() throws Exception {
    //when
    final PullRequestLineFeedback details =
        new PullRequestLineFeedback(defaultPolicyViolations(10), "Test Component",
            lookup(BaseUrl.class).getConfigured(), "123");

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestLineFeedback_multipleWithSuggestion.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(removeDateFromOutput(contents.get())).isEqualTo(removeDateFromOutput(expectedContent));
  }

  @Test
  public void testPullRequestFeedback_singleNoSuggestion() throws Exception {
    //when
    final PullRequestLineFeedback details =
        new PullRequestLineFeedback(defaultPolicyViolations(1), "Test Component", lookup(BaseUrl.class).getConfigured(),
            null);

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestLineFeedback_singleNoSuggestions.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(removeDateFromOutput(contents.get())).isEqualTo(removeDateFromOutput(expectedContent));
  }

  @Test
  public void testPullRequestFeedback_singleWithSuggestion() throws Exception {
    //when
    final PullRequestLineFeedback details =
        new PullRequestLineFeedback(defaultPolicyViolations(1), "Test Component", lookup(BaseUrl.class).getConfigured(),
            "123");

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestLineFeedback_singleWithSuggestion.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(removeDateFromOutput(contents.get())).isEqualTo(removeDateFromOutput(expectedContent));
  }

  @Test
  public void testPullRequestFeedback_nullViolations() throws Exception {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
      new PullRequestLineFeedback(null, "Test Component", lookup(BaseUrl.class).getConfigured(), null);
    }).withMessageContaining("violations is required and cannot be null");
  }

  @Test
  public void testPullRequestFeedback_emptyViolations() throws Exception {
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
      new PullRequestLineFeedback(new ArrayList<>(), "Test Component", lookup(BaseUrl.class).getConfigured(), null)
          .renderTemplateAndGetContents();
    }).withMessageContaining("violations cannot be empty");
  }

  @Test
  public void testPullRequestFeedback_nullDisplayName() throws Exception {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
      new PullRequestLineFeedback(new ArrayList<>(), null, lookup(BaseUrl.class).getConfigured(), null);
    }).withMessageContaining("displayName is required and cannot be null");
  }

  private static List<PolicyViolation> defaultPolicyViolations(int count) {
    final List<PolicyViolation> policyViolations = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      policyViolations.add(defaultPolicyViolation(i));
    }
    return policyViolations;
  }

  private static PolicyViolation defaultPolicyViolation(int number) {
    final ConditionFact conditionFact = conditionFact(MatchStateConditionType.ID, "is", "exact");
    final ConstraintFact constraintFact = constraintFact("constraint_" + number, "Constraint " + number, "OR");
    constraintFact.addConditionFact(conditionFact);

    PolicyEvaluation evaluation = new PolicyEvaluation();
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policy_" + number, "Policy " + number, 0,
        PolicyThreatCategory.OTHER, "H", ComponentIdentifier.createMavenCoordinates("G", "A", "V"),
        Collections.singletonList(constraintFact), "filename");

    return policyViolation;
  }

  private static ConstraintFact constraintFact(final String id, final String name, final String operator) {
    return new ConstraintFact(id, name, operator);
  }

  private static ConditionFact conditionFact(final String conditionTypeId, final String operator, final String value) {
    final Condition condition = new Condition(conditionTypeId, operator, value);
    return ComponentPolicyEvaluator.createConditionFact(condition,
        new MatchFact(ComponentFactory.forGav("G", "A", "V", MatchState.EXACT), null /* policyId */,
            null /* constraintId */, Collections.emptyList() /* conditionTriggers */));
  }

  private String removeDateFromOutput(final String value) {
    return value.replaceAll("as of _.*", "");
  }
}
