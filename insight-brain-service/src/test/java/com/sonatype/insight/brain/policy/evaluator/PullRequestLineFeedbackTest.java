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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PullRequestLineFeedbackTest
    extends AbstractComponentTest
{
  public static final String MULTIPLE_NO_SUGGESTIONS = "multipleNoSuggestions";

  public static final String MULTIPLE_WITH_SUGGESTION = "multipleWithSuggestion";

  public static final String SINGLE_NO_SUGGESTION = "singleNoSuggestion";

  public static final String SINGLE_WITH_SUGGESTION = "singleWithSuggestion";

  @Inject
  private InsightConfig config;
  
  private Map<String, PullRequestLineFeedback> testCases;

  @Before
  public void before() {
    config.setBaseUrl("http://localhost:1122");
    String baseUrl = lookup(BaseUrl.class).getConfigured();
    testCases = ImmutableMap.<String, PullRequestLineFeedback>builder()
        .put(MULTIPLE_NO_SUGGESTIONS, new PullRequestLineFeedback(defaultPolicyViolations(10), "Test Component",
            baseUrl, null))
        .put(MULTIPLE_WITH_SUGGESTION, new PullRequestLineFeedback(defaultPolicyViolations(10), "Test Component",
            baseUrl, "123"))
        .put(SINGLE_NO_SUGGESTION, new PullRequestLineFeedback(defaultPolicyViolations(1), "Test Component", baseUrl,
            null))
        .put(SINGLE_WITH_SUGGESTION, new PullRequestLineFeedback(defaultPolicyViolations(1), "Test Component", baseUrl,
            "123"))
        .build();
  }

  @Test
  public void testPullRequestFeedback_multipleNoSuggestion() throws Exception {
    assertContents(testCases.get(MULTIPLE_NO_SUGGESTIONS), "PullRequestLineFeedback_multipleNoSuggestions.md", true);
  }

  @Test
  public void testPullRequestFeedback_multipleWithSuggestion() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_SUGGESTION), "PullRequestLineFeedback_multipleWithSuggestion.md", true);
  }

  @Test
  public void testPullRequestFeedback_singleNoSuggestion() throws Exception {
    assertContents(testCases.get(SINGLE_NO_SUGGESTION), "PullRequestLineFeedback_singleNoSuggestions.md", true);
  }

  @Test
  public void testPullRequestFeedback_singleWithSuggestion() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_SUGGESTION), "PullRequestLineFeedback_singleWithSuggestion.md", true);
  }

  @Test
  public void testPullRequestFeedback_multipleNoSuggestion_no_html() throws Exception {
    assertContents(testCases.get(MULTIPLE_NO_SUGGESTIONS), "PullRequestLineFeedback_multipleNoSuggestions_noHtml.md",
        false);
  }

  @Test
  public void testPullRequestFeedback_multipleWithSuggestion_no_html() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_SUGGESTION), "PullRequestLineFeedback_multipleWithSuggestion_noHtml.md",
        false);
  }

  @Test
  public void testPullRequestFeedback_singleNoSuggestion_no_html() throws Exception {
    assertContents(testCases.get(SINGLE_NO_SUGGESTION), "PullRequestLineFeedback_singleNoSuggestions_noHtml.md", false);
  }

  @Test
  public void testPullRequestFeedback_singleWithSuggestion_no_html() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_SUGGESTION), "PullRequestLineFeedback_singleWithSuggestion_noHtml.md",
        false);
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
          .renderTemplateAndGetContents(true);
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
      policyViolations.add(defaultPolicyViolation(i + 1));
    }
    return policyViolations;
  }

  private static PolicyViolation defaultPolicyViolation(int number) {
    final ConditionFact conditionFact = conditionFact(MatchStateConditionType.ID, "is", "exact");
    final ConstraintFact constraintFact = constraintFact("constraint_" + number, "Constraint " + number, "OR");
    constraintFact.addConditionFact(conditionFact);

    List<ConstraintFact> constraintFactList;
    PolicyEvaluation evaluation = new PolicyEvaluation();
    if (number == 5) { // add a little variance
      ConstraintFact constraintFact2 = constraintFact("constraint_5.1", "Constraint 5.1", "OR");
      constraintFact2.addConditionFact(conditionFact);
      constraintFactList = new LinkedList<>();
      constraintFactList.add(constraintFact);
      constraintFactList.add(constraintFact2);
    }
    else {
      constraintFactList = Collections.singletonList(constraintFact);
    }
    PolicyViolation policyViolation =
        new PolicyViolation(evaluation, "policy_" + number, "Policy " + number, number,
            PolicyThreatCategory.OTHER, "H", ComponentIdentifier.createMavenCoordinates("G", "A", "V"),
            constraintFactList, "filename");

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
    return value.trim().replaceAll("as of _.*", "");
  }

  private String readResource(String resourceName) throws Exception {
    final Path path = Paths.get(getClass().getResource("/PullRequestLineFeedbackTest/" + resourceName).toURI());
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }
  
  private void assertContents(
      final PullRequestLineFeedback details,
      final String expectedContentFile,
      boolean includeEmbeddedHtml)
      throws Exception
  {
    final String expectedContent = readResource(expectedContentFile);
    final Optional<String> contents = details.renderTemplateAndGetContents(includeEmbeddedHtml);
    assertThat(contents).isNotEmpty();
    assertThat(removeDateFromOutput(contents.get())).isEqualTo(removeDateFromOutput(expectedContent));
  }
}
