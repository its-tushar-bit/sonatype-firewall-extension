/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sonatype.guide.api.dto.RecommendationResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideRecommendationResult;
import com.sonatype.insight.brain.guide.api.dto.RecommendedVersionInfo;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceLevel;

/**
 * Pure function: takes the upstream recommendations response, the per-version map of candidate
 * evaluation PURLs (built once by {@link GuidePolicyService} so the build and lookup sides share a
 * single PURL string), and the per-PURL compliance map, and returns a filtered + annotated copy.
 *
 * <p>
 * Mirrors Guide SaaS's {@code filterByPolicy} (RecommendationSelectionService) — filtering is
 * <em>per-candidate</em>, not all-or-nothing:
 * <ul>
 * <li>evaluated and compliant → keep, annotated compliant;
 * <li>evaluated and non-compliant → drop;
 * <li>no compliance data (the candidate's PURL is absent from {@code complianceByPurl} — e.g. a
 * brand-new release the evaluator/HDS has no data for) → keep, treated as compliant. This
 * matches SaaS, which treats a "not found" version as compliant ("no evidence of violation")
 * rather than failing the whole set open;
 * <li>no evaluation PURL (the version is absent from {@code purlByVersion} — a blank version, or an
 * unparseable parent PURL, for which the facade passes an empty map so every candidate lands
 * here) → drop;
 * <li>if no candidate survives → {@link RecommendationResponse.Outcome#BLOCKED_BY_POLICY}.
 * </ul>
 *
 * <p>
 * Package-private: only {@link GuidePolicyService} (same package) invokes this.
 */
final class GuideRecommendationsPolicyFilter
{
  private GuideRecommendationsPolicyFilter() {
  }

  static GuideRecommendationResult apply(
      GuideRecommendationResult upstream,
      Map<String, String> purlByVersion,
      Map<String, GuidePolicyCompliance> complianceByPurl)
  {
    if (upstream == null || upstream.toVersions() == null || upstream.toVersions().isEmpty()) {
      return upstream;
    }

    List<RecommendedVersionInfo> survivors = new ArrayList<>();
    for (RecommendedVersionInfo candidate : upstream.toVersions()) {
      String purl = purlByVersion.get(candidate.version());
      if (purl == null) {
        // No evaluation PURL was built for this candidate (blank version, or an unparseable parent
        // PURL — in which case purlByVersion is empty, every candidate lands here, the survivor list
        // stays empty, and BLOCKED_BY_POLICY is returned, matching SaaS). Not policy-checkable → drop.
        continue;
      }
      GuidePolicyCompliance compliance = complianceByPurl.get(purl);
      if (compliance != null && !compliance.compliant()) {
        // Evaluated and non-compliant (complianceLevel == FAIL) → drop.
        continue;
      }
      // Compliant (PASS or WARN), or no evaluation data (treated as compliant — PASS — matching
      // SaaS's not-found handling). Recommendation candidates are a list/badge surface, so attach
      // the badge only (compliant flag + the candidate's real level), not the full detail.
      GuidePolicyComplianceLevel level =
          (compliance != null) ? compliance.complianceLevel() : GuidePolicyComplianceLevel.PASS;
      survivors.add(withCompliance(candidate, GuidePolicyCompliance.badge(level)));
    }

    if (survivors.isEmpty()) {
      return new GuideRecommendationResult(
          RecommendationResponse.Outcome.BLOCKED_BY_POLICY, upstream.fromVersion(), List.of());
    }
    return new GuideRecommendationResult(upstream.outcome(), upstream.fromVersion(), survivors);
  }

  private static RecommendedVersionInfo withCompliance(
      RecommendedVersionInfo c,
      GuidePolicyCompliance compliance)
  {
    return new RecommendedVersionInfo(
        c.version(), c.breakingChangesCount(), c.directVulnerabilities(),
        c.transitiveVulnerabilities(), c.licenseThreatLevels(),
        c.vulnerableMethods(), c.developerTrustScore(), c.maxSeverity(), compliance);
  }
}
