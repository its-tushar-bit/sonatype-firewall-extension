/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import com.sonatype.insight.brain.guide.api.error.GuideApiException;

import jakarta.ws.rs.core.Response;

/**
 * Shared request-parameter validation for Guide API resources. Failures throw
 * {@link GuideApiException} with a {@code BAD_REQUEST} status so they surface through
 * {@link com.sonatype.insight.brain.guide.api.error.GuideExceptionMapper} as the same
 * {@code {"success":false,"message":"..."}} envelope every other Guide error returns.
 */
final class GuideValidation
{
  private GuideValidation() {
  }

  static void requireNonBlankId(String id, String paramName) {
    if (id == null || id.isBlank()) {
      throw new GuideApiException(Response.Status.BAD_REQUEST, paramName + " is required");
    }
  }
}
