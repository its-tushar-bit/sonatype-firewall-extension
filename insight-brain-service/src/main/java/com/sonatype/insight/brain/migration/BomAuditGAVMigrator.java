/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.service.InsightWork;

/**
 * Update BomAudit from GAV to ComponentIdentifier.
 *
 * @since 1.13.0
 */
@Named
public class BomAuditGAVMigrator
    extends AbstractAuditGAVMigrator
{
  @Inject
  public BomAuditGAVMigrator(final InsightWork insightWork) {
    super(insightWork);
  }

  @Override
  protected String getAuditFileName() {
    return "bom.json";
  }

  @Override
  protected String getMarkerFilename() {
    return "bomauditgav-migrated";
  }
}
