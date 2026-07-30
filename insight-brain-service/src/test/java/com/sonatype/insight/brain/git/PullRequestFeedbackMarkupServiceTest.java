/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import static com.sonatype.insight.brain.git.PullRequestCommentingService.MINIMUM_THREAT_LEVEL;
import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static com.sonatype.insight.brain.utils.TemplateHelper.assertRenderedOutput;
import static com.sonatype.insight.brain.utils.TemplateHelper.readResource;
import static com.sonatype.nexus.scm.SourceControlProvider.AZURE;
import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritiesUtilsService;
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
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.DiffPosition;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mock;

public class PullRequestFeedbackMarkupServiceTest
    extends AbstractComponentTest
{
  private static final String DEFAULT_SCM_URL = "https://scm.mycompany.com";

  private static final String AZURE_CLOUD_SCM_URL = "https://dev.azure.com/mycompany";

  @Rule
  public TestName name = new TestName();

  @Mock
  private ScmReducedSecurityService mockScmReducedSecurityService;

  @Inject
  private PolicyEvaluationDiffService policyEvaluationDiffService;

  @Inject
  private SourceControlComponentLoader sourceControlComponentLoader;

  @Inject
  private PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService;

  @Inject
  private DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService;

  @Inject
  private InsightWork insightWork;

  private TimeZone initialTimezone;

  private String expectedRenderedOutputFilename;

  @Before
  public void before() {
    setBaseUrl("http://localhost:1122");
    initialTimezone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    // The markdown fixture must match the name of the test method
    this.expectedRenderedOutputFilename = name.getMethodName() + ".md";

    lenient().when(mockScmReducedSecurityService.isReducedSecurityData(anyString())).thenReturn(false);
  }

  @After
  public void after() {
    TimeZone.setDefault(initialTimezone);
  }

  @Test
  public void testCreateSummaryMarkup_withUxImprovement_RegularSecurityData() throws Exception {
    runCreateSummaryMarkupTest(true, "PullRequestFeedbackMarkup_violationAdded_withUxImprovement_regular.md");
  }

  @Test
  public void testCreateSummaryMarkup_withUxImprovement_ReducedSecurityData() throws Exception {
    when(mockScmReducedSecurityService.isReducedSecurityData(anyString())).thenReturn(true);
    runCreateSummaryMarkupTest(true, "PullRequestFeedbackMarkup_violationAdded_withUxImprovement_reduced.md");
  }

  @Test
  public void testCreateSummaryMarkup_noUxImprovement_RegularsecurityData() throws Exception {
    runCreateSummaryMarkupTest(false, "PullRequestFeedbackMarkup_violationAdded_noUxImprovement_regular.md");
  }

  @Test
  public void testCreateSummaryMarkup_noUxImprovement_ReducedSecurityData() throws Exception {
    when(mockScmReducedSecurityService.isReducedSecurityData(anyString())).thenReturn(true);
    runCreateSummaryMarkupTest(false, "PullRequestFeedbackMarkup_violationAdded_noUxImprovement_reduced.md");
  }

  @Test
  public void testCreateLineMarkup_noUXImprovement_github() throws Exception {
    runCreateLineMarkupTest(GITHUB, false);
  }

  @Test
  public void testCreateLineMarkup_withUXImprovement_github() throws Exception {
    runCreateLineMarkupTest(GITHUB, true);
  }

  @Test
  public void testCreateLineMarkup_noUXImprovement_gitlab() throws Exception {
    runCreateLineMarkupTest(GITLAB, false);
  }

  @Test
  public void testCreateLineMarkup_withUXImprovement_gitlab() throws Exception {
    runCreateLineMarkupTest(GITLAB, true);
  }

  @Test
  public void testCreateLineMarkup_noUXImprovement_bitbucket() throws Exception {
    runCreateLineMarkupTest(BITBUCKET, false);
  }

  @Test
  public void testCreateLineMarkup_withUXImprovement_bitbucket() throws Exception {
    runCreateLineMarkupTest(BITBUCKET, true);
  }

  @Test
  public void testCreateLineMarkup_noUXImprovement_azureOnPrem() throws Exception {
    runCreateLineMarkupTest(AZURE, false);
  }

  @Test
  public void testCreateLineMarkup_withUXImprovement_azureOnPrem() throws Exception {
    runCreateLineMarkupTest(AZURE, true);
  }

  @Test
  public void testCreateLineMarkup_noUXImprovement_azureCloud() throws Exception {
    runCreateLineMarkupTest(AZURE, false, AZURE_CLOUD_SCM_URL);
  }

  @Test
  public void testCreateLineMarkup_withUXImprovement_azureCloud() throws Exception {
    runCreateLineMarkupTest(AZURE, true, AZURE_CLOUD_SCM_URL);
  }

  private void runCreateSummaryMarkupTest(
      final boolean enableUxImprovement,
      final String expectedMarkupOutputFile) throws Exception
  {
    // given: Evaluation in feature branch with new vulnerabilities (some with remediation)
    final String FROM_SCAN_ID = "fromScanId";
    final String TO_SCAN_ID = "toScanId";
    Application app = tempEntity.newApplicationWithParent("TEST_APP_PUBLIC_ID", "TEST APP", "TEST ORG");
    // setup reports
    createReportFile(app.getId(), FROM_SCAN_ID,
        zipReportDir("/PullRequestFeedbackMarkupServiceTest/from-report", tempDir),
        insightWork);
    createReportFile(app.getId(), TO_SCAN_ID, zipReportDir("/PullRequestFeedbackMarkupServiceTest/to-report", tempDir),
        insightWork);

    int pullRequestNumber = 10;
    // setup evaluations
    PolicyEvaluation defaultBranchPolicyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID);
    PolicyEvaluation featureBranchPolicyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID);
    featureBranchPolicyEvaluation.setTime(new GregorianCalendar(2020, Calendar.JUNE, 21, 9, 15, 32).getTime());

    // setup diff
    PolicyViolationDiff<PolicyViolation> diff =
        policyEvaluationDiffService.createPolicyViolationDiffByComponents(defaultBranchPolicyEvaluation,
            featureBranchPolicyEvaluation, MINIMUM_THREAT_LEVEL).get();

    // setup remediationVersionMap
    Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = new HashMap<>();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.h2database", "h2", "1.4.190", "", "jar");
    remediationVersionMap.put(componentIdentifier, new RemediationVersionDTO("1.4.200",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS));

    // setup pullRequestLineComments
    List<PullRequestLineCommentDTO> pullRequestLineComments = new ArrayList<>();
    PullRequestLineCommentDTO lineCommentDTO =
        new PullRequestLineCommentDTO(componentIdentifier, new DiffPosition("path", 1, 0, 1, null, 1));
    lineCommentDTO.setScmId(12345L);
    pullRequestLineComments.add(lineCommentDTO);

    // setup gitRepositoryInfo
    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo("http://example.com/project/repository", null, null,
        "token", GITHUB, "master", true, true, true, true, true, true, false, null);

    // setup source control component details
    SourceControlComponentDetails componentDetails = sourceControlComponentLoader.getSourceControlComponentDetails(
        featureBranchPolicyEvaluation.getOwnerId(), featureBranchPolicyEvaluation.getScanId());

    // add some dependency info manually
    ComponentInfo componentInfo = componentDetails.getComponentInfo("df71536d44e3b07f0c15");
    ComponentInfo newComponentInfo = new ComponentInfo(componentInfo.getDisplayName(), true);
    componentDetails.getHashToComponentInfoMap().put("df71536d44e3b07f0c15", newComponentInfo);
    componentInfo = componentDetails.getComponentInfo("7a03e737484ca232d714");
    newComponentInfo = new ComponentInfo(componentInfo.getDisplayName(), false);
    componentDetails.getHashToComponentInfoMap().put("7a03e737484ca232d714", newComponentInfo);

    PullRequestCommentTelemetry commentTelemetry =
        new PullRequestCommentTelemetry(app.getId(), pullRequestNumber, app.getId());

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
        commentTelemetry,
        enableUxImprovement,
        developmentPrioritiesUtilsService);

    // then: markup is created and telemetry information updated
    final String expectedContent = readResource(getClass(), expectedMarkupOutputFile);
    assertThat(markup).isNotEmpty();
    assertThat(commentTelemetry.newViolationsComponentCount).isEqualTo(4);
    assertThat(commentTelemetry.clearedViolationsComponentCount).isEqualTo(0);
    assertThat(markup.get()).isEqualTo(expectedContent);
  }

  private void runCreateLineMarkupTest(
      final SourceControlProvider provider,
      final boolean scmUxImprovementFeatureEnabled) throws Exception
  {
    runCreateLineMarkupTest(provider, scmUxImprovementFeatureEnabled, DEFAULT_SCM_URL);
  }

  private void runCreateLineMarkupTest(
      final SourceControlProvider provider,
      final boolean scmUxImprovementFeatureEnabled,
      final String scmUrl) throws Exception
  {

    // given: Evaluation in feature branch with new vulnerabilities
    final String SCAN_ID = "myScanId";
    Application app = tempEntity.newApplicationWithParent("TEST_APP_PUBLIC_ID", "TEST APP", "TEST ORG");

    // given: component introduce new policy violation for github
    final Condition condition = new Condition(MatchStateConditionType.ID, "is", "exact");
    final ConditionFact conditionFact = ComponentPolicyEvaluator.createConditionFact(condition,
        new MatchFact(ComponentFactory.forGav("G", "A", "V", MatchState.EXACT), null /* policyId */,
            null /* constraintId */, Collections.emptyList() /* conditionTriggers */));
    final ConstraintFact constraintFact = new ConstraintFact("constraint1", "Constraint 1", "OR");
    constraintFact.addConditionFact(conditionFact);

    List<ConstraintFact> constraintFactList = Collections.singletonList(constraintFact);
    PolicyEvaluation evaluation = new PolicyEvaluation();
    evaluation.setOwnerId(app.getId());
    evaluation.setScanId(SCAN_ID);
    PolicyViolation policyViolation =
        new PolicyViolation(evaluation, "policy1", "Policy 1", 1,
            PolicyThreatCategory.OTHER, "H", ComponentIdentifier.createMavenCoordinates("G", "A", "V"),
            constraintFactList, "filename");
    policyViolation.setId("pv1");

    final List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(policyViolation);

    // when: when generating comment line markup
    final Optional<String> contents = pullRequestFeedbackMarkupService.createLineMarkup(
        policyViolations,
        "Test Component",
        new RemediationVersionDTO(
            "123",
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS),
        Optional.ofNullable(null),
        provider,
        scmUrl,
        evaluation.getOwnerId(),
        evaluation.getScanId(),
        scmUxImprovementFeatureEnabled);

    // then: markup is generated
    assertRenderedOutput(contents, this.getClass(), expectedRenderedOutputFilename);
  }
}
