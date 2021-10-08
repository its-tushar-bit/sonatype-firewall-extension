/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class QuarantinedComponentService
{
  private final QuarantinedComponentAccessManager quarantinedComponentAccessManager;

  @Inject
  public QuarantinedComponentService(
      final DbQuarantinedComponentAccessManager quarantinedComponentAccessManager)
  {
    this.quarantinedComponentAccessManager = quarantinedComponentAccessManager;
  }

  public QuarantinedComponentDto getQuarantinedComponent(final String token) {
    final QuarantinedComponentDto quarantinedComponentDto = new QuarantinedComponentDto();
    quarantinedComponentDto.repositoryComponentId =
        quarantinedComponentAccessManager.getRepositoryComponentIdFromToken(token);
    quarantinedComponentDto.success = true;
    return quarantinedComponentDto;
  }
}
