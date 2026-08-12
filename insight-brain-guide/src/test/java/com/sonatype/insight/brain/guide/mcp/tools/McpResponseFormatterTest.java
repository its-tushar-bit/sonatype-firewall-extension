/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.mcp.tools;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceLevel;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceSummary;
import com.sonatype.insight.brain.guide.mcp.model.McpPolicyCompliance;
import com.sonatype.insight.brain.guide.mcp.model.McpPolicyConstraint;
import com.sonatype.insight.brain.guide.mcp.model.McpPolicyViolation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class McpResponseFormatterTest
{
  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String PURL = "pkg:maven/org.apache.commons/commons-lang3@3.14.0";

  private static final String COMPONENT_DETAIL_JSON = """
      {
        "format": "maven",
        "namespace": "org.apache.commons",
        "name": "commons-lang3",
        "version": "3.14.0",
        "registryLink": "https://central.sonatype.com/artifact/org.apache.commons/commons-lang3/3.14.0",
        "components": [
          {
            "extension": "jar",
            "classifier": "",
            "sha1": "abc123",
            "publishedDate": "2023-11-18T10:23:25Z",
            "refids": [
              {"refid": "CVE-2025-48924", "severity": 6.9, "isMalware": false}
            ]
          }
        ],
        "licenses": [
          {"licenseName": "Apache-2.0", "licenseThreatGroup": "Permissive", "licenseThreatLevel": 0}
        ],
        "publishedDate": "2023-11-18T10:23:25Z",
        "isMalware": false,
        "dts": {"overall": 96, "age": 100, "license": 100, "popularity": 90, "security": 85}
      }""";

  private static final String LATEST_VERSION_JSON = """
      {
        "format": "maven",
        "namespace": "org.apache.commons",
        "name": "commons-lang3",
        "version": "3.20.0",
        "registryLink": "https://central.sonatype.com/artifact/org.apache.commons/commons-lang3/3.20.0",
        "components": [
          {"extension": "jar", "classifier": "", "sha1": "def456", "publishedDate": "2025-11-12T20:08:07Z"}
        ],
        "licenses": [
          {"licenseName": "Apache-2.0", "licenseThreatGroup": "Permissive", "licenseThreatLevel": 0}
        ],
        "publishedDate": "2025-11-12T20:08:07Z",
        "isMalware": false
      }""";

  private static final String RECOMMENDATION_JSON =
      """
          {
            "outcome": "FOUND_RECOMMENDATIONS",
            "fromVersion": {
              "version": "3.14.0",
              "breakingChangesCount": "0",
              "directVulnerabilities": {"CVE-2025-48924": 6.9},
              "transitiveVulnerabilities": {},
              "licenseThreatLevels": {"Apache-2.0": 0},
              "vulnerableMethods": [
                {
                  "refid": "CVE-2025-48924",
                  "methodSignatures": [
                    {"type": "JVM", "signature": "org/apache/commons/lang3/ClassUtils.getClass", "vulnerableParameters": [1]}
                  ]
                }
              ],
              "developerTrustScore": 96,
              "maxSeverity": null,
              "dtsDimensions": null
            },
            "toVersions": [
              {
                "version": "3.18.0",
                "breakingChangesCount": "2",
                "directVulnerabilities": null,
                "transitiveVulnerabilities": null,
                "licenseThreatLevels": {"Apache-2.0": 0},
                "vulnerableMethods": [],
                "developerTrustScore": 99,
                "dtsDimensions": null
              }
            ]
          }""";

  @Test
  public void formatComponentVersion_extractsFields() throws Exception {
    String result = McpResponseFormatter.formatComponentVersion(PURL, COMPONENT_DETAIL_JSON, null);

    JsonNode root = mapper.readTree(result);
    assertThat(root.isArray()).isTrue();
    assertThat(root).hasSize(1);

    JsonNode item = root.get(0);
    assertThat(item.get("packageUrl").asText()).isEqualTo(PURL);
    assertThat(item.get("success").asBoolean()).isTrue();

    JsonNode data = item.get("data");
    assertThat(data.get("version").asText()).isEqualTo("3.14.0");
    assertThat(data.get("endOfLife").asBoolean()).isFalse();
    assertThat(data.get("malicious").asBoolean()).isFalse();
    assertThat(data.get("catalogDate").asLong()).isEqualTo(1700303005000L);
    assertThat(data.has("dts")).isFalse();
    assertThat(data.has("registryLink")).isFalse();
    assertThat(data.has("components")).isFalse();
  }

  @Test
  public void formatComponentVersion_extractsLicensesAsStrings() throws Exception {
    String result = McpResponseFormatter.formatComponentVersion(PURL, COMPONENT_DETAIL_JSON, null);

    JsonNode licenses = mapper.readTree(result).get(0).get("data").get("licenses");
    assertThat(licenses.isArray()).isTrue();
    assertThat(licenses.get(0).asText()).isEqualTo("Apache-2.0");
  }

  @Test
  public void formatComponentVersion_extractsVulnerabilitiesFromRefids() throws Exception {
    String result = McpResponseFormatter.formatComponentVersion(PURL, COMPONENT_DETAIL_JSON, null);

    JsonNode vulns = mapper.readTree(result).get(0).get("data").get("vulnerabilities");
    assertThat(vulns.get("cves")).hasSize(1);
    assertThat(vulns.get("cves").get(0).get("id").asText()).isEqualTo("CVE-2025-48924");
    assertThat(vulns.get("cves").get(0).get("cvssScore").floatValue()).isEqualTo(6.9f);
  }

  @Test
  public void formatComponentVersion_withPolicy() throws Exception {
    McpPolicyCompliance policy = new McpPolicyCompliance(
        false, GuidePolicyComplianceLevel.FAIL, "build", "my-app",
        new GuidePolicyComplianceSummary(9, "fail", 1, 0,
            Map.of("SECURITY", 1, "LICENSE", 0, "QUALITY", 0, "OTHER", 0)),
        List.of(new McpPolicyViolation(
            "Security-Critical", 9, List.of("fail"), false, null,
            List.of(new McpPolicyConstraint("Critical CVSS score", List.of("Found CVE-2025-48924"))))));

    String result = McpResponseFormatter.formatComponentVersion(PURL, COMPONENT_DETAIL_JSON, policy);

    JsonNode data = mapper.readTree(result).get(0).get("data");
    // Legacy dual-emission field is gone; only the slim policyCompliance remains.
    assertThat(data.has("oldPolicyComplianceToBeRemoved")).isFalse();

    JsonNode policyNode = data.get("policyCompliance");
    assertThat(policyNode.get("compliant").asBoolean()).isFalse();
    assertThat(policyNode.get("stage").asText()).isEqualTo("build");
    assertThat(policyNode.get("ownerId").asText()).isEqualTo("my-app");
    assertThat(policyNode.get("summary").get("worstAction").asText()).isEqualTo("fail");

    JsonNode violation = policyNode.get("violations").get(0);
    assertThat(violation.get("policyName").asText()).isEqualTo("Security-Critical");
    assertThat(violation.get("actions").get(0).asText()).isEqualTo("fail");
    // Slim shape: no policyId, no constraintViolations — reasons live under named constraints.
    assertThat(violation.has("policyId")).isFalse();
    assertThat(violation.has("constraintViolations")).isFalse();
    JsonNode constraint = violation.get("constraints").get(0);
    assertThat(constraint.get("constraintName").asText()).isEqualTo("Critical CVSS score");
    assertThat(constraint.get("reasons").get(0).asText()).isEqualTo("Found CVE-2025-48924");
  }

  @Test
  public void formatLatestVersion_noVulnerabilities() throws Exception {
    String result = McpResponseFormatter.formatLatestVersion(PURL, LATEST_VERSION_JSON, null);

    JsonNode data = mapper.readTree(result).get(0).get("data");
    assertThat(data.get("version").asText()).isEqualTo("3.20.0");
    assertThat(data.has("vulnerabilities")).isFalse();
    assertThat(data.get("licenses").get(0).asText()).isEqualTo("Apache-2.0");
    assertThat(data.get("catalogDate").asLong()).isEqualTo(1762978087000L);
  }

  @Test
  public void formatRecommendations_omitsPolicyCompliance() throws Exception {
    String result = McpResponseFormatter.formatRecommendations(PURL, RECOMMENDATION_JSON);

    JsonNode root = mapper.readTree(result);
    JsonNode from = root.get(0).get("fromVersion");
    assertThat(from.has("policyCompliance")).isFalse();

    JsonNode toVersions = root.get(0).get("toVersions");
    assertThat(toVersions.get(0).has("policyCompliance")).isFalse();
  }

  @Test
  public void formatRecommendations_extractsStructure() throws Exception {
    String result = McpResponseFormatter.formatRecommendations(PURL, RECOMMENDATION_JSON);

    JsonNode root = mapper.readTree(result);
    assertThat(root.isArray()).isTrue();
    assertThat(root).hasSize(1);

    JsonNode item = root.get(0);
    assertThat(item.get("packageUrl").asText()).isEqualTo(PURL);
    assertThat(item.get("success").asBoolean()).isTrue();
    assertThat(item.get("outcome").asText()).isEqualTo("FOUND_RECOMMENDATIONS");

    JsonNode from = item.get("fromVersion");
    assertThat(from.get("version").asText()).isEqualTo("3.14.0");
    assertThat(from.get("developerTrustScore").asInt()).isEqualTo(96);
    assertThat(from.has("maxSeverity")).isFalse();
    assertThat(from.has("dtsDimensions")).isFalse();

    JsonNode methods = from.get("vulnerableMethods");
    assertThat(methods).hasSize(1);
    assertThat(methods.get(0).get("refid").asText()).isEqualTo("CVE-2025-48924");
    assertThat(methods.get(0).get("methodSignatures").get(0).get("type").asText()).isEqualTo("JVM");

    JsonNode toVersions = item.get("toVersions");
    assertThat(toVersions).hasSize(1);
    assertThat(toVersions.get(0).get("version").asText()).isEqualTo("3.18.0");
    assertThat(toVersions.get(0).get("developerTrustScore").asInt()).isEqualTo(99);
  }

  @Test
  public void formatComponentVersion_malformedJson_returnsFailureWithSanitizedMessage() throws Exception {
    String result = McpResponseFormatter.formatComponentVersion(PURL, "not json", null);

    JsonNode root = mapper.readTree(result);
    assertThat(root.get(0).get("success").asBoolean()).isFalse();
    assertThat(root.get(0).get("error").asText()).isEqualTo("Failed to process component data");
  }

  @Test
  public void formatComponentVersion_detectsMalwareFromRefids() throws Exception {
    String malwareJson =
        """
            {"version":"1.0.0","licenses":[],"publishedDate":"2024-01-01T00:00:00Z",\
            "components":[{"extension":"jar","refids":[{"refid":"sonatype-2024-malware","severity":10.0,"isMalware":true}]}]}""";

    String result = McpResponseFormatter.formatComponentVersion(PURL, malwareJson, null);

    JsonNode data = mapper.readTree(result).get(0).get("data");
    assertThat(data.get("malicious").asBoolean()).isTrue();
    assertThat(data.has("vulnerabilities")).isFalse();
  }

  @Test
  public void formatComponentVersion_detectsMalwareFromTopLevel() throws Exception {
    String malwareJson = """
        {"version":"1.0.0","licenses":[],"publishedDate":"2024-01-01T00:00:00Z","isMalware":true,"components":[]}""";

    String result = McpResponseFormatter.formatComponentVersion(PURL, malwareJson, null);

    assertThat(mapper.readTree(result).get(0).get("data").get("malicious").asBoolean()).isTrue();
  }

  @Test
  public void formatComponentVersion_handlesNumericCatalogDate() throws Exception {
    String json = """
        {"version":"1.0.0","licenses":[],"catalogDate":1700319805000,"components":[]}""";

    String result = McpResponseFormatter.formatComponentVersion(PURL, json, null);

    assertThat(mapper.readTree(result).get(0).get("data").get("catalogDate").asLong())
        .isEqualTo(1700319805000L);
  }

  @Test
  public void formatComponentVersion_fallsBackToTopLevelVulnerabilities_whenComponentsHaveNoRefids() throws Exception {
    String json = """
        {"version":"1.0.0","licenses":[],"publishedDate":"2024-01-01T00:00:00Z",\
        "components":[{"extension":"jar"}],\
        "vulnerabilities":{"cves":[{"id":"CVE-2024-9999","cvssScore":8.1}]}}""";

    String result = McpResponseFormatter.formatComponentVersion(PURL, json, null);

    JsonNode vulns = mapper.readTree(result).get(0).get("data").get("vulnerabilities");
    assertThat(vulns.get("cves")).hasSize(1);
    assertThat(vulns.get("cves").get(0).get("id").asText()).isEqualTo("CVE-2024-9999");
    assertThat(vulns.get("cves").get(0).get("cvssScore").floatValue()).isEqualTo(8.1f);
  }

  @Test
  public void formatComponentVersion_mergesBothRefidsAndTopLevelVulnerabilities() throws Exception {
    String json = """
        {"version":"1.0.0","licenses":[],"publishedDate":"2024-01-01T00:00:00Z",\
        "components":[{"extension":"jar","refids":[{"refid":"CVE-2024-1111","severity":7.5,"isMalware":false}]}],\
        "vulnerabilities":{"cves":[{"id":"CVE-2024-2222","cvssScore":9.0},{"id":"CVE-2024-1111","cvssScore":7.5}]}}""";

    String result = McpResponseFormatter.formatComponentVersion(PURL, json, null);

    JsonNode vulns = mapper.readTree(result).get(0).get("data").get("vulnerabilities");
    assertThat(vulns.get("cves")).hasSize(2);
    assertThat(vulns.get("cves").get(0).get("id").asText()).isEqualTo("CVE-2024-1111");
    assertThat(vulns.get("cves").get(1).get("id").asText()).isEqualTo("CVE-2024-2222");
  }

  @Test
  public void formatComponentVersion_extractsEndOfLifeWhenPresent() throws Exception {
    String json = """
        {"version":"1.0.0","licenses":[],"publishedDate":"2024-01-01T00:00:00Z","endOfLife":true,"components":[]}""";

    String result = McpResponseFormatter.formatComponentVersion(PURL, json, null);

    assertThat(mapper.readTree(result).get(0).get("data").get("endOfLife").asBoolean()).isTrue();
  }

  @Test
  public void formatRecommendations_missingFromVersion_returnsSuccessWithOutcome() throws Exception {
    String json = "{\"outcome\":\"NO_VERSIONS_FOUND\",\"toVersions\":[]}";

    String result = McpResponseFormatter.formatRecommendations(PURL, json);

    JsonNode root = mapper.readTree(result);
    assertThat(root.get(0).get("success").asBoolean()).isTrue();
    assertThat(root.get(0).get("packageUrl").asText()).isEqualTo(PURL);
    assertThat(root.get(0).get("outcome").asText()).isEqualTo("NO_VERSIONS_FOUND");
    assertThat(root.get(0).has("fromVersion")).isFalse();
    assertThat(root.get(0).has("toVersions")).isFalse();
  }

  @Test
  public void formatRecommendations_malformedJson_returnsFailureWithSanitizedMessage() throws Exception {
    String result = McpResponseFormatter.formatRecommendations(PURL, "broken");

    JsonNode root = mapper.readTree(result);
    assertThat(root.get(0).get("success").asBoolean()).isFalse();
    assertThat(root.get(0).get("packageUrl").asText()).isEqualTo(PURL);
    assertThat(root.get(0).get("error").asText()).isEqualTo("Failed to process recommendation data");
  }
}
