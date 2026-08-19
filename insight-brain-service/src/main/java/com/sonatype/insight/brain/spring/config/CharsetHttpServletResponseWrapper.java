/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * HttpServletResponseWrapper that adds UTF-8 charset to text-based Content-Type headers.
 * This restores the behavior from Dropwizard's AssetBundle which always included
 * charset=UTF-8 for text-based MIME types.
 */
class CharsetHttpServletResponseWrapper
    extends HttpServletResponseWrapper
{
  private static final String CHARSET_UTF8 = ";charset=UTF-8";

  CharsetHttpServletResponseWrapper(HttpServletResponse response) {
    super(response);
  }

  @Override
  public void setContentType(String type) {
    // Add charset to text-based content types that don't already have one
    if (type != null && shouldAddCharset(type) && !type.contains("charset")) {
      super.setContentType(type + CHARSET_UTF8);
    }
    else {
      super.setContentType(type);
    }
  }

  private boolean shouldAddCharset(String contentType) {
    String lowerContentType = contentType.toLowerCase();
    return lowerContentType.startsWith("text/") ||
        lowerContentType.startsWith("application/javascript") ||
        lowerContentType.startsWith("application/json");
  }
}
