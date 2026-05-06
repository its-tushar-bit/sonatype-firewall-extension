/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.mcp.McpServletProvider.SearchFunction;
import com.sonatype.insight.brain.mcp.model.McpPolicyContext;
import com.sonatype.insight.brain.mcp.model.McpPolicyViolation;
import com.sonatype.insight.brain.mcp.model.McpStageResult;
import com.sonatype.insight.brain.mcp.policy.PolicyAnnotator;
import com.sonatype.insight.brain.mcp.search.SearchApiClient;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
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

  @Mock
  private SearchApiClient searchApiClient;

  @Mock
  private PolicyAnnotator policyAnnotator;

  private McpServletProvider underTest;

  @Before
  public void setUp() {
    underTest = new McpServletProvider();
    underTest.initialize(searchApiClient, policyAnnotator);
  }

  @Test
  public void callTool_success_includesMetadata() {
    SearchFunction fn = purl -> "{\"data\":\"ok\"}";
    CallToolRequest request = new CallToolRequest("test", Map.of("purl", PURL), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn);

    assertThat(result.isError()).isFalse();
    assertThat(result.content()).hasSize(1);
    String text = ((TextContent) result.content().get(0)).text();
    assertThat(text).contains("\"metadata\"");
    assertThat(text).contains("\"source\":\"search-server\"");
  }

  @Test
  public void callTool_withApplicationIdToolArg_evaluatesPolicy() {
    McpPolicyContext policyContext = new McpPolicyContext(
        "app-123", "develop",
        new McpStageResult("develop", true, "None", 0),
        null, List.of());
    when(policyAnnotator.evaluatePolicy(PURL, "app-123", null)).thenReturn(policyContext);

    SearchFunction fn = purl -> "{\"data\":\"ok\"}";
    CallToolRequest request = new CallToolRequest("test",
        Map.of("purl", PURL, "applicationId", "app-123"), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn);

    assertThat(result.isError()).isFalse();
    String text = ((TextContent) result.content().get(0)).text();
    assertThat(text).contains("\"policy\"");
    assertThat(text).contains("\"applicationId\":\"app-123\"");
    verify(policyAnnotator).evaluatePolicy(PURL, "app-123", null);
  }

  @Test
  public void callTool_applicationIdFromHeader_fallback() {
    McpPolicyContext policyContext = new McpPolicyContext(
        "header-app", "build",
        new McpStageResult("build", true, "None", 0),
        null, List.of());
    when(policyAnnotator.evaluatePolicy(PURL, "header-app", "build")).thenReturn(policyContext);

    SearchFunction fn = purl -> "{\"data\":\"ok\"}";
    CallToolRequest request = new CallToolRequest("test", Map.of("purl", PURL), null);
    McpTransportContext ctx = McpTransportContext.create(Map.of(
        "X-Application-Id", "header-app",
        "X-Stage", "build"));

    CallToolResult result = underTest.callTool(ctx, request, fn);

    assertThat(result.isError()).isFalse();
    String text = ((TextContent) result.content().get(0)).text();
    assertThat(text).contains("\"applicationId\":\"header-app\"");
    verify(policyAnnotator).evaluatePolicy(PURL, "header-app", "build");
  }

  @Test
  public void callTool_toolArgOverridesHeader() {
    McpPolicyContext policyContext = new McpPolicyContext(
        "arg-app", "release",
        new McpStageResult("release", true, "None", 0),
        null, List.of());
    when(policyAnnotator.evaluatePolicy(PURL, "arg-app", "release")).thenReturn(policyContext);

    SearchFunction fn = purl -> "{\"data\":\"ok\"}";
    Map<String, Object> args = new HashMap<>();
    args.put("purl", PURL);
    args.put("applicationId", "arg-app");
    args.put("stage", "release");
    CallToolRequest request = new CallToolRequest("test", args, null);
    McpTransportContext ctx = McpTransportContext.create(Map.of(
        "X-Application-Id", "header-app",
        "X-Stage", "build"));

    CallToolResult result = underTest.callTool(ctx, request, fn);

    verify(policyAnnotator).evaluatePolicy(PURL, "arg-app", "release");
  }

  @Test
  public void callTool_noPolicyWithoutApplicationId() {
    SearchFunction fn = purl -> "{\"data\":\"ok\"}";
    CallToolRequest request = new CallToolRequest("test", Map.of("purl", PURL), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn);

    assertThat(result.isError()).isFalse();
    String text = ((TextContent) result.content().get(0)).text();
    assertThat(text).contains("\"metadata\"");
    assertThat(text).doesNotContain("\"policy\":{\"applicationId\"");
    verify(policyAnnotator).evaluatePolicy(PURL, null, null);
  }

  @Test
  public void callTool_policyEvaluationFailure_stillReturnsSearchData() {
    when(policyAnnotator.evaluatePolicy(any(), any(), any()))
        .thenThrow(new RuntimeException("evaluation error"));

    SearchFunction fn = purl -> "{\"data\":\"ok\"}";
    CallToolRequest request = new CallToolRequest("test", Map.of("purl", PURL), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn);

    assertThat(result.isError()).isFalse();
    String text = ((TextContent) result.content().get(0)).text();
    assertThat(text).contains("\"data\":\"ok\"");
    assertThat(text).contains("\"metadata\"");
  }

  @Test
  public void callTool_withPolicyViolations_includesViolationDetails() {
    McpPolicyViolation violation = new McpPolicyViolation(
        "Security-High", 8, "Fail", List.of("CVE-2021-44228"), false);
    McpPolicyContext policyContext = new McpPolicyContext(
        "app-123", "develop",
        new McpStageResult("develop", false, "Fail", 1),
        null, List.of(violation));
    when(policyAnnotator.evaluatePolicy(PURL, "app-123", null)).thenReturn(policyContext);

    SearchFunction fn = purl -> "{\"data\":\"ok\"}";
    CallToolRequest request = new CallToolRequest("test",
        Map.of("purl", PURL, "applicationId", "app-123"), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn);

    String text = ((TextContent) result.content().get(0)).text();
    assertThat(text).contains("\"policyName\":\"Security-High\"");
    assertThat(text).contains("\"actionType\":\"Fail\"");
  }

  @Test
  public void callTool_nullArguments() {
    SearchFunction fn = purl -> "unused";
    CallToolRequest request = new CallToolRequest("test", null, null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn);

    assertThat(result.isError()).isTrue();
    assertThat(((TextContent) result.content().get(0)).text()).contains("purl parameter is required");
  }

  @Test
  public void callTool_missingPurl() {
    SearchFunction fn = purl -> "unused";
    CallToolRequest request = new CallToolRequest("test", Map.of(), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn);

    assertThat(result.isError()).isTrue();
    assertThat(((TextContent) result.content().get(0)).text()).contains("purl parameter is required");
  }

  @Test
  public void callTool_blankPurl() {
    SearchFunction fn = purl -> "unused";
    CallToolRequest request = new CallToolRequest("test", Map.of("purl", "  "), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn);

    assertThat(result.isError()).isTrue();
    assertThat(((TextContent) result.content().get(0)).text()).contains("purl parameter is required");
  }

  @Test
  public void callTool_nullSearchResult() {
    SearchFunction fn = purl -> null;
    CallToolRequest request = new CallToolRequest("test", Map.of("purl", PURL), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn);

    assertThat(result.isError()).isTrue();
    assertThat(((TextContent) result.content().get(0)).text()).contains("Search API returned no data");
  }

  @Test
  public void callTool_runtimeException() {
    SearchFunction fn = purl -> {
      throw new RuntimeException("something broke");
    };
    CallToolRequest request = new CallToolRequest("test", Map.of("purl", PURL), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn);

    assertThat(result.isError()).isTrue();
    assertThat(((TextContent) result.content().get(0)).text()).contains("Component lookup failed");
  }

  @Test
  public void callTool_exceptionWithNullMessage() {
    SearchFunction fn = purl -> {
      throw new NullPointerException();
    };
    CallToolRequest request = new CallToolRequest("test", Map.of("purl", PURL), null);

    CallToolResult result = underTest.callTool(McpTransportContext.EMPTY, request, fn);

    assertThat(result.isError()).isTrue();
    assertThat(((TextContent) result.content().get(0)).text()).contains("Component lookup failed");
  }

  @Test
  public void initialize_registersToolsWithSearchClient() {
    // setUp already called initialize — verify transport was created
    assertThat(underTest.getServlet()).isNotNull();
  }

  @Test
  public void initialize_throwsOnDoubleInit() {
    // setUp already called initialize once
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
}
