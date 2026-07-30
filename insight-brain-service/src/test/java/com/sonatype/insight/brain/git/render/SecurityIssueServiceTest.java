/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackHelper.TEST_COMPONENT_IDENTIFIER;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackHelper.generateConditionFact;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackHelper.generateConstraintFact;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackHelper.generatePV;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackHelper.generatePVWithManyConditionFacts;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackHelper.generateSecurityVulnerabilityData;
import static com.sonatype.insight.brain.git.render.model.MDImages.SONATYPE_DEEP_DIVE_TAG;
import static com.sonatype.insight.brain.git.render.model.MDImages.SONATYPE_FAST_TRACK_TAG;
import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.DEEP_DIVE;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.FAST_TRACK;
import static com.sonatype.nexus.scm.SourceControlProvider.AZURE;
import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.service.VulnerabilityDetailsService;
import com.sonatype.insight.brain.git.render.model.SecurityIssue;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType;
import com.sonatype.nexus.scm.SourceControlProvider;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;

public class SecurityIssueServiceTest
    extends AbstractComponentTest
{
  private static final String PV_ID_1 = "pv1";

  private static final String PV_ID_2 = "pv2";

  private static final String PV_ID_3 = "pv3";

  private static final String REF_ID_1 = "CVE-123-123";

  private static final String DESCRIPTION = "some description";

  private static final float CVSS_SCORE_1 = 7.5f;

  private static final PolicyViolation PV_1 = generatePVWithManyConditionFacts(
      PV_ID_1, TEST_COMPONENT_IDENTIFIER, REF_ID_1);

  private static final String IQ_BASE_URL = "https://iq.example.com";

  @Inject
  private SecurityIssueService underTest;

  @Mock
  private VulnerabilityDetailsService vulnerabilityDetailsServiceMock;

  @Test
  public void testGetSecurityIssuesFromViolations_customCVSS() {
    final SecurityVulnerabilityData[] vulnerabilities = {
      generateSecurityVulnerabilityData("CVE-123-01", DESCRIPTION, 7.8f, 9.1f, null),
      generateSecurityVulnerabilityData("CVE-123-02", DESCRIPTION, 9.9f, 2.1f, null),
      generateSecurityVulnerabilityData("CVE-123-03", DESCRIPTION, null, 1.1f, null),
      generateSecurityVulnerabilityData("CVE-123-04", DESCRIPTION, null, null, null)
    };
    final String[] refIds = stream(vulnerabilities).map(c -> c.identifier).toArray(String[]::new);
    final PolicyViolation pv1 = generatePVWithManyConditionFacts(PV_ID_1, TEST_COMPONENT_IDENTIFIER, 5, refIds);

    setupVulnerabilityService(pv1, vulnerabilities);

    final List<SecurityIssue> actualSecurityIssues = underTest.getSecurityIssuesFromViolations(IQ_BASE_URL,
        ImmutableList.of(pv1), GITHUB);

    // Then expect security issues
    assertThat(actualSecurityIssues).hasSize(vulnerabilities.length);

    assertThat(actualSecurityIssues.get(0).getSeverityInfo().getRefId()).isEqualTo(vulnerabilities[0].identifier);
    assertThat(actualSecurityIssues.get(0).getSeverityInfo().getCvssScore())
        .isEqualTo(vulnerabilities[0].customData.cvssSeverity);

    assertThat(actualSecurityIssues.get(1).getSeverityInfo().getRefId()).isEqualTo(vulnerabilities[1].identifier);
    assertThat(actualSecurityIssues.get(1).getSeverityInfo().getCvssScore())
        .isEqualTo(vulnerabilities[1].customData.cvssSeverity);

    assertThat(actualSecurityIssues.get(2).getSeverityInfo().getRefId()).isEqualTo(vulnerabilities[2].identifier);
    assertThat(actualSecurityIssues.get(2).getSeverityInfo().getCvssScore())
        .isEqualTo(vulnerabilities[2].customData.cvssSeverity);

    assertThat(actualSecurityIssues.get(3).getSeverityInfo().getRefId()).isEqualTo(vulnerabilities[3].identifier);
    assertThat(actualSecurityIssues.get(3).getSeverityInfo().getCvssScore()).isNull();
  }

  @Test
  public void testGetSecurityIssuesFromViolations_vulnAndPVSorting() {
    // Given a set of vulnerabilities
    final SecurityVulnerabilityData[] vulnerabilities = {
      generateSecurityVulnerabilityData("CVE-123-01", DESCRIPTION, 7.8f, null),
      generateSecurityVulnerabilityData("CVE-123-02", DESCRIPTION, 9.9f, null),
      generateSecurityVulnerabilityData("CVE-123-03", DESCRIPTION, 7.8f, null),
      generateSecurityVulnerabilityData("CVE-123-04", DESCRIPTION, 9.0f, null),
      generateSecurityVulnerabilityData("CVE-123-05", DESCRIPTION, 8.1f, null),
      generateSecurityVulnerabilityData("CVE-123-06", DESCRIPTION, 3.4f, null),
    };

    // And 3 PVs each with 2 Vulnerabilities
    final PolicyViolation pv1 = generatePVWithManyConditionFacts(
        PV_ID_1, TEST_COMPONENT_IDENTIFIER, 4, vulnerabilities[0].identifier, vulnerabilities[1].identifier);
    final PolicyViolation pv2 = generatePVWithManyConditionFacts(
        PV_ID_2, TEST_COMPONENT_IDENTIFIER, 8, vulnerabilities[2].identifier, vulnerabilities[3].identifier);
    final PolicyViolation pv3 = generatePVWithManyConditionFacts(
        PV_ID_3, TEST_COMPONENT_IDENTIFIER, 6, vulnerabilities[4].identifier, vulnerabilities[5].identifier);

    setupVulnerabilityService(pv1, vulnerabilities[0], vulnerabilities[1]);
    setupVulnerabilityService(pv2, vulnerabilities[2], vulnerabilities[3]);
    setupVulnerabilityService(pv3, vulnerabilities[4], vulnerabilities[5]);

    // When SecurityIssueService is called
    final List<SecurityIssue> actualSecurityIssues = underTest.getSecurityIssuesFromViolations(IQ_BASE_URL,
        ImmutableList.of(pv1, pv2, pv3), GITHUB);

    // Then expect security issues
    assertThat(actualSecurityIssues).hasSize(6);

    /**
     * Then expect the uniqueIssue ids for the actual result to match that
     * of the expected result. Since SecurityIssue does not have a
     * unique identifier (it doesnt need one),
     * we create them for the actual and expected results using the uniqueIssueId method
     */
    final List<String> actualIds = actualSecurityIssues
        .stream()
        .map(SecurityIssueServiceTest::uniqueIssueId)
        .collect(toImmutableList());

    final List<String> expectedIds = ImmutableList.of(
        uniqueIssueId(pv2, vulnerabilities[3]),
        uniqueIssueId(pv2, vulnerabilities[2]),
        uniqueIssueId(pv3, vulnerabilities[4]),
        uniqueIssueId(pv3, vulnerabilities[5]),
        uniqueIssueId(pv1, vulnerabilities[1]),
        uniqueIssueId(pv1, vulnerabilities[0]));
    assertThat(actualIds).isEqualTo(expectedIds);
  }

  @Test
  public void testGetSecurityIssuesFromViolations_exceptionThrownForVulnFetch() {
    // Given a PV with 1 vulnerability that does not have a CVSS score
    final List<PolicyViolation> violations = ImmutableList.of(PV_1);

    // And setup the VulnerabilityDetailsService to not find the referenceId
    setupVulnerabilityServiceException(
        REF_ID_1, PV_1.getComponentIdentifier(), PV_1.getOwnerId(), RuntimeException.class);

    // When SecurityIssueService is called
    final List<SecurityIssue> actualSecurityIssues = underTest.getSecurityIssuesFromViolations(
        IQ_BASE_URL, violations, GITHUB);

    // Then expected 1 Security issue
    assertThat(actualSecurityIssues).hasSize(1);
    final SecurityIssue actualSecurityIssue = actualSecurityIssues.get(0);

    // then expect the security issue to have a null cvss score
    assertThat(actualSecurityIssue.getThreatLevel()).isEqualTo(PV_1.getThreatLevel());
    assertThat(actualSecurityIssue.getSeverityInfo()).isNull();
    assertThat(actualSecurityIssue.getDescription()).isNull();
    assertThat(actualSecurityIssue.getPolicyViolationDetailsLink())
        .isEqualTo("https://iq.example.com/ui/links/policyViolationReport/pv1?utm_source=github");
  }

  @Test
  public void testGetSecurityIssuesFromViolations_withoutVulns() {
    // Given a PV with no Vulnerabilities
    final PolicyViolation pv = generatePV(PV_ID_1, TEST_COMPONENT_IDENTIFIER,
        generateConstraintFact(generateConditionFact(AgeInDaysConditionType.ID, "1")));

    // When SecurityIssueService is called
    final List<SecurityIssue> actualSecurityIssues = underTest.getSecurityIssuesFromViolations(IQ_BASE_URL,
        ImmutableList.of(pv), GITHUB);

    // Then expected 1 security issue
    assertThat(actualSecurityIssues).hasSize(1);
    final SecurityIssue actualSecurityIssue = actualSecurityIssues.get(0);

    // then assert the input PolicyViolation has ConstraintFacts
    assertThat(pv.getConstraintFacts()).isNotNull();

    // then expect the security issue to have a null vulnerability field and null description
    assertThat(actualSecurityIssue.getThreatLevel()).isEqualTo(pv.getThreatLevel());
    assertThat(actualSecurityIssue.getSeverityInfo()).isNull();
    assertThat(actualSecurityIssue.getDescription()).isNull();
    assertThat(actualSecurityIssue.getPolicyViolationDetailsLink())
        .isEqualTo("https://iq.example.com/ui/links/policyViolationReport/pv1?utm_source=github");

  }

  @Test
  public void testGetSecurityIssuesFromViolations_withoutCVSSScore() {
    // Given a PV with 1 vuln that does not have a CVSS score
    final List<PolicyViolation> violations = ImmutableList.of(PV_1);

    // And a SecurityVulnerabilityData for the vuln
    setupVulnerabilityService(PV_1, REF_ID_1, DESCRIPTION, 0f, DEEP_DIVE);

    // When SecurityIssueService is called
    final List<SecurityIssue> actualSecurityIssues = underTest.getSecurityIssuesFromViolations(
        IQ_BASE_URL, violations, GITHUB);

    // Then expected 1 Security issue
    assertThat(actualSecurityIssues).hasSize(1);
    final SecurityIssue actualSecurityIssue = actualSecurityIssues.get(0);

    // then expect the security issue to have a null cvss score
    assertThat(actualSecurityIssue.getThreatLevel()).isEqualTo(PV_1.getThreatLevel());
    assertThat(actualSecurityIssue.getSeverityInfo()).isNotNull();
    assertThat(actualSecurityIssue.getSeverityInfo().getRefId()).isEqualTo(REF_ID_1);
    assertThat(actualSecurityIssue.getSeverityInfo().getCvssScore()).isNull();
    assertThat(actualSecurityIssue.getSeverityInfo().getVerificationImage()).isEqualTo(SONATYPE_DEEP_DIVE_TAG);
    assertThat(actualSecurityIssue.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(actualSecurityIssue.getPolicyViolationDetailsLink())
        .isEqualTo("https://iq.example.com/ui/links/policyViolationReport/pv1?utm_source=github");
  }

  @Test
  public void testGetSecurityIssuesFromViolations_withNormalVuln() {
    // Given a PV with 1 normal vuln
    final List<PolicyViolation> violations = ImmutableList.of(PV_1);

    // And a SecurityVulnerabilityData for the vuln
    setupVulnerabilityService(PV_1, REF_ID_1, DESCRIPTION, CVSS_SCORE_1, FAST_TRACK);

    // When SecurityIssueService is called
    final List<SecurityIssue> actualSecurityIssues = underTest.getSecurityIssuesFromViolations(
        IQ_BASE_URL, violations, GITHUB);

    // Then expected 1 Security issue
    assertThat(actualSecurityIssues).hasSize(1);
    final SecurityIssue actualSecurityIssue = actualSecurityIssues.get(0);

    // then expect the security issue to have all fields populated
    assertThat(actualSecurityIssue.getThreatLevel()).isEqualTo(PV_1.getThreatLevel());
    assertThat(actualSecurityIssue.getSeverityInfo()).isNotNull();
    assertThat(actualSecurityIssue.getSeverityInfo().getRefId()).isEqualTo(REF_ID_1);
    assertThat(actualSecurityIssue.getSeverityInfo().getCvssScore()).isEqualTo(CVSS_SCORE_1);
    assertThat(actualSecurityIssue.getSeverityInfo().getVerificationImage()).isEqualTo(SONATYPE_FAST_TRACK_TAG);
    assertThat(actualSecurityIssue.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(actualSecurityIssue.getPolicyViolationDetailsLink())
        .isEqualTo("https://iq.example.com/ui/links/policyViolationReport/pv1?utm_source=github");
  }

  @Test
  public void testGetSecurityIssuesFromViolations_withoutDescription() {
    // Given a PV with 1 vuln
    final List<PolicyViolation> violations = ImmutableList.of(PV_1);

    // And mock the vulnerabilityService to return a vuln without a description
    setupVulnerabilityService(PV_1, REF_ID_1, null, CVSS_SCORE_1, FAST_TRACK);

    // When SecurityIssueService is called
    final List<SecurityIssue> actualSecurityIssues = underTest.getSecurityIssuesFromViolations(
        IQ_BASE_URL, violations, GITHUB);

    // Then expected 1 Security issue
    assertThat(actualSecurityIssues).hasSize(1);
    final SecurityIssue actualSecurityIssue = actualSecurityIssues.get(0);

    // then expect the security issue to have a null description
    assertThat(actualSecurityIssue.getThreatLevel()).isEqualTo(PV_1.getThreatLevel());
    assertThat(actualSecurityIssue.getSeverityInfo()).isNotNull();
    assertThat(actualSecurityIssue.getSeverityInfo().getRefId()).isEqualTo(REF_ID_1);
    assertThat(actualSecurityIssue.getSeverityInfo().getCvssScore()).isEqualTo(CVSS_SCORE_1);
    assertThat(actualSecurityIssue.getSeverityInfo().getVerificationImage()).isEqualTo(SONATYPE_FAST_TRACK_TAG);
    assertThat(actualSecurityIssue.getDescription()).isNull();
    assertThat(actualSecurityIssue.getPolicyViolationDetailsLink())
        .isEqualTo("https://iq.example.com/ui/links/policyViolationReport/pv1?utm_source=github");
  }

  @Test
  public void testGetSecurityIssuesFromViolations_withoutResearchType() {
    // Given a PV with 1 vuln
    final List<PolicyViolation> violations = ImmutableList.of(PV_1);

    // And mock the vulnerabilityService to return a vuln without a research type
    setupVulnerabilityService(PV_1, REF_ID_1, DESCRIPTION, CVSS_SCORE_1, null);

    // When SecurityIssueService is called
    final List<SecurityIssue> actualSecurityIssues = underTest.getSecurityIssuesFromViolations(
        IQ_BASE_URL, violations, GITHUB);

    // Then expected 1 Security issue
    assertThat(actualSecurityIssues).hasSize(1);
    final SecurityIssue actualSecurityIssue = actualSecurityIssues.get(0);

    // then assert the security issue has a null vuln verification image
    assertThat(actualSecurityIssue.getThreatLevel()).isEqualTo(PV_1.getThreatLevel());
    assertThat(actualSecurityIssue.getSeverityInfo()).isNotNull();
    assertThat(actualSecurityIssue.getSeverityInfo().getRefId()).isEqualTo(REF_ID_1);
    assertThat(actualSecurityIssue.getSeverityInfo().getCvssScore()).isEqualTo(CVSS_SCORE_1);
    assertThat(actualSecurityIssue.getSeverityInfo().getVerificationImage()).isNull();
    assertThat(actualSecurityIssue.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(actualSecurityIssue.getPolicyViolationDetailsLink())
        .isEqualTo("https://iq.example.com/ui/links/policyViolationReport/pv1?utm_source=github");
  }

  @Test
  public void testGetSecurityIssuesFromViolations_2PVsWithSameVuln() {
    // Given a PV with 1 vuln
    final PolicyViolation pv1 = generatePVWithManyConditionFacts(PV_ID_1, TEST_COMPONENT_IDENTIFIER, 5, REF_ID_1);
    final PolicyViolation pv2 = generatePVWithManyConditionFacts(PV_ID_2, TEST_COMPONENT_IDENTIFIER, 9, REF_ID_1);

    // And mock the vulnerabilityService to return a normal vuln
    setupVulnerabilityService(pv1, REF_ID_1, DESCRIPTION, CVSS_SCORE_1, FAST_TRACK);

    // When SecurityIssueService is called
    final List<SecurityIssue> actualSecurityIssues = underTest.getSecurityIssuesFromViolations(IQ_BASE_URL,
        ImmutableList.of(pv1, pv2), GITHUB);

    // Then expect 2 security issues
    assertThat(actualSecurityIssues).hasSize(2);
    final SecurityIssue actual1 = actualSecurityIssues.get(0);
    final SecurityIssue actual2 = actualSecurityIssues.get(1);

    // Then expect the first security issue to be pv2
    assertThat(actual1.getThreatLevel()).isEqualTo(pv2.getThreatLevel());
    assertThat(actual1.getSeverityInfo()).isNotNull();
    assertThat(actual1.getSeverityInfo().getRefId()).isEqualTo(REF_ID_1);
    assertThat(actual1.getSeverityInfo().getCvssScore()).isEqualTo(CVSS_SCORE_1);
    assertThat(actual1.getSeverityInfo().getVerificationImage()).isEqualTo(SONATYPE_FAST_TRACK_TAG);
    assertThat(actual1.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(actual1.getPolicyViolationDetailsLink())
        .isEqualTo("https://iq.example.com/ui/links/policyViolationReport/pv2?utm_source=github");

    // Then expect the second security issue to be pv1
    assertThat(actual2.getThreatLevel()).isEqualTo(pv1.getThreatLevel());
    assertThat(actual2.getSeverityInfo()).usingRecursiveComparison().isEqualTo(actual1.getSeverityInfo());
    assertThat(actual2.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(actual2.getPolicyViolationDetailsLink())
        .isEqualTo("https://iq.example.com/ui/links/policyViolationReport/pv1?utm_source=github");
  }

  @Test
  public void testGetSecurityIssuesFromViolations_withUTMSource_github() {
    runUtmSourceTest(GITHUB, "https://iq.example.com/ui/links/policyViolationReport/pv1?utm_source=github");
  }

  @Test
  public void testGetSecurityIssuesFromViolations_withUTMSource_gitlab() {
    runUtmSourceTest(GITLAB, "https://iq.example.com/ui/links/policyViolationReport/pv1?utm_source=gitlab");
  }

  @Test
  public void testGetSecurityIssuesFromViolations_withoutUTMSource_azure() {
    runUtmSourceTest(AZURE, "https://iq.example.com/ui/links/policyViolationReport/pv1");
  }

  @Test
  public void testGetSecurityIssuesFromViolations_withoutUTMSource_bitbucket() {
    runUtmSourceTest(BITBUCKET, "https://iq.example.com/ui/links/policyViolationReport/pv1");
  }

  public void runUtmSourceTest(final SourceControlProvider provider, final String expectedDetailsLink) {
    // Given a PV with 1 normal vuln
    final List<PolicyViolation> violations = ImmutableList.of(PV_1);

    // And a SecurityVulnerabilityData for the vuln
    setupVulnerabilityService(PV_1, REF_ID_1, DESCRIPTION, CVSS_SCORE_1, FAST_TRACK);

    // When SecurityIssueService is called
    final List<SecurityIssue> actualSecurityIssues = underTest.getSecurityIssuesFromViolations(
        IQ_BASE_URL, violations, provider);

    // Then expected 1 Security issue
    assertThat(actualSecurityIssues).hasSize(1);
    final SecurityIssue actualSecurityIssue = actualSecurityIssues.get(0);

    // then expect the security issue to have all fields populated
    assertThat(actualSecurityIssue.getThreatLevel()).isEqualTo(PV_1.getThreatLevel());
    assertThat(actualSecurityIssue.getSeverityInfo()).isNotNull();
    assertThat(actualSecurityIssue.getSeverityInfo().getRefId()).isEqualTo(REF_ID_1);
    assertThat(actualSecurityIssue.getSeverityInfo().getCvssScore()).isEqualTo(CVSS_SCORE_1);
    assertThat(actualSecurityIssue.getSeverityInfo().getVerificationImage()).isEqualTo(SONATYPE_FAST_TRACK_TAG);
    assertThat(actualSecurityIssue.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(actualSecurityIssue.getPolicyViolationDetailsLink()).isEqualTo(expectedDetailsLink);
    // assertThat(actualSecurityIssue.getPolicyViolationDetailsLink())
    // .isEqualTo("https://iq.example.com/assets/index.html#/violation/" +
    // "pv1?type=violation&sidebarReference=filter&utm_source=github");
  }

  private void setupVulnerabilityService(
      final PolicyViolation pv,
      final String refId,
      final String description,
      final float cvssCcore,
      final SecurityVulnerabilityResearchType researchType)
  {
    final SecurityVulnerabilityData securityVulnerabilityData = generateSecurityVulnerabilityData(
        refId,
        description,
        cvssCcore,
        researchType);
    setupVulnerabilityService(pv, securityVulnerabilityData);
  }

  private void setupVulnerabilityServiceException(
      final String refId,
      final ComponentIdentifier componentIdentifier,
      final String applicationId,
      final Class<? extends Throwable> throwableClazz)
  {
    when(
        vulnerabilityDetailsServiceMock.getSecurityVulnerabilityDetails(
            refId,
            componentIdentifier,
            null,
            null,
            APPLICATION,
            applicationId,
            true)).thenThrow(throwableClazz);
  }

  private void setupVulnerabilityService(final PolicyViolation pv, final SecurityVulnerabilityData... details) {
    stream(details)
        .forEach(vulnData -> when(
            vulnerabilityDetailsServiceMock.getSecurityVulnerabilityDetails(
                vulnData.identifier,
                pv.getComponentIdentifier(),
                null,
                null,
                APPLICATION,
                pv.getOwnerId(),
                true)).thenReturn(vulnData));
  }

  private static String uniqueIssueId(final SecurityIssue issue) {
    return uniqueIssueId(
        issue.getThreatLevel(),
        issue.getSeverityInfo().getCvssScore(),
        issue.getSeverityInfo().getRefId());
  }

  private static String uniqueIssueId(final PolicyViolation pv, final SecurityVulnerabilityData data) {
    return uniqueIssueId(pv.getThreatLevel(), data.mainSeverity.score, data.identifier);
  }

  private static String uniqueIssueId(final int threatLevel, final float cvssScore, final String refId) {
    return threatLevel + "_" + "_" + cvssScore + refId;
  }
}
