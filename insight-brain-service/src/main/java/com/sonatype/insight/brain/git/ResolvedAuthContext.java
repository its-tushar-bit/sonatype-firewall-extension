/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType;

/**
 * Snapshot of the auth context the worker actually used when contacting the SCM provider. Carries identifiers only;
 * never carries tokens, keys, or any secret material. Used by audit emission and trace persistence in
 * {@code PullRequestTask} so the recorded auth matches what was used at execution time, not a queue-time snapshot.
 */
public final class ResolvedAuthContext
{
  private final AuthenticationType authenticationType;

  private final String authOwnerId;

  private final Integer githubAppId;

  private final Long installationId;

  private ResolvedAuthContext(
      final AuthenticationType authenticationType,
      final String authOwnerId,
      final Integer githubAppId,
      final Long installationId)
  {
    this.authenticationType = authenticationType;
    this.authOwnerId = authOwnerId;
    this.githubAppId = githubAppId;
    this.installationId = installationId;
  }

  public static ResolvedAuthContext forPat(final String authOwnerId) {
    return new ResolvedAuthContext(AuthenticationType.PAT, authOwnerId, null, null);
  }

  public static ResolvedAuthContext forGithubApp(
      final String authOwnerId,
      final Integer githubAppId,
      final Long installationId)
  {
    return new ResolvedAuthContext(AuthenticationType.GITHUB_APP, authOwnerId, githubAppId, installationId);
  }

  public AuthenticationType getAuthenticationType() {
    return authenticationType;
  }

  public String getAuthOwnerId() {
    return authOwnerId;
  }

  public Integer getGithubAppId() {
    return githubAppId;
  }

  public Long getInstallationId() {
    return installationId;
  }

  /**
   * The GitHub App id as a string, or null if absent. The string form is what the audit/persistence/log layers
   * write — wrapper provided here so call sites don't repeat the null-guard.
   */
  public String getGithubAppIdAsString() {
    return githubAppId == null ? null : String.valueOf(githubAppId);
  }

  /**
   * The installation id as a string, or null if absent. See {@link #getGithubAppIdAsString()} for rationale.
   */
  public String getInstallationIdAsString() {
    return installationId == null ? null : String.valueOf(installationId);
  }

  /**
   * The authentication-type enum's name, or null if absent. Same rationale.
   */
  public String getAuthenticationTypeName() {
    return authenticationType == null ? null : authenticationType.name();
  }
}
