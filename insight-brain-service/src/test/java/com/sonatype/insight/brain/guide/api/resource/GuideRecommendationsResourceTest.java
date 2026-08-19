/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.guide.api.dto.RecommendationResponse;
import com.sonatype.guide.api.request.RecommendationRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideRecommendationResult;
import com.sonatype.insight.brain.guide.api.dto.RecommendedVersionInfo;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceLevel;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceSummary;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.api.error.GuideNotFoundException;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.guide.policy.GuidePolicyEvaluator;
import com.sonatype.insight.brain.guide.policy.GuidePolicyService;
import com.sonatype.insight.brain.security.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GuideRecommendationsResourceTest
{
  private static final String PURL = "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1";

  private static final String CANDIDATE_PURL = "pkg:maven/org.apache.logging.log4j/log4j-core@2.21.1?type=jar";

  @Mock
  private SearchApiClient searchApiClient;

  @Mock
  private GuidePolicyEvaluator guidePolicyEvaluator;

  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private OwnerDAO ownerDAO;

  @Mock
  private PermissionService permissionService;

  private GuideRecommendationsResource underTest;

  @BeforeEach
  public void setUp() {
    underTest = new GuideRecommendationsResource(
        searchApiClient,
        new GuidePolicyService(guidePolicyEvaluator, applicationDAO, ownerDAO, permissionService));
  }

  @Test
  public void getRecommendations_returnsBadRequest_whenPurlIsNull() {
    RecommendationRequest request = new RecommendationRequest(null, null, null);

    assertThatThrownBy(() -> underTest.getRecommendations(request))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Purl is required");
    verifyNoInteractions(searchApiClient);
    verifyNoInteractions(guidePolicyEvaluator);
  }

  @Test
  public void getRecommendations_returnsBadRequest_whenPurlIsBlank() {
    RecommendationRequest request = new RecommendationRequest("   ", null, null);

    assertThatThrownBy(() -> underTest.getRecommendations(request))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Purl is required");
    verifyNoInteractions(searchApiClient);
    verifyNoInteractions(guidePolicyEvaluator);
  }

  @Test
  public void getRecommendations_propagatesNotFoundFromClient_withUpstreamMessage() throws Exception {
    RecommendationRequest request = new RecommendationRequest(PURL, null, null);
    when(searchApiClient.getRecommendations(PURL, null, null))
        .thenThrow(new GuideNotFoundException("Recommendations not found for PURL: " + PURL));

    assertThatThrownBy(() -> underTest.getRecommendations(request))
        .isInstanceOf(GuideNotFoundException.class)
        .hasMessage("Recommendations not found for PURL: " + PURL);
    verifyNoInteractions(guidePolicyEvaluator);
  }

  @Test
  public void getRecommendations_forwardsExtensionAndClassifier() throws Exception {
    GuideRecommendationResult upstream = new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        version("2.14.1"),
        List.of(version("2.21.1")));
    when(searchApiClient.getRecommendations(PURL, "war", "sources")).thenReturn(upstream);
    when(guidePolicyEvaluator.evaluate(anyList())).thenReturn(Map.of());

    underTest.getRecommendations(new RecommendationRequest(PURL, "war", "sources"));

    verify(searchApiClient).getRecommendations(PURL, "war", "sources");
  }

  // --- GUIDE-3174: normalization of extension/classifier at API boundary

  @Test
  public void getRecommendations_blankExtension_normalizedToNull() throws Exception {
    GuideRecommendationResult upstream = new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        version("2.14.1"),
        List.of(version("2.21.1")));
    when(searchApiClient.getRecommendations(PURL, null, null)).thenReturn(upstream);
    when(guidePolicyEvaluator.evaluate(anyList())).thenReturn(Map.of());

    underTest.getRecommendations(new RecommendationRequest(PURL, "  ", null));

    verify(searchApiClient).getRecommendations(PURL, null, null);
  }

  @Test
  public void getRecommendations_emptyExtension_normalizedToNull() throws Exception {
    GuideRecommendationResult upstream = new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        version("2.14.1"),
        List.of(version("2.21.1")));
    when(searchApiClient.getRecommendations(PURL, null, null)).thenReturn(upstream);
    when(guidePolicyEvaluator.evaluate(anyList())).thenReturn(Map.of());

    underTest.getRecommendations(new RecommendationRequest(PURL, "", null));

    verify(searchApiClient).getRecommendations(PURL, null, null);
  }

  @Test
  public void getRecommendations_whitespaceExtension_trimmed() throws Exception {
    GuideRecommendationResult upstream = new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        version("2.14.1"),
        List.of(version("2.21.1")));
    when(searchApiClient.getRecommendations(PURL, "jar", null)).thenReturn(upstream);
    when(guidePolicyEvaluator.evaluate(anyList())).thenReturn(Map.of());

    underTest.getRecommendations(new RecommendationRequest(PURL, " jar ", null));

    verify(searchApiClient).getRecommendations(PURL, "jar", null);
  }

  @Test
  public void getRecommendations_extensionWithNullClassifier() throws Exception {
    GuideRecommendationResult upstream = new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        version("2.14.1"),
        List.of(version("2.21.1")));
    when(searchApiClient.getRecommendations(PURL, "war", null)).thenReturn(upstream);
    when(guidePolicyEvaluator.evaluate(anyList())).thenReturn(Map.of());

    underTest.getRecommendations(new RecommendationRequest(PURL, "war", null));

    verify(searchApiClient).getRecommendations(PURL, "war", null);
  }

  @Test
  public void getRecommendations_filtersOutNonCompliantCandidates() throws Exception {
    GuideRecommendationResult upstream = new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        version("2.14.1"),
        List.of(version("2.21.0"), version("2.21.1")));
    when(searchApiClient.getRecommendations(PURL, null, null)).thenReturn(upstream);
    GuidePolicyCompliance compliant = compliantOf();
    when(guidePolicyEvaluator.evaluate(anyList())).thenReturn(Map.of(
        "pkg:maven/org.apache.logging.log4j/log4j-core@2.21.0?type=jar", nonCompliantOf(),
        CANDIDATE_PURL, compliant));

    RecommendationResponse result = underTest.getRecommendations(new RecommendationRequest(PURL, null, null));

    assertThat(result.toVersions()).hasSize(1);
    assertThat(((RecommendedVersionInfo) result.toVersions().get(0)).version()).isEqualTo("2.21.1");
    // Recommendation candidates are a list surface → slim compliance (flag only).
    GuidePolicyCompliance attached =
        ((RecommendedVersionInfo) result.toVersions().get(0)).policyCompliance();
    assertThat(attached).isNotNull();
    assertThat(attached.compliant()).isTrue();
    assertThat(attached.summary()).isNull();
    assertThat(attached.violations()).isNull();
    assertThat(result.outcome()).isEqualTo(RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS);
  }

  @Test
  public void getRecommendations_emptyAfterFilter_returnsBlockedByPolicy() throws Exception {
    GuideRecommendationResult upstream = new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        version("2.14.1"),
        List.of(version("2.21.1")));
    when(searchApiClient.getRecommendations(PURL, null, null)).thenReturn(upstream);
    when(guidePolicyEvaluator.evaluate(anyList())).thenReturn(Map.of(CANDIDATE_PURL, nonCompliantOf()));

    RecommendationResponse result = underTest.getRecommendations(new RecommendationRequest(PURL, null, null));

    assertThat(result.outcome()).isEqualTo(RecommendationResponse.Outcome.BLOCKED_BY_POLICY);
    assertThat(result.toVersions()).isEmpty();
  }

  @Test
  public void getRecommendations_noEvaluationData_keepsCandidatesTreatedCompliant() throws Exception {
    GuideRecommendationResult upstream = new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        version("2.14.1"),
        List.of(version("2.21.1")));
    when(searchApiClient.getRecommendations(PURL, null, null)).thenReturn(upstream);
    when(guidePolicyEvaluator.evaluate(anyList())).thenReturn(Map.of());

    RecommendationResponse result = underTest.getRecommendations(new RecommendationRequest(PURL, null, null));

    assertThat(result.toVersions()).hasSize(1);
    RecommendedVersionInfo v = (RecommendedVersionInfo) result.toVersions().get(0);
    assertThat(v.version()).isEqualTo("2.21.1");
    // No evaluation data for the candidate → treated as compliant (SaaS not-found rule), kept with
    // slim compliance — not returned with null policyCompliance.
    assertThat(v.policyCompliance()).isNotNull();
    assertThat(v.policyCompliance().compliant()).isTrue();
    assertThat(v.policyCompliance().summary()).isNull();
    assertThat(result.outcome()).isEqualTo(RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS);
    verify(searchApiClient).getRecommendations(PURL, null, null);
  }

  private static RecommendedVersionInfo version(String v) {
    return new RecommendedVersionInfo(v, null, null, null, null, null, null, null, null);
  }

  private static GuidePolicyCompliance compliantOf() {
    Map<String, Integer> counts = new LinkedHashMap<>();
    counts.put("SECURITY", 0);
    counts.put("LICENSE", 0);
    counts.put("QUALITY", 0);
    counts.put("OTHER", 0);
    return new GuidePolicyCompliance(true, GuidePolicyComplianceLevel.PASS, "release", "ROOT_ORGANIZATION_ID",
        new GuidePolicyComplianceSummary(0, "none", 0, 0, counts), List.of());
  }

  private static GuidePolicyCompliance nonCompliantOf() {
    Map<String, Integer> counts = new LinkedHashMap<>();
    counts.put("SECURITY", 1);
    counts.put("LICENSE", 0);
    counts.put("QUALITY", 0);
    counts.put("OTHER", 0);
    return new GuidePolicyCompliance(false, GuidePolicyComplianceLevel.FAIL, "release", "ROOT_ORGANIZATION_ID",
        new GuidePolicyComplianceSummary(8, "fail", 1, 0, counts), List.of());
  }
}
