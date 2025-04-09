/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.utils;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
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
}
