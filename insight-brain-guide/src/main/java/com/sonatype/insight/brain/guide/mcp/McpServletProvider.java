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
import java.util.Objects;

import com.sonatype.insight.brain.guide.mcp.model.McpBatchItem;
import com.sonatype.insight.brain.guide.mcp.model.McpPolicyCompliance;
import com.sonatype.insight.brain.guide.mcp.model.McpRecommendationItem;
import com.sonatype.insight.brain.guide.api.error.GuideNotFoundException;
import com.sonatype.insight.brain.guide.mcp.policy.McpStageResolver;
import com.sonatype.insight.brain.guide.mcp.policy.McpPolicyAnnotator;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.guide.mcp.tools.McpResponseFormatter;
import com.sonatype.insight.brain.guide.mcp.util.McpPurlCompleter;
import com.sonatype.insight.brain.guide.telemetry.GuideChannel;
import com.sonatype.insight.brain.guide.telemetry.GuideChannelContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * The transport and server are always created at startup; access is gated by {@code McpLicenseFilter}.
 */
@Named
@Singleton
public class McpServletProvider
{
  private static final Logger log = LoggerFactory.getLogger(McpServletProvider.class);

  private static final int MAX_BATCH_SIZE = 20;

  private static final ObjectMapper mapper = new ObjectMapper();

  private HttpServletStatelessServerTransport transport;

  private McpPolicyAnnotator policyAnnotator;

  @Inject
  public McpServletProvider() {
  }

  /**
   * Creates the MCP transport and registers tools backed by the given search client and policy annotator.
   * Must be called exactly once before {@link #getServlet()}.
   *
   * @throws IllegalStateException if called more than once
   */
  public void initialize(SearchApiClient searchApiClient, McpPolicyAnnotator policyAnnotator) {
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
        // Run tool callbacks on the servlet request thread so the Shiro Subject
        // (and other request-scoped ThreadLocals like MDC and tenant context)
        // remain bound when downstream services like ApiComponentEvaluationServiceV2
        // are invoked. Without this, the SDK offloads sync tools to
        // Schedulers.boundedElastic() and Shiro's ThreadContext is empty there,
        // causing "Anonymous access forbidden" from AuthorizeMethodInterceptor.
        .immediateExecution(true)
        .tools(
            tool("getComponentVersion",
                "Returns detailed analysis of a specific dependency or multiple dependencies with metadata about "
                    + "quality, license and security. Dependencies can be referred to as packages, components or "
                    + "libraries. They can be transitive (brought in by other dependencies) or direct (explicitly "
                    + "added to the project).",
                searchApiClient::getComponentByPurl, ToolType.COMPONENT_VERSION),
            tool("getLatestComponentVersion",
                "Returns the latest version of a dependency or multiple dependencies with quality, license and "
                    + "security data. Dependencies can be referred to as packages, components or libraries. They "
                    + "can be transitive (brought in by other dependencies) or direct (explicitly added to the "
                    + "project).",
                searchApiClient::getLatestComponentVersion, ToolType.LATEST_VERSION),
            tool("getRecommendedComponentVersions",
                "Returns top dependency version recommendations ranked by Developer Trust Score with security, "
                    + "licensing, and quality analysis. Developer Trust Score is a measure of quality, security, "
                    + "licensing, and maintainability. Use this when selecting a new component to add to a project "
                    + "(without version) or when upgrading an existing component (with version). Dependencies can be "
                    + "referred to as packages, components or libraries. They can be transitive (brought in by other "
                    + "dependencies) or direct (explicitly added to the project).",
                purl -> serializeOrNull(searchApiClient.getRecommendations(purl)), ToolType.RECOMMENDATIONS))
        .build();
  }

  /**
   * @throws IllegalStateException if {@link #initialize(SearchApiClient, McpPolicyAnnotator)} has not been called
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
      SearchFunction fn,
      ToolType toolType)
  {
    return McpStatelessServerFeatures.SyncToolSpecification.builder()
        .tool(Tool.builder()
            .name(name)
            .description(description)
            .inputSchema(toolSchema())
            .build())
        .callHandler((ctx, request) -> callTool(ctx, request, fn, toolType))
        .build();
  }

  @VisibleForTesting
  CallToolResult callTool(McpTransportContext ctx, CallToolRequest request, SearchFunction fn, ToolType toolType) {
    GuideChannelContext.set(GuideChannel.MCP);
    try {
      Map<String, Object> arguments = request.arguments();
      if (arguments == null) {
        return errorResult("packageUrls parameter is required");
      }

      Object packageUrlsValue = arguments.get("packageUrls");
      if (!(packageUrlsValue instanceof List<?> rawList) || rawList.isEmpty()) {
        return errorResult("packageUrls parameter is required and cannot be empty");
      }
      if (rawList.size() > MAX_BATCH_SIZE) {
        return errorResult(String.format(
            "Too many package URLs provided. Maximum allowed is %d, but received %d",
            MAX_BATCH_SIZE, rawList.size()));
      }

      String applicationId = resolveParam(arguments, ctx, "applicationId", "X-Application-Id");
      String stage = resolveParam(arguments, ctx, "stage", "X-Stage");

      // Up-front stage validation. Stage is a per-call argument; rejecting it as a top-level
      // error envelope is more useful than emitting a per-PURL failure for every entry.
      if (stage != null && !stage.isBlank()) {
        try {
          McpStageResolver.resolve(stage);
        }
        catch (IllegalArgumentException e) {
          return errorResult(e.getMessage());
        }
      }

      try {
        // Evaluate policy for the whole batch in a single call (one owner resolution + HDS fetch +
        // Drools session for all PURLs), keyed by completed PURL. RECOMMENDATIONS carries no policy.
        // A failure — including an authorization denial — soft-degrades to "no policy" rather than
        // failing the lookup, matching the per-surface soft-fail behavior.
        Map<String, McpPolicyCompliance> policyByPurl = Map.of();
        if (toolType != ToolType.RECOMMENDATIONS) {
          List<String> purlsToEvaluate = rawList.stream()
              .filter(item -> item instanceof String s && !s.isBlank())
              .map(item -> McpPurlCompleter.complete((String) item))
              .toList();
          try {
            policyByPurl = policyAnnotator.evaluatePolicies(purlsToEvaluate, applicationId, stage);
          }
          catch (Exception e) {
            log.warn("Policy evaluation failed for batch (app={}): {}", applicationId, e.getMessage());
          }
        }

        List<String> results = new ArrayList<>();
        for (Object item : rawList) {
          if (!(item instanceof String s) || s.isBlank()) {
            String invalid = item != null ? item.toString() : "null";
            results.add(formatError(invalid, toolType, "Invalid package URL: must be a non-blank string"));
          }
          else {
            results.add(processOnePurl(s, fn, toolType, policyByPurl.get(McpPurlCompleter.complete(s))));
          }
        }

        String merged = mergeJsonArrays(results);
        return CallToolResult.builder()
            .content(List.of(new TextContent(merged)))
            .build();
      }
      catch (Exception e) {
        log.warn("Unexpected error calling search tool", e);
        return errorResult("Component lookup failed — check server logs for details");
      }
    }
    finally {
      GuideChannelContext.clear();
    }
  }

  private String processOnePurl(
      String purl,
      SearchFunction fn,
      ToolType toolType,
      McpPolicyCompliance policyResult)
  {
    try {
      String rawJson;
      try {
        rawJson = fn.call(purl);
      }
      catch (GuideNotFoundException e) {
        // SearchApiClient now throws on upstream 404; for the MCP path that maps to "no data
        // for this purl" rather than an error, preserving the prior null-returning behavior.
        return formatError(purl, toolType, "Search API returned no data");
      }
      if (rawJson == null) {
        return formatError(purl, toolType, "Search API returned no data");
      }

      return switch (toolType) {
        case COMPONENT_VERSION -> McpResponseFormatter.formatComponentVersion(purl, rawJson, policyResult);
        case LATEST_VERSION -> McpResponseFormatter.formatLatestVersion(purl, rawJson, policyResult);
        case RECOMMENDATIONS -> McpResponseFormatter.formatRecommendations(purl, rawJson);
      };
    }
    catch (Exception e) {
      log.warn("Error processing purl {}: {}", purl, e.getMessage(), e);
      return formatError(purl, toolType, "Component lookup failed");
    }
  }

  private static String formatError(String purl, ToolType toolType, String message) {
    try {
      if (toolType == ToolType.RECOMMENDATIONS) {
        return mapper.writeValueAsString(List.of(McpRecommendationItem.failure(purl, message)));
      }
      return mapper.writeValueAsString(List.of(McpBatchItem.failure(purl, message)));
    }
    catch (Exception e) {
      return McpBatchItem.FALLBACK_FAILURE_JSON;
    }
  }

  private static String mergeJsonArrays(List<String> jsonArrays) {
    List<Object> merged = new ArrayList<>();
    for (String json : jsonArrays) {
      try {
        List<Object> items = mapper.readValue(json, new TypeReference<>()
        {
        });
        merged.addAll(items);
      }
      catch (Exception e) {
        log.warn("Failed to parse batch element, substituting fallback: {}", e.getMessage());
        merged.add(Map.of("success", false, "error", "Formatting failed"));
      }
    }
    try {
      return mapper.writeValueAsString(merged);
    }
    catch (Exception e) {
      log.warn("Failed to serialize merged batch results: {}", e.getMessage());
      return McpBatchItem.FALLBACK_FAILURE_JSON;
    }
  }

  @VisibleForTesting
  enum ToolType
  {
    COMPONENT_VERSION,
    LATEST_VERSION,
    RECOMMENDATIONS
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

  private static String serializeOrNull(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return mapper.writeValueAsString(value);
    }
    catch (Exception e) {
      log.warn("Failed to serialize search response: {}", e.getMessage());
      return null;
    }
  }

  private static JsonSchema toolSchema() {
    return new JsonSchema(
        "object",
        Map.of(
            "packageUrls", Map.of("type", "array",
                "items", Map.of("type", "string"),
                "maxItems", MAX_BATCH_SIZE,
                "description",
                "Package URL (PURL) or list of PURLs identifying the component(s). Maven requires namespace "
                    + "(groupId). Version is required for getComponentVersion, optional for others. "
                    + "When providing multiple package URLs, limit to 20 maximum."),
            "applicationId", Map.of("type", "string",
                "description", "IQ application ID for policy evaluation (optional)"),
            "stage", Map.of("type", "string",
                "description", "Lifecycle stage to evaluate against (optional, defaults to 'release'). "
                    + "Controls which stage's policy actions apply, so different stages (e.g. 'build' vs "
                    + "'release') can yield different violations and actions. Case-insensitive; must be a "
                    + "recognized stage.")),
        List.of("packageUrls"),
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
   * requires json-schema-validator 1.5.9. These versions are binary-incompatible.
   *
   * <p>
   * <b>TODO: Remove this workaround</b> when cyclonedx-core-java 13.0.0 is released, which
   * upgrades to json-schema-validator 2.x.
   */
  private static JsonSchemaValidator noOpJsonSchemaValidator() {
    return (Map<String, Object> schema, Object structuredContent) -> JsonSchemaValidator.ValidationResponse.asValid(
        structuredContent != null ? structuredContent.toString() : null);
  }
}
