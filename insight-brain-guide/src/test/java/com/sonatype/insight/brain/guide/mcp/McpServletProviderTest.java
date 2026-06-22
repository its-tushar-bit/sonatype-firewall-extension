/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.mcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.guide.api.error.GuideNotFoundException;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceLevel;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceSummary;
import com.sonatype.insight.brain.guide.mcp.McpServletProvider.SearchFunction;
import com.sonatype.insight.brain.guide.mcp.McpServletProvider.ToolType;
import com.sonatype.insight.brain.guide.mcp.model.McpPolicyCompliance;
import com.sonatype.insight.brain.guide.mcp.model.McpPolicyConstraint;
import com.sonatype.insight.brain.guide.mcp.model.McpPolicyViolation;
import com.sonatype.insight.brain.guide.mcp.policy.McpPolicyAnnotator;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.guide.telemetry.GuideChannel;
import com.sonatype.insight.brain.guide.telemetry.GuideChannelContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class McpServletProviderTest
{
  private static final String PURL = "pkg:maven/org.example/lib@1.0.0";

  private static final String PURL_COMPLETED = "pkg:maven/org.example/lib@1.0.0?type=jar";

  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String COMPONENT_JSON = """
      {"version":"1.0.0","licenses":[{"licenseName":"Apache-2.0","licenseThreatLevel":0}],\
      "publishedDate":"2024-01-15T10:00:00Z","isMalware":false,\
      "components":[{"extension":"jar","refids":[{"refid":"CVE-2024-1234","severity":7.5}]}]}""";

  private static final String RECOMMENDATION_JSON = """
      {"outcome":"FOUND_RECOMMENDATIONS",\
      "fromVersion":{"version":"1.0.0","breakingChangesCount":"0",\
      "directVulnerabilities":{"CVE-2024-1234":7.5},"transitiveVulnerabilities":{},\
      "licenseThreatLevels":{"Apache-2.0":0},"vulnerableMethods":[],\
      "developerTrustScore":90,"maxSeverity":null,"dtsDimensions":null},\
      "toVersions":[{"version":"2.0.0","breakingChangesCount":"1",\
      "directVulnerabilities":null,"transitiveVulnerabilities":null,\
      "licenseThreatLevels":{"Apache-2.0":0},"vulnerableMethods":[],\
      "developerTrustScore":99,"dtsDimensions":null}]}""";

  @Mock
  private SearchApiClient searchApiClient;

  @Mock
  private McpPolicyAnnotator policyAnnotator;

  private McpServletProvider underTest;

  @Before
  public void setUp() {
    underTest = new McpServletProvider();
    underTest.initialize(searchApiClient, policyAnnotator);
  }

  @After
  public void clearChannel() {
    GuideChannelContext.clear();
  }

  @Test
  public void callTool_marksMcpChannelDuringCall_andClearsAfter() {
    GuideChannel[] captured = new GuideChannel[1];
    SearchFunction fn = purl -> {
      // capture the channel at the SearchApiClient seam (fn is invoked by processOnePurl)
      captured[0] = GuideChannelContext.getOrDefault();
      return COMPONENT_JSON;
    };
    CallToolRequest request = new CallToolRequest("getComponentVersion",
        Map.of("packageUrls", List.of(PURL)), null);

    underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(captured[0]).isEqualTo(GuideChannel.MCP);
    // cleared after the call (defaults to API when unset)
    assertThat(GuideChannelContext.getOrDefault()).isEqualTo(GuideChannel.API);
  }

  @Test
  public void callTool_componentVersion_returnsBatchArray() throws Exception {
    SearchFunction fn = purl -> COMPONENT_JSON;
    CallToolRequest request = new CallToolRequest("getComponentVersion",
        Map.of("packageUrls", List.of(PURL)), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(result.isError()).isFalse();
    JsonNode root = parseResult(result);
    assertThat(root.isArray()).isTrue();
    assertThat(root).hasSize(1);

    JsonNode item = root.get(0);
    assertThat(item.get("packageUrl").asText()).isEqualTo(PURL);
    assertThat(item.get("success").asBoolean()).isTrue();

    JsonNode data = item.get("data");
    assertThat(data.get("version").asText()).isEqualTo("1.0.0");
    assertThat(data.get("endOfLife").asBoolean()).isFalse();
    assertThat(data.get("malicious").asBoolean()).isFalse();
    assertThat(data.get("licenses").get(0).asText()).isEqualTo("Apache-2.0");
    assertThat(data.get("catalogDate").asLong()).isEqualTo(1705312800000L);
    assertThat(data.get("vulnerabilities").get("cves")).hasSize(1);
    assertThat(data.get("vulnerabilities").get("cves").get(0).get("id").asText()).isEqualTo("CVE-2024-1234");
  }

  @Test
  public void callTool_noPolicyWithoutApplicationId_omitsPolicyCompliance() throws Exception {
    SearchFunction fn = purl -> COMPONENT_JSON;
    CallToolRequest request = new CallToolRequest("getComponentVersion",
        Map.of("packageUrls", List.of(PURL)), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(result.isError()).isFalse();
    JsonNode root = parseResult(result);
    assertThat(root.get(0).get("data").has("policyCompliance")).isFalse();
  }

  @Test
  public void callTool_withPolicy_includesPolicyComplianceInData() throws Exception {
    McpPolicyCompliance policy =
        new McpPolicyCompliance(true, GuidePolicyComplianceLevel.PASS, "develop", "app-123", null, List.of());
    when(policyAnnotator.evaluatePolicies(List.of(PURL_COMPLETED), "app-123", null))
        .thenReturn(Map.of(PURL_COMPLETED, policy));

    SearchFunction fn = purl -> COMPONENT_JSON;
    CallToolRequest request = new CallToolRequest("getComponentVersion",
        Map.of("packageUrls", List.of(PURL), "applicationId", "app-123"), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    JsonNode root = parseResult(result);
    JsonNode policyNode = root.get(0).get("data").get("policyCompliance");
    assertThat(policyNode.get("compliant").asBoolean()).isTrue();
    assertThat(policyNode.get("ownerId").asText()).isEqualTo("app-123");
    assertThat(policyNode.get("stage").asText()).isEqualTo("develop");
    verify(policyAnnotator).evaluatePolicies(List.of(PURL_COMPLETED), "app-123", null);
  }

  @Test
  public void callTool_applicationIdFromHeader_fallback() throws Exception {
    McpPolicyCompliance policy =
        new McpPolicyCompliance(true, GuidePolicyComplianceLevel.PASS, "build", "header-app", null, List.of());
    when(policyAnnotator.evaluatePolicies(List.of(PURL_COMPLETED), "header-app", "build"))
        .thenReturn(Map.of(PURL_COMPLETED, policy));

    SearchFunction fn = purl -> COMPONENT_JSON;
    CallToolRequest request = new CallToolRequest("getComponentVersion",
        Map.of("packageUrls", List.of(PURL)), null);
    McpTransportContext ctx = McpTransportContext.create(Map.of(
        "X-Application-Id", "header-app",
        "X-Stage", "build"));

    CallToolResult result = underTest.callTool(ctx, request, fn, ToolType.COMPONENT_VERSION);

    JsonNode root = parseResult(result);
    assertThat(root.get(0).get("data").get("policyCompliance").get("ownerId").asText())
        .isEqualTo("header-app");
    verify(policyAnnotator).evaluatePolicies(List.of(PURL_COMPLETED), "header-app", "build");
  }

  @Test
  public void callTool_toolArgOverridesHeader() {
    McpPolicyCompliance policy =
        new McpPolicyCompliance(true, GuidePolicyComplianceLevel.PASS, "release", "arg-app", null, List.of());
    when(policyAnnotator.evaluatePolicies(List.of(PURL_COMPLETED), "arg-app", "release"))
        .thenReturn(Map.of(PURL_COMPLETED, policy));

    SearchFunction fn = purl -> COMPONENT_JSON;
    Map<String, Object> args = new HashMap<>();
    args.put("packageUrls", List.of(PURL));
    args.put("applicationId", "arg-app");
    args.put("stage", "release");
    CallToolRequest request = new CallToolRequest("getComponentVersion", args, null);
    McpTransportContext ctx = McpTransportContext.create(Map.of(
        "X-Application-Id", "header-app",
        "X-Stage", "build"));

    underTest.callTool(ctx, request, fn, ToolType.COMPONENT_VERSION);

    verify(policyAnnotator).evaluatePolicies(List.of(PURL_COMPLETED), "arg-app", "release");
  }

  @Test
  public void callTool_policyEvaluationFailure_stillReturnsData() throws Exception {
    when(policyAnnotator.evaluatePolicies(any(), any(), any()))
        .thenThrow(new RuntimeException("evaluation error"));

    SearchFunction fn = purl -> COMPONENT_JSON;
    CallToolRequest request = new CallToolRequest("getComponentVersion",
        Map.of("packageUrls", List.of(PURL)), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(result.isError()).isFalse();
    JsonNode root = parseResult(result);
    assertThat(root.get(0).get("success").asBoolean()).isTrue();
    assertThat(root.get(0).get("data").get("version").asText()).isEqualTo("1.0.0");
    assertThat(root.get(0).get("data").has("policyCompliance")).isFalse();
  }

  @Test
  public void callTool_withViolations_includesViolationDetails() throws Exception {
    McpPolicyCompliance policy = new McpPolicyCompliance(
        false, GuidePolicyComplianceLevel.FAIL, "develop", "app-123",
        new GuidePolicyComplianceSummary(8, "fail", 1, 0,
            Map.of("SECURITY", 1, "LICENSE", 0, "QUALITY", 0, "OTHER", 0)),
        List.of(new McpPolicyViolation(
            "Security-High", 8, List.of("fail"), false, null,
            List.of(new McpPolicyConstraint("High risk CVSS score", List.of("Found CVE-2024-1234"))))));
    when(policyAnnotator.evaluatePolicies(List.of(PURL_COMPLETED), "app-123", null))
        .thenReturn(Map.of(PURL_COMPLETED, policy));

    SearchFunction fn = purl -> COMPONENT_JSON;
    CallToolRequest request = new CallToolRequest("getComponentVersion",
        Map.of("packageUrls", List.of(PURL), "applicationId", "app-123"), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    JsonNode root = parseResult(result);
    JsonNode violations = root.get(0).get("data").get("policyCompliance").get("violations");
    assertThat(violations).hasSize(1);
    assertThat(violations.get(0).get("policyName").asText()).isEqualTo("Security-High");
    assertThat(violations.get(0).get("actions").get(0).asText()).isEqualTo("fail");
    JsonNode constraint = violations.get(0).get("constraints").get(0);
    assertThat(constraint.get("constraintName").asText()).isEqualTo("High risk CVSS score");
    assertThat(constraint.get("reasons").get(0).asText()).isEqualTo("Found CVE-2024-1234");
  }

  @Test
  public void callTool_recommendations_returnsBatchArray() throws Exception {
    SearchFunction fn = purl -> RECOMMENDATION_JSON;
    CallToolRequest request = new CallToolRequest("getRecommendedComponentVersions",
        Map.of("packageUrls", List.of(PURL)), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.RECOMMENDATIONS);

    assertThat(result.isError()).isFalse();
    JsonNode root = parseResult(result);
    assertThat(root.isArray()).isTrue();
    assertThat(root).hasSize(1);

    JsonNode item = root.get(0);
    assertThat(item.get("packageUrl").asText()).isEqualTo(PURL);
    assertThat(item.get("success").asBoolean()).isTrue();
    assertThat(item.get("outcome").asText()).isEqualTo("FOUND_RECOMMENDATIONS");
    assertThat(item.get("fromVersion").get("version").asText()).isEqualTo("1.0.0");
    assertThat(item.get("toVersions")).hasSize(1);
    assertThat(item.get("toVersions").get(0).get("version").asText()).isEqualTo("2.0.0");
  }

  @Test
  public void callTool_multiplePurls_returnsCombinedArray() throws Exception {
    String purl2 = "pkg:maven/org.example/other@2.0.0";
    SearchFunction fn = purl -> COMPONENT_JSON;
    CallToolRequest request = new CallToolRequest("getComponentVersion",
        Map.of("packageUrls", List.of(PURL, purl2)), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(result.isError()).isFalse();
    JsonNode root = parseResult(result);
    assertThat(root).hasSize(2);
    assertThat(root.get(0).get("packageUrl").asText()).isEqualTo(PURL);
    assertThat(root.get(1).get("packageUrl").asText()).isEqualTo(purl2);
  }

  @Test
  public void callTool_nullArguments() {
    SearchFunction fn = purl -> "unused";
    CallToolRequest request = new CallToolRequest("test", null, null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(result.isError()).isTrue();
    assertThat(((TextContent) result.content().get(0)).text()).contains("packageUrls parameter is required");
  }

  @Test
  public void callTool_missingPackageUrls() {
    SearchFunction fn = purl -> "unused";
    CallToolRequest request = new CallToolRequest("test", Map.of(), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(result.isError()).isTrue();
    assertThat(((TextContent) result.content().get(0)).text()).contains("packageUrls parameter is required");
  }

  @Test
  public void callTool_emptyPackageUrls() {
    SearchFunction fn = purl -> "unused";
    CallToolRequest request = new CallToolRequest("test", Map.of("packageUrls", List.of()), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(result.isError()).isTrue();
    assertThat(((TextContent) result.content().get(0)).text()).contains("packageUrls parameter is required");
  }

  @Test
  public void callTool_invalidPackageUrlEntries_returnsFailureItems() throws Exception {
    SearchFunction fn = purl -> COMPONENT_JSON;
    List<Object> mixedList = new ArrayList<>();
    mixedList.add(PURL);
    mixedList.add("");
    mixedList.add(123);
    mixedList.add(null);
    Map<String, Object> args = new HashMap<>();
    args.put("packageUrls", mixedList);
    CallToolRequest request = new CallToolRequest("test", args, null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(result.isError()).isFalse();
    JsonNode root = parseResult(result);
    assertThat(root).hasSize(4);
    assertThat(root.get(0).get("success").asBoolean()).isTrue();
    assertThat(root.get(1).get("success").asBoolean()).isFalse();
    assertThat(root.get(1).get("error").asText()).contains("Invalid package URL");
    assertThat(root.get(2).get("success").asBoolean()).isFalse();
    assertThat(root.get(3).get("success").asBoolean()).isFalse();
  }

  @Test
  public void callTool_invalidPackageUrlEntries_recommendations_returnsCorrectStructure() throws Exception {
    SearchFunction fn = purl -> RECOMMENDATION_JSON;
    List<Object> mixedList = new ArrayList<>();
    mixedList.add("");
    mixedList.add(PURL);
    Map<String, Object> args = new HashMap<>();
    args.put("packageUrls", mixedList);
    CallToolRequest request = new CallToolRequest("test", args, null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.RECOMMENDATIONS);

    assertThat(result.isError()).isFalse();
    JsonNode root = parseResult(result);
    assertThat(root).hasSize(2);
    JsonNode failureItem = root.get(0);
    assertThat(failureItem.get("success").asBoolean()).isFalse();
    assertThat(failureItem.get("error").asText()).contains("Invalid package URL");
    assertThat(failureItem.has("outcome")).isFalse();
    JsonNode successItem = root.get(1);
    assertThat(successItem.get("success").asBoolean()).isTrue();
    assertThat(successItem.get("outcome").asText()).isEqualTo("FOUND_RECOMMENDATIONS");
  }

  @Test
  public void callTool_tooManyPackageUrls() {
    SearchFunction fn = purl -> "unused";
    List<String> tooMany = new ArrayList<>();
    for (int i = 0; i < 21; i++) {
      tooMany.add("pkg:maven/org.example/lib" + i + "@1.0.0");
    }
    CallToolRequest request = new CallToolRequest("test", Map.of("packageUrls", tooMany), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(result.isError()).isTrue();
    assertThat(((TextContent) result.content().get(0)).text()).contains("Too many package URLs");
  }

  @Test
  public void callTool_nullSearchResult_returnsFailureItem() throws Exception {
    SearchFunction fn = purl -> null;
    CallToolRequest request = new CallToolRequest("test",
        Map.of("packageUrls", List.of(PURL)), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(result.isError()).isFalse();
    JsonNode root = parseResult(result);
    assertThat(root.get(0).get("success").asBoolean()).isFalse();
    assertThat(root.get(0).get("error").asText()).contains("no data");
  }

  @Test
  public void callTool_searchFunctionException_returnsFailureItemWithSanitizedMessage() throws Exception {
    SearchFunction fn = purl -> {
      throw new RuntimeException("something broke");
    };
    CallToolRequest request = new CallToolRequest("test",
        Map.of("packageUrls", List.of(PURL)), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(result.isError()).isFalse();
    JsonNode root = parseResult(result);
    assertThat(root.get(0).get("success").asBoolean()).isFalse();
    assertThat(root.get(0).get("error").asText()).isEqualTo("Component lookup failed");
  }

  @Test
  public void callTool_searchFunctionThrowsGuideNotFoundException_returnsNoDataMessage() throws Exception {
    // SearchApiClient throws GuideNotFoundException (not null) when the upstream HDS returns 404.
    // McpServletProvider.processOnePurl distinguishes this case from generic exceptions:
    // GuideNotFoundException → "Search API returned no data" (the original null-returning UX),
    // anything else → "Component lookup failed" (above test). Locks in that routing.
    SearchFunction fn = purl -> {
      throw new GuideNotFoundException("Component not found: " + purl);
    };
    CallToolRequest request = new CallToolRequest("test",
        Map.of("packageUrls", List.of(PURL)), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(result.isError()).isFalse();
    JsonNode root = parseResult(result);
    assertThat(root.get(0).get("success").asBoolean()).isFalse();
    assertThat(root.get(0).get("error").asText()).contains("no data");
  }

  @Test
  public void callTool_nullMessageException_returnsFailureItem() throws Exception {
    SearchFunction fn = purl -> {
      throw new RuntimeException((String) null);
    };
    CallToolRequest request = new CallToolRequest("test",
        Map.of("packageUrls", List.of(PURL)), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.COMPONENT_VERSION);

    assertThat(result.isError()).isFalse();
    JsonNode root = parseResult(result);
    assertThat(root.get(0).get("success").asBoolean()).isFalse();
    assertThat(root.get(0).get("error").asText()).isEqualTo("Component lookup failed");
  }

  @Test
  public void callTool_latestVersion_returnsBatchArray() throws Exception {
    String latestJson = """
        {"version":"2.0.0","licenses":[{"licenseName":"MIT","licenseThreatLevel":0}],\
        "publishedDate":"2025-06-01T00:00:00Z","isMalware":false,"components":[]}""";
    SearchFunction fn = purl -> latestJson;
    CallToolRequest request = new CallToolRequest("getLatestComponentVersion",
        Map.of("packageUrls", List.of(PURL)), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn, ToolType.LATEST_VERSION);

    assertThat(result.isError()).isFalse();
    JsonNode root = parseResult(result);
    assertThat(root.isArray()).isTrue();
    assertThat(root).hasSize(1);

    JsonNode data = root.get(0).get("data");
    assertThat(data.get("version").asText()).isEqualTo("2.0.0");
    assertThat(data.get("licenses").get(0).asText()).isEqualTo("MIT");
    assertThat(data.has("vulnerabilities")).isFalse();
  }

  @Test
  public void initialize_registersToolsWithSearchClient() {
    assertThat(underTest.getServlet()).isNotNull();
  }

  @Test
  public void initialize_throwsOnDoubleInit() {
    assertThatThrownBy(() -> underTest.initialize(searchApiClient, policyAnnotator))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already initialized");
  }

  @Test
  public void getServlet_throwsBeforeInit() {
    McpServletProvider uninitializedProvider = new McpServletProvider();
    assertThatThrownBy(uninitializedProvider::getServlet)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("initialize()");
  }

  private static JsonNode parseResult(CallToolResult result) throws Exception {
    String text = ((TextContent) result.content().get(0)).text();
    return mapper.readTree(text);
  }
}
