/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.junit.Test;

import static com.sonatype.insight.brain.git.PullRequestFailureCategorizer.AUTH_INVALID;
import static com.sonatype.insight.brain.git.PullRequestFailureCategorizer.BRANCH_CONFLICT;
import static com.sonatype.insight.brain.git.PullRequestFailureCategorizer.PERMISSION_DENIED;
import static com.sonatype.insight.brain.git.PullRequestFailureCategorizer.PROVIDER_RATE_LIMIT;
import static com.sonatype.insight.brain.git.PullRequestFailureCategorizer.PROVIDER_UNAVAILABLE;
import static com.sonatype.insight.brain.git.PullRequestFailureCategorizer.REPO_NOT_FOUND;
import static com.sonatype.insight.brain.git.PullRequestFailureCategorizer.UNKNOWN_PROVIDER_ERROR;
import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestFailureCategorizerTest
{
  private final PullRequestFailureCategorizer categorizer = new PullRequestFailureCategorizer();

  @Test
  public void nullReturnsUnknown() {
    assertThat(categorizer.categorize(null)).isEqualTo(UNKNOWN_PROVIDER_ERROR);
  }

  @Test
  public void unrecognizedExceptionReturnsUnknown() {
    assertThat(categorizer.categorize(new IllegalStateException("something"))).isEqualTo(UNKNOWN_PROVIDER_ERROR);
  }

  @Test
  public void authenticationLikeNamesMapToAuthInvalid() {
    assertThat(categorizer.categorize(new AuthenticationFailedException())).isEqualTo(AUTH_INVALID);
    assertThat(categorizer.categorize(new UnauthorizedException())).isEqualTo(AUTH_INVALID);
  }

  @Test
  public void permissionLikeNamesMapToPermissionDenied() {
    assertThat(categorizer.categorize(new ForbiddenException())).isEqualTo(PERMISSION_DENIED);
    assertThat(categorizer.categorize(new AccessDeniedException())).isEqualTo(PERMISSION_DENIED);
  }

  @Test
  public void branchConflictMaps() {
    assertThat(categorizer.categorize(new BranchExistsException())).isEqualTo(BRANCH_CONFLICT);
    // ConflictException intentionally excluded: "Conflict" is too broad and matches JGit's
    // MergeConflictException (a commit-level conflict unrelated to branch-already-exists).
    assertThat(categorizer.categorize(new ConflictException())).isEqualTo(UNKNOWN_PROVIDER_ERROR);
  }

  @Test
  public void repoNotFoundMaps() {
    assertThat(categorizer.categorize(new RepositoryNotFoundException())).isEqualTo(REPO_NOT_FOUND);
    assertThat(categorizer.categorize(new InvalidRepositoryUrlException())).isEqualTo(REPO_NOT_FOUND);
  }

  @Test
  public void rateLimitMaps() {
    assertThat(categorizer.categorize(new RateLimitExceededException())).isEqualTo(PROVIDER_RATE_LIMIT);
    assertThat(categorizer.categorize(new TooManyRequestsException())).isEqualTo(PROVIDER_RATE_LIMIT);
  }

  @Test
  public void networkAndUnavailableMaps() {
    assertThat(categorizer.categorize(new ConnectException("refused"))).isEqualTo(PROVIDER_UNAVAILABLE);
    assertThat(categorizer.categorize(new SocketTimeoutException("timeout"))).isEqualTo(PROVIDER_UNAVAILABLE);
    assertThat(categorizer.categorize(new UnknownHostException("host"))).isEqualTo(PROVIDER_UNAVAILABLE);
    assertThat(categorizer.categorize(new ServiceUnavailableException())).isEqualTo(PROVIDER_UNAVAILABLE);
  }

  @Test
  public void wrappedCauseIsMatched() {
    Throwable wrapped = new RuntimeException("wrapper", new ConnectException("inner"));
    assertThat(categorizer.categorize(wrapped)).isEqualTo(PROVIDER_UNAVAILABLE);
  }

  @Test
  public void deeplyNestedCauseIsMatched() {
    Throwable inner = new AuthenticationFailedException();
    Throwable mid = new RuntimeException("mid", inner);
    Throwable outer = new IOException("outer", mid);
    assertThat(categorizer.categorize(outer)).isEqualTo(AUTH_INVALID);
  }

  @Test
  public void outputDoesNotLeakAnyCharFromInputMessage() {
    String secret = "ghp_PRIVATE_KEY_LIKE_TOKEN_AAAAAAAAAA";
    String result = categorizer.categorize(new RuntimeException(secret));
    assertThat(result).doesNotContain(secret);
    assertThat(result).doesNotContain("ghp_");
    assertThat(result).doesNotContain("PRIVATE_KEY");
  }

  @Test
  public void outputIsAlwaysFromClosedVocabulary() {
    String[] vocab = {AUTH_INVALID, PERMISSION_DENIED, BRANCH_CONFLICT, REPO_NOT_FOUND,
      PROVIDER_RATE_LIMIT, PROVIDER_UNAVAILABLE, UNKNOWN_PROVIDER_ERROR};
    Throwable[] inputs = {null, new RuntimeException(), new IllegalStateException(), new IllegalArgumentException(),
      new NullPointerException(), new IOException()};
    for (Throwable t : inputs) {
      assertThat(categorizer.categorize(t)).isIn((Object[]) vocab);
    }
  }

  // Synthetic exception types used to drive the class-name matchers without depending on provider library types.

  private static class AuthenticationFailedException
      extends RuntimeException
  {
  }

  private static class UnauthorizedException
      extends RuntimeException
  {
  }

  private static class ForbiddenException
      extends RuntimeException
  {
  }

  private static class AccessDeniedException
      extends RuntimeException
  {
  }

  private static class BranchExistsException
      extends RuntimeException
  {
  }

  private static class ConflictException
      extends RuntimeException
  {
  }

  private static class RepositoryNotFoundException
      extends RuntimeException
  {
  }

  private static class InvalidRepositoryUrlException
      extends RuntimeException
  {
  }

  private static class RateLimitExceededException
      extends RuntimeException
  {
  }

  private static class TooManyRequestsException
      extends RuntimeException
  {
  }

  private static class ServiceUnavailableException
      extends RuntimeException
  {
  }
}
