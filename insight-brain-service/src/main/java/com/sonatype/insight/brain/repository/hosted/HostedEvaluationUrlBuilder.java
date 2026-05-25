/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.BaseUrl;

/**
 * Builds the absolute URL returned to NXRM in a synchronous hosted-enforcement response.
 * <p>
 * Until the dedicated per-evaluation report page ships, this is an interim URL pointing at the
 * existing hosted-repository view in the Lifecycle UI. When the per-evaluation page is ready the
 * only change needed is in this class; neither NXRM nor the rest of the IQ codebase references
 * the URL format.
 */
@Named
@Singleton
public class HostedEvaluationUrlBuilder
{
  private static final String HOSTED_REPO_UI_PATH_FORMAT =
      "assets/index.html#/hostedRepos/%s/components?repositoryPublicId=%s";

  private final BaseUrl baseUrl;

  @Inject
  public HostedEvaluationUrlBuilder(final BaseUrl baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * Returns the interim evaluation URL for the given repository.
   *
   * @param repository the hosted repository (non-null); its id and publicId populate the URL
   * @return an absolute URL rooted at IQ's configured BaseUrl
   * @throws IllegalArgumentException if repository is null
   * @throws IllegalStateException if BaseUrl cannot be determined (bubbles up from {@link BaseUrl#get()})
   */
  public String build(final Repository repository) {
    if (repository == null) {
      throw new IllegalArgumentException("repository must not be null");
    }
    String base = baseUrl.get();
    // BaseUrl.get() returns with a trailing slash; the UI fragment path must not start with one.
    // publicId is URL-encoded as a query-parameter value: the repository.public_id column has
    // no charset constraint, so values containing spaces, '+', '&', '%' would otherwise produce
    // a malformed URL when stored in the audit record or sent to NXRM. Repository ID is
    // UUID-style and always URL-safe, so it is interpolated as-is into the fragment path.
    String encodedPublicId = URLEncoder.encode(repository.getPublicId(), StandardCharsets.UTF_8);
    return base + String.format(HOSTED_REPO_UI_PATH_FORMAT, repository.getId(), encodedPublicId);
  }
}
