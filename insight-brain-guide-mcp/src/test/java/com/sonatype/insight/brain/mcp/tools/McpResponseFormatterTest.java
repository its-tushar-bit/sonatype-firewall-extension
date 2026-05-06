/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp.tools;

import java.util.List;

import com.sonatype.insight.brain.mcp.model.McpPolicyContext;
import com.sonatype.insight.brain.mcp.model.McpPolicyViolation;
import com.sonatype.insight.brain.mcp.model.McpStageResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class McpResponseFormatterTest
{
  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testFormat_withoutPolicy() throws Exception {
    String input = "{\"component\":{\"name\":\"lib\",\"version\":\"1.0.0\"}}";

    String result = McpResponseFormatter.format(input, null);

    JsonNode root = mapper.readTree(result);
    assertThat(root.has("component")).isTrue();
    assertThat(root.get("component").get("name").asText()).isEqualTo("lib");
    assertThat(root.has("policy")).isFalse();
    assertThat(root.has("metadata")).isTrue();
    assertThat(root.get("metadata").get("source").asText()).isEqualTo("search-server");
    assertThat(root.get("metadata").get("policy").isNull()).isTrue();
  }

  @Test
  public void testFormat_withPolicy() throws Exception {
    String input = "{\"component\":{\"name\":\"lib\",\"version\":\"1.0.0\"}}";
    McpPolicyContext policy = new McpPolicyContext(
        "my-app",
        "build",
        new McpStageResult("build", false, "Fail", 1),
        null,
        List.of(new McpPolicyViolation("Security-Critical", 9, "Fail", List.of("CVE-2024-1234"), false)));

    String result = McpResponseFormatter.format(input, policy);

    JsonNode root = mapper.readTree(result);
    assertThat(root.has("component")).isTrue();
    assertThat(root.has("policy")).isTrue();
    assertThat(root.get("policy").get("applicationId").asText()).isEqualTo("my-app");
    assertThat(root.get("policy").get("stage").asText()).isEqualTo("build");
    assertThat(root.get("policy").get("violations")).hasSize(1);
    assertThat(root.get("policy").get("violations").get(0).get("policyName").asText())
        .isEqualTo("Security-Critical");
    assertThat(root.get("policy").get("stageResult").get("compliant").asBoolean()).isFalse();
    assertThat(root.has("metadata")).isTrue();
    assertThat(root.get("metadata").get("source").asText()).isEqualTo("search-server");
  }

  @Test
  public void testFormat_malformedJson_returnsFallback() {
    String malformed = "this is not json";

    String result = McpResponseFormatter.format(malformed, null);

    assertThat(result).isEqualTo(malformed);
  }

  @Test
  public void testFormat_preservesExistingFields() throws Exception {
    String input = "{\"component\":{\"name\":\"lib\"},\"vulnerabilities\":[{\"id\":\"CVE-1\"}]}";

    String result = McpResponseFormatter.format(input, null);

    JsonNode root = mapper.readTree(result);
    assertThat(root.has("component")).isTrue();
    assertThat(root.has("vulnerabilities")).isTrue();
    assertThat(root.get("vulnerabilities")).hasSize(1);
    assertThat(root.has("metadata")).isTrue();
  }
}
