/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpBatchItem(
    String packageUrl,
    boolean success,
    McpVersionDetails data,
    String error)
{

  public static final String FALLBACK_FAILURE_JSON = "[{\"success\":false,\"error\":\"Formatting failed\"}]";

  public static McpBatchItem success(String packageUrl, McpVersionDetails data) {
    return new McpBatchItem(packageUrl, true, data, null);
  }

  public static McpBatchItem failure(String packageUrl, String error) {
    return new McpBatchItem(packageUrl, false, null, error);
  }
}
