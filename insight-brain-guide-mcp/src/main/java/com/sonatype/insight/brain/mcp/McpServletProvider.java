/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sonatype.insight.brain.mcp.model.McpPolicyContext;
import com.sonatype.insight.brain.mcp.policy.PolicyAnnotator;
import com.sonatype.insight.brain.mcp.search.SearchApiClient;
import com.sonatype.insight.brain.mcp.tools.McpResponseFormatter;

import com.google.common.annotations.VisibleForTesting;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.Servlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the MCP servlet, transport, and tool registrations.
 * The transport and server are created lazily via {@link #initialize(SearchApiClient, PolicyAnnotator)} to avoid
 * allocating resources when the GUIDE_MCP feature flag is disabled.
 */
@Named
@Singleton
public class McpServletProvider
{
  private static final Logger log = LoggerFactory.getLogger(McpServletProvider.class);

  private HttpServletStatelessServerTransport transport;

  private PolicyAnnotator policyAnnotator;

  @Inject
  public McpServletProvider() {
    // No-op: transport is created lazily in initialize()
  }

  /**
   * Creates the MCP transport and registers tools backed by the given search client and policy annotator.
   * Must be called exactly once before {@link #getServlet()}.
   *
   * @throws IllegalStateException if called more than once
   */
  public void initialize(SearchApiClient searchApiClient, PolicyAnnotator policyAnnotator) {
    Objects.requireNonNull(searchApiClient, "searchApiClient must not be null");
    Objects.requireNonNull(policyAnnotator, "policyAnnotator must not be null");
    if (transport != null) {
      throw new IllegalStateException("MCP transport already initialized");
    }
    this.policyAnnotator = policyAnnotator;
    this.transport = HttpServletStatelessServerTransport.builder()
        .messageEndpoint("/mcp")
        .contextExtractor(req -> {
          Map<String, Object> headers = new HashMap<>();
          headers.put("X-Application-Id", nullToEmpty(req.getHeader("X-Application-Id")));
          headers.put("X-Stage", nullToEmpty(req.getHeader("X-Stage")));
          return McpTransportContext.create(headers);
        })
        .build();

    McpServer.sync(transport)
        .serverInfo("iq-mcp", "1.0.0")
        .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
        .jsonSchemaValidator(noOpJsonSchemaValidator())
        .tools(
            tool("getComponentVersion", "Get component details by PURL",
                searchApiClient::getComponentByPurl),
            tool("getLatestComponentVersion", "Get latest version of a component",
                searchApiClient::getLatestComponentVersion),
            tool("getRecommendedComponentVersions", "Get upgrade recommendations",
                searchApiClient::getRecommendations))
        .build();
  }

  /**
   * @throws IllegalStateException if {@link #initialize(SearchApiClient, PolicyAnnotator)} has not been called
   */
  public Servlet getServlet() {
    if (transport == null) {
      throw new IllegalStateException("initialize() must be called first");
    }
    return transport;
  }

  private McpStatelessServerFeatures.SyncToolSpecification tool(
      String name,
      String description,
      SearchFunction fn)
  {
    return McpStatelessServerFeatures.SyncToolSpecification.builder()
        .tool(Tool.builder()
            .name(name)
            .description(description)
            .inputSchema(toolSchema())
            .build())
        .callHandler((ctx, request) -> callTool(ctx, request, fn))
        .build();
  }

  @VisibleForTesting
  CallToolResult callTool(McpTransportContext ctx, CallToolRequest request, SearchFunction fn) {
    Map<String, Object> arguments = request.arguments();
    if (arguments == null) {
      return errorResult("purl parameter is required");
    }
    Object purlValue = arguments.get("purl");
    if (!(purlValue instanceof String purl) || purl.isBlank()) {
      return errorResult("purl parameter is required");
    }
    try {
      String result = fn.call(purl);
      if (result == null) {
        return errorResult("Search API returned no data");
      }

      String applicationId = resolveParam(arguments, ctx, "applicationId", "X-Application-Id");
      String stage = resolveParam(arguments, ctx, "stage", "X-Stage");

      McpPolicyContext policyContext = null;
      try {
        policyContext = policyAnnotator.evaluatePolicy(purl, applicationId, stage);
      }
      catch (Exception e) {
        log.warn("Policy evaluation failed for purl={}, app={}: {}", purl, applicationId, e.getMessage());
      }

      String formatted = McpResponseFormatter.format(result, policyContext);
      return CallToolResult.builder()
          .content(List.of(new TextContent(formatted)))
          .build();
    }
    catch (Exception e) {
      log.warn("Unexpected error calling search tool for purl {}", purl, e);
      return errorResult("Component lookup failed — check server logs for details");
    }
  }

  @FunctionalInterface
  interface SearchFunction
  {
    String call(String purl);
  }

  private static String resolveParam(
      Map<String, Object> arguments,
      McpTransportContext ctx,
      String paramName,
      String headerName)
  {
    Object argValue = arguments.get(paramName);
    if (argValue instanceof String s && !s.isBlank()) {
      return s;
    }
    Object headerValue = ctx.get(headerName);
    if (headerValue instanceof String s && !s.isBlank()) {
      return s;
    }
    return null;
  }

  private static String nullToEmpty(String value) {
    return value != null ? value : "";
  }

  private static JsonSchema toolSchema() {
    return new JsonSchema(
        "object",
        Map.of(
            "purl", Map.of("type", "string",
                "description", "Package URL (e.g., pkg:maven/org.example/lib@1.0.0)"),
            "applicationId", Map.of("type", "string",
                "description", "IQ application ID for policy evaluation (optional)"),
            "stage", Map.of("type", "string",
                "description", "Stage label for response context (optional, defaults to 'develop'). "
                    + "Does not control which stage is evaluated — evaluation uses the application's configured default.")),
        List.of("purl"),
        null, null, null);
  }

  private static CallToolResult errorResult(String message) {
    return CallToolResult.builder()
        .content(List.of(new TextContent(message)))
        .isError(true)
        .build();
  }

  /**
   * Returns a no-op JSON schema validator that skips all validation.
   *
   * <p>
   * This is required to work around a dependency conflict between the MCP Java SDK and
   * cyclonedx-core-java. The MCP SDK's default {@code DefaultJsonSchemaValidator} (from
   * mcp-json-jackson2) requires json-schema-validator 2.x, but cyclonedx-core-java 12.1.0
   * requires json-schema-validator 1.5.9. These versions are binary-incompatible (2.x removed
   * classes like {@code SchemaMapper} that 1.5.x uses, and added classes like {@code Dialects}
   * that 1.5.x lacks).
   *
   * <p>
   * We exclude json-schema-validator from mcp-json-jackson2 in the POM so that cyclonedx
   * gets its required 1.5.9 version, and provide this no-op validator to avoid the MCP SDK
   * attempting to load the missing 2.x classes.
   *
   * <p>
   * <b>TODO: Remove this workaround</b> when cyclonedx-core-java 13.0.0 is released, which
   * upgrades to json-schema-validator 2.x (see
   * <a href="https://github.com/CycloneDX/cyclonedx-core-java/pull/802">cyclonedx-core-java#802</a>).
   * At that point, remove the json-schema-validator exclusion from the POM and delete this method
   * so the MCP SDK uses its default validator.
   */
  private static JsonSchemaValidator noOpJsonSchemaValidator() {
    return (Map<String, Object> schema, Object structuredContent) -> JsonSchemaValidator.ValidationResponse.asValid(
        structuredContent != null ? structuredContent.toString() : null);
  }
}
