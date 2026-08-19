/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.guide.api.dto.AffectedComponentVersion;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersion;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersionSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceLevel;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceSummary;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GuideVulnerabilitiesResourceTest
{
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

  private GuideVulnerabilitiesResource underTest;

  @BeforeEach
  public void setUp() {
    underTest = new GuideVulnerabilitiesResource(
        searchApiClient,
        new GuidePolicyService(guidePolicyEvaluator, applicationDAO, ownerDAO, permissionService));
  }

  @Test
  public void getVulnerabilityAffectedComponents_attachesPolicyComplianceToHits() throws Exception {
    GuideAffectedComponentVersion v = new GuideAffectedComponentVersion(
        "npm", null, "@types/node", "25.9.2", "@types/node", null, null, null);
    GuideAffectedComponentVersionSearchResponse upstream = new GuideAffectedComponentVersionSearchResponse(
        List.of(v), 1L, 0, 20, Map.of());
    when(searchApiClient.getVulnerabilityAffectedComponents(any())).thenReturn(upstream);
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(List.of("pkg:npm/%40types%2Fnode@25.9.2")))
        .thenReturn(Map.of("pkg:npm/%40types%2Fnode@25.9.2", compliance));

    ApiSearchResponse<AffectedComponentVersion> result = underTest.getVulnerabilityAffectedComponents(
        "CVE-2025-12345", null, null, null, null, null);

    GuideAffectedComponentVersion enriched = (GuideAffectedComponentVersion) result.hits().get(0);
    // Affected-components is a list endpoint → only the compliant flag is attached.
    assertSlim(enriched.policyCompliance(), true);
  }

  @Test
  public void getVulnerabilityAffectedComponents_softFailsWhenEvaluatorReturnsEmpty() throws Exception {
    GuideAffectedComponentVersion v = new GuideAffectedComponentVersion(
        "npm", null, "@types/node", "25.9.2", "@types/node", null, null, null);
    GuideAffectedComponentVersionSearchResponse upstream = new GuideAffectedComponentVersionSearchResponse(
        List.of(v), 1L, 0, 20, Map.of());
    when(searchApiClient.getVulnerabilityAffectedComponents(any())).thenReturn(upstream);
    when(guidePolicyEvaluator.evaluate(any(List.class))).thenReturn(Map.of());

    ApiSearchResponse<AffectedComponentVersion> result = underTest.getVulnerabilityAffectedComponents(
        "CVE-2025-12345", null, null, null, null, null);

    GuideAffectedComponentVersion enriched = (GuideAffectedComponentVersion) result.hits().get(0);
    assertThat(enriched.policyCompliance()).isNull();
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

  private static void assertSlim(GuidePolicyCompliance pc, boolean expectedCompliant) {
    assertThat(pc).isNotNull();
    assertThat(pc.compliant()).isEqualTo(expectedCompliant);
    assertThat(pc.stage()).isNull();
    assertThat(pc.ownerId()).isNull();
    assertThat(pc.summary()).isNull();
    assertThat(pc.violations()).isNull();
  }
}
