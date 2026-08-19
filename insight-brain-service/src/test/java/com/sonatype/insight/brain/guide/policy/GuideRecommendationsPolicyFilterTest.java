/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.policy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.guide.api.dto.RecommendationResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideRecommendationResult;
import com.sonatype.insight.brain.guide.api.dto.RecommendedVersionInfo;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceLevel;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceSummary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GuideRecommendationsPolicyFilterTest
{
  @Test
  public void allCandidatesCompliant_keepsAllAndAnnotates() {
    GuideRecommendationResult upstream = result(
        version("1.1"), version("1.2"), version("2.0"));
    Map<String, String> purlByVersion = purlsFor("1.1", "1.2", "2.0");
    Map<String, GuidePolicyCompliance> compliance = Map.of(
        purl("1.1"), compliantOf(),
        purl("1.2"), compliantOf(),
        purl("2.0"), compliantOf());

    GuideRecommendationResult filtered =
        GuideRecommendationsPolicyFilter.apply(upstream, purlByVersion, compliance);

    assertThat(filtered.toVersions()).hasSize(3);
    // Recommendation candidates are a list surface → slim compliance (flag only, no summary).
    assertThat(filtered.toVersions()).allMatch(
        v -> v.policyCompliance() != null
            && v.policyCompliance().compliant()
            && v.policyCompliance().summary() == null);
    assertThat(filtered.outcome()).isEqualTo(RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS);
  }

  @Test
  public void mixedCompliance_dropsNonCompliantAndAnnotatesSurvivors() {
    GuideRecommendationResult upstream = result(
        version("1.1"), version("1.2"), version("2.0"));
    Map<String, String> purlByVersion = purlsFor("1.1", "1.2", "2.0");
    Map<String, GuidePolicyCompliance> compliance = Map.of(
        purl("1.1"), nonCompliantOf(),
        purl("1.2"), compliantOf(),
        purl("2.0"), compliantOf());

    GuideRecommendationResult filtered =
        GuideRecommendationsPolicyFilter.apply(upstream, purlByVersion, compliance);

    assertThat(filtered.toVersions()).extracting(RecommendedVersionInfo::version)
        .containsExactly("1.2", "2.0");
    // Survivors carry slim compliance (compliant flag only), not the full evaluated object.
    assertThat(filtered.toVersions()).allMatch(
        v -> v.policyCompliance() != null
            && v.policyCompliance().compliant()
            && v.policyCompliance().summary() == null);
    assertThat(filtered.outcome()).isEqualTo(RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS);
  }

  @Test
  public void allCandidatesNonCompliant_returnsBlockedByPolicy() {
    GuideRecommendationResult upstream = result(version("1.1"));
    Map<String, GuidePolicyCompliance> compliance = Map.of(purl("1.1"), nonCompliantOf());

    GuideRecommendationResult filtered =
        GuideRecommendationsPolicyFilter.apply(upstream, purlsFor("1.1"), compliance);

    assertThat(filtered.toVersions()).isEmpty();
    assertThat(filtered.outcome()).isEqualTo(RecommendationResponse.Outcome.BLOCKED_BY_POLICY);
  }

  @Test
  public void candidateMissingFromComplianceMap_keptAndTreatedCompliant() {
    // Both versions have a PURL in purlByVersion, so both are policy-checkable. 1.1 has compliance
    // data (compliant); 1.2 has none (e.g. a brand-new release). SaaS treats the no-data version as
    // compliant and keeps it, rather than failing the whole set open.
    GuideRecommendationResult upstream = result(version("1.1"), version("1.2"));
    Map<String, GuidePolicyCompliance> compliance = Map.of(purl("1.1"), compliantOf());

    GuideRecommendationResult filtered =
        GuideRecommendationsPolicyFilter.apply(upstream, purlsFor("1.1", "1.2"), compliance);

    assertThat(filtered.toVersions()).extracting(RecommendedVersionInfo::version)
        .containsExactly("1.1", "1.2");
    assertThat(filtered.toVersions()).allMatch(
        v -> v.policyCompliance() != null
            && v.policyCompliance().compliant()
            && v.policyCompliance().summary() == null);
    assertThat(filtered.outcome()).isEqualTo(RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS);
  }

  @Test
  public void nonCompliantDroppedEvenWhenAnotherCandidateHasNoData() {
    // The fix: a single no-data candidate no longer disables filtering for the whole set. 1.1 is
    // evaluated non-compliant (dropped); 1.2 compliant (kept); 2.0 has no compliance data (kept,
    // compliant).
    GuideRecommendationResult upstream = result(version("1.1"), version("1.2"), version("2.0"));
    Map<String, GuidePolicyCompliance> compliance = Map.of(
        purl("1.1"), nonCompliantOf(),
        purl("1.2"), compliantOf());

    GuideRecommendationResult filtered =
        GuideRecommendationsPolicyFilter.apply(upstream, purlsFor("1.1", "1.2", "2.0"), compliance);

    assertThat(filtered.toVersions()).extracting(RecommendedVersionInfo::version)
        .containsExactly("1.2", "2.0");
    assertThat(filtered.outcome()).isEqualTo(RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS);
  }

  @Test
  public void emptyComplianceMap_keepsCandidatesTreatedCompliant() {
    // No compliance data for any candidate → all treated as compliant and kept (SaaS not-found rule),
    // rather than returned unfiltered with null compliance.
    GuideRecommendationResult upstream = result(version("1.1"));

    GuideRecommendationResult filtered =
        GuideRecommendationsPolicyFilter.apply(upstream, purlsFor("1.1"), Map.of());

    assertThat(filtered.toVersions()).hasSize(1);
    GuidePolicyCompliance pc = filtered.toVersions().get(0).policyCompliance();
    assertThat(pc).isNotNull();
    assertThat(pc.compliant()).isTrue();
    assertThat(pc.summary()).isNull();
    assertThat(filtered.outcome()).isEqualTo(RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS);
  }

  @Test
  public void versionAbsentFromPurlMap_isDropped() {
    // The facade omits un-buildable candidates (e.g. a blank version) from purlByVersion. A candidate
    // with no PURL is not policy-checkable and is dropped, while candidates that do have a PURL are
    // evaluated normally.
    GuideRecommendationResult upstream = result(version("1.1"), version("1.2"));
    Map<String, GuidePolicyCompliance> compliance = Map.of(purl("1.1"), compliantOf());

    // 1.2 is intentionally absent from purlByVersion.
    GuideRecommendationResult filtered =
        GuideRecommendationsPolicyFilter.apply(upstream, purlsFor("1.1"), compliance);

    assertThat(filtered.toVersions()).extracting(RecommendedVersionInfo::version)
        .containsExactly("1.1");
    assertThat(filtered.outcome()).isEqualTo(RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS);
  }

  @Test
  public void emptyPurlMap_dropsAllAndReturnsBlockedByPolicy() {
    // An unparseable parent PURL makes the facade pass an empty purlByVersion; every candidate is then
    // un-checkable and dropped, yielding BLOCKED_BY_POLICY (what SaaS returns when nothing survives).
    GuideRecommendationResult upstream = result(version("1.1"), version("1.2"));

    GuideRecommendationResult filtered =
        GuideRecommendationsPolicyFilter.apply(upstream, Map.of(), Map.of());

    assertThat(filtered.toVersions()).isEmpty();
    assertThat(filtered.outcome()).isEqualTo(RecommendationResponse.Outcome.BLOCKED_BY_POLICY);
  }

  @Test
  public void emptyToVersions_returnsUpstreamUnchanged() {
    GuideRecommendationResult upstream = new GuideRecommendationResult(
        RecommendationResponse.Outcome.NO_UPGRADE_NEEDED, version("1.0"), List.of());

    GuideRecommendationResult filtered =
        GuideRecommendationsPolicyFilter.apply(upstream, Map.of(), Map.of());

    assertThat(filtered).isSameAs(upstream);
  }

  private static RecommendedVersionInfo version(String v) {
    return new RecommendedVersionInfo(v, null, null, null, null, null, null, null, null);
  }

  private static GuideRecommendationResult result(RecommendedVersionInfo... versions) {
    return new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        version("1.0"),
        List.of(versions));
  }

  /** The policy-eval PURL the facade builds for a maven candidate at the given version. */
  private static String purl(String version) {
    return "pkg:maven/org.example/lib@" + version + "?type=jar";
  }

  /** A {@code version -> PURL} map mirroring what the facade hands the filter for these versions. */
  private static Map<String, String> purlsFor(String... versions) {
    Map<String, String> purlByVersion = new LinkedHashMap<>();
    for (String version : versions) {
      purlByVersion.put(version, purl(version));
    }
    return purlByVersion;
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
