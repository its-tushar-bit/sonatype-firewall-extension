/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.InsightWork;

/**
 * Update the security.json from using GAV to ComponentIdentifier. Note: This file is used for auditing and security
 * overrides.
 *
 * @since 1.13.0
 */
public class SecurityAuditGAVMigrator
    extends AbstractAuditGAVMigrator
{
  @Inject
  public SecurityAuditGAVMigrator(final InsightWork insightWork) {
    super(insightWork);
  }

  @Override
  protected String getAuditFileName() {
    return "security.json";
  }

  @Override
  protected String getMarkerFilename() {
    return "securityauditgav-migrated";
  }
}
