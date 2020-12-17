/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

public interface BaseUrl
{
  void capture(HttpServletRequest httpRequest);

  void release();

  /**
   * Returns the server base URL:
   * - if the base URL is not forced (in the server configuration), it tries to extract the base URL from the incoming
   * HTTP request (if any);
   * - otherwise, it returns the configured server base URL.
   *
   * @throws IllegalStateException if the base URL cannot be determined.
   */
  String get();

  /**
   * Returns the configured server base URL.
   *
   * @throws IllegalStateException if the base URL is not configured.
   */
  String getConfigured();

  UriBuilder redirect();
}
