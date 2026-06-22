/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto.policy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersion;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDetailDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.RecommendedVersionInfo;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GuidePolicyComplianceJsonTest
{
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  public void emptyButPresent_serializesAllRequiredFields() throws Exception {
    Map<String, Integer> categories = new LinkedHashMap<>();
    categories.put("SECURITY", 0);
    categories.put("LICENSE", 0);
    categories.put("QUALITY", 0);
    categories.put("OTHER", 0);
    GuidePolicyCompliance compliance = new GuidePolicyCompliance(
        true,
        GuidePolicyComplianceLevel.PASS,
        "release",
        "ROOT_ORGANIZATION_ID",
        new GuidePolicyComplianceSummary(0, "none", 0, 0, categories),
        List.of());

    JsonNode json = mapper.valueToTree(compliance);

    assertThat(json.get("compliant").asBoolean()).isTrue();
    assertThat(json.get("complianceLevel").asText()).isEqualTo("PASS");
    assertThat(json.get("stage").asText()).isEqualTo("release");
    assertThat(json.get("ownerId").asText()).isEqualTo("ROOT_ORGANIZATION_ID");
    assertThat(json.get("violations").isArray()).isTrue();
    assertThat(json.get("violations")).isEmpty();

    JsonNode summary = json.get("summary");
    assertThat(summary.get("highestThreatLevel").asInt()).isZero();
    assertThat(summary.get("worstAction").asText()).isEqualTo("none");
    assertThat(summary.get("activeViolationCount").asInt()).isZero();
    assertThat(summary.get("waivedViolationCount").asInt()).isZero();

    JsonNode counts = summary.get("violationCountsByCategory");
    assertThat(counts.fieldNames()).toIterable()
        .containsExactlyInAnyOrder(
            "SECURITY", "LICENSE", "QUALITY", "OTHER");
    assertThat(counts.get("SECURITY").asInt()).isZero();
    assertThat(counts.get("OTHER").asInt()).isZero();
  }

  @Test
  public void badge_serializesCompliantFlagAndLevelOnly() throws Exception {
    JsonNode json = mapper.valueToTree(GuidePolicyCompliance.badge(GuidePolicyComplianceLevel.FAIL));

    // List/badge responses carry the flag + level only — the null stage/ownerId/summary/violations
    // are dropped by @JsonInclude(NON_NULL), leaving exactly {"compliant":false,"complianceLevel":"FAIL"}.
    assertThat(json.get("compliant").asBoolean()).isFalse();
    assertThat(json.get("complianceLevel").asText()).isEqualTo("FAIL");
    assertThat(json.has("stage")).isFalse();
    assertThat(json.has("ownerId")).isFalse();
    assertThat(json.has("summary")).isFalse();
    assertThat(json.has("violations")).isFalse();
    assertThat(json.size()).isEqualTo(2);
  }

  @Test
  public void badge_warn_isCompliantTrueWithWarnLevel() throws Exception {
    JsonNode json = mapper.valueToTree(GuidePolicyCompliance.badge(GuidePolicyComplianceLevel.WARN));

    // WARN (e.g. warn-action or fully-waived) is still compliant=true (the "check"), distinct from FAIL.
    assertThat(json.get("compliant").asBoolean()).isTrue();
    assertThat(json.get("complianceLevel").asText()).isEqualTo("WARN");
    assertThat(json.size()).isEqualTo(2);
  }

  @Test
  public void richViolationWithWaiver_serializesAllFields() throws Exception {
    GuideTriggerReference triggerRef = new GuideTriggerReference("vulnerability", "CVE-2021-44228");
    GuideViolationReason reason = new GuideViolationReason(
        "Found security vulnerability CVE-2021-44228", triggerRef);
    GuideConstraintViolation cv = new GuideConstraintViolation(
        "abc", "Critical CVSS", List.of(reason));
    GuideWaiverInfo waiver = new GuideWaiverInfo(
        "organization",
        "ROOT_ORGANIZATION_ID",
        Instant.parse("2026-12-01T00:00:00Z"),
        "Approved by legal");
    GuidePolicyViolation violation = new GuidePolicyViolation(
        "f8a7", "Security-Critical", 9, List.of("fail"), true, waiver, List.of(cv));

    JsonNode json = mapper.valueToTree(violation);

    assertThat(json.get("policyId").asText()).isEqualTo("f8a7");
    assertThat(json.get("policyName").asText()).isEqualTo("Security-Critical");
    assertThat(json.get("threatLevel").asInt()).isEqualTo(9);
    assertThat(json.get("actions").get(0).asText()).isEqualTo("fail");
    assertThat(json.get("waived").asBoolean()).isTrue();
    assertThat(json.get("waiver").get("scopeOwnerType").asText()).isEqualTo("organization");
    assertThat(json.get("waiver").get("expiryTime").asText()).isEqualTo("2026-12-01T00:00:00Z");
    assertThat(json.get("waiver").get("comment").asText()).isEqualTo("Approved by legal");
    assertThat(json.get("constraintViolations").get(0).get("constraintName").asText())
        .isEqualTo("Critical CVSS");
    assertThat(json.get("constraintViolations").get(0).get("reasons").get(0).get("reason").asText())
        .isEqualTo("Found security vulnerability CVE-2021-44228");
    assertThat(json.get("constraintViolations")
        .get(0)
        .get("reasons")
        .get(0)
        .get("reference")
        .get("type")
        .asText()).isEqualTo("vulnerability");
  }

  @Test
  public void waiverInfo_deserializesIso8601ExpiryTime() throws Exception {
    String json = "{\"scopeOwnerType\":\"organization\",\"scopeOwnerId\":\"ROOT_ORGANIZATION_ID\","
        + "\"expiryTime\":\"2026-12-01T00:00:00Z\",\"comment\":\"ok\"}";

    GuideWaiverInfo parsed = mapper.readValue(json, GuideWaiverInfo.class);

    assertThat(parsed.expiryTime()).isEqualTo(Instant.parse("2026-12-01T00:00:00Z"));
    assertThat(parsed.scopeOwnerType()).isEqualTo("organization");
  }

  @Test
  public void absentWaiver_omitsField() throws Exception {
    GuidePolicyViolation violation = new GuidePolicyViolation(
        "id", "name", 5, List.of("warn"), false, null, List.of());

    JsonNode json = mapper.valueToTree(violation);

    assertThat(json.has("waiver")).isFalse();
  }

  @Test
  public void absentReference_omitsField() throws Exception {
    GuideViolationReason reason = new GuideViolationReason("Some reason", null);

    JsonNode json = mapper.valueToTree(reason);

    assertThat(json.has("reference")).isFalse();
  }

  @Test
  public void componentDocument_withCompliance_serializesField() throws Exception {
    Map<String, Integer> categories = new LinkedHashMap<>();
    categories.put("SECURITY", 0);
    categories.put("LICENSE", 0);
    categories.put("QUALITY", 0);
    categories.put("OTHER", 0);
    GuidePolicyCompliance compliance = new GuidePolicyCompliance(
        true, GuidePolicyComplianceLevel.PASS, "release", "ROOT_ORGANIZATION_ID",
        new GuidePolicyComplianceSummary(0, "none", 0, 0, categories),
        List.of());

    GuideComponentDocument doc =
        new GuideComponentDocument(
            "maven", null, "org.example", "lib", "1.0", null,
            null, null, null, null, null, null, null, null, compliance);

    JsonNode json = mapper.valueToTree(doc);

    assertThat(json.has("policyCompliance")).isTrue();
    assertThat(json.get("policyCompliance").get("compliant").asBoolean()).isTrue();
  }

  @Test
  public void componentDocument_nullCompliance_omitsField() throws Exception {
    GuideComponentDocument doc =
        new GuideComponentDocument(
            "maven", null, "org.example", "lib", "1.0", null,
            null, null, null, null, null, null, null, null, null);

    JsonNode json = mapper.valueToTree(doc);

    assertThat(json.has("policyCompliance")).isFalse();
  }

  @Test
  public void componentDetailDocument_withCompliance_serializesField() throws Exception {
    Map<String, Integer> categories = new LinkedHashMap<>();
    categories.put("SECURITY", 0);
    categories.put("LICENSE", 0);
    categories.put("QUALITY", 0);
    categories.put("OTHER", 0);
    GuidePolicyCompliance compliance = new GuidePolicyCompliance(
        true, GuidePolicyComplianceLevel.PASS, "release", "ROOT_ORGANIZATION_ID",
        new GuidePolicyComplianceSummary(0, "none", 0, 0, categories),
        List.of());

    GuideComponentDetailDocument doc =
        new GuideComponentDetailDocument(
            "maven", null, "org.example", "lib", "1.0", null, null, null,
            null, null, null, null, null, null, null, null, compliance);

    JsonNode json = mapper.valueToTree(doc);

    assertThat(json.has("policyCompliance")).isTrue();
    assertThat(json.get("policyCompliance").get("compliant").asBoolean()).isTrue();
  }

  @Test
  public void componentDetailDocument_nullCompliance_omitsField() throws Exception {
    GuideComponentDetailDocument doc =
        new GuideComponentDetailDocument(
            "maven", null, "org.example", "lib", "1.0", null, null, null,
            null, null, null, null, null, null, null, null, null);

    JsonNode json = mapper.valueToTree(doc);

    assertThat(json.has("policyCompliance")).isFalse();
  }

  @Test
  public void affectedComponent_nullCompliance_omitsField() throws Exception {
    GuideAffectedComponentVersion v =
        new GuideAffectedComponentVersion(
            "maven", "org.example", "lib", "1.0", "lib", null);

    JsonNode json = mapper.valueToTree(v);

    assertThat(json.has("policyCompliance")).isFalse();
  }

  @Test
  public void recommendedVersion_nullCompliance_omitsField() throws Exception {
    RecommendedVersionInfo v =
        new RecommendedVersionInfo(
            "1.0", null, null, null, null, null, null, null, null);

    JsonNode json = mapper.valueToTree(v);

    assertThat(json.has("policyCompliance")).isFalse();
  }
}
