/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.mcp.policy;

import java.util.Locale;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.guide.mcp.McpServletProvider;

/**
 * Resolves and validates the {@code stage} parameter for MCP tool calls. Lives in the
 * {@code insight-brain-guide} module so {@link McpServletProvider}
 * can call it for up-front validation and {@code McpPolicyAnnotator} can call it for
 * policy evaluation — same parsing rules, single source of truth.
 *
 * <p>
 * Mirrors the case-insensitive validation used by {@code ApiPolicyWaiverService} and
 * {@code ApiPolicyViolationServiceV2}. Throws {@link IllegalArgumentException} on invalid
 * input; the MCP servlet turns that into the top-level error envelope.
 */
public final class McpStageResolver
{
  private McpStageResolver() {
  }

  public static Stage resolve(String stage) {
    if (stage == null || stage.isBlank()) {
      return new Stage(Stage.ID_RELEASE);
    }
    String normalized = stage.toLowerCase(Locale.ROOT);
    if (!Stage.isValidStageTypeId(normalized)) {
      throw new IllegalArgumentException("Invalid stage: " + stage);
    }
    return new Stage(normalized);
  }
}
