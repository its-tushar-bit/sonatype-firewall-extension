/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.service.InsightWork;

/**
 * Update LicenseOverrideAudit from GAV to ComponentIdentifier.
 *
 * @since 1.13.0
 */
@Named
public class LicenseOverrideAuditGAVMigrator
    extends AbstractAuditGAVMigrator
{
  @Inject
  public LicenseOverrideAuditGAVMigrator(InsightWork insightWork) {
    super(insightWork);
  }

  @Override
  protected String getAuditFileName() {
    return "licenses.json";
  }

  @Override
  protected String getMarkerFilename() {
    return "licenseoverrideauditgav-migrated";
  }
}
