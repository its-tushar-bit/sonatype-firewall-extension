/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.policy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersion;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDetailDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceLevel;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceSummary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GuidePolicyResponseEnricherTest
{
  private static final GuidePolicyCompliance COMPLIANT = compliantOf();

  @Test
  public void enrichComponent_compliantOnly_attachesSlimCompliance() {
    GuideComponentDocument doc = new GuideComponentDocument(
        "maven", null, "org.apache.logging.log4j", "log4j-core", "2.14.0", null,
        null, null, null, null, null, null, null, null, null);
    Map<String, GuidePolicyCompliance> map = Map.of(
        "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.0?type=jar", COMPLIANT);

    GuideComponentDocument enriched = GuidePolicyResponseEnricher.enrichComponent(
        doc, map, GuidePolicyResponseEnricher.PolicyDetail.COMPLIANT_ONLY);

    assertSlim(enriched.policyCompliance(), true);
    assertThat(enriched.format()).isEqualTo(doc.format());
    assertThat(enriched.name()).isEqualTo(doc.name());
  }

  @Test
  public void enrichComponent_complianceMissing_returnsSameInstance() {
    GuideComponentDocument doc = new GuideComponentDocument(
        "maven", null, "org.apache.logging.log4j", "log4j-core", "2.14.0", null,
        null, null, null, null, null, null, null, null, null);

    GuideComponentDocument enriched = GuidePolicyResponseEnricher.enrichComponent(
        doc, Map.of(), GuidePolicyResponseEnricher.PolicyDetail.COMPLIANT_ONLY);

    assertThat(enriched).isSameAs(doc);
  }

  @Test
  public void enrichDetail_full_attachesFullCompliance() {
    GuideComponentDetailDocument doc = new GuideComponentDetailDocument(
        "maven", null, "org.apache.logging.log4j", "log4j-core", "2.14.0", null, null, null,
        null, null, null, null, null, null, null, null, null);
    Map<String, GuidePolicyCompliance> map = Map.of(
        "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.0?type=jar", COMPLIANT);

    GuideComponentDetailDocument enriched = GuidePolicyResponseEnricher.enrichDetail(
        doc, map, GuidePolicyResponseEnricher.PolicyDetail.FULL);

    assertThat(enriched.policyCompliance()).isSameAs(COMPLIANT);
  }

  @Test
  public void enrichDetail_compliantOnly_attachesSlimCompliance() {
    GuideComponentDetailDocument doc = new GuideComponentDetailDocument(
        "maven", null, "org.apache.logging.log4j", "log4j-core", "2.14.0", null, null, null,
        null, null, null, null, null, null, null, null, null);
    Map<String, GuidePolicyCompliance> map = Map.of(
        "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.0?type=jar", COMPLIANT);

    GuideComponentDetailDocument enriched = GuidePolicyResponseEnricher.enrichDetail(
        doc, map, GuidePolicyResponseEnricher.PolicyDetail.COMPLIANT_ONLY);

    assertSlim(enriched.policyCompliance(), true);
  }

  @Test
  public void enrichAffected_compliantOnly_attachesSlimCompliance() {
    GuideAffectedComponentVersion v = new GuideAffectedComponentVersion(
        "npm", null, "@types/node", "25.9.2", "@types/node", null);
    Map<String, GuidePolicyCompliance> map = Map.of("pkg:npm/%40types%2Fnode@25.9.2", COMPLIANT);

    GuideAffectedComponentVersion enriched = GuidePolicyResponseEnricher.enrichAffected(
        v, map, GuidePolicyResponseEnricher.PolicyDetail.COMPLIANT_ONLY);

    assertSlim(enriched.policyCompliance(), true);
  }

  private static void assertSlim(GuidePolicyCompliance pc, boolean expectedCompliant) {
    assertThat(pc).isNotNull();
    assertThat(pc.compliant()).isEqualTo(expectedCompliant);
    assertThat(pc.stage()).isNull();
    assertThat(pc.ownerId()).isNull();
    assertThat(pc.summary()).isNull();
    assertThat(pc.violations()).isNull();
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
}
