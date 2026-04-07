/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp;

import java.util.Map;

import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.Servlet;

/**
 * Provides the MCP servlet and manages its lifecycle.
 * The transport and server are created lazily via {@link #initialize()} to avoid
 * allocating resources when the GUIDE_MCP feature flag is disabled.
 * McpToolProvider (GUIDE-1821) will replace the minimal server with a tool-wired one.
 */
@Named
@Singleton
public class McpServletProvider
{
  private HttpServletStatelessServerTransport transport;

  @Inject
  public McpServletProvider() {
    // No-op: transport is created lazily in initialize()
  }

  /**
   * Creates the MCP transport and starts a minimal server.
   * Must be called exactly once before {@link #getServlet()}.
   *
   * @throws IllegalStateException if called more than once
   */
  public void initialize() {
    if (transport != null) {
      throw new IllegalStateException("MCP transport already initialized");
    }
    this.transport = HttpServletStatelessServerTransport.builder()
        .messageEndpoint("/mcp")
        .build();

    McpServer.sync(transport)
        .serverInfo("iq-mcp", "1.0.0")
        .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
        .jsonSchemaValidator(noOpJsonSchemaValidator())
        .build();
  }

  /**
   * @throws IllegalStateException if {@link #initialize()} has not been called
   */
  public Servlet getServlet() {
    if (transport == null) {
      throw new IllegalStateException("initialize() must be called first");
    }
    return transport;
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
