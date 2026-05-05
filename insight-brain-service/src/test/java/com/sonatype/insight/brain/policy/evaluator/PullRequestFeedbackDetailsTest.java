/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritiesUtilsService;
import com.sonatype.insight.brain.git.PullRequestLineCommentDTO;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.SourceControlComponentDetails;
import com.sonatype.insight.brain.git.SourceControlComponentDetails.ComponentInfo;
import com.sonatype.insight.brain.git.SourceControlComponentLoader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.utils.TemplateHelper;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.DiffPosition;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Lists;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_MAVEN;
import static com.sonatype.insight.brain.git.PullRequestCommentingService.MINIMUM_THREAT_LEVEL;
import static com.sonatype.insight.brain.policy.evaluator.PullRequestDetailsBaseTest.CONVERT_URLS;
import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static com.sonatype.insight.brain.utils.TemplateHelper.assertRenderedOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class PullRequestFeedbackDetailsTest
    extends AbstractComponentTest
{
  private static final String FROM_SCAN_ID = "fromScanId";

  private static final String TO_SCAN_ID = "toScanId";

  private static final String ORG_NAME = "TEST ORG";

  private static final String ORG_ID = "TEST_ORG_ID";

  private static final String APP_NAME = "TEST APP";

  private static final String APP_INTERNAL_ID = "TEST_APP_INTERNAL_ID";

  private static final String APP_PUBLIC_ID = "TEST_APP_PUBLIC_ID";

  // The majority of tests will default to use the full security data, not reduced. For readability.
  private static final boolean FULL_DATA = false;

  private PolicyEvaluation featureBranchPolicyEvaluation;

  private PolicyEvaluation defaultBranchPolicyEvaluation;

  private PolicyViolationDiff<PolicyViolation> diff;

  private Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap;

  private List<PullRequestLineCommentDTO> pullRequestLineComments;

  private GitRepositoryInfo githubGitRepositoryInfo;

  private GitRepositoryInfo gitlabGitRepositoryInfo;

  private GitRepositoryInfo bitbucketGitRepositoryInfo;

  private GitRepositoryInfo azureGitRepositoryInfo;

  private GitRepositoryInfo azureOnPremGitRepositoryInfo;

  private final int pullRequestNumber = 10;

  private SourceControlComponentDetails componentDetails;

  @Inject
  private PolicyEvaluationDiffService policyEvaluationDiffService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private SourceControlComponentLoader sourceControlComponentLoader;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService;

  private Application app;

  private TimeZone initialTimezone;

  @Before
  public void before() {
    setBaseUrl("http://localhost:1122");
    tempEntity.newOrganizationWithSpecificId(ORG_ID, ORG_NAME);
    app = tempEntity.newApplicationWithSpecificId(APP_INTERNAL_ID, APP_NAME, APP_PUBLIC_ID, ORG_ID);
    initialTimezone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  @After
  public void after() {
    TimeZone.setDefault(initialTimezone);
  }

  @Test
  public void testPullRequestFeedback_addedOnly() throws Exception {
    // setup test data
    setupTestData();

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Added.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_addedOnly_prioritiesUrlAllowed() throws Exception {
    // setup test data
    setupTestData();
    SystemConfigurationPropertyFeature.PRIORITIZED_FINDINGS_REPORT.setEnabled(true);

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Added_prioritiesUrlAllowed.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_addedOnly_prioritiesUrlAllowed_Gitlab() throws Exception {
    // setup test data
    setupTestData();
    SystemConfigurationPropertyFeature.PRIORITIZED_FINDINGS_REPORT.setEnabled(true);

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, gitlabGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Added_prioritiesUrlAllowed_Gitlab.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_addedOnly_noEmbeddedHtml_urlPrioritiesAllowed() throws Exception {
    // setup test data
    setupTestData();
    SystemConfigurationPropertyFeature.PRIORITIZED_FINDINGS_REPORT.setEnabled(true);

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, bitbucketGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertRenderedOutput(
        contents, getClass(), "PullRequestFeedback_Added_noEmbeddedHtml_prioritiesUrlAllowed.md");
  }

  @Test
  public void testPullRequestFeedback_addedOnly_noEmbeddedHtml() throws Exception {
    // setup test data
    setupTestData();

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, bitbucketGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertRenderedOutput(contents, getClass(), "PullRequestFeedback_Added_noEmbeddedHtml.md");
  }

  @Test
  public void testPullRequestFeedback_addedOnly_noEmbeddedHtml_WithContext_Bitbucket() throws Exception {
    // setup test data
    setupTestData();

    // override repository information to include webContext
    bitbucketGitRepositoryInfo =
        new GitRepositoryInfo("https://bitbucket.com/webContext/scm/sonatype/enhanced-commit-information", null,
            "user", "token", SourceControlProvider.BITBUCKET, "master", true, true, true, true, true, true, false,
            null);

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, bitbucketGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertRenderedOutput(contents, getClass(), "PullRequestFeedback_Added_noEmbeddedHtml_WithContext.md");
  }

  private String readResource(String contentFile) throws Exception {
    return TemplateHelper.readResource(PullRequestFeedbackDetailsTest.class, contentFile);
  }

  @Test
  public void testPullRequestFeedback_AzureCloud_richHtml() throws Exception {
    // setup test data
    setupTestData();

    // when we get details for azure cloud
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, azureGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents follow the HTML template
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).contains("<details>");
  }

  @Test
  public void testPullRequestFeedback_AzureOnPrem_noEmbeddedHtml() throws Exception {
    // setup test data
    setupTestData();

    // when get details for azure on prem
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, azureOnPremGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents are following the no-html template
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).doesNotContain("<details>");
  }

  @Test
  public void testPullRequestFeedback_addedOnly_GitLab() throws Exception {
    // setup test data
    setupTestData();

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, gitlabGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Added_GitLab.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_clearedOnly() throws Exception {
    // setup test data
    setupTestDataForCleared();

    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff, remediationVersionMap, pullRequestLineComments,
            githubGitRepositoryInfo, pullRequestNumber, app, lookup(BaseUrl.class).getConfigured(), false,
            organizationDAO, developmentPrioritiesUtilsService, FULL_DATA);

    // then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Cleared.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_clearedOnly_noEmbeddedHtml() throws Exception {
    // setup test data
    setupTestDataForCleared();

    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, bitbucketGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Cleared_noEmbeddedHtml.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_clearedOnly_GitLab() throws Exception {
    // setup test data
    setupTestDataForCleared();

    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, gitlabGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Cleared_GitLab.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_clearedDoNotShowAsFixedWhenAlsoAppearedByAnotherVersionOfSameComponent() throws Exception {
    final GitRepositoryInfo githubGitRepositoryInfo = getGitRepositoryInfo();

    final PolicyEvaluation baseBranchEvalEvaluation = tempEntity
        .newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), TO_SCAN_ID);
    baseBranchEvalEvaluation.setTime(
        new GregorianCalendar(2020, Calendar.JUNE, 20, 9, 15, 32).getTime());

    final PolicyEvaluation policyEvaluation = tempEntity
        .newPolicyEvaluation(app.getId(), StageTypes.DEVELOP.getId(), TO_SCAN_ID);
    policyEvaluation.setTime(
        new GregorianCalendar(2020, Calendar.JUNE, 21, 9, 15, 32).getTime());

    // ====

    // component 1 has 2 violations, 1 of them appears only in cleared, meaning it was truly fixed by the pr
    // the vulnerability underlying the second violation was also re-introduced by the new version of the component
    // and so should not count as fixed
    final ComponentIdentifier component1 = createComponentIdentifier(
        "com.test",
        "component-1",
        "0.0.1",
        null);
    final ComponentIdentifier component1MinorVersionBump = component1.createAlternativeVersion("0.0.2");

    // policy violation 1 only in cleared so actually shown as fixed
    final PolicyViolation policyViolation1 = createPolicyViolation(
        component1,
        "component-1-hash",
        app.getId(),
        "build",
        "policy-id-1",
        "policy-name-1",
        9,
        createConstraintFact("policy-violation-1-constraint", "CVE-2024-1"));

    // policy violation 2, present against the new component version
    final PolicyViolation policyViolation2 = createPolicyViolation(
        component1,
        "component-1-hash",
        app.getId(),
        "build",
        "policy-id-2",
        "policy-name-2",
        7,
        createConstraintFact("policy-violation-2-constraint", "CVE-2024-2"));
    final PolicyViolation policyViolation2AgainstNewVersion = createPolicyViolation(
        component1MinorVersionBump, // component 1 at a new version
        "component-1-version-bump-hash",
        app.getId(),
        "build",
        "policy-id-2",
        "policy-name-2",
        7,
        createConstraintFact("policy-violation-2-constraint", "CVE-2024-2"));

    final PolicyViolationDiff<PolicyViolation> evaluationDiff = createDiff(
        Lists.newArrayList(policyViolation2AgainstNewVersion),
        Lists.newArrayList(policyViolation1, policyViolation2));

    // === When ===
    final SourceControlComponentDetails sourceControlComponentDetails = createSourceControlComponentDetails(
        Lists.newArrayList(
            Pair.of("component-1-hash", component1),
            Pair.of("component-1-version-bump-hash", component1MinorVersionBump)));

    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(
            sourceControlComponentDetails,
            policyEvaluation,
            baseBranchEvalEvaluation,
            evaluationDiff,
            new HashMap<>(),
            Lists.newArrayList(),
            githubGitRepositoryInfo,
            pullRequestNumber,
            app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    final Optional<String> contents = details.renderTemplateAndGetContents();

    // === Then ===
    final String expectedContent = readResource(
        "testPullRequestFeedback_clearedDoNotShowAsFixedWhenAlsoAppearedByAnotherVersionOfSameComponent.md");
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_appearedDoNotShowAsIntroducedWhenAlsoClearedFromAnotherVersionOfSameComponent() throws Exception {
    // === Given ===
    final GitRepositoryInfo githubGitRepositoryInfo = getGitRepositoryInfo();

    final PolicyEvaluation baseBranchEvalEvaluation = tempEntity
        .newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), TO_SCAN_ID);
    baseBranchEvalEvaluation.setTime(
        new GregorianCalendar(2020, Calendar.JUNE, 20, 9, 15, 32).getTime());

    final PolicyEvaluation policyEvaluation = tempEntity
        .newPolicyEvaluation(app.getId(), StageTypes.DEVELOP.getId(), TO_SCAN_ID);
    policyEvaluation.setTime(
        new GregorianCalendar(2020, Calendar.JUNE, 21, 9, 15, 32).getTime());

    // component 1 has 2 violations one of which is represented in both appeared and cleared, the other is
    // only in cleared --
    // this means only 1 violation was truly fixed
    // by represented I mean that a violation with:
    // the same constraint json and the same component (without regard to violation)
    final ComponentIdentifier component1 = createComponentIdentifier(
        "com.test",
        "component-1",
        "0.0.1",
        null);

    // this one will get filtered out and not show in the summary comments as either fixed or introduced
    final PolicyViolation policyViolation1 = createPolicyViolation(
        component1,
        "component-1-hash",
        app.getId(),
        "build",
        "policy-id-1",
        "policy-name-1",
        9,
        createConstraintFact("policy-violation-1-constraint", "CVE-2024-1"));

    // this one will show up as introduced
    final PolicyViolation policyViolation2 = createPolicyViolation(
        component1,
        "component-1-hash",
        app.getId(),
        "build",
        "policy-id-2",
        "policy-name-2",
        7,
        createConstraintFact("policy-violation-2-constraint", "CVE-2024-2"));

    final ComponentIdentifier component1MinorVersionBump = component1.createAlternativeVersion("0.0.2");
    final PolicyViolation policyViolation1AgainstNewVersion = createPolicyViolation(
        component1MinorVersionBump, // came component at a new version
        "component-1-hash-bump",
        app.getId(),
        "build",
        "policy-id-1",
        "policy-name-1",
        9,
        // same constraint json as policyViolation1
        createConstraintFact("policy-violation-1-constraint", "CVE-2024-1"));

    // This component has 1 violation in appeared and that same 1 violation in cleared for bumped version of the
    // component. The component should not show in the comment at all
    final ComponentIdentifier component2 = createComponentIdentifier(
        "com.test",
        "component-2",
        "0.0.1",
        null);
    final ComponentIdentifier component2MinorVersionBump = component2.createAlternativeVersion("0.0.2");

    final PolicyViolation policyViolation3 = createPolicyViolation(
        component2,
        "component-2-hash",
        app.getId(),
        "build",
        "policy-id-3",
        "policy-name-3",
        5,
        createConstraintFact("policy-violation-3-constraint", "CVE-2024-3"));

    final PolicyViolation policyViolation3AgainstNewVersion = createPolicyViolation(
        component2MinorVersionBump,
        "component-2-bumped-hash",
        app.getId(),
        "build",
        "policy-id-3",
        "policy-name-3",
        5,
        createConstraintFact("policy-violation-3-constraint", "CVE-2024-3"));

    final PolicyViolationDiff<PolicyViolation> evaluationDiff = createDiff(
        Lists.newArrayList(policyViolation1, policyViolation2, policyViolation3),
        Lists.newArrayList(policyViolation1AgainstNewVersion, policyViolation3AgainstNewVersion));

    // === When ===
    final SourceControlComponentDetails sourceControlComponentDetails = createSourceControlComponentDetails(
        Lists.newArrayList(
            Pair.of("component-1-hash", component1),
            Pair.of("component-1-hash-bump", component1MinorVersionBump),
            Pair.of("component-2-hash", component2),
            Pair.of("component-2-bumped-hash", component2MinorVersionBump)));

    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(
            sourceControlComponentDetails,
            policyEvaluation,
            baseBranchEvalEvaluation,
            evaluationDiff,
            new HashMap<>(),
            Lists.newArrayList(),
            githubGitRepositoryInfo,
            pullRequestNumber,
            app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    final Optional<String> contents = details.renderTemplateAndGetContents();

    // === Then ===
    final String expectedContent = readResource(
        "PullRequestFeedback_clearedDoNotShowAsIntroducedWhenAlsoAddedToAnotherVersionOfSameComponent.md");
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_addedAndCleared() throws Exception {
    // setup test data
    setupTestData();
    // create cleared policy violation that does not exist in the bom file
    PolicyViolation policyViolation = createClearedPolicyViolation();
    diff.getCleared().add(policyViolation);

    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_AddedAndCleared.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_addedAndCleared_noEmbeddedHtml() throws Exception {
    // setup test data
    setupTestData();

    // Add all the same violations that appeared to cleared. To make sure they don't get filtered out when we
    // de-duplicate cleared violations that appear to be the same as appeared violations for the same component,
    // we tweak the component identifiers
    final List<PolicyViolation> clearedViolations = diff.getAppeared()
        .stream()
        .map(policyViolation -> clonePolicyViolationWithModifiedComponentIdentifier(policyViolation, "-cleared"))
        .collect(Collectors.toList());

    diff.getCleared().addAll(clearedViolations);

    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, bitbucketGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_AddedAndCleared_noEmbeddedHtml.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_addedAndCleared_GitLab() throws Exception {
    // setup test data
    setupTestData();
    // create cleared policy violation that does not exist in the bom file
    PolicyViolation policyViolation = createClearedPolicyViolation();
    diff.getCleared().add(policyViolation);

    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, gitlabGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_AddedAndCleared_GitLab.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_noAddedOrCleared() throws Exception {
    // setup test data
    setupTestData();
    diff.getAppeared().clear();

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_NoAddedOrCleared.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_noAddedOrCleared_noEmbeddedHtml() throws Exception {
    // setup test data
    setupTestData();
    diff.getAppeared().clear();

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, bitbucketGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_NoAddedOrCleared_noEmbeddedHtml.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_noAddedOrCleared_GitLab() throws Exception {
    // setup test data
    setupTestData();
    diff.getAppeared().clear();

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, gitlabGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_NoAddedOrCleared_GitLab.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_singlePolicyViolationPlurality() throws IOException, URISyntaxException {
    // setup test data
    setupTestData();

    final PolicyViolation first = diff.getAppeared().get(0);
    diff.getAppeared().clear();
    diff.getAppeared().add(first);

    // setup source control component details
    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents has singular violation in heading
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).startsWith("### :thinking: Sonatype Lifecycle found a policy violation");
  }

  @Test
  public void testPullRequestFeedback_singleClearedViolationPlurality() throws IOException, URISyntaxException {
    // setup test data
    setupTestData();

    final PolicyViolation first = diff.getAppeared().get(0);
    diff.getAppeared().clear();
    diff.getCleared().add(first);

    // setup source control component details
    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    // when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // then assert that created contents has singular violation in heading
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get())
        .contains("### :sunglasses: Sonatype Lifecycle determined that you fixed an outstanding policy violation:");
  }

  @Test
  public void testPullRequestFeedback_nullBom() throws IOException, URISyntaxException {
    // setup test data
    setupTestData();

    // when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(
        () -> new PullRequestFeedbackDetails(null, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA));
  }

  @Test
  public void testPullRequestFeedback_nullDiff() throws IOException, URISyntaxException {
    // setup test data
    setupTestData();

    // when
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation,
            null, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA));
  }

  @Test
  public void testPullRequestFeedback_nullApp() throws URISyntaxException, IOException {
    // setup test data
    setupTestData();

    // when
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, null,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA));
  }

  @Test
  public void testPullRequestFeedback_invalidAppNoOrg() throws IOException, URISyntaxException {
    // setup test data
    setupTestData();
    app.setOrganizationId("FAKE_ID");

    // when
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> new PullRequestFeedbackDetails(componentDetails,
        featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
        diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
        lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService, FULL_DATA)
            .renderTemplateAndGetContents());
  }

  @Test
  public void testPullRequestFeedback_nullFeatureBranchEvaluation() throws IOException, URISyntaxException {
    // setup test data
    setupTestData();

    // when
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new PullRequestFeedbackDetails(componentDetails, null, defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA));
  }

  @Test
  public void testPullRequestFeedback_nullDefaultBranchEvaluation() throws IOException, URISyntaxException {
    // setup test data
    setupTestData();

    // when
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, null, diff,
            remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA));
  }

  @Test
  public void testGetComponentFeedbackList_noComponents() throws IOException, URISyntaxException {
    // given
    setupTestData();
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // when
    final List<Map<String, Object>> result = details.getNewComponentFeedbackList(new HashMap<>(),
        remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber,
        getBaseUrl());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetComponentFeedbackList_componentWithoutViolations() throws IOException, URISyntaxException {
    // setup
    setupTestData();

    final Map<String, List<PolicyViolation>> componentMap = new HashMap<>();
    componentMap.put("hash-1", Collections.singletonList(diff.getAppeared().get(0)));
    componentMap.put("hash-2", Collections.emptyList());

    SourceControlComponentDetails componentDetails = new SourceControlComponentDetails();
    ComponentInfo componentInfo = new ComponentInfo("NAME", true);
    componentDetails.getHashToComponentInfoMap().put("hash-1", componentInfo);
    componentInfo = new ComponentInfo("NAME_EMPTY", true);
    componentDetails.getHashToComponentInfoMap().put("hash-2", componentInfo);

    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    // when
    final List<Map<String, Object>> result = details.getNewComponentFeedbackList(componentMap,
        remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber,
        getBaseUrl());

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0)).isNotNull();
    assertThat(result.get(0).get("componentNameAndVersion")).isEqualTo("NAME");
    assertThat(result.get(0).get("highestThreatLevel")).isEqualTo(diff.getAppeared().get(0).getThreatLevel());
    assertThat((List<?>) (result.get(0).get("policiesViolated"))).hasSize(1);
    assertThat(result.get(1)).isNotNull();
    assertThat(result.get(1).get("componentNameAndVersion")).isEqualTo("NAME_EMPTY");
    assertThat(result.get(1).get("highestThreatLevel")).isEqualTo(0);
    assertThat((List<?>) (result.get(1).get("policiesViolated"))).isEmpty();
  }

  @Test
  public void testGetHighestThreatLevel_noViolations() {
    assertThat(PullRequestFeedbackDetails.getHighestThreatLevel(Collections.emptyList())).isEqualTo(0);
  }

  @Test
  public void testGetHighestThreatLevel_singleViolation() throws IOException, URISyntaxException {
    setupTestData();
    final int result =
        PullRequestFeedbackDetails.getHighestThreatLevel(Collections.singletonList(diff.getAppeared().get(0)));

    assertThat(result).isEqualTo(diff.getAppeared().get(0).getThreatLevel());
  }

  @Test
  public void testGetPoliciesViolatedMap_noPolicies() {
    assertThat(
        PullRequestFeedbackDetails.getPoliciesViolatedMap(Collections.emptyList(), getBaseUrl(), CONVERT_URLS,
            FULL_DATA, null /* unused */, null /* unused */)).isEmpty();
  }

  @Test
  public void testGetPoliciesViolatedMap_allSamePolicyId() throws IOException, URISyntaxException {
    setupTestData();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails.getPoliciesViolatedMap(
        diff.getAppeared()
            .stream()
            .peek(policyViolation -> policyViolation.setPolicyId("1"))
            .collect(Collectors.toList()),
        getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    assertThat(result).hasSize(1);
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_NoViolations() {
    assertThat(PullRequestFeedbackDetails.getConstraintsForPolicyViolationsPerPolicy(Collections.emptyList(),
        getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */))
            .isEmpty();
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_SingleViolation() throws IOException, URISyntaxException {
    setupTestData();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(Collections.singletonList(diff.getAppeared().get(0)), getBaseUrl(),
            CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    assertThat(result).hasSize(1);
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_MultipleViolationsNoConstraints() throws IOException, URISyntaxException {
    setupTestData();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(diff.getAppeared().stream().peek(policyViolation -> {
          policyViolation.setPolicyId("1");
          policyViolation.setConstraintFacts(new ArrayList<>(policyViolation.getConstraintFacts()));
          policyViolation.getConstraintFacts().clear();
        }).collect(Collectors.toList()), getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_MultipleViolationsWithConstraints() throws IOException, URISyntaxException {
    setupTestData();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(
            diff.getAppeared()
                .stream()
                .peek(policyViolation -> policyViolation.setPolicyId("1"))
                .collect(Collectors.toList()),
            getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    assertThat(result).hasSize(6);
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_SingleViolationWithoutConstraints() throws IOException, URISyntaxException {
    setupTestData();
    diff.getAppeared().get(0).setConstraintFacts(new ArrayList<>(diff.getAppeared().get(0).getConstraintFacts()));
    diff.getAppeared().get(0).getConstraintFacts().clear();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(Collections.singletonList(diff.getAppeared().get(0)), getBaseUrl(),
            CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    assertThat(result).isEmpty();
  }

  @Test
  public void testPullRequestFeedback_Bitbucket_LimitedViolatingComponents() throws Exception {
    setupTestData("/PullRequestFeedbackDetailsTest/from-report", "/PullRequestFeedbackDetailsTest/to-report-large",
        false);
    bitbucketGitRepositoryInfo =
        new GitRepositoryInfo("https://bitbucket.org/scm/project/repo", null, "user", "token",
            SourceControlProvider.BITBUCKET, "master", true, true, true, true, true, true, false, null);

    PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, bitbucketGitRepositoryInfo, pullRequestNumber, app,
            lookup(BaseUrl.class).getConfigured(), false, organizationDAO, developmentPrioritiesUtilsService,
            FULL_DATA);

    Optional<String> contents = details.renderTemplateAndGetContents();
    assertRenderedOutput(contents, getClass(), "PullRequestFeedback_Added_noEmbeddedHtml_LimitedComponents.md");
  }

  @Test
  public void testPullRequestFeedback_Bitbucket_LimitedFixedComponents() throws Exception {
    setupTestData("/PullRequestFeedbackDetailsTest/to-report-large", "/PullRequestFeedbackDetailsTest/from-report",
        false);
    bitbucketGitRepositoryInfo =
        new GitRepositoryInfo("https://bitbucket.org/scm/project/repo", null, "user", "token",
            SourceControlProvider.BITBUCKET, "master", true, true, true, true, true, true, false, null);
    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), defaultBranchPolicyEvaluation.getScanId());
    PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff, remediationVersionMap, pullRequestLineComments,
            bitbucketGitRepositoryInfo, pullRequestNumber, app, lookup(BaseUrl.class).getConfigured(), false,
            organizationDAO, developmentPrioritiesUtilsService, FULL_DATA);

    Optional<String> contents = details.renderTemplateAndGetContents();
    assertRenderedOutput(contents, getClass(), "PullRequestFeedback_Cleared_noEmbeddedHtml_LimitedComponents.md");
  }

  private PolicyViolation createClearedPolicyViolation() {
    PolicyViolation existingViolation = diff.getAppeared().get(0);
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setHash("12345678abcd12345678");
    policyViolation.setComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("org.group.fixed", "fixed-artifact", "1.0"));
    policyViolation.setConstraintFacts(existingViolation.getConstraintFacts());
    policyViolation.setPolicyId(existingViolation.getPolicyId());
    policyViolation.setPolicyName(existingViolation.getPolicyName());
    return policyViolation;
  }

  private void setupTestData() throws IOException, URISyntaxException {
    setupTestData("/PullRequestFeedbackDetailsTest/from-report", "/PullRequestFeedbackDetailsTest/to-report", true);
  }

  private void setupTestDataForCleared() throws IOException, URISyntaxException {
    setupTestData("/PullRequestFeedbackDetailsTest/to-report", "/PullRequestFeedbackDetailsTest/from-report", false);
  }

  private void setupTestData(
      final String defaultBranchReportLocation,
      final String featureBranchReportLocation,
      boolean forAdded) throws IOException, URISyntaxException
  {
    // setup reports
    createReportFile(app.getId(), FROM_SCAN_ID, zipReportDir(defaultBranchReportLocation, tempDir),
        insightWork);
    createReportFile(app.getId(), TO_SCAN_ID, zipReportDir(featureBranchReportLocation, tempDir),
        insightWork);

    // setup evaluations
    defaultBranchPolicyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID);
    featureBranchPolicyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID);
    featureBranchPolicyEvaluation.setTime(new GregorianCalendar(2020, Calendar.JUNE, 21, 9, 15, 32).getTime());

    // setup diff
    diff = policyEvaluationDiffService.createPolicyViolationDiffByComponents(defaultBranchPolicyEvaluation,
        featureBranchPolicyEvaluation, MINIMUM_THREAT_LEVEL).get();

    // setup remediationVersionMap
    remediationVersionMap = new HashMap<>();
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("com.h2database", "h2", "1.4.190", "", "jar");
    remediationVersionMap
        .put(ci, new RemediationVersionDTO("1.4.200", ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, 3));
    // setup it is explicitly set the remediation with dependencies in order to check the variation on the message
    ComponentIdentifier ci2 = ComponentIdentifier
        .createMavenCoordinates("org.springframework.security", "spring-security-web", "4.2.3.RELEASE", "", "jar");
    remediationVersionMap.put(ci2, new RemediationVersionDTO("4.5.0.RELEASE",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES));

    ComponentIdentifier ci3 = ComponentIdentifier
        .createMavenCoordinates("org.apache.kafka", "kafka-clients", "3.7.0", "", "jar");
    remediationVersionMap.put(ci3, new RemediationVersionDTO("3.8.0",
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES));

    // setup pullRequestLineComments
    pullRequestLineComments = new ArrayList<>();
    PullRequestLineCommentDTO lineCommentDTO = new PullRequestLineCommentDTO(ci, new DiffPosition("path",
        1, 0, 1, "456", 1));
    lineCommentDTO.setScmId(12345L);
    pullRequestLineComments.add(lineCommentDTO);

    // setup gitRepositoryInfo
    githubGitRepositoryInfo = getGitRepositoryInfo();

    gitlabGitRepositoryInfo =
        new GitRepositoryInfo("https://gitlab.com/sonatype/enhanced-commit-information", null, null, "token",
            SourceControlProvider.GITLAB, "master", true, true, true, true, true, true, false, null);

    bitbucketGitRepositoryInfo =
        new GitRepositoryInfo("https://bitbucket.com/scm/sonatype/enhanced-commit-information", null, "user", "token",
            SourceControlProvider.BITBUCKET, "master", true, true, true, true, true, true, false, null);

    azureGitRepositoryInfo =
        new GitRepositoryInfo("https://dev.azure.com/sonatype/int/_git/enhanced-commit-information", null,
            "user@sonatype.com", "token", SourceControlProvider.AZURE, "main", true, true, true, true, true, true,
            false,
            null);

    azureOnPremGitRepositoryInfo =
        new GitRepositoryInfo("https://azure-on-prem.com/sonatype/int/_git/enhanced-commit-information", null,
            "user@sonatype.com", "token", SourceControlProvider.AZURE, "main", true, true, true, true, true, true,
            false,
            null);

    // setup source control component details
    componentDetails = sourceControlComponentLoader.getSourceControlComponentDetails(
        featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());

    if (forAdded) {
      // add some dependency info manually
      ComponentInfo componentInfo = componentDetails.getComponentInfo("df71536d44e3b07f0c15");
      ComponentInfo newComponentInfo = new ComponentInfo(componentInfo.getDisplayName(), true);
      componentDetails.getHashToComponentInfoMap().put("df71536d44e3b07f0c15", newComponentInfo);
      componentInfo = componentDetails.getComponentInfo("7a03e737484ca232d714");
      newComponentInfo = new ComponentInfo(componentInfo.getDisplayName(), false);
      componentDetails.getHashToComponentInfoMap().put("7a03e737484ca232d714", newComponentInfo);
    }
  }

  private PolicyViolation clonePolicyViolationWithModifiedComponentIdentifier(
      final PolicyViolation originalPolicyViolation,
      final String postfix)
  {
    final ComponentIdentifier appearedComponent = originalPolicyViolation.getComponentIdentifier();

    final PolicyViolation clearedPolicyViolation1 = new PolicyViolation();

    if (appearedComponent != null) {
      clearedPolicyViolation1.setComponentIdentifier(
          createComponentIdentifier(
              appearedComponent.getCoordinates().get("groupId"),
              appearedComponent.getCoordinates().get("artifactId") + postfix,
              appearedComponent.getCoordinates().get("version"),
              appearedComponent.getCoordinates().get("classifier")));

      clearedPolicyViolation1.setHash(originalPolicyViolation.getHash() + postfix);
    }
    else {
      // this is not a component with an identifier, so it won't be affected by code to detect when different
      // versions of the same component are pulling in the same vulnerabilities,
      // just copy over the hash so it can be matched to a file in the test data via sourceControlComponentDetails
      clearedPolicyViolation1.setHash(originalPolicyViolation.getHash());
    }

    clearedPolicyViolation1.setApplicationId(originalPolicyViolation.getApplicationId());
    clearedPolicyViolation1.setStageTypeId(originalPolicyViolation.getStageTypeId());

    clearedPolicyViolation1.setPolicyId(originalPolicyViolation.getPolicyId() + postfix);
    clearedPolicyViolation1.setPolicyName(originalPolicyViolation.getPolicyName());
    clearedPolicyViolation1.setThreatLevel(originalPolicyViolation.getThreatLevel());
    clearedPolicyViolation1.setConstraintFacts(originalPolicyViolation.getConstraintFacts());

    return clearedPolicyViolation1;
  }

  private ComponentIdentifier createComponentIdentifier(
      final String groupId,
      final String artifactId,
      final String version,
      final String classifier)
  {
    return new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>()
    {
      {
        this.put("groupId", groupId);
        this.put("artifactId", artifactId);
        this.put("version", version);
        this.put("classifier", classifier);
      }
    });
  }

  private PolicyViolation createPolicyViolation(
      final ComponentIdentifier componentIdentifier,
      final String hash,
      final String applicationId,
      final String stageTypeId,
      final String policyId,
      final String policyName,
      final int threatLevel,
      final ConstraintFact constraintFact)
  {
    final PolicyViolation policyViolation = new PolicyViolation();

    policyViolation.setComponentIdentifier(componentIdentifier);
    policyViolation.setHash(hash);
    policyViolation.setApplicationId(applicationId);
    policyViolation.setStageTypeId(stageTypeId);
    policyViolation.setPolicyId(policyId);
    policyViolation.setPolicyName(policyName);
    policyViolation.setThreatLevel(threatLevel);
    policyViolation.setConstraintFacts(Collections.singletonList(constraintFact));

    return policyViolation;
  }

  private void addComponentToSourceControlComponentDetails(
      final SourceControlComponentDetails sourceControlComponentDetails,
      final String componentHash,
      final ComponentIdentifier componentIdentifier)
  {
    final ComponentInfo componentInfo =
        new ComponentInfo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString(), true);

    sourceControlComponentDetails.getHashToComponentInfoMap()
        .put(
            componentHash,
            componentInfo);

    sourceControlComponentDetails.getIdentifierToComponentInfoMap()
        .put(componentIdentifier,
            componentInfo);
  }

  private ConstraintFact createConstraintFact(
      final String constraintName,
      final String cve) throws IOException
  {
    final String str = "[" +
        "{" +
        "   \"constraintId\":\"feca8c475e2047839d8838823e7affef\"," +
        "   \"constraintName\":\"%s\"," +
        "   \"operatorName\":\"AND\"," +
        "   \"conditionFacts\": [{" +
        "      \"conditionTypeId\":\"SecurityVulnerabilitySeverity\"," +
        "\"conditionIndex\":0," +
        "\"summary\":\"Security Vulnerability Severity >= 0\"," +
        "\"reason\":\"Found security vulnerability %s with severity >= 0 (severity = 3.5)\"," +
        "\"reference\":{\"value\":\"%s\",\"type\":\"SECURITY_VULNERABILITY_REFID\"}," +
        "\"triggerJson\":\"{}\"}]}]";

    return JsonUtils.parse(String.format(str, constraintName, cve, cve), ConstraintFact[].class)[0];
  }

  private PolicyViolationDiff<PolicyViolation> createDiff(
      final List<PolicyViolation> appeared,
      final List<PolicyViolation> cleared)
  {
    final PolicyViolationDiff<PolicyViolation> evaluationDiff = new PolicyViolationDiff<>();

    evaluationDiff.getAppeared().addAll(appeared);

    evaluationDiff.getCleared().addAll(cleared);

    return evaluationDiff;
  }

  private SourceControlComponentDetails createSourceControlComponentDetails(
      List<Pair<String, ComponentIdentifier>> hashesToComponent)
  {
    final SourceControlComponentDetails sourceControlComponentDetails = new SourceControlComponentDetails();

    hashesToComponent.forEach(pair -> addComponentToSourceControlComponentDetails(
        sourceControlComponentDetails,
        pair.getLeft(),
        pair.getRight()));

    return sourceControlComponentDetails;
  }

  private GitRepositoryInfo getGitRepositoryInfo() {
    return new GitRepositoryInfo(
        "https://github.com/sonatype/enhanced-commit-information",
        null,
        null,
        "token",
        SourceControlProvider.GITHUB,
        "master",
        true,
        true,
        true,
        true,
        true,
        true,
        false,
        null);
  }
}
