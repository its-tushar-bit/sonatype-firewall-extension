/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.git.SourceControlComponentDetails.ComponentInfo;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.policy.evaluator.ComponentFactory;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.DiffPosition;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.git.PullRequestCommentingService.MINIMUM_THREAT_LEVEL;
import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestFeedbackMarkupServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyEvaluationDiffService policyEvaluationDiffService;

  @Inject
  private SourceControlComponentLoader sourceControlComponentLoader;

  @Inject
  private PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private InsightConfig config;

  private TimeZone initialTimezone;

  @Before
  public void before() {
    config.setBaseUrl("http://localhost:1122");
    initialTimezone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  @After
  public void after() {
    TimeZone.setDefault(initialTimezone);
  }

  @Test
  public void testCreateMarkup() throws Exception {
    // given: Evaluation in feature branch with new vulnerabilities (some with remediation)
    final String FROM_SCAN_ID = "fromScanId";
    final String TO_SCAN_ID = "toScanId";
    Application app = tempEntity.newApplicationWithParent("TEST_APP_PUBLIC_ID", "TEST APP", "TEST ORG");
    //setup reports
    createReportFile(app.getId(), FROM_SCAN_ID,
        zipReportDir("/PullRequestFeedbackMarkupServiceTest/from-report", tempDir),
        insightWork);
    createReportFile(app.getId(), TO_SCAN_ID, zipReportDir("/PullRequestFeedbackMarkupServiceTest/to-report", tempDir),
        insightWork);

    int pullRequestNumber = 10;
    //setup evaluations
    PolicyEvaluation defaultBranchPolicyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID);
    PolicyEvaluation featureBranchPolicyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID);
    featureBranchPolicyEvaluation.setTime(new GregorianCalendar(2020, 5, 21, 9, 15, 32).getTime());

    //setup diff
    PolicyViolationDiff<PolicyViolation> diff =
        policyEvaluationDiffService.createPolicyViolationDiff(defaultBranchPolicyEvaluation,
            featureBranchPolicyEvaluation, MINIMUM_THREAT_LEVEL).get();

    //setup remediationVersionMap
    Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = new HashMap<>();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.h2database", "h2", "1.4.190", "", "jar");
    remediationVersionMap.put(componentIdentifier, new RemediationVersionDTO("1.4.200",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS));

    //setup pullRequestLineComments
    List<PullRequestLineCommentDTO> pullRequestLineComments = new ArrayList<>();
    PullRequestLineCommentDTO lineCommentDTO =
        new PullRequestLineCommentDTO(componentIdentifier, new DiffPosition("path", 1, 1, 1));
    lineCommentDTO.setScmId(12345);
    pullRequestLineComments.add(lineCommentDTO);

    //setup gitRepositoryInfo
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo("http://example.com/project/repository", null, "token",
            SourceControlProvider.GITHUB, "master", true, true);

    //setup source control component details
    SourceControlComponentDetails componentDetails = sourceControlComponentLoader.getSourceControlComponentDetails(
        featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());

    // add some dependency info manually
    ComponentInfo componentInfo = componentDetails.getComponentInfo("df71536d44e3b07f0c15");
    ComponentInfo newComponentInfo = new ComponentInfo(componentInfo.getDisplayName(), true);
    componentDetails.getHashToComponentInfoMap().put("df71536d44e3b07f0c15", newComponentInfo);
    componentInfo = componentDetails.getComponentInfo("7a03e737484ca232d714");
    newComponentInfo = new ComponentInfo(componentInfo.getDisplayName(), false);
    componentDetails.getHashToComponentInfoMap().put("7a03e737484ca232d714", newComponentInfo);

    PullRequestCommentTelemetry commentTelemetry = new PullRequestCommentTelemetry(app.getId(), pullRequestNumber);

    // when: creating pull request comment markup
    Optional<String> markup = pullRequestFeedbackMarkupService.createMarkup(
        diff,
        remediationVersionMap,
        pullRequestLineComments,
        gitRepositoryInfo,
        pullRequestNumber,
        featureBranchPolicyEvaluation,
        defaultBranchPolicyEvaluation,
        componentDetails,
        commentTelemetry
    );

    // then: markup is created and telemetry information updated
    final String expectedContent = readResource("PullRequestFeedbackMarkup_violationAdded.md");
    assertThat(markup).isNotEmpty();
    assertThat(markup.get()).isEqualTo(expectedContent);
    assertThat(commentTelemetry.newViolationsComponentCount).isEqualTo(4);
    assertThat(commentTelemetry.clearedViolationsComponentCount).isEqualTo(0);
  }

  @Test
  public void testCreateLineMarkup() throws Exception {
    // given: component introduce new policy violation for github
    final Condition condition = new Condition(MatchStateConditionType.ID, "is", "exact");
    final ConditionFact conditionFact = ComponentPolicyEvaluator.createConditionFact(condition,
        new MatchFact(ComponentFactory.forGav("G", "A", "V", MatchState.EXACT), null /* policyId */,
            null /* constraintId */, Collections.emptyList() /* conditionTriggers */));
    final ConstraintFact constraintFact = new ConstraintFact("constraint1", "Constraint 1", "OR");
    constraintFact.addConditionFact(conditionFact);

    List<ConstraintFact> constraintFactList = Collections.singletonList(constraintFact);
    PolicyEvaluation evaluation = new PolicyEvaluation();
    PolicyViolation policyViolation =
        new PolicyViolation(evaluation, "policy1", "Policy 1", 1,
            PolicyThreatCategory.OTHER, "H", ComponentIdentifier.createMavenCoordinates("G", "A", "V"),
            constraintFactList, "filename");

    final List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(policyViolation);

    // when: when generating comment line markup
    final Optional<String> contents =
        pullRequestFeedbackMarkupService.createLineMarkup(policyViolations, "Test Component",
            new RemediationVersionDTO("123", ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS),
            SourceControlProvider.GITHUB);

    // then: markup is generated
    final String expectedContent =
        readResource("PullRequestFeedbackMarkup_singlePolicyWithSuggestion.md");
    assertThat(contents).isNotEmpty();
    assertThat(removeDateFromOutput(contents.get())).isEqualTo(removeDateFromOutput(expectedContent));
  }

  private String readResource(String resourceName) throws Exception {
    final Path path = Paths.get(
        getClass().getResource("/PullRequestFeedbackMarkupServiceTest/" + resourceName).toURI());
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }

  private String removeDateFromOutput(final String value) {
    return value.trim().replaceAll("as of _.*", "");
  }
}
