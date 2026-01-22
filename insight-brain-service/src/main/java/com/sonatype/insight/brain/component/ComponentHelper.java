/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.git.PullRequestCommentingRemediationService;
import com.sonatype.insight.brain.git.RemediationVersionDTO;

/**
 * Helper class for component-related operations.
 */
@Named
@Singleton
public class ComponentHelper
{
  private PullRequestCommentingRemediationService remediationService;

  @Inject
  public ComponentHelper(PullRequestCommentingRemediationService remediationService) {
    this.remediationService = remediationService;
  }

  /**
   * Determines if a component version is a "golden" version.
   * A golden version is one that is recommended as non-breaking with dependencies.
   *
   * @param toVersion the target component identifier
   * @param appId the application ID
   * @return true if the version is a golden version, false otherwise
   */
  public boolean isGoldenVersion(ComponentIdentifier toVersion, String appId) {
    var optionalRemediationVersion = remediationService.getRemediationVersion(toVersion, appId);
    return optionalRemediationVersion
        .map(RemediationVersionDTO::getRemediationType)
        .map(ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES::equals)
        .orElse(false);
  }
}
