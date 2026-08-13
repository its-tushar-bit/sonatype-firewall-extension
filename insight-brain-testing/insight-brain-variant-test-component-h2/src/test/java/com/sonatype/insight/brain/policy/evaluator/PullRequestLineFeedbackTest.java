/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.utils.TemplateHelper.assertRenderedOutput;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class PullRequestLineFeedbackTest
    extends AbstractComponentH2Test
{
  private static final Optional<String> EMPTY_OPTIONAL_STRING = Optional.empty();

  public static final String MULTIPLE_NO_SUGGESTIONS = "multipleNoSuggestions";

  public static final String MULTIPLE_WITH_SUGGESTION = "multipleWithSuggestion";

  public static final String MULTIPLE_WITH_SUGGESTION_AND_DEPENDENCY_REMEDIATION =
      "multipleWithSuggestionAndDependencyRemediation";

  public static final String MULTIPLE_WITH_GOLDEN = "multipleWithGolden";

  public static final String MULTIPLE_WITH_RECOMMENDED = "multipleWithRecommended";

  public static final String MULTIPLE_WITH_GOLDEN_SCM_IMPROVEMENTS = "multipleWithGoldenScmImprovements";

  public static final String MULTIPLE_WITH_RECOMMENDED_SCM_IMPROVEMENTS = "multipleWithRecommendedScmImprovements";

  public static final String SINGLE_NO_SUGGESTION = "singleNoSuggestion";

  public static final String SINGLE_WITH_SUGGESTION = "singleWithSuggestion";

  public static final String SINGLE_WITH_SUGGESTION_AZCLOUD = "singleWithSuggestion_azureCloud";

  public static final String SINGLE_WITH_SUGGESTION_AZONPREM = "singleWithSuggestion_azureOnPrem";

  public static final String SINGLE_WITH_GOLDEN = "singleWithGolden";

  public static final String SINGLE_WITH_RECOMMENDED = "singleWithRecommended";

  public static final String SINGLE_WITH_GOLDEN_SCM_IMPROVEMENTS = "singleWithGoldenScmImprovements";

  public static final String SINGLE_WITH_RECOMMENDED_SCM_IMPROVEMENTS = "singleWithRecommendedScmImprovements";

  private static final String SCM_ON_PREM_BASE_URL = "https://scm.mycompany.com";

  private static final String APPLICATION_PUBLIC_ID = "myApp";

  private static final String FEATURE_BRANCH_SCAN_ID = "myScanId";

  // The majority of tests will default to use the full security data, not reduced. For readability.
  private static final boolean FULL_DATA = false;

  private Map<String, PullRequestLineFeedback> testCases;

  @Inject
  private OrganizationDAO organizationDAO;

  @BeforeEach
  public void before() {
    setBaseUrl("http://localhost:1122");
    String iqBaseUrl = lookup(BaseUrl.class).getConfigured();
    testCases = ImmutableMap.<String, PullRequestLineFeedback>builder()
        .put(MULTIPLE_NO_SUGGESTIONS, new PullRequestLineFeedback(defaultPolicyViolations(10), "Test Component",
            iqBaseUrl, null, SCM_ON_PREM_BASE_URL, APPLICATION_PUBLIC_ID,
            FEATURE_BRANCH_SCAN_ID, EMPTY_OPTIONAL_STRING, false, organizationDAO, FULL_DATA))
        .put(MULTIPLE_WITH_SUGGESTION, new PullRequestLineFeedback(defaultPolicyViolations(10), "Test Component",
            iqBaseUrl, new RemediationVersionDTO("123", ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, 3),
            SCM_ON_PREM_BASE_URL, APPLICATION_PUBLIC_ID, FEATURE_BRANCH_SCAN_ID,
            EMPTY_OPTIONAL_STRING, false, organizationDAO, FULL_DATA))
        .put(MULTIPLE_WITH_SUGGESTION_AND_DEPENDENCY_REMEDIATION,
            new PullRequestLineFeedback(defaultPolicyViolations(10), "Test Component", iqBaseUrl,
                new RemediationVersionDTO("123", ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, 3),
                SCM_ON_PREM_BASE_URL, APPLICATION_PUBLIC_ID, FEATURE_BRANCH_SCAN_ID,
                EMPTY_OPTIONAL_STRING, false, organizationDAO, FULL_DATA))
        .put(SINGLE_NO_SUGGESTION, new PullRequestLineFeedback(defaultPolicyViolations(1), "Test Component", iqBaseUrl,
            null, SCM_ON_PREM_BASE_URL, APPLICATION_PUBLIC_ID, FEATURE_BRANCH_SCAN_ID, EMPTY_OPTIONAL_STRING,
            false, organizationDAO, FULL_DATA))
        .put(SINGLE_WITH_SUGGESTION, new PullRequestLineFeedback(defaultPolicyViolations(1), "Test Component",
            iqBaseUrl, new RemediationVersionDTO("123", ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS),
            SCM_ON_PREM_BASE_URL, APPLICATION_PUBLIC_ID, FEATURE_BRANCH_SCAN_ID,
            EMPTY_OPTIONAL_STRING, false, organizationDAO, FULL_DATA))
        .put(SINGLE_WITH_SUGGESTION_AZCLOUD, new PullRequestLineFeedback(defaultPolicyViolations(1), "Test Component",
            "http://dev.azure.com/foo/bar/_git/baz",
            new RemediationVersionDTO(
                "123",
                ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS),
            "http://dev.azure.com",
            APPLICATION_PUBLIC_ID,
            FEATURE_BRANCH_SCAN_ID,
            EMPTY_OPTIONAL_STRING,
            false, organizationDAO, FULL_DATA))
        .put(SINGLE_WITH_SUGGESTION_AZONPREM, new PullRequestLineFeedback(defaultPolicyViolations(1), "Test Component",
            iqBaseUrl, new RemediationVersionDTO("123", ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS),
            SCM_ON_PREM_BASE_URL, APPLICATION_PUBLIC_ID, FEATURE_BRANCH_SCAN_ID,
            EMPTY_OPTIONAL_STRING, false, organizationDAO, FULL_DATA))
        .put(
            MULTIPLE_WITH_GOLDEN,
            new PullRequestLineFeedback(
                defaultPolicyViolations(10),
                "Test Component",
                iqBaseUrl,
                new RemediationVersionDTO(
                    "123",
                    ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
                    0),
                SCM_ON_PREM_BASE_URL,
                APPLICATION_PUBLIC_ID,
                FEATURE_BRANCH_SCAN_ID,
                EMPTY_OPTIONAL_STRING,
                false,
                organizationDAO, FULL_DATA))
        .put(
            MULTIPLE_WITH_RECOMMENDED,
            new PullRequestLineFeedback(
                defaultPolicyViolations(10),
                "Test Component",
                iqBaseUrl,
                new RemediationVersionDTO(
                    "123",
                    ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING,
                    0),
                SCM_ON_PREM_BASE_URL,
                APPLICATION_PUBLIC_ID,
                FEATURE_BRANCH_SCAN_ID,
                EMPTY_OPTIONAL_STRING,
                false,
                organizationDAO, FULL_DATA))
        .put(
            SINGLE_WITH_GOLDEN,
            new PullRequestLineFeedback(
                defaultPolicyViolations(1),
                "Test Component",
                iqBaseUrl,
                new RemediationVersionDTO(
                    "123",
                    ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
                    0),
                SCM_ON_PREM_BASE_URL,
                APPLICATION_PUBLIC_ID,
                FEATURE_BRANCH_SCAN_ID,
                EMPTY_OPTIONAL_STRING,
                false,
                organizationDAO, FULL_DATA))
        .put(
            SINGLE_WITH_RECOMMENDED,
            new PullRequestLineFeedback(
                defaultPolicyViolations(1),
                "Test Component",
                iqBaseUrl,
                new RemediationVersionDTO(
                    "123",
                    ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING,
                    0),
                SCM_ON_PREM_BASE_URL,
                APPLICATION_PUBLIC_ID,
                FEATURE_BRANCH_SCAN_ID,
                EMPTY_OPTIONAL_STRING,
                false,
                organizationDAO, FULL_DATA))
        .put(
            MULTIPLE_WITH_GOLDEN_SCM_IMPROVEMENTS,
            new PullRequestLineFeedback(
                defaultPolicyViolations(10),
                "Test Component",
                iqBaseUrl,
                new RemediationVersionDTO(
                    "123",
                    ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
                    0),
                SCM_ON_PREM_BASE_URL,
                APPLICATION_PUBLIC_ID,
                FEATURE_BRANCH_SCAN_ID,
                EMPTY_OPTIONAL_STRING,
                true,
                organizationDAO, FULL_DATA))
        .put(
            MULTIPLE_WITH_RECOMMENDED_SCM_IMPROVEMENTS,
            new PullRequestLineFeedback(
                defaultPolicyViolations(10),
                "Test Component",
                iqBaseUrl,
                new RemediationVersionDTO(
                    "123",
                    ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING,
                    0),
                SCM_ON_PREM_BASE_URL,
                APPLICATION_PUBLIC_ID,
                FEATURE_BRANCH_SCAN_ID,
                EMPTY_OPTIONAL_STRING,
                true,
                organizationDAO, FULL_DATA))
        .put(
            SINGLE_WITH_GOLDEN_SCM_IMPROVEMENTS,
            new PullRequestLineFeedback(
                defaultPolicyViolations(1),
                "Test Component",
                iqBaseUrl,
                new RemediationVersionDTO(
                    "123",
                    ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
                    0),
                SCM_ON_PREM_BASE_URL,
                APPLICATION_PUBLIC_ID,
                FEATURE_BRANCH_SCAN_ID,
                EMPTY_OPTIONAL_STRING,
                true,
                organizationDAO,
                FULL_DATA))
        .put(
            SINGLE_WITH_RECOMMENDED_SCM_IMPROVEMENTS,
            new PullRequestLineFeedback(
                defaultPolicyViolations(1),
                "Test Component",
                iqBaseUrl,
                new RemediationVersionDTO(
                    "123",
                    ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING,
                    0),
                SCM_ON_PREM_BASE_URL,
                APPLICATION_PUBLIC_ID,
                FEATURE_BRANCH_SCAN_ID,
                EMPTY_OPTIONAL_STRING,
                true,
                organizationDAO,
                FULL_DATA))
        .build();
  }

  @Test
  public void testPullRequestFeedback_multipleNoSuggestion_github() throws Exception {
    assertContents(testCases.get(MULTIPLE_NO_SUGGESTIONS),
        "PullRequestLineFeedback_multipleNoSuggestions.md", GITHUB);
  }

  @Test
  public void testPullRequestFeedback_multipleWithSuggestion_github() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_SUGGESTION),
        "PullRequestLineFeedback_multipleWithSuggestion.md", GITHUB);
  }

  @Test
  public void testPullRequestFeedback_multipleWithSuggestionAndDependencyRemediation_github() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_SUGGESTION_AND_DEPENDENCY_REMEDIATION),
        "PullRequestLineFeedback_multipleWithSuggestionAndDependencyRemediation.md", GITHUB);
  }

  @Test
  public void testPullRequestFeedback_multipleWithGolden_github() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_GOLDEN),
        "PullRequestLineFeedback_multipleWithGolden.md", GITHUB);
  }

  @Test
  public void testPullRequestFeedback_multipleWithRecommended_github() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_RECOMMENDED),
        "PullRequestLineFeedback_multipleWithRecommended.md", GITHUB);
  }

  @Test
  public void testPullRequestFeedback_multipleWithGolden_github_ScmImprovements() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_GOLDEN_SCM_IMPROVEMENTS),
        "PullRequestLineFeedback_multipleWithGolden_ScmImprovements.md", GITHUB);
  }

  @Test
  public void testPullRequestFeedback_multipleWithRecommended_github_ScmImprovements() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_RECOMMENDED_SCM_IMPROVEMENTS),
        "PullRequestLineFeedback_multipleWithRecommended_ScmImprovements.md", GITHUB);
  }

  @Test
  public void testPullRequestFeedback_singleNoSuggestion_github() throws Exception {
    assertContents(testCases.get(SINGLE_NO_SUGGESTION),
        "PullRequestLineFeedback_singleNoSuggestions.md", GITHUB);
  }

  @Test
  public void testPullRequestFeedback_singleWithSuggestion_github() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_SUGGESTION),
        "PullRequestLineFeedback_singleWithSuggestion.md", GITHUB);
  }

  @Test
  public void testPullRequestFeedback_singleWithGolden_github() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_GOLDEN),
        "PullRequestLineFeedback_singleWithGolden.md", GITHUB);
  }

  @Test
  public void testPullRequestFeedback_singleWithRecommended_github() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_RECOMMENDED),
        "PullRequestLineFeedback_singleWithRecommended.md", GITHUB);
  }

  @Test
  public void testPullRequestFeedback_singleWithGolden_github_ScmImprovements() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_GOLDEN_SCM_IMPROVEMENTS),
        "PullRequestLineFeedback_singleWithGolden_ScmImprovements.md", GITHUB);
  }

  @Test
  public void testPullRequestFeedback_singleWithRecommended_github_ScmImprovements() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_RECOMMENDED_SCM_IMPROVEMENTS),
        "PullRequestLineFeedback_singleWithRecommended_ScmImprovements.md", GITHUB);
  }

  @Test
  public void testPullRequestFeedback_multipleNoSuggestion_gitlab() throws Exception {
    assertContents(testCases.get(MULTIPLE_NO_SUGGESTIONS),
        "PullRequestLineFeedback_multipleNoSuggestions_gitlab.md", SourceControlProvider.GITLAB);
  }

  @Test
  public void testPullRequestFeedback_multipleWithSuggestion_gitlab() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_SUGGESTION),
        "PullRequestLineFeedback_multipleWithSuggestion_gitlab.md", SourceControlProvider.GITLAB);
  }

  @Test
  public void testPullRequestFeedback_multipleWithSuggestionAndDependencyRemediation_gitlab() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_SUGGESTION_AND_DEPENDENCY_REMEDIATION),
        "PullRequestLineFeedback_multipleWithSuggestionAndDependencyRemediation_gitlab.md",
        SourceControlProvider.GITLAB);
  }

  @Test
  public void testPullRequestFeedback_multipleWithGolden_gitlab() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_GOLDEN),
        "PullRequestLineFeedback_multipleWithGold_gitlab.md", SourceControlProvider.GITLAB);
  }

  @Test
  public void testPullRequestFeedback_multipleWithRecommended_gitlab() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_RECOMMENDED),
        "PullRequestLineFeedback_multipleWithReco_gitlab.md", SourceControlProvider.GITLAB);
  }

  @Test
  public void testPullRequestFeedback_multipleWithGolden_gitlab_ScmImprovements() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_GOLDEN_SCM_IMPROVEMENTS),
        "PullRequestLineFeedback_multipleWithGolden_gitlab_ScmImprovements.md", SourceControlProvider.GITLAB);
  }

  @Test
  public void testPullRequestFeedback_multipleWithRecommended_gitlab_ScmImprovements() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_RECOMMENDED_SCM_IMPROVEMENTS),
        "PullRequestLineFeedback_multipleWithRecommended_gitlab_ScmImprovements.md", SourceControlProvider.GITLAB);
  }

  @Test
  public void testPullRequestFeedback_singleNoSuggestion_gitlab() throws Exception {
    assertContents(testCases.get(SINGLE_NO_SUGGESTION),
        "PullRequestLineFeedback_singleNoSuggestions_gitlab.md", SourceControlProvider.GITLAB);
  }

  @Test
  public void testPullRequestFeedback_singleWithSuggestion_gitlab() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_SUGGESTION),
        "PullRequestLineFeedback_singleWithSuggestion_gitlab.md", SourceControlProvider.GITLAB);
  }

  @Test
  public void testPullRequestFeedback_singleWithGolden_gitlab() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_GOLDEN),
        "PullRequestLineFeedback_singleWithGolden_gitlab.md", SourceControlProvider.GITLAB);
  }

  @Test
  public void testPullRequestFeedback_singleWithRecommended_gitlab() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_RECOMMENDED),
        "PullRequestLineFeedback_singleWithRecommended_gitlab.md", SourceControlProvider.GITLAB);
  }

  @Test
  public void testPullRequestFeedback_singleWithGolden_gitlab_ScmImprovements() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_GOLDEN_SCM_IMPROVEMENTS),
        "PullRequestLineFeedback_singleWithGolden_gitlab_ScmImprovements.md", SourceControlProvider.GITLAB);
  }

  @Test
  public void testPullRequestFeedback_singleWithRecommended_gitlab_ScmImprovements() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_RECOMMENDED_SCM_IMPROVEMENTS),
        "PullRequestLineFeedback_singleWithRecommended_gitlab_ScmImprovements.md", SourceControlProvider.GITLAB);
  }

  @Test
  public void testPullRequestFeedback_multipleNoSuggestion_bitbucket() throws Exception {
    assertContents(testCases.get(MULTIPLE_NO_SUGGESTIONS),
        "PullRequestLineFeedback_multipleNoSuggestions_noHtml.md", SourceControlProvider.BITBUCKET);
  }

  @Test
  public void testPullRequestFeedback_multipleWithSuggestion_bitbucket() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_SUGGESTION),
        "PullRequestLineFeedback_multipleWithSuggestion_noHtml.md", SourceControlProvider.BITBUCKET);
  }

  @Test
  public void testPullRequestFeedback_multipleWithSuggestionAndDependencyRemediation_bitbucket() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_SUGGESTION_AND_DEPENDENCY_REMEDIATION),
        "PullRequestLineFeedback_multipleWithSuggestionAndDependencyRemediation_noHtml.md",
        SourceControlProvider.BITBUCKET);
  }

  @Test
  public void testPullRequestFeedback_multipleWithGolden_bitbucket() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_GOLDEN),
        "PullRequestLineFeedback_multipleWithGolden_noHtml.md",
        SourceControlProvider.BITBUCKET);
  }

  @Test
  public void testPullRequestFeedback_multipleWithRecommended_bitbucket() throws Exception {
    assertContents(testCases.get(MULTIPLE_WITH_RECOMMENDED),
        "PullRequestLineFeedback_multipleWithRecommended_noHtml.md",
        SourceControlProvider.BITBUCKET);
  }

  @Test
  public void testPullRequestFeedback_singleNoSuggestion_bitbucket() throws Exception {
    assertContents(testCases.get(SINGLE_NO_SUGGESTION),
        "PullRequestLineFeedback_singleNoSuggestions_noHtml.md", SourceControlProvider.BITBUCKET);
  }

  @Test
  public void testPullRequestFeedback_singleWithSuggestion_bitbucket() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_SUGGESTION),
        "PullRequestLineFeedback_singleWithSuggestion_noHtml.md", SourceControlProvider.BITBUCKET);
  }

  @Test
  public void testPullRequestFeedback_singleWithGolden_bitbucket() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_GOLDEN),
        "PullRequestLineFeedback_singleWithGolden_noHtml.md", SourceControlProvider.BITBUCKET);
  }

  @Test
  public void testPullRequestFeedback_singleWithRecommended_bitbucket() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_RECOMMENDED),
        "PullRequestLineFeedback_singleWithRecommended_noHtml.md", SourceControlProvider.BITBUCKET);
  }

  @Test
  public void testPullRequestFeedback_singleWithSuggestion_azureCloud() throws Exception {
    // Azure Cloud uses the standard template with embedded HTML
    assertContents(testCases.get(SINGLE_WITH_SUGGESTION_AZCLOUD),
        "PullRequestLineFeedback_singleWithSuggestion_azureCloud.md", SourceControlProvider.AZURE);
  }

  @Test
  public void testPullRequestFeedback_singleWithSuggestion_azureOnPrem() throws Exception {
    // on-prem Azure uses the no-HTML template
    assertContents(testCases.get(SINGLE_WITH_SUGGESTION_AZONPREM),
        "PullRequestLineFeedback_singleWithSuggestion_noHtml.md", SourceControlProvider.AZURE);
  }

  @Test
  public void testPullRequestFeedback_singleWithGolden_azureOnPrem() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_GOLDEN),
        "PullRequestLineFeedback_singleWithGolden_noHtml.md", SourceControlProvider.AZURE);
  }

  @Test
  public void testPullRequestFeedback_singleWithRecommended_azureOnPrem() throws Exception {
    assertContents(testCases.get(SINGLE_WITH_RECOMMENDED),
        "PullRequestLineFeedback_singleWithRecommended_noHtml.md", SourceControlProvider.AZURE);
  }

  @Test
  public void testPullRequestFeedback_nullViolations() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(
            () -> new PullRequestLineFeedback(null,
                "Test Component",
                lookup(BaseUrl.class).getConfigured(),
                null,
                null,
                APPLICATION_PUBLIC_ID,
                FEATURE_BRANCH_SCAN_ID,
                null,
                false, organizationDAO, FULL_DATA))
        .withMessageContaining("violations is required and cannot be null");
  }

  @Test
  public void testPullRequestFeedback_emptyViolations() {
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> new PullRequestLineFeedback(new ArrayList<>(), "Test Component",
            lookup(BaseUrl.class).getConfigured(), null, null,
            APPLICATION_PUBLIC_ID, FEATURE_BRANCH_SCAN_ID, null, false, organizationDAO, FULL_DATA)
                .renderTemplateAndGetContents(GITHUB))
        .withMessageContaining("violations cannot be empty");
  }

  @Test
  public void testPullRequestFeedback_nullDisplayName() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(
            () -> new PullRequestLineFeedback(new ArrayList<>(), null, lookup(BaseUrl.class).getConfigured(),
                null, null, APPLICATION_PUBLIC_ID, FEATURE_BRANCH_SCAN_ID, null,
                false, organizationDAO, FULL_DATA))
        .withMessageContaining("displayName is required and cannot be null");
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

  private void assertContents(
      final PullRequestLineFeedback details,
      final String expectedContentFile,
      SourceControlProvider provider) throws Exception
  {
    final Optional<String> contents = details.renderTemplateAndGetContents(provider);
    assertRenderedOutput(contents, getClass(), expectedContentFile);
  }
}
