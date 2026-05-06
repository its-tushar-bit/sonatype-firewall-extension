/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp.tools;

import com.sonatype.insight.brain.mcp.model.McpPolicyContext;
import com.sonatype.insight.brain.mcp.model.McpResponseMetadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McpResponseFormatter
{
  private static final Logger log = LoggerFactory.getLogger(McpResponseFormatter.class);

  private static final ObjectMapper mapper = new ObjectMapper();

  private McpResponseFormatter() {
    // utility class
  }

  /**
   * Formats a search-server JSON response, optionally enriching it with policy context and metadata.
   *
   * @param searchServerJson raw JSON from search-server
   * @param policy policy evaluation context, or null if no policy evaluation was requested
   * @return formatted JSON string with metadata and optional policy fields
   */
  public static String format(String searchServerJson, McpPolicyContext policy) {
    try {
      ObjectNode root = (ObjectNode) mapper.readTree(searchServerJson);
      if (policy != null) {
        root.set("policy", mapper.valueToTree(policy));
      }
      root.set("metadata", mapper.valueToTree(new McpResponseMetadata("search-server", policy)));
      return mapper.writeValueAsString(root);
    }
    catch (Exception e) {
      log.warn("Failed to format MCP response, returning raw search-server JSON: {}", e.getMessage());
      return searchServerJson;
    }
  }
}
