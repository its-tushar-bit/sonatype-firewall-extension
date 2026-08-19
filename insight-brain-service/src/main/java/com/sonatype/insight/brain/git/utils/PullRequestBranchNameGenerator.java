/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.utils;

import java.util.Optional;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.git.RemediationBranchNamePrefixGenerator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.nexus.git.utils.VersionRemediationTitleGenerator;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named
@Singleton
public class PullRequestBranchNameGenerator
{
  private final RemediationBranchNamePrefixGenerator remediationBranchNamePrefixGenerator;

  private final VersionRemediationTitleGenerator versionRemediationTitleGenerator;

  @Inject
  public PullRequestBranchNameGenerator(
      final RemediationBranchNamePrefixGenerator remediationBranchNamePrefixGenerator,
      final VersionRemediationTitleGenerator versionRemediationTitleGenerator)
  {
    this.remediationBranchNamePrefixGenerator = remediationBranchNamePrefixGenerator;
    this.versionRemediationTitleGenerator = versionRemediationTitleGenerator;
  }

  public String getBranchName(
      final Application application,
      final ComponentIdentifier componentIdentifier,
      final String nextVersion)
  {
    String branchPrefix = remediationBranchNamePrefixGenerator.generatePrefixForApplication(application.getId());
    return versionRemediationTitleGenerator.generateBranchNameForVersionRemediation(
        branchPrefix, componentIdentifier, nextVersion);
  }

  public String getBranchName(
      final ComponentIdentifier componentIdentifier,
      final Application application,
      final ApiVersionChangeOptionDTO suggestedVersion)
  {
    String version = Optional.ofNullable(suggestedVersion)
        .map(ApiVersionChangeOptionDTO::getData)
        .map(ApiComponentChangeActionDTO::getComponent)
        .map(component -> component.componentIdentifier)
        .map(ApiComponentIdentifierDTOV2::getCoordinates)
        .map(coordinates -> coordinates.get(ComponentIdentifier.VERSION))
        .orElseThrow(() -> new IllegalStateException(
            "Suggested remediation is missing version information."));

    return getBranchName(application, componentIdentifier, version);
  }
}
