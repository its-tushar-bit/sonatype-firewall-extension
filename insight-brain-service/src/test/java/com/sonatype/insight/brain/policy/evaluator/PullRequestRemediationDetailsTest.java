/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestRemediationDetailsTest
    extends AbstractComponentTest
{
  private static final String SCAN_ID = "5ea5363997ee2ba0c8730a5f785ae2c6";

  private static final String TEST_SCM_URL = "https://scm.mycompany.com";

  // The majority of tests will default to use the full security data, not reduced. For readability.
  private static final boolean FULL_DATA = false;

  @Inject
  private OrganizationDAO organizationDAO;

  private Application app;

  @Before
  public void before() {
    setBaseUrl("http://localhost:1122");
    app = tempEntity.newApplicationWithParent("my-public-app-id", "MyTestApp", "Integrations");
    PullRequestRemediationDetails.clock = Clock
        .fixed(Instant.parse("2019-11-26T18:15:30Z"), ZoneId.of("America/Los_Angeles"));
  }

  @Test
  public void testSecurityVulnerabilityReport_richHtml() throws Exception {
    testSecurityVulnerabilityReport(SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport.md", null, "next-no-violations");
  }

  @Test
  public void testSecurityVulnerabilityReport_richHtml_noBreakingChanges() throws Exception {
    testSecurityVulnerabilityReport(SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_noBreakingChanges.md", 0, "next-no-violations");
  }

  @Test
  public void testSecurityVulnerabilityReport_richHtml_fewBreakingChanges() throws Exception {
    testSecurityVulnerabilityReport(SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_fewBreakingChanges.md", 2, "next-no-violations");
  }

  @Test
  public void testSecurityVulnerabilityReport_richHtml_severalBreakingChanges() throws Exception {
    testSecurityVulnerabilityReport(SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_severalBreakingChanges.md", 3, "next-no-violations");
  }

  @Test
  public void testSecurityVulnerabilityReport_richHtml_manyBreakingChanges() throws Exception {
    testSecurityVulnerabilityReport(SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_manyBreakingChanges.md", 7, "next-no-violations");
  }

  @Test
  public void testSecurityVulnerabilityReport_richHtml_Golden() throws Exception {
    testSecurityVulnerabilityReport(SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_Golden.md", 0,
        "recommended-non-breaking-with-dependencies");

    testSecurityVulnerabilityReport(SourceControlProvider.GITLAB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_Golden_Gitlab.md", 0,
        "recommended-non-breaking-with-dependencies");
  }

  @Test
  public void testSecurityVulnerabilityReport_richHtml_Suggested() throws Exception {
    testSecurityVulnerabilityReport(SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_Suggested.md", 0, "recommended-non-breaking");

    testSecurityVulnerabilityReport(SourceControlProvider.GITLAB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_Suggested_Gitlab.md", 0, "recommended-non-breaking");
  }

  @Test
  public void testSecurityVulnerabilityReport_minimalHtml() throws Exception {
    testSecurityVulnerabilityReport(SourceControlProvider.BITBUCKET,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_noHtml.md", null, "next-no-violations");
  }

  @Test
  public void testSecurityVulnerabilityReport_minimalHtml_noBreakingChanges() throws Exception {
    testSecurityVulnerabilityReport(SourceControlProvider.BITBUCKET,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_bb_noBreakingChanges.md", 0, "next-no-violations");
  }

  @Test
  public void testSecurityVulnerabilityReport_minimalHtml_fewBreakingChanges() throws Exception {
    testSecurityVulnerabilityReport(SourceControlProvider.BITBUCKET,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_bb_fewBreakingChanges.md", 2, "next-no-violations");
  }

  @Test
  public void testSecurityVulnerabilityReport_minimalHtml_manyBreakingChanges() throws Exception {
    testSecurityVulnerabilityReport(SourceControlProvider.BITBUCKET,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_bb_manyBreakingChanges.md", 7, "next-no-violations");
  }

  @Test
  public void testSecurityVulnerabilityReport_minimalHtml_Golden() throws Exception {
    testSecurityVulnerabilityReport(
        SourceControlProvider.BITBUCKET,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_bb_Golden.md",
        0,
        "recommended-non-breaking-with-dependencies");
  }

  @Test
  public void testSecurityVulnerabilityReport_minimalHtml_Suggested() throws Exception {
    testSecurityVulnerabilityReport(
        SourceControlProvider.BITBUCKET,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_bb_Suggested.md",
        0,
        "recommended-non-breaking");
  }

  @Test
  public void testSecurityVulnerabilityReport_azure_noHtml() throws Exception {
    // azure uses minimal HTML
    testSecurityVulnerabilityReport(SourceControlProvider.AZURE,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_noHtml.md", null, "next-no-violations");
  }

  @Test
  public void testSecurityVulnerabilityReport_azure_Golden_noHtml() throws Exception {
    // azure uses minimal HTML
    testSecurityVulnerabilityReport(SourceControlProvider.AZURE,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_Golden_noHtml.md", 0,
        "recommended-non-breaking-with-dependencies");
  }

  @Test
  public void testSecurityVulnterabilityReport_azure_Suggested_noHtml() throws Exception {
    // azure uses minimal HTML
    testSecurityVulnerabilityReport(SourceControlProvider.AZURE,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_Suggested_noHtml.md", 0, "recommended-non-breaking");
  }

  @Test
  public void testSecurityVulnerabilityReport_RecommendedNonBreakingWithDependencies_Manual() throws Exception {
    testSecurityVulnerabilityReport(
        SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_Golden_manual.md",
        0,
        "recommended-non-breaking-with-dependencies",
        true);
  }

  @Test
  public void testSecurityVulnerabilityReport_RecommendedNonBreaking_Manual() throws Exception {
    testSecurityVulnerabilityReport(
        SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_Suggested_manual.md",
        0,
        "recommended-non-breaking",
        true);
  }

  @Test
  public void testSecurityVulnerabilityReport_NextNoViolations_Manual() throws Exception {
    testSecurityVulnerabilityReport(
        SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_manual.md",
        null,
        "next-no-violations",
        true);
  }

  @Test
  public void testSecurityVulnerabilityReport_Minimal_RecommendedNonBreakingWithDependencies_Manual() throws Exception {
    testSecurityVulnerabilityReport(
        SourceControlProvider.AZURE,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_Golden_noHtml_manual.md",
        0,
        "recommended-non-breaking-with-dependencies",
        true);
  }

  @Test
  public void testSecurityVulnerabilityReport_Minimal_RecommendedNonBreaking_Manual() throws Exception {
    testSecurityVulnerabilityReport(
        SourceControlProvider.AZURE,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_Suggested_noHtml_manual.md",
        0,
        "recommended-non-breaking",
        true);
  }

  @Test
  public void testSecurityVulnerabilityReport_Minimal_NextNoViolations_Manual() throws Exception {
    testSecurityVulnerabilityReport(
        SourceControlProvider.AZURE,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_noHtml_manual.md",
        null,
        "next-no-violations",
        true);
  }

  private void testSecurityVulnerabilityReport(
      SourceControlProvider provider,
      String expectedResource,
      Integer breakingChangesCount,
      String targetVersionType) throws Exception
  {
    testSecurityVulnerabilityReport(provider, expectedResource, breakingChangesCount, targetVersionType, false);
  }

  private void testSecurityVulnerabilityReport(
      SourceControlProvider provider,
      String expectedResource,
      Integer breakingChangesCount,
      String targetVersionType,
      boolean isManualPullRequest) throws Exception
  {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("org.jooq", "jooq", "3.11.2");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("foo", "bar", "baz");

    List<PolicyNotification> policyNotifications = createPolicyNotifications(componentIdentifier, componentIdentifier2);

    PullRequestRemediationDetails details =
        new PullRequestRemediationDetails(componentIdentifier, "3.11.3", targetVersionType, breakingChangesCount,
            "pullRequest", policyNotifications, app, SCAN_ID, Stage.ID_BUILD, getBaseUrl(), provider, TEST_SCM_URL,
            organizationDAO, isManualPullRequest, isManualPullRequest ? "Bob Smith" : null, FULL_DATA, false);

    assertThat(details.getTitle()).isEqualTo("Bump jooq to 3.11.3");

    String expectedContent = loadResource(expectedResource);
    assertThat(details.getContents()).isEqualTo(expectedContent);
  }

  private String loadResource(String resource) throws Exception {
    return new String(Files.readAllBytes(Paths.get(getClass().getResource(resource).toURI())), StandardCharsets.UTF_8);
  }

  @Test
  public void testSecurityVulnerabilityReport_npmComponent() throws Exception {
    testSecurityVulnerabilityReport_npmComponent(SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_npm.md");
  }

  @Test
  public void testMinimalMarkdownSecurityVulnerabilityReport_npmComponent() throws Exception {
    testSecurityVulnerabilityReport_npmComponent(SourceControlProvider.BITBUCKET,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_npm_bitbucket.md");
  }

  private void testSecurityVulnerabilityReport_npmComponent(
      SourceControlProvider provider,
      String expectedResultResource) throws Exception
  {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("@sonatype/foo", "1.0");

    List<PolicyNotification> policyNotifications = new ArrayList<>();

    PolicyNotification criticalPolicyNotification = new PolicyNotification(
        new PolicyFact("critical-id", "Security-Critical", 10),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact criticalComponentFact = new ComponentFact(componentIdentifier, "dummy-hash");
    ConstraintFact criticalConstraintFact = new ConstraintFact("constraint-id", "Critical risk CVSS score", "OR");
    criticalConstraintFact.addConditionFact(new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0,
        "Security Vulnerability Severity >= 9",
        "Found security vulnerability CVE-2016-1000031 with severity >= 9 (severity = 9.8)",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "CVE-2016-1000031")));
    criticalComponentFact.addConstraintFact(criticalConstraintFact);
    criticalPolicyNotification.getPolicyFact().addComponentFact(criticalComponentFact);

    policyNotifications.add(criticalPolicyNotification);

    PullRequestRemediationDetails details =
        new PullRequestRemediationDetails(componentIdentifier, "1.1", "next-no-violations", null, "pullRequest",
            policyNotifications, app, SCAN_ID, Stage.ID_BUILD, getBaseUrl(), provider, TEST_SCM_URL, organizationDAO,
            FULL_DATA);

    assertThat(details.getTitle()).isEqualTo("Bump @sonatype/foo to 1.1");

    String expectedContent = loadResource(expectedResultResource);
    assertThat(details.getContents()).isEqualTo(expectedContent);
  }

  @Test
  public void testSecurityVulnerabilityReport_golangComponent() throws Exception {
    testSecurityVulnerabilityReport_golangComponent(SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_golang.md");
  }

  @Test
  public void testMinimalMarkdownSecurityVulnerabilityReport_golangComponent() throws Exception {
    testSecurityVulnerabilityReport_golangComponent(SourceControlProvider.BITBUCKET,
        "/PullRequestRemediationDetailsTest/VulnerabilityReport_golang_bitbucket.md");
  }

  @Test
  public void testInnerSourcePR_Auto() throws Exception {
    testInnerSourcePR(SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/InnerSourceReport_Auto.md", false);
  }

  @Test
  public void testInnerSourcePR_Manual() throws Exception {
    testInnerSourcePR(SourceControlProvider.GITHUB,
        "/PullRequestRemediationDetailsTest/InnerSourceReport_Manual.md", true);
  }

  @Test
  public void testInnerSourcePR_Auto_Minimal() throws Exception {
    testInnerSourcePR(SourceControlProvider.BITBUCKET,
        "/PullRequestRemediationDetailsTest/InnerSourceReport_Auto_Minimal.md", false);
  }

  @Test
  public void testInnerSourcePR_Manual_Minimal() throws Exception {
    testInnerSourcePR(SourceControlProvider.BITBUCKET,
        "/PullRequestRemediationDetailsTest/InnerSourceReport_Manual_Minimal.md", true);
  }

  private void testInnerSourcePR(
      SourceControlProvider provider,
      String expectedResource,
      boolean isManualPullRequest) throws Exception
  {
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.example", "inner-source-produced", "1.58.7");
    List<PolicyNotification> policyNotifications = createPolicyNotifications(componentIdentifier, componentIdentifier);

    PullRequestRemediationDetails details =
        new PullRequestRemediationDetails(componentIdentifier, "1.58.8", "next-no-violations", 0,
            "pullRequest", policyNotifications, app, SCAN_ID, Stage.ID_BUILD, getBaseUrl(), provider, TEST_SCM_URL,
            organizationDAO, isManualPullRequest, isManualPullRequest ? "Bob Smith" : null, FULL_DATA, true);

    assertThat(details.getTitle()).isEqualTo("Bump inner-source-produced to 1.58.8");

    String expectedContent = loadResource(expectedResource);
    assertThat(details.getContents()).isEqualTo(expectedContent);
  }

  private void testSecurityVulnerabilityReport_golangComponent(
      SourceControlProvider provider,
      String expectedResultResource) throws Exception
  {
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createGolangCoordinates("golang.org/x/text", "v0.3.0");

    List<PolicyNotification> policyNotifications = new ArrayList<>();

    PolicyNotification criticalPolicyNotification = new PolicyNotification(
        new PolicyFact("critical-id", "Security-Critical", 9), null);
    ComponentFact criticalComponentFact = new ComponentFact(componentIdentifier, "dummy-hash");
    ConstraintFact criticalConstraintFact = new ConstraintFact("constraint-id", "Critical risk CVSS score", "OR");
    criticalConstraintFact.addConditionFact(new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0,
        "Security Vulnerability Severity >= 9",
        "Found security vulnerability CVE-2020-14040 with severity >= 9 (severity = 9.8)",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "CVE-2020-14040")));
    criticalComponentFact.addConstraintFact(criticalConstraintFact);
    criticalPolicyNotification.getPolicyFact().addComponentFact(criticalComponentFact);

    policyNotifications.add(criticalPolicyNotification);

    PullRequestRemediationDetails details =
        new PullRequestRemediationDetails(componentIdentifier, "v0.3.3", "next-no-violations", null, "pullRequest",
            policyNotifications, app, SCAN_ID, Stage.ID_BUILD, getBaseUrl(), provider, TEST_SCM_URL, organizationDAO,
            FULL_DATA);

    assertThat(details.getTitle()).isEqualTo("Bump golang.org/x/text to v0.3.3");

    String expectedContent = loadResource(expectedResultResource);
    assertThat(details.getContents()).isEqualTo(expectedContent);
  }

  @Test
  public void testSinglePolicyViolationPlurality() throws IOException {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("org.jooq", "jooq", "3.11.2");
    List<PolicyNotification> policyNotifications = ImmutableList.of(new PolicyNotification(
        new PolicyFact("critical-id", "Security-Critical", 10), new Notifications()));

    PullRequestRemediationDetails details =
        new PullRequestRemediationDetails(componentIdentifier, "3.11.3", "next-no-violations", null, "pullRequest",
            policyNotifications, app, SCAN_ID, Stage.ID_BUILD, getBaseUrl(), SourceControlProvider.GITHUB, TEST_SCM_URL,
            organizationDAO, FULL_DATA);

    assertThat(details.getContents().replace("\r\n", "\n"))
        .startsWith("## :shield: Automated pull request: Sonatype Lifecycle found 1 Policy Violation\n");
  }

  /**
   * A complex set of policy violations covering multiple components and policies. Intended as a superset of possible
   * scenarios we could encounter when synthesizing these into a simple aggregated display.
   */
  private List<PolicyNotification> createPolicyNotifications(
      final ComponentIdentifier componentIdentifier,
      final ComponentIdentifier componentIdentifier2)
  {
    List<PolicyNotification> policyNotifications = new ArrayList<>();

    // critical - one constraint violation with one security severity condition
    PolicyNotification criticalPolicyNotification = new PolicyNotification(
        new PolicyFact("critical-id", "Security-Critical", 10),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact criticalComponentFact = new ComponentFact(componentIdentifier, "dummy-hash");
    ConstraintFact criticalConstraintFact1 = new ConstraintFact("constraint-id", "Critical risk CVSS score", "OR");
    criticalConstraintFact1.addConditionFact(createSecuritySeverityConditionFact("CVE-2016-1000031", ">= 9", "9.8"));
    criticalComponentFact.addConstraintFact(criticalConstraintFact1);
    criticalPolicyNotification.getPolicyFact().addComponentFact(criticalComponentFact);

    // introduce a second fact for the same component, with differing CVE
    ComponentFact criticalComponentFact2 =
        new ComponentFact(criticalComponentFact.getComponentIdentifier(), criticalComponentFact.getHash());
    criticalComponentFact2.addConstraintFact(new ConstraintFact(criticalConstraintFact1.getConstraintId(),
        criticalConstraintFact1.getConstraintName(), criticalConstraintFact1.getOperatorName(),
        createSecuritySeverityConditionFact("CVE-2016-1000032", ">= 9", "9.8")));
    criticalPolicyNotification.getPolicyFact().addComponentFact(criticalComponentFact2);

    // introduce a different Component violating the same policy, should be omitted from output
    ComponentFact criticatComponentFact2 = new ComponentFact(componentIdentifier2, "dummy-hash");
    ConstraintFact criticalConstraintFact2 =
        new ConstraintFact("constraint-id", "Another Critical risk CVSS score", "OR");
    criticalConstraintFact2.addConditionFact(createSecuritySeverityConditionFact("SONATYPE-2019-01", ">= 9", "9.8"));
    criticatComponentFact2.addConstraintFact(criticalConstraintFact2);
    criticalPolicyNotification.getPolicyFact().addComponentFact(criticatComponentFact2);

    policyNotifications.add(criticalPolicyNotification);

    // high - two security severity constraint violations, each with two conditions, plus an 'age and popularity'
    // constraint with two conditions
    PolicyNotification highPolicyNotification = new PolicyNotification(
        new PolicyFact("high-id", "Security-High", 9),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact highComponentFact = new ComponentFact(componentIdentifier, "dummy-high-hash");
    ConstraintFact highConstraintFact = new ConstraintFact("constraint-id-high", "High risk CVSS score", "OR");
    highConstraintFact.addConditionFact(createSecuritySeverityConditionFact("SONATYPE-2017-0312", ">= 7", "8.5"));
    highConstraintFact.addConditionFact(createSecuritySeverityConditionFact("SONATYPE-2017-0312", "< 9", "8.5"));
    highComponentFact.addConstraintFact(highConstraintFact);
    highPolicyNotification.getPolicyFact().addComponentFact(highComponentFact);

    ComponentFact highComponentFact2 = new ComponentFact(componentIdentifier, "dummy-high-hash");
    ConstraintFact highConstraintFact2 = new ConstraintFact("constraint-id-high", "High risk CVSS score", "OR");
    highConstraintFact2.addConditionFact(createSecuritySeverityConditionFact("SONATYPE-2017-9999", ">= 7", "8.5"));
    highConstraintFact2.addConditionFact(createSecuritySeverityConditionFact("SONATYPE-2017-9999", "< 9", "8.5"));
    // add another non-security ConstraintFact to this ComponentFact
    highConstraintFact2.addConditionFact(createLabelConditionFact("foo"));
    highComponentFact2.addConstraintFact(highConstraintFact2);
    highPolicyNotification.getPolicyFact().addComponentFact(highComponentFact2);

    // third ComponentFact on high, not a security vulnerability
    ComponentFact highComponentFact3 = new ComponentFact(componentIdentifier, "dummy-high-hash");
    ConstraintFact highConstraintFact3 = new ConstraintFact("constraint-id-old", "Version is old and unpopular", "OR");
    highConstraintFact3.addConditionFact(createAgeConditionFact("6 years, 5 months, and 2 days"));
    highConstraintFact3.addConditionFact(createPopularityConditionFact("<3%"));
    highComponentFact3.addConstraintFact(highConstraintFact3);
    highPolicyNotification.getPolicyFact().addComponentFact(highComponentFact3);

    policyNotifications.add(highPolicyNotification);

    // medium - one constraint violation with two security severity conditions
    PolicyNotification mediumPolicyNotification = new PolicyNotification(
        new PolicyFact("medium-id", "Security-Medium", 7),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact mediumComponentFact = new ComponentFact(componentIdentifier, "dummy-hash");
    ConstraintFact mediumConstraintFact = new ConstraintFact("med-constraint-id", "Medium risk CVSS score", "OR");
    mediumConstraintFact.addConditionFact(createSecuritySeverityConditionFact("CVE-2018-10237", "< 7", "5.9"));
    mediumConstraintFact.addConditionFact(createSecuritySeverityConditionFact("CVE-2018-10237", ">= 4", "5.9"));
    // add a security status ConstraintFact with the same CVE to this ComponentFact, this should get rolled into one
    mediumConstraintFact
        .addConditionFact(createSecurityStatusConditionFact("CVE-2018-10237", "Open", "Not Applicable"));
    mediumComponentFact.addConstraintFact(mediumConstraintFact);
    mediumPolicyNotification.getPolicyFact().addComponentFact(mediumComponentFact);
    policyNotifications.add(mediumPolicyNotification);

    // low - one constraint violation with one security STATUS condition (rendered the same as severity)
    PolicyNotification lowPolicyNotification = new PolicyNotification(
        new PolicyFact("low-id", "Security-Low", 4),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact lowComponentFact = new ComponentFact(componentIdentifier, "dummy-low-hash");
    ConstraintFact lowConstraintFact = new ConstraintFact("constraint-id-low", "Low risk CVSS score", "OR");
    lowConstraintFact
        .addConditionFact(createSecurityStatusConditionFact("sonatype-2017-1234", "Open", "Not Applicable"));
    lowComponentFact.addConstraintFact(lowConstraintFact);
    lowPolicyNotification.getPolicyFact().addComponentFact(lowComponentFact);
    policyNotifications.add(lowPolicyNotification);

    return policyNotifications;
  }

  private ConditionFact createSecuritySeverityConditionFact(
      final String cve,
      final String summary,
      final String severity)
  {
    return new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0,
        String.format("Security Vulnerability Severity %s", summary),
        String.format("Found security vulnerability %s with severity >= 9 (severity = %s)", cve, severity),
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, cve));
  }

  private ConditionFact createSecurityStatusConditionFact(
      final String cve,
      final String status1,
      final String status2)
  {
    return new ConditionFact(SecurityVulnerabilityStatusConditionType.ID, 0,
        String.format("Security Vulnerability Status %s", status1),
        String.format("Found security vulnerability %s with status '%s', not '%s'", cve, status1, status2),
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, cve));
  }

  private ConditionFact createAgeConditionFact(final String age) {
    return new ConditionFact(AgeInDaysConditionType.ID, 0, "Age in days", String.format("Age was %s", age));
  }

  private ConditionFact createPopularityConditionFact(final String popularity) {
    return new ConditionFact(RelativePopularityConditionType.ID, 0, "Relative popularity",
        String.format("Relative popularity was %s", popularity));
  }

  private ConditionFact createLabelConditionFact(final String label) {
    return new ConditionFact(LabelConditionType.ID, 0,
        String.format("Label is '%s'", label),
        String.format("Found label '%s'", label));
  }
}
