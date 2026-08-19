/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter that adds charset=UTF-8 to Content-Type header for text-based static assets.
 * This restores the behavior from Dropwizard's AssetBundle which always included
 * charset=UTF-8 for text-based MIME types.
 *
 * <p>
 * This filter wraps the response and intercepts setContentType() calls to add
 * charset when the content type is text-based.
 */
public class StaticAssetsCharsetFilter
    implements Filter
{
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // No initialization needed
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    // Wrap the response to add charset to text-based content types
    CharsetHttpServletResponseWrapper wrappedResponse = new CharsetHttpServletResponseWrapper(
        (HttpServletResponse) response);

    chain.doFilter(request, wrappedResponse);
  }

  @Override
  public void destroy() {
    // No cleanup needed
  }
}
