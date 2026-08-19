/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Maps a pull-request creation failure to one of a closed-vocabulary set of tokens. The categorizer is the only
 * sanctioned path from a provider failure to anything written to {@code AuditData} — its contract is that the output
 * is exactly one of the public token constants and contains no substring of the input's message, response body, or
 * stack trace. Adding a new "smarter" branch that includes the original message would defeat the point.
 */
@Named
@Singleton
public class PullRequestFailureCategorizer
{
  public static final String AUTH_INVALID = "auth_invalid";

  public static final String PERMISSION_DENIED = "permission_denied";

  public static final String BRANCH_CONFLICT = "branch_conflict";

  public static final String REPO_NOT_FOUND = "repo_not_found";

  public static final String PROVIDER_RATE_LIMIT = "provider_rate_limit";

  public static final String PROVIDER_UNAVAILABLE = "provider_unavailable";

  public static final String UNKNOWN_PROVIDER_ERROR = "unknown_provider_error";

  /**
   * Categorize a thrown failure. Walks the cause chain so wrapped exceptions are matched on the underlying type. Null
   * input maps to the catch-all token. The output never includes input data.
   */
  public String categorize(final Throwable failure) {
    if (failure == null) {
      return UNKNOWN_PROVIDER_ERROR;
    }
    Throwable current = failure;
    int depth = 0;
    while (current != null && depth < 8) {
      String token = matchByClassName(current.getClass().getSimpleName());
      if (token != null) {
        return token;
      }
      current = current.getCause();
      depth++;
    }
    return UNKNOWN_PROVIDER_ERROR;
  }

  private String matchByClassName(final String className) {
    if (className == null) {
      return null;
    }
    if (className.contains("Authentication") || className.contains("Unauthorized")) {
      return AUTH_INVALID;
    }
    if (className.contains("Forbidden") || className.contains("Permission") || className.contains("AccessDenied")) {
      return PERMISSION_DENIED;
    }
    // "Conflict" intentionally excluded: too broad (e.g. MergeConflictException is a commit-level
    // conflict, not a branch-already-exists condition). Only exact branch-exists signals map here.
    if (className.contains("BranchExists") || className.contains("BranchConflict")) {
      return BRANCH_CONFLICT;
    }
    if (className.contains("RepositoryNotFound") || className.contains("NoSuchPullRequest")
        || className.contains("InvalidRepositoryUrl"))
    {
      return REPO_NOT_FOUND;
    }
    if (className.contains("RateLimit") || className.contains("TooManyRequests")) {
      return PROVIDER_RATE_LIMIT;
    }
    if (className.contains("Unavailable") || className.contains("ServiceUnavailable")
        || className.contains("ConnectException") || className.contains("SocketTimeout")
        || className.contains("UnknownHost"))
    {
      return PROVIDER_UNAVAILABLE;
    }
    return null;
  }
}
