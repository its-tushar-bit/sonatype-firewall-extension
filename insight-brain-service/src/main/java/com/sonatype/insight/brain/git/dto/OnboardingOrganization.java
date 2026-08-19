/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.dto;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.model.Organization;

/**
 * The response object used when querying for organizations. It combines the Organization object with
 * the source-control details so that the UI can retrieve details about the token and SCM config
 */
public class OnboardingOrganization
{
  public final Organization organization;

  public final ApiCompositeSourceControlDTO sourceControl;

  public OnboardingOrganization(
      final Organization organization,
      final ApiCompositeSourceControlDTO compositeSourceControl)
  {
    this.organization = organization;
    this.sourceControl = compositeSourceControl;
  }
}
