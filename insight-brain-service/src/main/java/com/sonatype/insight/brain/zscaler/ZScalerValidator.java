/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.zscaler;

import java.net.URI;

import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for validating URL format.
 */
public final class ZScalerValidator
{
  private static final Logger log = LoggerFactory.getLogger(ZScalerValidator.class);

  private static final String HTTP_PROTOCOL = "http";

  private static final String HTTPS_PROTOCOL = "https";

  private ZScalerValidator() {
  }

  /**
   * Validates URL format.
   *
   * @param hostName The base URL to validate
   * @throws BadRequestException if URL is invalid
   */
  public static void validateHostName(String hostName) throws BadRequestException {
    if (hostName == null || hostName.trim().isEmpty()) {
      throw new BadRequestException("Host name is required");
    }

    String trimmedHostName = hostName.trim();

    try {
      URI uri = URI.create(trimmedHostName);

      // Validate protocol
      String scheme = uri.getScheme();

      if (scheme == null) {
        throw new BadRequestException("Not a valid URL");
      }

      if (!HTTP_PROTOCOL.equalsIgnoreCase(scheme) && !HTTPS_PROTOCOL.equalsIgnoreCase(scheme)) {
        throw new BadRequestException("Protocol must be http or https");
      }

      // Validate hostname exists
      String host = uri.getHost();
      if (host == null || host.trim().isEmpty()) {
        throw new BadRequestException("Hostname is required in URL");
      }

      // Validate no trailing slash or paths
      String path = uri.getPath();

      if ((path != null && !path.isEmpty() && !"/".equals(path)) || trimmedHostName.endsWith("/")) {
        throw new BadRequestException("Only base URL allowed - no paths or trailing slashes");
      }

      // Validate no query parameters or fragments
      if (uri.getQuery() != null || uri.getFragment() != null) {
        throw new BadRequestException("Query parameters and fragments not allowed");
      }
    }
    catch (IllegalArgumentException e) {
      log.debug("Hostname validation failed for '{}': {}", hostName, e.getMessage());
      throw new BadRequestException("Not a valid URL");
    }
  }
}
