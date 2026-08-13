/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.policy;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.guide.api.dto.GuideAffectedAsset;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersion;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDetailDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.guide.api.purl.GuidePurlAssembler;

/**
 * Per-element helper used by {@link GuidePolicyService} to attach a {@link GuidePolicyCompliance} to a
 * single Guide component-shaped record. PURL assembly is delegated to {@link
 * GuidePurlAssembler#purlFor}, so the canonicalization here matches what {@link GuidePolicyEvaluator}
 * uses for its map keys.
 *
 * <p>
 * If the component's PURL can't be assembled (missing format/name/version), {@link
 * GuidePurlAssembler#purlFor} returns null and the corresponding {@code enrich*} call returns the
 * original document unchanged — the response then carries that row without a {@code policyCompliance}
 * field (soft-fail for malformed upstream data).
 *
 * <p>
 * Package-private: only {@link GuidePolicyService} (same package) orchestrates enrichment — resources
 * must not call this directly.
 */
final class GuidePolicyResponseEnricher
{
  /**
   * How much of the policy result to attach. {@code COMPLIANT_ONLY} is for list/badge responses
   * (search, versions, dependencies, global search, vuln-affected, recommendation candidates) —
   * they only need {@code policyCompliance.compliant}. {@code FULL} is for single-component detail
   * responses ({@code /detail}, {@code /latest-version}). The flavor is chosen per call site because
   * the same DTO ({@code GuideComponentDetailDocument}) is returned by both a list endpoint
   * ({@code /versions}) and a detail endpoint.
   */
  enum PolicyDetail
  {
    FULL,
    COMPLIANT_ONLY
  }

  private GuidePolicyResponseEnricher() {
  }

  private static GuidePolicyCompliance reduce(GuidePolicyCompliance compliance, PolicyDetail detail) {
    return detail == PolicyDetail.COMPLIANT_ONLY
        ? GuidePolicyCompliance.badge(compliance.complianceLevel())
        : compliance;
  }

  static GuideComponentDocument enrichComponent(
      GuideComponentDocument doc,
      Map<String, GuidePolicyCompliance> complianceByPurl,
      PolicyDetail detail)
  {
    String purl = GuidePurlAssembler.purlFor(doc);
    GuidePolicyCompliance compliance = (purl == null) ? null : complianceByPurl.get(purl);
    if (compliance == null) {
      return doc;
    }
    return new GuideComponentDocument(
        doc.format(), doc.originId(), doc.namespace(), doc.name(), doc.version(), doc.registryLink(),
        doc.licenses(), doc.categories(), doc.latestStable(), doc.versionScore(), doc.maxCvss(),
        doc.publishedDate(), doc.isMalware(), doc.dtsDimensions(), reduce(compliance, detail));
  }

  static GuideComponentDetailDocument enrichDetail(
      GuideComponentDetailDocument doc,
      Map<String, GuidePolicyCompliance> complianceByPurl,
      PolicyDetail detail)
  {
    String purl = GuidePurlAssembler.purlFor(doc);
    GuidePolicyCompliance compliance = (purl == null) ? null : complianceByPurl.get(purl);
    if (compliance == null) {
      return doc;
    }
    return new GuideComponentDetailDocument(
        doc.format(), doc.originId(), doc.namespace(), doc.name(), doc.version(), doc.registryLink(),
        doc.components(), doc.licenses(), doc.categories(), doc.latestStable(), doc.versionScore(),
        doc.maxCvss(), doc.publishedDate(), doc.directDependencies(), doc.isMalware(),
        doc.dtsDimensions(), reduce(compliance, detail));
  }

  static GuideAffectedComponentVersion enrichAffected(
      GuideAffectedComponentVersion v,
      Map<String, GuidePolicyCompliance> complianceByPurl,
      PolicyDetail detail)
  {
    String purl = GuidePurlAssembler.purlFor(v);
    GuidePolicyCompliance compliance = (purl == null) ? null : complianceByPurl.get(purl);
    if (compliance == null) {
      return v;
    }
    List<GuideAffectedAsset> assets = v.affectedAssets();
    return new GuideAffectedComponentVersion(
        v.ecosystem(), v.namespace(), v.packageName(), v.version(), v.fullPackageName(),
        reduce(compliance, detail), v.affectsPrimaryAsset(), assets);
  }
}
