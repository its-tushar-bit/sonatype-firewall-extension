/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import com.sonatype.nexus.scm.github.GitHubAppManagementClient;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.InternalServerErrorException;
import com.sonatype.nexus.scm.github.dto.GitHubAppCredentials;
import com.sonatype.insight.error.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for interacting with GitHub's manifest-based App registration API.
 *
 */
@Named
@Singleton
public class GitHubManifestService
{
  private static final Logger log = LoggerFactory.getLogger(GitHubManifestService.class);

  private static final String GITHUB_API_BASE_URL = "https://api.github.com";

  private final ObjectMapper objectMapper;

  @Inject
  public GitHubManifestService(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Exchange the temporary manifest conversion code for GitHub App credentials.
   *
   * @param code the temporary code from GitHub's redirect after manifest submission
   * @return GitHub App credentials including app ID, slug, client_id, client_secret, and private key (pem)
   * @throws BadRequestException if the code is invalid or GitHub returns 4xx error
   * @throws InternalServerErrorException if network/IO failure or GitHub returns 5xx error
   */
  public GitHubAppCredentials convertManifestCode(
      final String code,
      final GitHubAppManagementClient client) throws IOException
  {
    if (code == null || code.trim().isEmpty()) {
      throw new BadRequestException("GitHub manifest code is required");
    }

    return client.createAppFromManifest(code);
  }

  /**
   * Extract error message from GitHub API error response.
   */
  private String extractErrorMessage(String responseBody) {
    try {
      // GitHub API errors typically have a "message" field
      if (responseBody != null && responseBody.contains("message")) {
        var errorNode = objectMapper.readTree(responseBody);
        if (errorNode.has("message")) {
          return errorNode.get("message").asText();
        }
      }
    }
    catch (Exception e) {
      log.debug("Could not parse GitHub API error response", e);
    }
    return "GitHub API error";
  }
}
