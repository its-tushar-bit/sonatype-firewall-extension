/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.28
 */
@Named
public class FirewallMigrationService
{
  static final String PROTOCOL_V1 = "v1";

  private final VersionService versionService;

  @Inject
  public FirewallMigrationService(final VersionService versionService) {
    this.versionService = versionService;
  }

  /**
   * Check whether the migration is supported for the specified protocol version.
   */
  void verifyMigrationSupport(String protocolVersion) {
    if (!PROTOCOL_V1.equals(protocolVersion)) {
      throw new BadRequestException(
          "IQ Server " + versionService.getVersion() + " does not support migration protocol " + protocolVersion +
              ", please update your IQ Server.");
    }
  }
}
