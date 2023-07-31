/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.VulnerabilityDetailsService;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.render.model.ComponentFeedbackContext;
import com.sonatype.insight.brain.git.render.model.SeverityInfo;
import com.sonatype.insight.brain.git.render.model.MDImages;
import com.sonatype.insight.brain.git.render.model.SecurityIssue;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.ResearchType;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.SecurityVulnerabilityCustomData;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.SecurityVulnerabilitySeverity;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mock;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackContextFactoryTest.BreakingChangeType.FEW;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackContextFactoryTest.BreakingChangeType.MANY;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackContextFactoryTest.BreakingChangeType.NONE;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackContextFactoryTest.TestCaseId.*;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackHelper.generatePVWithManyConditionFacts;
import static com.sonatype.insight.brain.git.render.model.MDImages.DIRECT_DEP_LOGO;
import static com.sonatype.insight.brain.git.render.model.MDImages.SONATYPE_DEEP_DIVE_TAG;
import static com.sonatype.insight.brain.git.render.model.MDImages.SONATYPE_FAST_TRACK_TAG;
import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.utils.TemplateHelper.assertRenderedOutput;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.ResearchType.DEEP_DIVE;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.ResearchType.FAST_TRACK;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

public class ComponentFeedbackContextFactoryTest
    extends AbstractComponentTest
{
  private static final String CURRENT_VERSION = "2.13.1";

  private static final String SUGGESTED_VERSION = "2.15.0";

  private static final String CODE_SUGGESTION = "            <version>" + SUGGESTED_VERSION + "</version>";

  private static final String NO_CODE_SUGGESTION = null;

  private static final ComponentIdentifier COMPONENT_IDENTIFIER = createMavenCoordinates(
      "com.fasterxml.jackson.core",
      "jackson-databind",
      CURRENT_VERSION
  );

  private static final String COMPONENT_DISPLAY_NAME = "com.fasterxml.jackson.core.jackson-databind:" + CURRENT_VERSION;

  private static final String COMPONENT_HASH = "myhash123";

  private static final String PUBLIC_APP_ID = "some-public-app-id";

  private static final String FEATURE_BRANCH_SCAN_ID = "some-feature-branch-scan-id";

  private static final String IQ_BASE_URL = "https://iq.example.com/";

  private static final String COMP_DETAILS_LINK =
      "https://iq.example.com/ui/links/application/some-public-app-id/" +
          "report/some-feature-branch-scan-id/componentDetails/myhash123?source=pr-line-commenting";

  private static final SecurityVulnerabilityData VULN_1 = generateVulnData("CVE-123-01", FAST_TRACK, 5.6f);

  private static final SecurityVulnerabilityData VULN_2 = generateVulnData("SONATYPE-123-01", DEEP_DIVE, 6.7f);

  private static final SecurityVulnerabilityData VULN_UNKNOWN = generateVulnData("SONATYPE-123-03", null, 8.7f);

  private static final PolicyViolation DEFAULT_PV =  generatePolicyViolation("pv1", 7);

  private static final RemediationVersionDTO NO_SUGGESTION_REMEDIATION = null;

  private static final RemediationVersionDTO SUGGESTION_NO_DEPS_REMEDIATION =
      new RemediationVersionDTO(SUGGESTED_VERSION, null, FEW.getNumBreakingChanges());

  private static final RemediationVersionDTO SUGGESTION_WITH_DEPS_REMEDIATION =
      new RemediationVersionDTO(
          SUGGESTED_VERSION,
          ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES,
          FEW.getNumBreakingChanges());

  private static final ThreatLevelDisplay CRITICAL_THREAT_LEVEL_DISPLAY = ThreatLevelDisplay.fromValue(10);

  private static final ThreatLevelDisplay SEVERE_THREAT_LEVEL_DISPLAY = ThreatLevelDisplay.fromValue(4);

  private static final ThreatLevelDisplay MODERATE_THREAT_LEVEL_DISPLAY = ThreatLevelDisplay.fromValue(2);

  private static final ThreatLevelDisplay LOW_THREAT_LEVEL_DISPLAY = ThreatLevelDisplay.fromValue(1);

  @Rule
  public TestName name = new TestName();

  @Mock
  private VulnerabilityDetailsService vulnerabilityDetailsServiceMock;

  @Inject
  private ComponentFeedbackContextFactory underTest;

  @Inject
  private ComponentFeedbackMDRenderer renderer;

  private EnumMap<TestCaseId, TestData> testCases = new EnumMap<>(TestCaseId.class);

  private String expectedRenderedOutputFilename;

  @Override
  public void configure(final Binder binder) {
    binder.bind(VulnerabilityDetailsService.class).toInstance(vulnerabilityDetailsServiceMock);
    super.configure(binder);
  }

  @Before
  public void before() {
    setUpSecurity();
    // The markdown fixture must match the name of the test method
    this.expectedRenderedOutputFilename = name.getMethodName() + ".md";
  }

  @Test
  public void testBuild_singleNoSuggestion_github() {
    runTest(NO_SUGGESTION, GITHUB);
  }

  @Test
  public void testBuild_singleWithSuggestion_github() {
    runTest(WITH_SUGGESTION, GITHUB);
  }

  @Test
  public void testBuild_singleWithSuggestionAndDepRemediation_github() {
    runTest(WITH_SUGGESTION_AND_DEPENDENCY_REMEDIATION, GITHUB);
  }

  @Test
  public void testBuild_manyBreakingChanges_github() {
    runTest(MANY_BREAKING_CHANGES, GITHUB);
  }

  @Test
  public void testBuild_fewBreakingChanges_github() {
    runTest(FEW_BREAKING_CHANGES, GITHUB);
  }

  @Test
  public void testBuild_noBreakingChanges_github() {
    runTest(NO_BREAKING_CHANGES, GITHUB);
  }

  @Test
  public void testBuild_hasDirectDep_github() {
    runTest(HAS_DIRECT_DEP, GITHUB);
  }

  @Test
  public void testBuild_criticalThreatLevel_github() {
    runTest(CRITICAL_THREAT_LEVEL, GITHUB);
  }

  @Test
  public void testBuild_severeThreatLevel_github() {
    runTest(SEVERE_THREAT_LEVEL, GITHUB);
  }

  @Test
  public void testBuild_moderateThreatLevel_github() {
    runTest(MODERATE_THREAT_LEVEL, GITHUB);
  }

  @Test
  public void testBuild_lowThreatLevel_github() {
    runTest(LOW_THREAT_LEVEL, GITHUB);
  }

  @Test
  public void testBuild_noVulnerability_github() {
    runTest(NO_VULN, GITHUB);
  }

  private void setupTestCases(final SourceControlProvider provider) {
    testCases.put(NO_SUGGESTION, buildSingleNoSuggestionTestData(provider));
    testCases.put(WITH_SUGGESTION, buildWithSuggestionTestData(provider));
    testCases.put(WITH_SUGGESTION_AND_DEPENDENCY_REMEDIATION, buildWithSuggestionWithDepRemediationTestData(provider));
    testCases.put(MANY_BREAKING_CHANGES, buildBreakingChangesTestData(provider, MANY));
    testCases.put(FEW_BREAKING_CHANGES,  buildBreakingChangesTestData(provider, FEW));
    testCases.put(NO_BREAKING_CHANGES,   buildBreakingChangesTestData(provider, NONE));
    testCases.put(HAS_DIRECT_DEP, buildDirectDepTestData(provider));
    testCases.put(LOW_THREAT_LEVEL,        buildThreatLevelTestData(provider, LOW_THREAT_LEVEL_DISPLAY));
    testCases.put(MODERATE_THREAT_LEVEL,   buildThreatLevelTestData(provider, MODERATE_THREAT_LEVEL_DISPLAY));
    testCases.put(SEVERE_THREAT_LEVEL,     buildThreatLevelTestData(provider, SEVERE_THREAT_LEVEL_DISPLAY));
    testCases.put(CRITICAL_THREAT_LEVEL,   buildThreatLevelTestData(provider, CRITICAL_THREAT_LEVEL_DISPLAY));
    testCases.put(NO_VULN, buildNoVulnTestData(provider));
  }

  private void setupAllVulnerabilities(final PolicyViolation pv) {
    setupVulnerabilityDetailsService(pv, VULN_1);
    setupVulnerabilityDetailsService(pv, VULN_2);
  }

  private void setupVulnerabilityDetailsService(
      final PolicyViolation pv,
      final SecurityVulnerabilityData securityVulnerabilityData)
  {
    lenient()
            .when(vulnerabilityDetailsServiceMock.getSecurityVulnerabilityDetails(securityVulnerabilityData.identifier,
                    pv.getComponentIdentifier(),
                    null,
                    null,
                    APPLICATION,
                    pv.getOwnerId(),
                    true
                    ))
            .thenReturn(securityVulnerabilityData);
  }

  private void runTest(final TestCaseId testCaseId, final SourceControlProvider provider) {
    setupTestCases(provider);
    final TestData testData = testCases.get(testCaseId);
    testData.factoryInput.violations.forEach(this::setupAllVulnerabilities);
    final ComponentFeedbackContext actual = underTest.build(
            testData.factoryInput.provider,
            testData.factoryInput.violations,
            testData.factoryInput.displayName,
            testData.factoryInput.remediationVersionDTO,
            testData.factoryInput.applicationPublicId,
            testData.factoryInput.featureBranchScanId,
            testData.factoryInput.iqBaseUrl,
            testData.factoryInput.codeSuggestion
    );
    assertThat(actual).usingRecursiveComparison().isEqualTo(testData.expected);
    renderAndAssert(actual);
  }

  private void renderAndAssert(final ComponentFeedbackContext actual) {
    final Optional<String> result = renderer.render(actual);
    try {
      assertRenderedOutput(result, this.getClass(), expectedRenderedOutputFilename);
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static FactoryInput initFactoryInput(final SourceControlProvider provider) {
    final FactoryInput input = new FactoryInput();
    input.applicationPublicId = PUBLIC_APP_ID;
    input.displayName = COMPONENT_DISPLAY_NAME;
    input.featureBranchScanId = FEATURE_BRANCH_SCAN_ID;
    input.iqBaseUrl = IQ_BASE_URL;
    input.provider = provider;
    input.codeSuggestion = Optional.ofNullable(NO_CODE_SUGGESTION);
    return input;
  }

  private static TestData buildSingleNoSuggestionTestData(final SourceControlProvider provider) {
    final FactoryInput factoryInput = initFactoryInput(provider);
    factoryInput.violations = ImmutableList.of(DEFAULT_PV);
    factoryInput.remediationVersionDTO = NO_SUGGESTION_REMEDIATION;
    return new TestData(buildNoSuggestionContext(provider), factoryInput);
  }

  private static TestData buildWithSuggestionTestData(final SourceControlProvider provider) {
    final FactoryInput factoryInput = initFactoryInput(provider);
    factoryInput.violations = ImmutableList.of(DEFAULT_PV);
    factoryInput.remediationVersionDTO = SUGGESTION_NO_DEPS_REMEDIATION;
    factoryInput.codeSuggestion = Optional.of(CODE_SUGGESTION);
    return new TestData(buildWithSuggestionContext(provider), factoryInput);
  }

  private static TestData buildWithSuggestionWithDepRemediationTestData(final SourceControlProvider provider) {
    final FactoryInput factoryInput = initFactoryInput(provider);
    factoryInput.violations = ImmutableList.of(DEFAULT_PV);
    factoryInput.remediationVersionDTO = SUGGESTION_WITH_DEPS_REMEDIATION;
    factoryInput.codeSuggestion = Optional.of(CODE_SUGGESTION);
    return new TestData(buildWithSuggestionWithDepRemediationContext(provider), factoryInput);
  }

  private static TestData buildBreakingChangesTestData(
      final SourceControlProvider provider,
      final BreakingChangeType breakingChangeType)
  {
    final FactoryInput factoryInput = initFactoryInput(provider);
    factoryInput.violations = ImmutableList.of(DEFAULT_PV);
    factoryInput.remediationVersionDTO = new RemediationVersionDTO(
        SUGGESTED_VERSION,
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES,
        breakingChangeType.getNumBreakingChanges());
    factoryInput.codeSuggestion = Optional.of(CODE_SUGGESTION);
    return new TestData(buildBreakingChangesContext(provider, breakingChangeType), factoryInput);
  }

  private static TestData buildDirectDepTestData(final SourceControlProvider provider) {
    final FactoryInput factoryInput = initFactoryInput(provider);
    factoryInput.violations = ImmutableList.of(DEFAULT_PV);
    return new TestData(buildDirectDepContext(provider), factoryInput);
  }

  private static TestData buildThreatLevelTestData(
      final SourceControlProvider provider,
      final ThreatLevelDisplay expectedThreatLevelDisplay)
  {
    final FactoryInput factoryInput = initFactoryInput(provider);
    final PolicyViolation pv = generatePolicyViolation("pv3", expectedThreatLevelDisplay.getValue());
    factoryInput.violations = ImmutableList.of(pv);
    final List<SecurityIssue> expectedSecurityIssues = ImmutableList.of(
            buildSecurityIssue(pv, VULN_2, SONATYPE_DEEP_DIVE_TAG),
            buildSecurityIssue(pv, VULN_1, SONATYPE_FAST_TRACK_TAG)
    );
    return new TestData(buildThreatLevelContext(provider, expectedThreatLevelDisplay,
        expectedSecurityIssues), factoryInput);
  }

  private static TestData buildNoVulnTestData(final SourceControlProvider provider) {
    final PolicyViolation pv =  generatePolicyViolationUnknownVuln("pv2", 7);
    final FactoryInput factoryInput = initFactoryInput(provider);
    factoryInput.violations = ImmutableList.of(pv);
    return new TestData(buildNoVulnContext(provider, pv), factoryInput);
  }

  private static ComponentFeedbackContext buildNoSuggestionContext(final SourceControlProvider provider) {
    return buildDefaultContext(provider, BreakingChangeType.NOT_APPLICABLE, "", false);
  }

  private static ComponentFeedbackContext buildWithSuggestionContext(final SourceControlProvider provider) {
    return buildDefaultContext(provider, FEW, SUGGESTED_VERSION, false);
  }

  private static ComponentFeedbackContext buildWithSuggestionWithDepRemediationContext(
      final SourceControlProvider provider)
  {
    return buildDefaultContext(provider, FEW, SUGGESTED_VERSION, true);
  }

  private static ComponentFeedbackContext buildBreakingChangesContext(
      final SourceControlProvider provider,
      final BreakingChangeType breakingChangeType)
  {
    return buildDefaultContext(provider, breakingChangeType, SUGGESTED_VERSION, true);
  }

  private static ComponentFeedbackContext buildDirectDepContext(final SourceControlProvider provider) {
    return buildDefaultContext(provider, BreakingChangeType.NOT_APPLICABLE, "", false);
  }

  private static ComponentFeedbackContext buildThreatLevelContext(
      final SourceControlProvider provider,
      final ThreatLevelDisplay threatLevelDisplay,
      final List<SecurityIssue> expectedSecurityIssues)
  {
    return new ComponentFeedbackContext(
            true,
            threatLevelDisplay,
            COMP_DETAILS_LINK,
            COMPONENT_DISPLAY_NAME,
            provider,
            BreakingChangeType.NOT_APPLICABLE.getNumBreakingChanges(),
            "",
            false,
            expectedSecurityIssues,
            DIRECT_DEP_LOGO,
            NO_CODE_SUGGESTION);
  }

  private static ComponentFeedbackContext buildNoVulnContext(
      final SourceControlProvider provider,
      final PolicyViolation pv)
  {
    final List<SecurityIssue> securityIssues  = ImmutableList.of(buildSecurityIssueWitUnknownVuln(pv));
    return new ComponentFeedbackContext(
            true,
            ThreatLevelDisplay.fromValue(pv.getThreatLevel()),
            COMP_DETAILS_LINK,
            COMPONENT_DISPLAY_NAME,
            provider,
            BreakingChangeType.NOT_APPLICABLE.getNumBreakingChanges(),
            "",
            false,
            securityIssues,
            DIRECT_DEP_LOGO,
            NO_CODE_SUGGESTION);
  }

  private static ComponentFeedbackContext buildDefaultContext(
      final SourceControlProvider provider,
      final BreakingChangeType breakingChangeType,
      final String suggestedVersion,
      final boolean hasRemediationDeps)
  {
    final ImmutableList<SecurityIssue> securityIssues = ImmutableList.of(
            buildSecurityIssue(DEFAULT_PV, VULN_2, SONATYPE_DEEP_DIVE_TAG),
            buildSecurityIssue(DEFAULT_PV, VULN_1, SONATYPE_FAST_TRACK_TAG)
    );
    final String codeSuggestion = suggestedVersion.equals(SUGGESTED_VERSION) ? CODE_SUGGESTION : NO_CODE_SUGGESTION;
    return new ComponentFeedbackContext(
            true,
            ThreatLevelDisplay.fromValue(DEFAULT_PV.getThreatLevel()),
            COMP_DETAILS_LINK,
            COMPONENT_DISPLAY_NAME,
            provider,
            breakingChangeType.getNumBreakingChanges(),
            suggestedVersion,
            hasRemediationDeps,
            securityIssues,
            DIRECT_DEP_LOGO,
            codeSuggestion);
  }

  private static SecurityVulnerabilityData generateVulnData(
      final String refId,
      final ResearchType researchType,
      final float cvssScore)
  {
    final SecurityVulnerabilityData securityVulnerabilityData = new SecurityVulnerabilityData();
    securityVulnerabilityData.identifier = refId;
    securityVulnerabilityData.description = "The is a description of " + refId;
    securityVulnerabilityData.researchType = researchType;
    securityVulnerabilityData.mainSeverity = new SecurityVulnerabilitySeverity();
    securityVulnerabilityData.mainSeverity.score = cvssScore;
    securityVulnerabilityData.customData = new SecurityVulnerabilityCustomData();
    securityVulnerabilityData.customData.cvssSeverity = null;
    return securityVulnerabilityData;
  }

  private static PolicyViolation generatePolicyViolation(final String id, final int threatLevel) {
    final PolicyViolation pv = generatePVWithManyConditionFacts(
        id, COMPONENT_IDENTIFIER, threatLevel, VULN_1.identifier, VULN_2.identifier);
    pv.setHash(COMPONENT_HASH);
    return pv;
  }

  private static PolicyViolation generatePolicyViolationUnknownVuln(final String id, final int threatLevel) {
    final PolicyViolation pv = generatePVWithManyConditionFacts(
        id, COMPONENT_IDENTIFIER, threatLevel, VULN_UNKNOWN.identifier);
    pv.setHash(COMPONENT_HASH);
    return pv;
  }

  private static SecurityIssue buildSecurityIssue(
      final PolicyViolation pv,
      final SecurityVulnerabilityData svd,
      final MDImages dependencyImage)
  {
    final SeverityInfo severityInfo = new SeverityInfo(svd.identifier, svd.mainSeverity.score, dependencyImage);
    return new SecurityIssue(pv.getThreatLevel(), severityInfo, svd.description, generatePVDetailsLink(pv.getId()));
  }

  private static SecurityIssue buildSecurityIssueWitUnknownVuln(final PolicyViolation pv) {
    return new SecurityIssue(pv.getThreatLevel(), null, null, generatePVDetailsLink(pv.getId()));
  }

  private static String generatePVDetailsLink(final String policyViolationId) {
    return "https://iq.example.com/assets/index.html#/violation/"
        + policyViolationId + "?type=violation&sidebarReference=filter";
  }

  enum TestCaseId
  {
    NO_SUGGESTION,
    WITH_SUGGESTION,
    WITH_SUGGESTION_AND_DEPENDENCY_REMEDIATION,
    MANY_BREAKING_CHANGES,
    FEW_BREAKING_CHANGES,
    NO_BREAKING_CHANGES,
    HAS_DIRECT_DEP,

    CRITICAL_THREAT_LEVEL,
    SEVERE_THREAT_LEVEL,
    MODERATE_THREAT_LEVEL,
    LOW_THREAT_LEVEL,
    NO_VULN
    ;

  }

  enum BreakingChangeType
  {
    MANY(6),
    FEW(4),
    NONE(0),
    NOT_APPLICABLE(-1);
    private final int numBreakingChanges;

    BreakingChangeType(final int numBreakingChanges) {
      this.numBreakingChanges = numBreakingChanges;
    }

    public int getNumBreakingChanges() {
      return numBreakingChanges;
    }
  }

  /**
   * This class contains the input to the factory and the expected output.
   * This is eventually used to compare the actual against the expected.
   */
  private static class TestData
  {
    public ComponentFeedbackContext expected;

    public FactoryInput factoryInput;

    public TestData(final ComponentFeedbackContext expected, final FactoryInput factoryInput) {
      this.expected = expected;
      this.factoryInput = factoryInput;
    }
  }

  /**
   * Input to the factory builder
   */
  private static class FactoryInput
  {
    public SourceControlProvider provider;

    public List<PolicyViolation> violations;

    public String displayName;

    public RemediationVersionDTO remediationVersionDTO;

    public String applicationPublicId;

    public String featureBranchScanId;

    public String iqBaseUrl;

    public String scmBaseUrl;

    public Optional<String> codeSuggestion;
  }
}
